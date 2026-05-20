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

    @Value("${app.ollama.model}")
    private String model;

    private final ObjectMapper mapper = new ObjectMapper();

    // =========================================================
    // SCHEDULER
    // =========================================================
    @Scheduled(cron = "0 * * * * *")
    public void generateDiscountProposals() {

        LocalDate targetDate = LocalDate.now().plusDays(1);

        log.info("🚀 AI Pricing engine started for {}", targetDate);

        // =========================================================
        // 1. CONTEXT (WEATHER + CALENDAR)
        // =========================================================
        String weather = weatherService.getCurrentWeather("Tunis");
        boolean isWeekend = isWeekend(targetDate);
        boolean isHoliday = false; // TODO: Tunisia holiday API

        log.info("🌤 Weather: {}", weather);

        List<Court> courts = courtRepository.findByActiveTrue();

        // =========================================================
        // 2. BUILD STRUCTURED DATA
        // =========================================================
        List<Map<String, Object>> slots = new ArrayList<>();

        for (Court court : courts) {
            for (int hour = 8; hour < 22; hour++) {

                LocalTime time = LocalTime.of(hour, 0);

                long bookings = reservationRepository.countByDateAndStartTime(targetDate, time);

                double occupancy = Math.min(1.0, bookings / 5.0); // simple normalization

                Map<String, Object> slot = new HashMap<>();
                slot.put("courtId", court.getId());
                slot.put("courtNumber", court.getNumber());
                slot.put("hour", hour);
                slot.put("bookings", bookings);
                slot.put("occupancy", occupancy);

                slots.add(slot);
            }
        }

        // =========================================================
        // 3. PROMPT LLaMA 3
        // =========================================================
        String prompt =
                """
                You are a STRICT JSON GENERATION ENGINE.
                
                CRITICAL RULES (NON-NEGOTIABLE):
                - You MUST output ONLY valid JSON
                - You MUST NOT write explanations
                - You MUST NOT include markdown (no ``` or text)
                - You MUST NOT include any extra keys outside schema
                - You MUST NOT wrap response in objects like {response: ...}
                - Output must start with [ and end with ]
                
                SCHEMA REQUIRED:
                [
                  {
                    "courtId": number,
                    "hour": number,
                    "discountPercent": number,
                    "reason": string
                  }
                ]
                
                VALIDATION RULES:
                - discountPercent must be between 10 and 30
                - DO NOT generate discounts for hours >= 17
                - Only include realistic demand-based suggestions
                
                INPUT DATA:
                %s
                
                WEATHER:
                %s
                
                CONTEXT:
                date=%s
                weekend=%s
                holiday=%s
                
                FINAL INSTRUCTION:
                Return ONLY the JSON array now.
                """.formatted(slots, weather, targetDate, isWeekend, isHoliday);
        // =========================================================
        // 4. CALL OLLAMA (LLaMA 3)
        // =========================================================
        Map<String, Object> request = Map.of(
                "model", model,
                "stream", false,
                "format", "json",
                "messages", List.of(
                        Map.of(
                                "role", "system",
                                "content", "Return ONLY a JSON ARRAY, not an object or a text."
                        ),
                        Map.of(
                                "role", "user",
                                "content", prompt
                        )
                )
        );

        Map response = webClientBuilder.build()
                .post()
                .uri(ollamaUrl + "/api/chat")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        if (response == null || response.get("message") == null) {
            log.error("❌ Empty response from LLM");
            return;
        }

        String content = (String) ((Map<String, Object>) response.get("message")).get("content");

        log.info("🤖 LLaMA OUTPUT:\n{}", content);

        // =========================================================
        // 5. PARSE JSON SAFE
        // =========================================================
        List<Map<String, Object>> results;

        try {
            results = mapper.readValue(content, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            log.error("❌ JSON parsing failed. Raw output:\n{}", content);
            return;
        }

        // =========================================================
        // 6. APPLY RULE ENGINE + SAVE
        // =========================================================
        for (Map<String, Object> r : results) {

            try {
                int courtId = (Integer) r.get("courtId");
                int hour = (Integer) r.get("hour");
                int discount = (Integer) r.get("discountPercent");

                // 🚫 RULE ENGINE (HARD SAFETY)
                if (hour >= 17 && hour <= 21) continue;

                discount = Math.max(10, Math.min(30, discount));

                LocalTime start = LocalTime.of(hour, 0);

                int basePrice = start.isBefore(LocalTime.NOON) ? 10 : 20;
                int finalPrice = basePrice - (basePrice * discount / 100);

                Court court = courtRepository.findById((long) courtId).orElse(null);
                if (court == null) continue;

                DiscountProposal proposal = DiscountProposal.builder()
                        .court(court)
                        .date(targetDate)
                        .startTime(start)
                        .endTime(start.plusHours(1))
                        .originalPrice(basePrice)
                        .discountedPrice(finalPrice)
                        .reason((String) r.getOrDefault("reason", "AI generated"))
                        .status(ProposalStatus.PENDING)
                        .createdAt(LocalDateTime.now())
                        .build();

                proposalRepository.save(proposal);

            } catch (Exception ex) {
                log.warn("⚠ Skipping invalid proposal: {}", r);
            }
        }

        log.info("✅ Discount generation completed");
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
                "Discount Court %d %s %s -> %d jetons"
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