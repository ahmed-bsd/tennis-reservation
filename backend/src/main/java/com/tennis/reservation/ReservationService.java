package com.tennis.reservation;

import com.tennis.court.Court;
import com.tennis.court.CourtRepository;
import com.tennis.jeton.JetonAccount;
import com.tennis.jeton.JetonRepository;
import com.tennis.notification.NotificationService;
import com.tennis.pricing.DiscountProposal;
import com.tennis.pricing.DiscountProposalRepository;
import com.tennis.pricing.PricingCalculator;
import com.tennis.pricing.ProposalStatus;
import com.tennis.user.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final CourtRepository courtRepository;
    private final JetonRepository jetonRepository;
    private final NotificationService notificationService;
    private final DiscountProposalRepository discountProposalRepository;

    @Value("${app.booking.max-days-ahead}") private int maxDaysAhead;
    @Value("${app.booking.opening-hour}")   private int openingHour;
    @Value("${app.booking.closing-hour}")   private int closingHour;
    @Value("${app.booking.price-morning}")  private int priceMorning;
    @Value("${app.booking.price-evening}")  private int priceEvening;

    @Transactional
    public Reservation create(User user, ReservationRequest request) {
        LocalDate today = LocalDate.now();
        if (request.getDate().isBefore(today) || request.getDate().isAfter(today.plusDays(maxDaysAhead))) {
            throw new IllegalArgumentException("Booking allowed only within J+2");
        }

        LocalTime start = request.getStartTime();
        LocalTime end = start.plusMinutes(request.getDurationMinutes());

        if (start.isBefore(LocalTime.of(openingHour, 0)) || end.isAfter(LocalTime.of(closingHour, 0))) {
            throw new IllegalArgumentException("Slot outside opening hours (08:00–22:00)");
        }

        Court court = courtRepository.findById(request.getCourtId())
                .orElseThrow(() -> new IllegalArgumentException("Court not found"));

        if (!reservationRepository.findOverlapping(court, request.getDate(), start, end).isEmpty()) {
            throw new IllegalStateException("Court already booked for this slot");
        }

        //recalculate the price before booking;
        PricingCalculator calculator = new PricingCalculator(5.0, 10.0, LocalTime.of(19, 0));
        List<DiscountProposal> proposals =
                discountProposalRepository.findByStatusOrderByCreatedAtDesc(ProposalStatus.APPROVED);

        double finalPrice = calculator.calculateFinalPrice(request.getStartTime(), proposals);
        log.info("final price"+ finalPrice);

/*
        JetonAccount account = jetonRepository.findByUser(user)
                .orElseThrow(() -> new IllegalStateException("Jeton account not found"));

        if (account.getBalance() < finalPrice) {
            throw new IllegalStateException("Insufficient jeton balance");
        }

        account.setBalance(account.getBalance() - finalPrice);
        jetonRepository.save(account);
*/
        Reservation reservation = Reservation.builder()
                .user(user)
                .court(court)
                .date(request.getDate())
                .startTime(start)
                .endTime(end)
                .durationMinutes(request.getDurationMinutes())
                .status(ReservationStatus.PENDING)
                .jetonCost(finalPrice)
                .build();

        reservation = reservationRepository.save(reservation);
        notificationService.sendBookingConfirmation(user, reservation);
        return reservation;
    }

    @Transactional
    public void cancel(User user, Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found"));

        if (!reservation.getUser().getId().equals(user.getId())) {
            throw new SecurityException("Not your reservation");
        }

        LocalDateTime bookingDateTime = reservation.getDate().atTime(reservation.getStartTime());
        boolean freeCancel = LocalDateTime.now().isBefore(bookingDateTime.minusHours(12));

        if (freeCancel) {
            JetonAccount account = jetonRepository.findByUser(user).orElseThrow();
            account.setBalance(account.getBalance() + reservation.getJetonCost());
            jetonRepository.save(account);
        }

        reservation.setStatus(ReservationStatus.CANCELLED);
        reservationRepository.save(reservation);
        notificationService.sendCancellationNotification(user, reservation, freeCancel);
    }

    public List<Reservation> getMyReservations(User user) {
        return reservationRepository.findByUserOrderByDateDescStartTimeDesc(user);
    }
    public List<Reservation> getAllReservations() {
        return reservationRepository.findAll();
    }

    @Transactional
    public Reservation confirm(Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found"));
        reservation.setStatus(ReservationStatus.CONFIRMED);
        return reservationRepository.save(reservation);
    }

    private int computeCost(LocalTime start, int durationMinutes) {
        int basePrice = start.isBefore(LocalTime.NOON) ? priceMorning : priceEvening;
        return basePrice * (durationMinutes / 30);
    }
}
