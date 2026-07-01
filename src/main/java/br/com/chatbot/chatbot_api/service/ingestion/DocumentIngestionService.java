package br.com.chatbot.chatbot_api.service.ingestion;

import br.com.chatbot.chatbot_api.entity.Document;
import br.com.chatbot.chatbot_api.entity.DocumentChunk;
import br.com.chatbot.chatbot_api.entity.DocumentStatus;
import br.com.chatbot.chatbot_api.entity.PGvector;
import br.com.chatbot.chatbot_api.exception.DocumentProcessingException;
import br.com.chatbot.chatbot_api.repository.DocumentRepository;
import br.com.chatbot.chatbot_api.service.chunking.TextChunker;
import br.com.chatbot.chatbot_api.service.embedding.EmbeddingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentIngestionService {

    private static final int EXPECTED_EMBEDDING_DIMENSION = 768;

    private final DocumentRepository documentRepository;
    private final TextChunker textChunker;
    private final EmbeddingService embeddingService;

    @Transactional
    public Document ingestContent(String content, String sourceName, String contentType) {
        log.info("DocumentIngestionService: ingerindo '{}' ({} caracteres)", sourceName, content.length());

        var chunks = textChunker.chunk(content);
        log.info("DocumentIngestionService: '{}' -> {} chunks gerados", sourceName, chunks.size());

        var document = Document.builder()
                .fileName(sourceName)
                .contentType(contentType)
                .fileSize((long) content.length())
                .status(DocumentStatus.PROCESSING)
                .build();

        var documentChunks = buildChunks(document, chunks);
        document.setChunks(documentChunks);
        document.setStatus(DocumentStatus.COMPLETED);

        var saved = documentRepository.save(document);

        log.info("DocumentIngestionService: '{}' persistido com {} chunks ({} dimensoes)",
                sourceName, documentChunks.size(), EXPECTED_EMBEDDING_DIMENSION);

        return saved;
    }

    private List<DocumentChunk> buildChunks(Document document, List<String> chunks) {
        var documentChunks = new ArrayList<DocumentChunk>();
        for (int i = 0; i < chunks.size(); i++) {
            var vector = embeddingService.generateEmbedding(chunks.get(i));
            if (vector.size() != EXPECTED_EMBEDDING_DIMENSION) {
                throw new DocumentProcessingException(
                        "Dimensão do embedding inesperada: " + vector.size()
                                + " (esperado: " + EXPECTED_EMBEDDING_DIMENSION + ")");
            }
            var pgVector = new PGvector(vector.stream().mapToDouble(Float::doubleValue).toArray());
            var chunk = DocumentChunk.builder()
                    .document(document)
                    .content(chunks.get(i))
                    .embedding(pgVector)
                    .chunkIndex(i)
                    .build();
            documentChunks.add(chunk);
        }
        return documentChunks;
    }
}
