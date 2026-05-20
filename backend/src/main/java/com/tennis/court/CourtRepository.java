package com.tennis.court;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CourtRepository extends JpaRepository<Court, Long> {
    List<Court> findByActiveTrue();
}
