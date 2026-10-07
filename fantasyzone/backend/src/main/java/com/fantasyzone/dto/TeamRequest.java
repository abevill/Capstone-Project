package com.fantasyzone.dto;
import jakarta.validation.constraints.*;
public record TeamRequest(@NotBlank @Size(max = 60) String name) {}
