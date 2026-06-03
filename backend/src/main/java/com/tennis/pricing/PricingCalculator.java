package com.tennis.pricing;

import java.time.LocalTime;
import java.util.List;

public class PricingCalculator {

    private final double morningPrice;
    private final double eveningPrice;
    private final LocalTime eveningStart;

    public PricingCalculator(double morningPrice, double eveningPrice, LocalTime eveningStart) {
        this.morningPrice = morningPrice;
        this.eveningPrice = eveningPrice;
        this.eveningStart = eveningStart;
    }

    public double calculateBasePrice(LocalTime slotStart) {
        return slotStart.isBefore(eveningStart)
                ? morningPrice
                : eveningPrice;
    }

    public DiscountProposal findDiscount(LocalTime slotStart, List<DiscountProposal> proposals) {
        return proposals.stream()
                .filter(p ->
                        p.getStatus() == ProposalStatus.APPROVED &&
                                !slotStart.isBefore(p.getStartTime()) &&
                                slotStart.isBefore(p.getEndTime())
                )
                .findFirst()
                .orElse(null);
    }

    public double calculateFinalPrice(LocalTime slotStart, List<DiscountProposal> proposals) {
        DiscountProposal discount = findDiscount(slotStart, proposals);

        if (discount != null) {
            return discount.getDiscountedPrice();
        }

        return calculateBasePrice(slotStart);
    }
}