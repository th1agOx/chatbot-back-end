package br.com.chatbot.chatbot_api.service.embedding;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EmbeddingServiceImpl implements EmbeddingService {

    private final OpenAiEmbeddingModel embeddingModel;

    @Value("${spring.ai.openai.embedding.options.model}")
    private String embeddingModelId;

    @Override
    public List<Float> generateEmbedding(String text) {
        var request = EmbeddingRequest.builder()
                .withModel(embeddingModelId)
                .withInstructions(List.of(text))
                .build();
        return embeddingModel.embed(request).getResult().getOutput();
    }
}
