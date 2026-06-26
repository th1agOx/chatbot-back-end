package br.com.chatbot.chatbot_api.dto.response;

import br.com.chatbot.chatbot_api.enums.MessageRole;

import java.time.LocalDateTime;

public record MessageResponse(
        Long id,
        MessageRole role,
        String content,
        LocalDateTime createdAt
) {}
