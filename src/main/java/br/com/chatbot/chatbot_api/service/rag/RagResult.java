package br.com.chatbot.chatbot_api.service.rag;

import br.com.chatbot.chatbot_api.dto.response.SourceReference;

import java.util.List;

public record RagResult(
        String context,
        List<SourceReference> sources,
        long executionTimeMs,
        int chunksConsumed
) {}
