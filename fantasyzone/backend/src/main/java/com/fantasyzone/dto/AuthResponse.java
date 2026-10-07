package com.fantasyzone.dto;
import java.util.List;
public record AuthResponse(Long userId, String token, String email, String displayName, List<String> roles) {}
