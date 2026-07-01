package br.com.chatbot.chatbot_api.dto.response;

public record KnowledgeResponse(
    Long documentId,
    String source,
    Integer chunkCount,
    String status
) {}
