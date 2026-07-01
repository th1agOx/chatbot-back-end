package br.com.chatbot.chatbot_api.mapper;

import br.com.chatbot.chatbot_api.dto.response.AttachmentResponse;
import br.com.chatbot.chatbot_api.dto.response.ConversationResponse;
import br.com.chatbot.chatbot_api.dto.response.MessageResponse;
import br.com.chatbot.chatbot_api.entity.Attachment;
import br.com.chatbot.chatbot_api.entity.Conversation;
import br.com.chatbot.chatbot_api.entity.Message;
import org.springframework.stereotype.Component;

@Component
public class EntityMapper {

    public ConversationResponse toConversationResponse(Conversation conversation) {
        return new ConversationResponse(
                conversation.getId(),
                conversation.getTitle(),
                conversation.getCreatedAt(),
                conversation.getUpdatedAt(),
                conversation.getMessageCount(),
                conversation.getLastMessageAt()
        );
    }

    public MessageResponse toMessageResponse(Message message) {
        return new MessageResponse(
                message.getId(),
                message.getRole(),
                message.getContent(),
                message.getCreatedAt()
        );
    }

    public AttachmentResponse toAttachmentResponse(Attachment attachment) {
        return new AttachmentResponse(
                attachment.getId(),
                attachment.getFileName(),
                attachment.getContentType(),
                attachment.getSize(),
                attachment.getUploadDate()
        );
    }
}
