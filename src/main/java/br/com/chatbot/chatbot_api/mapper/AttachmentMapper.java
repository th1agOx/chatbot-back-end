package br.com.chatbot.chatbot_api.mapper;

import br.com.chatbot.chatbot_api.dto.response.AttachmentResponse;
import br.com.chatbot.chatbot_api.entity.Attachment;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AttachmentMapper {

    AttachmentResponse toAttachmentResponse(Attachment attachment);
}
