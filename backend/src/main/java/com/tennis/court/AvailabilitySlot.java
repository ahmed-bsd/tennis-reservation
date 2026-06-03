package com.tennis.court;

import com.tennis.pricing.ProposalStatus;
import com.tennis.reservation.Reservation;
import com.tennis.reservation.ReservationStatus;
import com.tennis.pricing.DiscountProposal;
import lombok.Builder;
import lombok.Data;
import com.tennis.pricing.PricingCalculator;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Data @Builder
public class AvailabilitySlot {
    private Long courtId;
    private Integer courtNumber;
    private String courtName;
    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;
    private Double price;
    private boolean available;


    public static List<AvailabilitySlot> generateSlots(
            Court court,
            LocalDate date,
            List<Reservation> reservations,
            List<DiscountProposal> proposals
    ) {

        List<AvailabilitySlot> slots = new ArrayList<>();

        LocalTime cursor = LocalTime.of(8, 0);
        LocalTime closing = LocalTime.of(22, 0);

        while (cursor.isBefore(closing)) {

            LocalTime end = cursor.plusMinutes(30);
            final LocalTime slotStart = cursor;

            // =========================
            // RESERVATION CHECK
            // =========================
            boolean booked = reservations.stream()
                    .filter(r -> r.getStatus() != ReservationStatus.CANCELLED)
                    .anyMatch(r ->
                            r.getStartTime().isBefore(end) &&
                                    r.getEndTime().isAfter(slotStart)
                    );

            PricingCalculator calculator = new PricingCalculator(5, 10, LocalTime.of(19, 0));

            double finalPrice = calculator.calculateFinalPrice(slotStart, proposals);

            // =========================
            // BUILD SLOT
            // =========================
            slots.add(AvailabilitySlot.builder()
                    .courtId(court.getId())
                    .courtNumber(court.getNumber())
                    .courtName(court.getName())
                    .date(date)
                    .startTime(slotStart)
                    .endTime(end)
                    .available(!booked)

                    // 💰 FINAL PRICE
                    .price(finalPrice)

                    .build());

            cursor = end;
        }

        return slots;
    }
}
