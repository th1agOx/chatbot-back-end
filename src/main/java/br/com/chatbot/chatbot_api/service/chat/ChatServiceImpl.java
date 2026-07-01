package br.com.chatbot.chatbot_api.service.chat;

import br.com.chatbot.chatbot_api.dto.request.ChatRequest;
import br.com.chatbot.chatbot_api.dto.response.ChatResponse;
import br.com.chatbot.chatbot_api.dto.response.ChatResponseV2;
import br.com.chatbot.chatbot_api.dto.response.MessageResponse;
import br.com.chatbot.chatbot_api.entity.Conversation;
import br.com.chatbot.chatbot_api.entity.Message;
import br.com.chatbot.chatbot_api.enums.MessageRole;
import br.com.chatbot.chatbot_api.mapper.MessageMapper;
import br.com.chatbot.chatbot_api.repository.MessageRepository;
import br.com.chatbot.chatbot_api.service.BotService;
import br.com.chatbot.chatbot_api.service.ChatService;
import br.com.chatbot.chatbot_api.service.ConversationService;
import br.com.chatbot.chatbot_api.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final MessageRepository messageRepository;
    private final ConversationService conversationService;
    private final MessageMapper messageMapper;
    private final BotService botService;
    private final DocumentService documentService;

    private Conversation resolveConversation(Long conversationId, String firstMessage) {
        if (conversationId == null) {
            return conversationService.createDefaultConversation(firstMessage);
        }
        return conversationService.findConversationOrThrow(conversationId);
    }

    @Override
    public ChatResponse sendMessage(ChatRequest request) {
        var conversation = resolveConversation(request.conversationId(), request.message());

        var userMessage = saveMessage(conversation, MessageRole.USER, request.message());
        var botAnswer = botService.responseGenerate(request.message());
        var botMessage = saveMessage(conversation, MessageRole.BOT, botAnswer);

        return new ChatResponse(
                conversation.getId(),
                messageMapper.toMessageResponse(userMessage),
                messageMapper.toMessageResponse(botMessage));
    }

    public ChatResponseV2 sendMessageV2(ChatRequest request) {
        var conversation = resolveConversation(request.conversationId(), request.message());

        var userMessage = saveMessage(conversation, MessageRole.USER, request.message());
        var ragResult = botService.responseGenerateWithMetadata(request.message());
        var botMessage = saveMessage(conversation, MessageRole.BOT, ragResult.context());

        return new ChatResponseV2(
                conversation.getId(),
                messageMapper.toMessageResponse(userMessage),
                messageMapper.toMessageResponse(botMessage),
                ragResult.context(),
                ragResult.sources(),
                ragResult.executionTimeMs(),
                ragResult.chunksConsumed()
        );
    }

    @Override
    public ChatResponseV2 sendMessageWithFile(Long conversationId, String message, MultipartFile file) {
        var conversation = resolveConversation(conversationId, message);

        if (file != null && !file.isEmpty()) {
            documentService.upload(file);
        }

        var userMessage = saveMessage(conversation, MessageRole.USER, message);
        var ragResult = botService.responseGenerateWithMetadata(message);
        var botMessage = saveMessage(conversation, MessageRole.BOT, ragResult.context());

        return new ChatResponseV2(
                conversation.getId(),
                messageMapper.toMessageResponse(userMessage),
                messageMapper.toMessageResponse(botMessage),
                ragResult.context(),
                ragResult.sources(),
                ragResult.executionTimeMs(),
                ragResult.chunksConsumed()
        );
    }

    @Override
    public List<MessageResponse> getHistory(Long conversationId) {
        conversationService.findConversationOrThrow(conversationId);
        return messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId)
                .stream()
                .map(messageMapper::toMessageResponse)
                .toList();
    }

    private Message saveMessage(Conversation conversation,
                                MessageRole role, String content) {
        var message = Message.builder()
                .conversation(conversation)
                .role(role)
                .content(content)
                .build();
        conversation.setUpdatedAt(LocalDateTime.now());
        return messageRepository.save(message);
    }
}
