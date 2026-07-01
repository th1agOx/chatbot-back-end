package br.com.chatbot.chatbot_api.service.impl;

import br.com.chatbot.chatbot_api.dto.request.ConversationRequest;
import br.com.chatbot.chatbot_api.entity.Conversation;
import br.com.chatbot.chatbot_api.exception.ResourceNotFoundException;
import br.com.chatbot.chatbot_api.mapper.ConversationMapper;
import br.com.chatbot.chatbot_api.repository.ConversationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConversationServiceImplTest {

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private ConversationMapper conversationMapper;

    @InjectMocks
    private ConversationServiceImpl conversationService;

    @Test
    void create_ShouldReturnConversationResponse() {
        var request = new ConversationRequest("Chat 1");
        var conversation = new Conversation();
        conversation.setId(1L);
        conversation.setTitle("Chat 1");

        when(conversationRepository.save(any())).thenReturn(conversation);
        when(conversationMapper.toConversationResponse(any())).then(invocation -> {
            var c = invocation.getArgument(0, Conversation.class);
            return new br.com.chatbot.chatbot_api.dto.response.ConversationResponse(
                    c.getId(), c.getTitle(), c.getCreatedAt(), c.getUpdatedAt(),
                    c.getMessageCount(), c.getLastMessageAt());
        });

        var response = conversationService.create(request);
        assertNotNull(response);
    }

    @Test
    void findById_WhenNotFound_ShouldThrowException() {
        when(conversationRepository.findWithMessagesById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> conversationService.findById(99L));
    }
}
