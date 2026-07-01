package br.com.chatbot.chatbot_api.service.impl;

import br.com.chatbot.chatbot_api.dto.request.ConversationRequest;
import br.com.chatbot.chatbot_api.dto.response.ConversationResponse;
import br.com.chatbot.chatbot_api.entity.Conversation;
import br.com.chatbot.chatbot_api.exception.ResourceNotFoundException;
import br.com.chatbot.chatbot_api.mapper.ConversationMapper;
import br.com.chatbot.chatbot_api.repository.ConversationRepository;
import br.com.chatbot.chatbot_api.service.ConversationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ConversationServiceImpl implements ConversationService {

    private final ConversationRepository conversationRepository;
    private final ConversationMapper conversationMapper;

    @Override
    public ConversationResponse create(ConversationRequest request) {
        var conversation = Conversation.builder()
                .title(request.title())
                .updatedAt(LocalDateTime.now())
                .build();
        var saved = conversationRepository.save(conversation);
        return conversationMapper.toConversationResponse(saved);
    }

    @Override
    public List<ConversationResponse> findAll() {
        return conversationRepository.findAll()
                .stream()
                .map(conversationMapper::toConversationResponse)
                .toList();
    }

    @Override
    public ConversationResponse findById(Long id) {
        var conversation = conversationRepository.findWithMessagesById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Historico de conversa não encontrado com id: " + id));
        return conversationMapper.toConversationResponse(conversation);
    }

    @Override
    public void deleteById(Long id) {
        var conversation = findConversationOrThrow(id);
        conversationRepository.delete(conversation);
    }

    @Override
    public ConversationResponse update(Long id, ConversationRequest request) {
        var conversation = findConversationOrThrow(id);
        conversation.setTitle(request.title());
        var saved = conversationRepository.save(conversation);
        return conversationMapper.toConversationResponse(saved);
    }

    public Conversation findConversationOrThrow(Long id) {
        return conversationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Historico de conversa não encontrado com id: " + id));
    }

    @Override
    public Conversation createDefaultConversation(String firstMessage) {
        var title = firstMessage.length() > 60
                ? firstMessage.substring(0, 60) + "..."
                : firstMessage;
        var conversation = Conversation.builder()
                .title(title)
                .build();
        return conversationRepository.save(conversation);
    }
}
