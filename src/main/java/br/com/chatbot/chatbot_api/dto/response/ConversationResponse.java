package br.com.chatbot.chatbot_api.dto.response;

import java.time.LocalDateTime;

public record ConversationResponse(
        Long id,
        String title,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
