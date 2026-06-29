package br.com.chatbot.chatbot_api.dto.response;

import java.time.LocalDateTime;

public record DocumentResponse(
        Long id,
        String fileName,
        String contentType,
        Long fileSize,
        Integer chunkCount,
        LocalDateTime uploadedAt
) {}
