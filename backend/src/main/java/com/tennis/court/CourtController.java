package com.tennis.court;

import com.tennis.pricing.DiscountProposal;
import com.tennis.pricing.DiscountProposalRepository;
import com.tennis.pricing.ProposalStatus;
import com.tennis.reservation.ReservationRepository;
import com.tennis.court.AvailabilitySlot;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/courts")
@RequiredArgsConstructor
public class CourtController {

    private final CourtRepository courtRepository;
    private final ReservationRepository reservationRepository;
    private final DiscountProposalRepository discountProposalRepository;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Court>> listActive() {
        return ResponseEntity.ok(courtRepository.findByActiveTrue());
    }

    @GetMapping("/availability")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<AvailabilitySlot>> getAvailability(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        List<Court> courts = courtRepository.findByActiveTrue();
        List<DiscountProposal> proposals =
                discountProposalRepository.findByStatusOrderByCreatedAtDesc(ProposalStatus.APPROVED);
        List<AvailabilitySlot> slots = courts.stream()
                .flatMap(court -> AvailabilitySlot.generateSlots(court, date,
                        reservationRepository.findByDateAndCourt(date, court),proposals).stream())
                .toList();

        return ResponseEntity.ok(slots);
    }
}
