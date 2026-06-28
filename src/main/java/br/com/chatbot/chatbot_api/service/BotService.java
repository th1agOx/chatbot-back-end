package br.com.chatbot.chatbot_api.service;

import br.com.chatbot.chatbot_api.service.rag.RagResult;

public interface BotService {
    String responseGenerate(String userMessage);
    RagResult responseGenerateWithMetadata(String userMessage);
}
