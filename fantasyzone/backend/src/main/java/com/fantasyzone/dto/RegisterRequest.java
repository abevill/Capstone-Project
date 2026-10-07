package com.fantasyzone.dto;
import jakarta.validation.constraints.*;
public record RegisterRequest(
    @NotBlank @Email String email,
    @NotBlank @Size(min = 3, max = 50) String displayName,
    @NotBlank @Size(min = 8, max = 100) String password) {}
