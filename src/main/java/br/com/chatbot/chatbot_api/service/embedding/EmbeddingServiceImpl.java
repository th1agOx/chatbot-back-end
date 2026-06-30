package br.com.chatbot.chatbot_api.service.embedding;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class EmbeddingServiceImpl implements EmbeddingService {

    private final OpenAiEmbeddingModel embeddingModel;

    @Value("${spring.ai.openai.embedding.options.model}")
    private String embeddingModelId;

    @Override
    public List<Float> generateEmbedding(String text) {
        var options = OpenAiEmbeddingOptions.builder()
                .model(embeddingModelId)
                .build();
        var request = new EmbeddingRequest(List.of(text), options);
        var raw = embeddingModel.call(request).getResult().getOutput();
        return IntStream.range(0, raw.length)
                .mapToObj(i -> raw[i])
                .toList();
    }
}
