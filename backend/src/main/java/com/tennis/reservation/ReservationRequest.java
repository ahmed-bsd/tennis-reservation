package com.tennis.reservation;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class ReservationRequest {
    @NotNull private Long courtId;
    @NotNull private LocalDate date;
    @NotNull private LocalTime startTime;
    @NotNull private Integer durationMinutes;


}
