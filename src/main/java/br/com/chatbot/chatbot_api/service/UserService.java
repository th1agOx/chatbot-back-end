package br.com.chatbot.chatbot_api.service;

import br.com.chatbot.chatbot_api.dto.request.UserCreateRequest;
import br.com.chatbot.chatbot_api.dto.response.UserResponse;
import br.com.chatbot.chatbot_api.entity.User;
import br.com.chatbot.chatbot_api.exception.DuplicateResourceException;
import br.com.chatbot.chatbot_api.mapper.UserMapper;
import br.com.chatbot.chatbot_api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public UserResponse create(UserCreateRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("Email já cadastrado: " + request.email());
        }

        var user = User.builder()
                .email(request.email())
                .password(request.password())
                .displayName(request.displayName())
                .build();

        var saved = userRepository.save(user);
        return userMapper.toUserResponse(saved);
    }
}
