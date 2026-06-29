package br.com.chatbot.chatbot_api.mapper;

import br.com.chatbot.chatbot_api.dto.response.ConversationResponse;
import br.com.chatbot.chatbot_api.entity.Conversation;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ConversationMapper {

    ConversationResponse toConversationResponse(Conversation conversation);
}
