package com.tennis.pricing;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tennis.court.Court;
import com.tennis.court.CourtRepository;
import com.tennis.notification.NotificationService;
import com.tennis.reservation.ReservationRepository;
import com.tennis.weather.WeatherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
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
    private final CourtRepository courtRepository;
    private final ReservationRepository reservationRepository;
    private final NotificationService notificationService;
    private final WeatherService weatherService;
    private final WebClient.Builder webClientBuilder;

    @Value("${app.ollama.url}")
    private String ollamaUrl;


    @Value("${app.groq.api-key}")
    private String groqApiKey;

    private final ObjectMapper mapper = new ObjectMapper();
    private double basePrice;
    @Transactional
    public void generateDiscountProposals() {

        LocalDate targetDate = LocalDate.now().plusDays(1);
        log.info("🚀 AI Pricing engine started for {}", targetDate);

        //String weather = weatherService.getCurrentWeather("Tunis");
        //log.info("weather:::",weather);
        Map<String, Map<String, Object>> weather =
                weatherService.getHourlyWeather(36.8, 10.2);

        String weatherForPrompt = weather.entrySet().stream()
                .map(e -> e.getKey() + " => " + e.getValue())
                .collect(Collectors.joining("\n"));
        log.info("weather : : "+weatherForPrompt);


        boolean isWeekend = isWeekend(targetDate);
        boolean isHoliday = false;

        List<Court> courts = courtRepository.findByActiveTrue();

        List<Map<String, Object>> slots = new ArrayList<>();

        for (Court court : courts) {
            for (int hour = 8; hour < 22; hour++) {

                LocalTime time = LocalTime.of(hour, 0);

                long bookings = reservationRepository.countByDateAndStartTime(targetDate, time);

                Map<String, Object> slot = new HashMap<>();
                slot.put("courtId", court.getId());
                slot.put("hour", hour);
                slot.put("bookings", bookings);

                slots.add(slot);
            }
        }

        String prompt = """
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

RULES:
- discountPercent must be between 10 and 30
- NO discount if hour >= 17
- ONLY output JSON, no explanation, no markdown

WEATHER-BASED PRICING RULES (VERY IMPORTANT):

You MUST adjust discountPercent based on weather severity:

1. RAIN / PRECIPITATION:
- 0–20% rain probability → no or minimal discount (0–10%)
- 20–50% → light discount (10–15%)
- 50–70% → medium discount (15–25%)
- 70–100% → high discount (25–30%)

2. WIND:
- 0–15 km/h → no impact
- 15–25 km/h → small discount increase (10–15%)
- 25–35 km/h → medium discount (15–25%)
- >35 km/h → high discount (25–30%)

3. TEMPERATURE:
- 20–27°C → no discount
- 27–32°C → light discount (10–15%)
- 32–37°C → medium discount (15–25%)
- >37°C → high discount (25–30%)

4. COMBINATION RULE:
- If multiple bad conditions occur (rain + wind + heat):
  ALWAYS increase discount toward maximum (up to 30%)

5. GOOD WEATHER RULE:
- If temperature 20–27°C AND wind <15 AND rain <20%:
  → no discount or very low discount (10% max only if low demand)

INPUT DATA:
%s

WEATHER (hourly):
%s

WEEKEND:
%s

HOLIDAY:
%s
""".formatted(slots, weatherForPrompt, isWeekend, isHoliday);

        // ✅ GROQ REQUEST (CORRECT FORMAT)
        Map<String, Object> request = new HashMap<>();
        request.put("model", "llama-3.3-70b-versatile");

        request.put("messages", List.of(
                Map.of(
                        "role", "system",
                        "content", "You are a strict JSON API. Output only JSON array."
                ),
                Map.of(
                        "role", "user",
                        "content", prompt
                )
        ));

        request.put("temperature", 0.2);
        request.put("max_tokens", 2000);

        Map response = webClientBuilder.build()
                .post()
                .uri("https://api.groq.com/openai/v1/chat/completions")
                .header("Authorization", "Bearer " + groqApiKey)
                .header("Content-Type", "application/json")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        if (response == null) {
            log.error("❌ Empty Groq response");
            return;
        }

        List<Map<String, Object>> choices =
                (List<Map<String, Object>>) response.get("choices");

        Map<String, Object> firstChoice = choices.get(0);

        Map<String, Object> message =
                (Map<String, Object>) firstChoice.get("message");

        String content = (String) message.get("content");

        log.info("AI RESPONSE => {}", content);

        // ✅ CLEAN JSON (important)
        content = content
                .replaceAll("```json", "")
                .replaceAll("```", "")
                .trim();

        ObjectMapper mapper = new ObjectMapper();

        List<Map<String, Object>> results;
        try {
            results = mapper.readValue(content, new TypeReference<>() {});
        } catch (Exception e) {
            log.error("❌ JSON parse error:\n{}", content);
            return;
        }

        for (Map<String, Object> r : results) {

            Long courtId = Long.valueOf(r.get("courtId").toString());
            int hour = Integer.parseInt(r.get("hour").toString());
            int discount = Integer.parseInt(r.get("discountPercent").toString());
            double basePrice = hour<=19 ? 5 : 10;

            if (hour >= 17) continue;

            Court court = courtRepository.findById(courtId).orElse(null);
            if (court == null) continue;

            DiscountProposal proposal = DiscountProposal.builder()
                    .court(court)
                    .date(targetDate)
                    .startTime(LocalTime.of(hour, 0))
                    .endTime(LocalTime.of(hour + 1, 0))
                    .originalPrice(basePrice)
                    .discountedPrice(basePrice - (basePrice * discount / 100))
                    .reason((String) r.getOrDefault("reason", "AI generated"))
                    .status(ProposalStatus.PENDING)
                    .createdAt(LocalDateTime.now())
                    .build();

            proposalRepository.save(proposal);
        }

        log.info("✅ Proposals generated successfully");
    }
    // =========================================================
    // UTIL
    // =========================================================
    private boolean isWeekend(LocalDate date) {
        DayOfWeek d = date.getDayOfWeek();
        return d == DayOfWeek.SATURDAY || d == DayOfWeek.SUNDAY;
    }

    // =========================================================
    // APPROVAL SYSTEM
    // =========================================================
    @Transactional
    public DiscountProposal approve(Long id) {

        DiscountProposal p = proposalRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Proposal not found"));

        p.setStatus(ProposalStatus.APPROVED);
        proposalRepository.save(p);

        notificationService.broadcastDiscountActivated(
                "Discount Court %d %s %s -> %.2f jetons"
                        .formatted(
                                p.getCourt().getNumber(),
                                p.getDate(),
                                p.getStartTime(),
                                p.getDiscountedPrice()
                        )
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