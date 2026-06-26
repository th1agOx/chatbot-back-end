package br.com.chatbot.chatbot_api.service;

import br.com.chatbot.chatbot_api.dto.request.ConversationRequest;
import br.com.chatbot.chatbot_api.dto.response.ConversationResponse;

import java.util.List;

public interface ConversationService {
    ConversationResponse create(ConversationRequest request);
    List<ConversationResponse> findAll();
    ConversationResponse findById(Long id);
    void deleteById(Long id);
}
