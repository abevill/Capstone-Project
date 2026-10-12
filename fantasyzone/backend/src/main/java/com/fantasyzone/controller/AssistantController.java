package com.fantasyzone.controller;

import com.fantasyzone.dto.DTO;
import com.fantasyzone.service.AssistantService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/** AI fantasy assistant. Requires a logged-in user (the existing security rules protect every /api path that is not public). */
@RestController
@RequestMapping("/api/assistant")
public class AssistantController {
    private final AssistantService assistant;

    public AssistantController(AssistantService assistant) { this.assistant = assistant; }

    @PostMapping("/ask")
    public DTO.AssistantResponse ask(@Valid @RequestBody DTO.AssistantRequest request, Authentication auth) {
        return assistant.ask(request, auth.getName());
    }
}
