package com.tennis.reservation;

import com.tennis.court.Court;
import com.tennis.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    List<Reservation> findByUserOrderByDateDescStartTimeDesc(User user);

    List<Reservation> findByDateAndCourt(LocalDate date, Court court);

    @Query("""
        SELECT r FROM Reservation r
        WHERE r.court = :court
          AND r.date = :date
          AND r.status <> 'CANCELLED'
          AND r.startTime < :endTime
          AND r.endTime > :startTime
    """)
    List<Reservation> findOverlapping(Court court, LocalDate date, LocalTime startTime, LocalTime endTime);

    @Query("SELECT COUNT(r) FROM Reservation r WHERE r.date = :date AND r.startTime = :startTime AND r.status <> 'CANCELLED'")
    long countByDateAndStartTime(LocalDate date, LocalTime startTime);
}
