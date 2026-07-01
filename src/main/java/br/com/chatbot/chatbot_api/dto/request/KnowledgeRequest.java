package br.com.chatbot.chatbot_api.dto.request;

import jakarta.validation.constraints.NotBlank;

public record KnowledgeRequest(
    @NotBlank String content,
    String source
) {}
