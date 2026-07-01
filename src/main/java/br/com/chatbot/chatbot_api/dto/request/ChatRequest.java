package br.com.chatbot.chatbot_api.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ChatRequest(
        Long conversationId,
        @NotBlank String message
) {}
