package com.tennis.ai;

import com.tennis.court.CourtRepository;
import com.tennis.reservation.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AIService {

    private final WebClient.Builder webClientBuilder;
    private final CourtRepository courtRepository;
    private final ReservationRepository reservationRepository;

    @Value("${app.ollama.url}")   private String ollamaUrl;
    @Value("${app.ollama.model}") private String model;

    public String chat(String userMessage) {
        String context = buildContext();
        String systemPrompt = """
            You are a helpful assistant for a tennis club reservation system.
            You help members find available courts, understand pricing, and manage bookings.

            Current club context:
            %s

            Pricing: Morning slots (before 12:00) cost 10 jetons per 30 minutes.
                     Evening slots (12:00+) cost 20 jetons per 30 minutes.
            Opening hours: 08:00 to 22:00.
            Bookings allowed up to 2 days ahead only.

            Respond concisely and helpfully. If the user wants to book, provide the exact
            courtId, date, startTime, and durationMinutes they should use in the booking form.
            """.formatted(context);

        Map<String, Object> body = Map.of(
                "model", model,
                "stream", false,
                "messages", List.of(
                        Map.of("role", "system",  "content", systemPrompt),
                        Map.of("role", "user",    "content", userMessage)
                )
        );

        Map response = webClientBuilder.build()
                .post()
                .uri(ollamaUrl + "/api/chat")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        Map<String, Object> message = (Map<String, Object>) response.get("message");
        return (String) message.get("content");
    }

    private String buildContext() {
        int activeCourts = courtRepository.findByActiveTrue().size();
        long bookingsToday = reservationRepository.countByDateAndStartTime(
                LocalDate.now(), java.time.LocalTime.of(10, 0));
        return "Active courts: %d. Today's bookings at 10:00: %d.".formatted(activeCourts, bookingsToday);
    }
}
