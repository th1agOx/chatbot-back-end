package br.com.chatbot.chatbot_api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ChatRequest(
        @NotNull Long conversationId,
        @NotBlank String message
) {}
