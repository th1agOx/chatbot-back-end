package br.com.chatbot.chatbot_api.dto.response;

import java.util.List;

public record ChatResponseV2(
        Long conversationId,
        MessageResponse userMessage,
        MessageResponse botMessage,
        String answer,
        List<SourceReference> sources,
        Long executionTimeMs,
        Integer chunksConsumed
) {}
