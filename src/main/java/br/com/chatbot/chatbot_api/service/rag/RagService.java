package br.com.chatbot.chatbot_api.service.rag;

import br.com.chatbot.chatbot_api.dto.response.SourceReference;
import br.com.chatbot.chatbot_api.entity.PGvector;
import br.com.chatbot.chatbot_api.repository.DocumentChunkRepository;
import br.com.chatbot.chatbot_api.service.embedding.EmbeddingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Service
@RequiredArgsConstructor
public class RagService {

    private final EmbeddingService embeddingService;
    private final DocumentChunkRepository chunkRepository;

    public RagResult retrieveContext(String question, int topK, double minSimilarity, int maxContextSize) {
        if (question == null || question.isBlank()) {
            throw new IllegalArgumentException("Pergunta não pode ser vazia");
        }
        if (topK <= 0) {
            throw new IllegalArgumentException("topK deve ser positivo");
        }
        if (minSimilarity < 0 || minSimilarity > 1) {
            throw new IllegalArgumentException("minSimilarity deve estar entre 0 e 1");
        }
        if (maxContextSize <= 0) {
            throw new IllegalArgumentException("maxContextSize deve ser positivo");
        }

        var startTime = System.currentTimeMillis();

        var queryVector = embeddingService.generateEmbedding(question);
        var pgVector = new PGvector(queryVector.stream().mapToDouble(Float::doubleValue).toArray());

        var similarChunks = chunkRepository.findSimilarChunks(pgVector, topK, minSimilarity);

        var contextBuilder = new StringBuilder();
        var sources = new ArrayList<SourceReference>();
        for (var chunk : similarChunks) {
            if (contextBuilder.length() + chunk.getContent().length() > maxContextSize) break;
            contextBuilder.append(chunk.getContent()).append("\n\n");
            sources.add(new SourceReference(
                    chunk.getDocumentId(),
                    chunk.getDocumentName(),
                    chunk.getContent().substring(0, Math.min(200, chunk.getContent().length()))
            ));
        }

        var elapsed = System.currentTimeMillis() - startTime;

        return new RagResult(
                contextBuilder.toString(),
                sources,
                elapsed,
                sources.size()
        );
    }
}
