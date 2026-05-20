package com.tennis.reservation;

import com.tennis.user.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;

    @PostMapping
    @PreAuthorize("hasAnyRole('MEMBER','MANAGER','ADMIN')")
    public ResponseEntity<Reservation> create(@AuthenticationPrincipal User user,
                                               @Valid @RequestBody ReservationRequest request) {
        return ResponseEntity.ok(reservationService.create(user, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('MEMBER','MANAGER','ADMIN')")
    public ResponseEntity<Void> cancel(@AuthenticationPrincipal User user, @PathVariable Long id) {
        reservationService.cancel(user, id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Reservation>> myReservations(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(reservationService.getMyReservations(user));
    }
    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ResponseEntity<List<Reservation>> allReservations() {
        return ResponseEntity.ok(reservationService.getAllReservations());
    }

    @PutMapping("/{id}/confirm")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ResponseEntity<Reservation> confirm(@PathVariable Long id) {
        return ResponseEntity.ok(reservationService.confirm(id));
    }
}
