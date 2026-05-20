package com.tennis.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class SignupRequest {
    @NotBlank private String firstName;
    @NotBlank private String lastName;
    @Email @NotBlank private String email;
    @Size(min = 8) @NotBlank private String password;
    private LocalDate dateOfBirth;
    private String gender;
    private boolean member;
}
