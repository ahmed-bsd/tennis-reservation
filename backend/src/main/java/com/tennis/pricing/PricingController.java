package com.tennis.pricing;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/pricing")
@RequiredArgsConstructor
public class PricingController {

    private final PricingService pricingService;

    @GetMapping("/proposals")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ResponseEntity<List<DiscountProposal>> getPending() {
        return ResponseEntity.ok(pricingService.getPendingProposals());
    }

    @GetMapping("/proposals/all")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ResponseEntity<List<DiscountProposal>> getAll() {
        return ResponseEntity.ok(pricingService.getAllProposals());
    }

    @PutMapping("/proposals/{id}/approve")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ResponseEntity<DiscountProposal> approve(@PathVariable Long id) {
        return ResponseEntity.ok(pricingService.approve(id));
    }

    @PutMapping("/proposals/{id}/reject")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ResponseEntity<DiscountProposal> reject(@PathVariable Long id) {
        return ResponseEntity.ok(pricingService.reject(id));
    }
}
