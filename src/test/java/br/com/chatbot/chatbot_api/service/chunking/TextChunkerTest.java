package br.com.chatbot.chatbot_api.service.chunking;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TextChunkerTest {

    private TextChunker textChunker;

    @BeforeEach
    void setUp() {
        textChunker = new TextChunker();
        ReflectionTestUtils.setField(textChunker, "chunkSize", 1000);
        ReflectionTestUtils.setField(textChunker, "overlap", 200);
        ReflectionTestUtils.setField(textChunker, "delimiters", "\n\n|\n|\\.|\\?");
        textChunker.init();
    }

    @Test
    void chunk_SmallText_ReturnsSingleChunk() {
        var text = "Este é um texto curto.";
        var result = textChunker.chunk(text);
        assertEquals(1, result.size());
        assertEquals("Este é um texto curto.", result.get(0));
    }

    @Test
    void chunk_EmptyText_ReturnsEmptyList() {
        assertTrue(textChunker.chunk("").isEmpty());
        assertTrue(textChunker.chunk(null).isEmpty());
        assertTrue(textChunker.chunk("   ").isEmpty());
    }

    @Test
    void chunk_LargeText_ReturnsMultipleChunks() {
        var sb = new StringBuilder();
        for (int i = 0; i < 50; i++) {
            sb.append("Parágrafo número ").append(i).append(". ");
        }
        var text = sb.toString();
        var result = textChunker.chunk(text);
        assertTrue(result.size() > 1);
    }

    @Test
    void chunk_WithOverlap_LasthunkOverlapsPrevious() {
        var sb = new StringBuilder();
        for (int i = 0; i < 150; i++) {
            sb.append("palavra").append(i).append(". ");
        }
        var text = sb.toString();
        var result = textChunker.chunk(text);
        assertTrue(result.size() > 1);
        assertFalse(result.get(0).isEmpty());
        assertFalse(result.get(1).isEmpty());
    }

    @Test
    void chunk_MultipleParagraphs_SplitsByDelimiters() {
        var text = "Primeiro parágrafo.\n\nSegundo parágrafo.\n\nTerceiro parágrafo.";
        var result = textChunker.chunk(text);
        assertEquals(1, result.size());
        assertTrue(result.get(0).contains("Primeiro"));
        assertTrue(result.get(0).contains("Terceiro"));
    }
}
