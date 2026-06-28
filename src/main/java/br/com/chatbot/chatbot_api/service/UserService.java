package br.com.chatbot.chatbot_api.service;

import br.com.chatbot.chatbot_api.dto.request.LoginRequest;
import br.com.chatbot.chatbot_api.dto.request.UserCreateRequest;
import br.com.chatbot.chatbot_api.dto.response.LoginResponse;
import br.com.chatbot.chatbot_api.dto.response.UserResponse;
import br.com.chatbot.chatbot_api.entity.User;
import br.com.chatbot.chatbot_api.exception.AuthenticationException;
import br.com.chatbot.chatbot_api.exception.DuplicateResourceException;
import br.com.chatbot.chatbot_api.mapper.UserMapper;
import br.com.chatbot.chatbot_api.repository.UserRepository;
import br.com.chatbot.chatbot_api.security.TokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final TokenProvider tokenProvider;

    public UserResponse create(UserCreateRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("Email já cadastrado: " + request.email());
        }

        var user = User.builder()
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .displayName(request.displayName())
                .build();

        var saved = userRepository.save(user);
        return userMapper.toUserResponse(saved);
    }

    public LoginResponse login(LoginRequest request) {
        var user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new AuthenticationException("Credenciais inválidas"));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new AuthenticationException("Credenciais inválidas");
        }

        var token = tokenProvider.createToken(user.getEmail());
        return new LoginResponse(token, "Bearer");
    }
}
