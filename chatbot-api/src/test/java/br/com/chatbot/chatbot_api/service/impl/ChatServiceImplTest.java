package br.com.chatbot.chatbot_api.service.impl;

import br.com.chatbot.chatbot_api.dto.request.ChatRequest;
import br.com.chatbot.chatbot_api.entity.Conversation;
import br.com.chatbot.chatbot_api.entity.Message;
import br.com.chatbot.chatbot_api.enums.MessageRole;
import br.com.chatbot.chatbot_api.mapper.EntityMapper;
import br.com.chatbot.chatbot_api.repository.MessageRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatServiceImplTest {

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private ConversationServiceImpl conversationService;

    @Mock
    private EntityMapper entityMapper;

    @InjectMocks
    private ChatServiceImpl chatService;

    @Test
    void sendMessage_ShouldReturnChatResponse() {
        var conversation = new Conversation();
        conversation.setId(1L);

        var request = new ChatRequest(1L, "Olá!");

        when(conversationService.findConversationOrThrow(1L)).thenReturn(conversation);
        when(messageRepository.save(any())).thenAnswer(invocation -> {
            var msg = invocation.getArgument(0, Message.class);
            msg.setId(msg.getRole() == MessageRole.USER ? 1L : 2L);
            return msg;
        });
        when(entityMapper.toMessageResponse(any())).thenAnswer(invocation -> {
            var msg = invocation.getArgument(0, Message.class);
            return new br.com.chatbot.chatbot_api.dto.response.MessageResponse(
                    msg.getId(), msg.getRole(), msg.getContent(), msg.getCreatedAt());
        });

        var response = chatService.sendMessage(request);
        assertNotNull(response);
        assertEquals(MessageRole.USER, response.userMessage().role());
        assertEquals(MessageRole.BOT, response.botMessage().role());
        assertEquals("Você disse: Olá!", response.botMessage().content());
    }

    @Test
    void getHistory_ShouldReturnMessages() {
        var conversation = new Conversation();
        conversation.setId(1L);

        var msg = new Message();
        msg.setId(1L);
        msg.setRole(MessageRole.USER);
        msg.setContent("Olá!");

        when(conversationService.findConversationOrThrow(1L)).thenReturn(conversation);
        when(messageRepository.findByConversationIdOrderByCreatedAtAsc(1L)).thenReturn(List.of(msg));
        when(entityMapper.toMessageResponse(any())).thenAnswer(invocation -> {
            var m = invocation.getArgument(0, Message.class);
            return new br.com.chatbot.chatbot_api.dto.response.MessageResponse(
                    m.getId(), m.getRole(), m.getContent(), m.getCreatedAt());
        });

        var history = chatService.getHistory(1L);
        assertEquals(1, history.size());
        assertEquals("Olá!", history.get(0).content());
    }
}
