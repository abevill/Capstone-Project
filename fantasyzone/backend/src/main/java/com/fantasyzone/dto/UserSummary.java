package com.fantasyzone.dto;
import java.util.List;
public record UserSummary(Long id, String email, String displayName, boolean enabled, List<String> roles) {}
