package com.tennis.notification;

import com.tennis.reservation.Reservation;
import com.tennis.user.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final SimpMessagingTemplate messagingTemplate;
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}") private String fromEmail;

    public void sendBookingConfirmation(User user, Reservation reservation) {
        pushToUser(user.getId(), Map.of(
                "type", "BOOKING_CONFIRMED",
                "courtNumber", reservation.getCourt().getNumber(),
                "date", reservation.getDate().toString(),
                "startTime", reservation.getStartTime().toString(),
                "jetonCost", reservation.getJetonCost()
        ));

        sendEmail(user.getEmail(),
                "Booking Confirmed — Court " + reservation.getCourt().getNumber(),
                "Your booking on %s at %s (Court %d) is confirmed. Cost: %.2f jetons."
                        .formatted(reservation.getDate(), reservation.getStartTime(),
                                reservation.getCourt().getNumber(), reservation.getJetonCost()));
    }

    public void sendCancellationNotification(User user, Reservation reservation, boolean refunded) {
        pushToUser(user.getId(), Map.of(
                "type", "BOOKING_CANCELLED",
                "refunded", refunded,
                "jetonCost", reservation.getJetonCost()
        ));

        String refundMsg = refunded ? " Your %.2f jetons have been refunded.".formatted(reservation.getJetonCost()) : "";
        sendEmail(user.getEmail(),
                "Booking Cancelled",
                "Your booking on %s at %s has been cancelled.%s"
                        .formatted(reservation.getDate(), reservation.getStartTime(), refundMsg));
    }

    public void broadcastDiscountActivated(String message) {
        messagingTemplate.convertAndSend("/topic/discounts", Map.of("message", message));
    }

    private void pushToUser(Long userId, Object payload) {
        messagingTemplate.convertAndSend("/topic/user/" + userId, payload);
    }

    private void sendEmail(String to, String subject, String text) {
        if (fromEmail == null || fromEmail.isBlank()) return;
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(fromEmail);
            msg.setTo(to);
            msg.setSubject(subject);
            msg.setText(text);
            mailSender.send(msg);
        } catch (Exception e) {
            log.warn("Failed to send email to {}: {}", to, e.getMessage());
        }
    }
}
