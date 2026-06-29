package br.com.chatbot.chatbot_api.dto.response;

public record SourceReference(
        Long documentId,
        String fileName,
        String excerpt
) {}
