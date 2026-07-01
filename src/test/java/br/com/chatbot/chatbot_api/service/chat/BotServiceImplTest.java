package br.com.chatbot.chatbot_api.service.chat;

import br.com.chatbot.chatbot_api.dto.response.SourceReference;
import br.com.chatbot.chatbot_api.service.rag.RagResult;
import br.com.chatbot.chatbot_api.service.rag.RagService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.ollama.OllamaChatModel;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BotServiceImplTest {

    @Mock
    private OllamaChatModel chatModel;

    @Mock
    private RagService ragService;

    @InjectMocks
    private BotServiceImpl botService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(botService, "topK", 5);
        ReflectionTestUtils.setField(botService, "minSimilarity", 0.75);
        ReflectionTestUtils.setField(botService, "maxContextSize", 4000);
        ReflectionTestUtils.setField(botService, "chatModelId", "llama3.2:1b");
    }

    @Test
    void responseGenerate_ValidMessage_ReturnsAnswer() {
        var ragResult = new RagResult("contexto", List.of(), 100L, 1);
        when(ragService.retrieveContext("pergunta", 5, 0.75, 4000)).thenReturn(ragResult);

        var mockGeneration = new Generation(new AssistantMessage("resposta gerada"));
        var mockResponse = new ChatResponse(List.of(mockGeneration));
        when(chatModel.call(any(Prompt.class))).thenReturn(mockResponse);

        var answer = botService.responseGenerate("pergunta");
        assertNotNull(answer);
        assertFalse(answer.isEmpty());
    }

    @Test
    void responseGenerateWithMetadata_ReturnsRagResultWithSources() {
        var sources = List.of(new SourceReference(1L, "doc.txt", "trecho do documento"));
        var ragResult = new RagResult("contexto", sources, 150L, 1);
        when(ragService.retrieveContext("pergunta", 5, 0.75, 4000)).thenReturn(ragResult);

        var mockGeneration = new Generation(new AssistantMessage("resposta com fontes"));
        var mockResponse = new ChatResponse(List.of(mockGeneration));
        when(chatModel.call(any(Prompt.class))).thenReturn(mockResponse);

        var result = botService.responseGenerateWithMetadata("pergunta");
        assertNotNull(result);
        assertEquals("resposta com fontes", result.context());
        assertEquals(1, result.sources().size());
        assertEquals("doc.txt", result.sources().get(0).fileName());
        assertTrue(result.executionTimeMs() >= 0);
        assertEquals(1, result.chunksConsumed());
    }
}
