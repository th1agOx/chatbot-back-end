package br.com.chatbot.chatbot_api.service.chat;

import br.com.chatbot.chatbot_api.service.BotService;
import br.com.chatbot.chatbot_api.service.rag.RagResult;
import br.com.chatbot.chatbot_api.service.rag.RagService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BotServiceImpl implements BotService {

    private final OpenAiChatModel chatModel;
    private final RagService ragService;

    @Value("${app.rag.top-k}")
    private int topK;

    @Value("${app.rag.min-similarity}")
    private double minSimilarity;

    @Value("${app.rag.max-context-size}")
    private int maxContextSize;

    @Value("${spring.ai.openai.chat.options.model}")
    private String chatModelId;

    @Override
    public String responseGenerate(String userMessage) {
        return responseGenerateWithMetadata(userMessage).context();
    }

    public RagResult responseGenerateWithMetadata(String userMessage) {
        var ragResult = ragService.retrieveContext(userMessage, topK, minSimilarity, maxContextSize);
        var prompt = buildPrompt(ragResult.context(), userMessage);
        var response = chatModel.call(
                new Prompt(prompt,
                        OpenAiChatOptions.builder()
                                .model(chatModelId)
                                .build())
        );
        return new RagResult(
                response.getResult().getOutput().getText(),
                ragResult.sources(),
                ragResult.executionTimeMs(),
                ragResult.chunksConsumed()
        );
    }

    private String buildPrompt(String context, String question) {
        return """
                Você é um assistente especializado. Use APENAS o contexto abaixo para responder.
                Se o contexto não contiver informação suficiente, diga que não sabe.

                Contexto:
                %s

                Pergunta:
                %s
                """.formatted(context, question);
    }
}
