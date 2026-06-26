package br.com.chatbot.chatbot_api.service;

import br.com.chatbot.chatbot_api.dto.request.ChatRequest;
import br.com.chatbot.chatbot_api.dto.response.ChatResponse;
import br.com.chatbot.chatbot_api.dto.response.MessageResponse;

import java.util.List;

public interface ChatService {
    ChatResponse sendMessage(ChatRequest request);
    List<MessageResponse> getHistory(Long conversationId);
}
