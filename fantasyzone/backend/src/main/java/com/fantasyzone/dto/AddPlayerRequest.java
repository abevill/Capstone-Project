package com.fantasyzone.dto;
import jakarta.validation.constraints.*;
public record AddPlayerRequest(@NotBlank @Size(max = 10) String positionSlot) {}
