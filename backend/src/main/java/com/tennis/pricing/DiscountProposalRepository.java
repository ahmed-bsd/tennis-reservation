package com.tennis.pricing;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DiscountProposalRepository extends JpaRepository<DiscountProposal, Long> {
    List<DiscountProposal> findByStatusOrderByCreatedAtDesc(ProposalStatus status);
    List<DiscountProposal> findAllByOrderByCreatedAtDesc();
}
