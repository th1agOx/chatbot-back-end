package br.com.chatbot.chatbot_api.service.impl;

import br.com.chatbot.chatbot_api.dto.request.ChatRequest;
import br.com.chatbot.chatbot_api.dto.response.ChatResponse;
import br.com.chatbot.chatbot_api.dto.response.MessageResponse;
import br.com.chatbot.chatbot_api.entity.Message;
import br.com.chatbot.chatbot_api.enums.MessageRole;
import br.com.chatbot.chatbot_api.mapper.EntityMapper;
import br.com.chatbot.chatbot_api.repository.MessageRepository;
import br.com.chatbot.chatbot_api.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final MessageRepository messageRepository;
    private final ConversationServiceImpl conversationService;
    private final EntityMapper entityMapper;

    @Override
    public ChatResponse sendMessage(ChatRequest request) {
        var conversation = conversationService.findConversationOrThrow(request.conversationId());

        var userMessage = saveMessage(conversation, MessageRole.USER, request.message());
        var botMessage = saveMessage(conversation, MessageRole.BOT, generateBotResponse(request.message()));

        return new ChatResponse(
                entityMapper.toMessageResponse(userMessage),
                entityMapper.toMessageResponse(botMessage)
        );
    }

    @Override
    public List<MessageResponse> getHistory(Long conversationId) {
        conversationService.findConversationOrThrow(conversationId);
        return messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId)
                .stream()
                .map(entityMapper::toMessageResponse)
                .toList();
    }

    private Message saveMessage(br.com.chatbot.chatbot_api.entity.Conversation conversation,
                                 MessageRole role, String content) {
        var message = Message.builder()
                .conversation(conversation)
                .role(role)
                .content(content)
                .createdAt(LocalDateTime.now())
                .build();
        return messageRepository.save(message);
    }

    private String generateBotResponse(String userMessage) {
        return "Você disse: " + userMessage;
    }
}
