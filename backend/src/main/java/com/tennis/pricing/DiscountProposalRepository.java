package com.tennis.pricing;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface DiscountProposalRepository extends JpaRepository<DiscountProposal, Long> {
    List<DiscountProposal> findByStatusOrderByCreatedAtDesc(ProposalStatus status);
    List<DiscountProposal> findAllByOrderByCreatedAtDesc();
    Optional<DiscountProposal> findByDateAndStartTime(LocalDate date, LocalTime startTime);

}
