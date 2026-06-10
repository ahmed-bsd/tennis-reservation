package com.tennis.pricing;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tennis.notification.NotificationService;
import com.tennis.reservation.ReservationRepository;
import com.tennis.weather.WeatherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class PricingService {

    private final DiscountProposalRepository proposalRepository;
    private final ReservationRepository reservationRepository;
    private final NotificationService notificationService;
    private final WeatherService weatherService;
    private final WebClient.Builder webClientBuilder;

    @Value("${app.groq.api-key}")
    private String groqApiKey;

    private final ObjectMapper mapper = new ObjectMapper();

    // =========================================================
    // SLOT-BASED GENERATION (UPSERT)
    // =========================================================
    @Transactional
    public void generateDiscountProposals() {

        LocalDate targetDate = LocalDate.now().plusDays(1);
        log.info("🚀 Slot-based pricing engine started for {}", targetDate);

        // =========================================================
        // WEATHER (SAFE FALLBACK)
        // =========================================================
        Map<String, Map<String, Object>> weather = new HashMap<>();

        try {
            weather = weatherService.getHourlyWeather(36.8, 10.2);
            log.info("🌦 Weather loaded successfully");
        } catch (Exception e) {
            log.warn("⚠️ Weather API failed → fallback mode (no weather)");
        }

        String weatherForPrompt = weather.entrySet().stream()
                .map(e -> e.getKey() + " => " + e.getValue())
                .collect(Collectors.joining("\n"));
        log.info(" weather {}",weather);
        log.info("weather prompt {}",weatherForPrompt);
        boolean isWeekend = isWeekend(targetDate);
        boolean isHoliday = false;

        // =========================================================
        // SLOT GENERATION (NO COURT LOOP)
        // =========================================================
        List<Map<String, Object>> slots = new ArrayList<>();

        for (int hour = 8; hour < 22; hour++) {

            LocalTime time = LocalTime.of(hour, 0);

            long bookings = reservationRepository.countByDateAndStartTime(targetDate, time);

            Map<String, Object> slot = new HashMap<>();
            slot.put("hour", hour);
            slot.put("bookings", bookings);

            slots.add(slot);
        }
        log.info("slots {}", slots);

        // =========================================================
        // PROMPT
        // =========================================================
        String prompt = String.format("""
    You are a strict JSON generator for dynamic tennis court pricing.
    
    Return ONLY a valid JSON array.
    
    FORMAT:
    [
      {
        "courtId": number,
        "hour": number,
        "discountPercent": number,
        "reason": string
      }
    ]
    REASON FORMAT (MANDATORY):
    - The "reason" MUST include ONLY the weather factors that CAUSED the discount
    - DO NOT include normal or irrelevant conditions
    - Use this STRICT format:
        "Temp: XX°C | Wind: XX km/h | Rain: XX%% → explanation"
    - The explanation MUST justify the discount level (low, medium, high)
    - If multiple bad conditions exist, mention ALL of them
    - DO NOT return generic reasons

    Examples:
    - "Temp: 35°C | Wind: 28 km/h | Rain: 10%% → High heat and moderate wind reduce play comfort"
    - "Temp: 25°C | Wind: 10 km/h | Rain: 5%% → Good conditions, low discount due to low demand"
    - "Temp: 38°C | Wind: 40 km/h | Rain: 80%% → Extreme conditions, maximum discount applied"
    
    RULES:
    - discountPercent must be between 10 and 30
    - NO discount if hour >= 17
    - NO discount if bookings > 2
    - ONLY output JSON, no explanation, no markdown
    
    WEATHER-BASED PRICING RULES (VERY IMPORTANT):
    
    You MUST adjust discountPercent based on weather severity:
    
    1. RAIN / PRECIPITATION:
    - 0–50%% → no discount
    - 50–70%% → medium discount (15–25%%)
    - 70–100%% → high discount (25–30%%)
    
    2. WIND:
    - 0–15 km/h → no impact
    - 15–25 km/h → small discount increase (10–15%%)
    - 25–35 km/h → medium discount (15–25%%)
    - >35 km/h → high discount (25–30%%)
    
    3. TEMPERATURE:
    - 20–27°C → no discount
    - 27–32°C → light discount (10–15%%)
    - 32–37°C → medium discount (15–25%%)
    - >37°C → high discount (25–30%%)
    
    4. COMBINATION RULE:
    - If multiple bad conditions occur (rain + wind + heat):
      ALWAYS increase discount toward maximum (up to 30%%)
    
    5. GOOD WEATHER RULE:
    - If temperature 20–27°C AND wind <15 AND rain <20%%:
      → no discount or very low discount (10%% max only if low demand)
    
    INPUT DATA:
    %s
    
    WEATHER (hourly):
    %s
    
    WEEKEND:
    %s
    
    HOLIDAY:
    %s
    """, slots, weather, isWeekend, isHoliday);


        // AI CALL
        Map<String, Object> request = new HashMap<>();
        request.put("model", "llama-3.3-70b-versatile");

        request.put("messages", List.of(
                Map.of("role", "system", "content", "Return only JSON array."),
                Map.of("role", "user", "content", prompt)
        ));

        Map response = webClientBuilder.build()
                .post()
                .uri("https://api.groq.com/openai/v1/chat/completions")
                .header("Authorization", "Bearer " + groqApiKey)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        if (response == null) {
            log.error("❌ Groq response null");
            return;
        }

        List<Map<String, Object>> choices =
                (List<Map<String, Object>>) response.get("choices");

        String content =
                (String) ((Map<String, Object>) choices.get(0).get("message")).get("content");

        content = content.replace("```json", "").replace("```", "").trim();

        List<Map<String, Object>> results;

        try {
            results = mapper.readValue(content, new TypeReference<>() {});
        } catch (Exception e) {
            log.error("❌ JSON parse error:\n{}", content);
            return;
        }


        // UPSERT LOGIC (IMPORTANT PART)
        for (Map<String, Object> r : results) {

            int hour = Integer.parseInt(r.get("hour").toString());
            double discount = Double.parseDouble(r.get("discountPercent").toString());

            if (hour >= 17) continue;

            LocalTime start = LocalTime.of(hour, 0);
            double basePrice = (hour <= 19 ? 5 : 10);

            // No duplication
            Optional<DiscountProposal> existingOpt =
                    proposalRepository.findByDateAndStartTime(targetDate, LocalTime.of(hour, 0));

            if (existingOpt.isPresent()) {

                DiscountProposal existing = existingOpt.get();
                if (existing.getStatus() == ProposalStatus.APPROVED ||
                        existing.getStatus() == ProposalStatus.REJECTED) {
                    continue;
                }

                //  UPDATE IF PENDING
                existing.setOriginalPrice(basePrice);
                existing.setDiscountedPrice(basePrice - (basePrice * discount / 100));
                existing.setReason((String) r.getOrDefault("reason", "AI slot update"));
                existing.setCreatedAt(LocalDateTime.now());

                proposalRepository.save(existing);

            } else {

                //  CREATE IF DOESNT EXIST
                DiscountProposal proposal = DiscountProposal.builder()
                        .date(targetDate)
                        .startTime(LocalTime.of(hour, 0))
                        .endTime(LocalTime.of(hour + 1, 0))
                        .originalPrice(basePrice)
                        .discountedPrice(basePrice - (basePrice * discount / 100))
                        .reason((String) r.getOrDefault("reason", "AI slot pricing"))
                        .status(ProposalStatus.PENDING)
                        .createdAt(LocalDateTime.now())
                        .build();

                proposalRepository.save(proposal);
            }
        }

        log.info("✅ Slot-based proposals upsert completed");
    }

    // =========================================================
    private boolean isWeekend(LocalDate date) {
        DayOfWeek d = date.getDayOfWeek();
        return d == DayOfWeek.SATURDAY || d == DayOfWeek.SUNDAY;
    }

    // APPROVAL
    @Transactional
    public DiscountProposal approve(Long id) {

        DiscountProposal p = proposalRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Proposal not found"));

        p.setStatus(ProposalStatus.APPROVED);
        proposalRepository.save(p);

        notificationService.broadcastDiscountActivated(
                "Slot %s → new price %.2f jetons"
                        .formatted(p.getStartTime(), p.getDiscountedPrice())
        );

        return p;
    }

    @Transactional
    public DiscountProposal reject(Long id) {

        DiscountProposal p = proposalRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Proposal not found"));

        p.setStatus(ProposalStatus.REJECTED);
        return proposalRepository.save(p);
    }

    public List<DiscountProposal> getPendingProposals() {
        return proposalRepository.findByStatusOrderByCreatedAtDesc(ProposalStatus.PENDING);
    }

    public List<DiscountProposal> getAllProposals() {
        return proposalRepository.findAllByOrderByCreatedAtDesc();
    }
}