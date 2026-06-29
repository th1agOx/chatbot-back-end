package br.com.chatbot.chatbot_api.mapper;

import br.com.chatbot.chatbot_api.dto.response.UserResponse;
import br.com.chatbot.chatbot_api.entity.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserResponse toUserResponse(User user);
}
