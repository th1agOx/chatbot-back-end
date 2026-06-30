package br.com.chatbot.chatbot_api.service.rag;

import br.com.chatbot.chatbot_api.dto.response.SourceReference;
import br.com.chatbot.chatbot_api.repository.ChunkSimilarityProjection;
import br.com.chatbot.chatbot_api.repository.DocumentChunkRepository;
import br.com.chatbot.chatbot_api.service.embedding.EmbeddingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RagServiceTest {

    @Mock
    private EmbeddingService embeddingService;

    @Mock
    private DocumentChunkRepository chunkRepository;

    @InjectMocks
    private RagService ragService;

    @Test
    void retrieveContext_ValidQuestion_ReturnsRagResult() {
        when(embeddingService.generateEmbedding("pergunta")).thenReturn(List.of(0.1f, 0.2f, 0.3f));

        var mockChunk = new ChunkSimilarityProjection() {
            @Override public UUID getId() { return UUID.randomUUID(); }
            @Override public Long getDocumentId() { return 1L; }
            @Override public String getDocumentName() { return "doc.txt"; }
            @Override public String getContent() { return "conteúdo do chunk"; }
            @Override public Integer getChunkIndex() { return 0; }
            @Override public java.time.LocalDateTime getCreatedAt() { return java.time.LocalDateTime.now(); }
            @Override public Double getSimilarity() { return 0.95; }
        };

        when(chunkRepository.findSimilarChunks(any(), anyInt(), anyDouble())).thenReturn(List.of(mockChunk));

        var result = ragService.retrieveContext("pergunta", 5, 0.75, 4000);
        assertNotNull(result);
        assertNotNull(result.context());
        assertFalse(result.context().isEmpty());
        assertEquals(1, result.sources().size());
        assertEquals("doc.txt", result.sources().get(0).fileName());
        assertTrue(result.executionTimeMs() >= 0);
        assertEquals(1, result.chunksConsumed());
    }

    @Test
    void retrieveContext_ExceedsMaxSize_TruncatesContext() {
        when(embeddingService.generateEmbedding("pergunta")).thenReturn(List.of(0.1f, 0.2f, 0.3f));

        var chunk1 = new ChunkSimilarityProjection() {
            @Override public UUID getId() { return UUID.randomUUID(); }
            @Override public Long getDocumentId() { return 1L; }
            @Override public String getDocumentName() { return "doc.txt"; }
            @Override public String getContent() { return "a".repeat(3000); }
            @Override public Integer getChunkIndex() { return 0; }
            @Override public java.time.LocalDateTime getCreatedAt() { return java.time.LocalDateTime.now(); }
            @Override public Double getSimilarity() { return 0.95; }
        };
        var chunk2 = new ChunkSimilarityProjection() {
            @Override public UUID getId() { return UUID.randomUUID(); }
            @Override public Long getDocumentId() { return 1L; }
            @Override public String getDocumentName() { return "doc.txt"; }
            @Override public String getContent() { return "a".repeat(3000); }
            @Override public Integer getChunkIndex() { return 1; }
            @Override public java.time.LocalDateTime getCreatedAt() { return java.time.LocalDateTime.now(); }
            @Override public Double getSimilarity() { return 0.90; }
        };

        when(chunkRepository.findSimilarChunks(any(), anyInt(), anyDouble())).thenReturn(List.of(chunk1, chunk2));

        var result = ragService.retrieveContext("pergunta", 5, 0.75, 4000);
        assertNotNull(result);
        assertTrue(result.context().length() <= 4000);
        assertEquals(1, result.chunksConsumed());
    }

    @Test
    void retrieveContext_InvalidQuestion_Throws() {
        assertThrows(IllegalArgumentException.class,
                () -> ragService.retrieveContext(null, 5, 0.75, 4000));
        assertThrows(IllegalArgumentException.class,
                () -> ragService.retrieveContext("", 5, 0.75, 4000));
        assertThrows(IllegalArgumentException.class,
                () -> ragService.retrieveContext("pergunta", -1, 0.75, 4000));
        assertThrows(IllegalArgumentException.class,
                () -> ragService.retrieveContext("pergunta", 5, -0.1, 4000));
        assertThrows(IllegalArgumentException.class,
                () -> ragService.retrieveContext("pergunta", 5, 0.75, 0));
    }
}
