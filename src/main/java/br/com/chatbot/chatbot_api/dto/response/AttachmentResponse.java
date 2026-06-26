package br.com.chatbot.chatbot_api.dto.response;

import java.time.LocalDateTime;

public record AttachmentResponse(
        Long id,
        String fileName,
        String contentType,
        Long size,
        LocalDateTime uploadDate
) {}
