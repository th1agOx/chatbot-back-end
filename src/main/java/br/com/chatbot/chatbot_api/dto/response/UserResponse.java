package br.com.chatbot.chatbot_api.dto.response;

import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        String email,
        String displayName,
        LocalDateTime createdAt
) {}
