package br.com.chatbot.chatbot_api.service.embedding;

import java.util.List;

public interface EmbeddingService {

    List<Float> generateEmbedding(String text);
}
