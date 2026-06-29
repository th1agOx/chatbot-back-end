package br.com.chatbot.chatbot_api.controller;

import br.com.chatbot.chatbot_api.dto.request.LoginRequest;
import br.com.chatbot.chatbot_api.dto.response.LoginResponse;
import br.com.chatbot.chatbot_api.security.TokenProvider;
import br.com.chatbot.chatbot_api.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Autenticação e login")
public class AuthController {

    private final UserService userService;

    @PostMapping("/login")
    @Operation(summary = "Autenticar usuário e obter token JWT")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        var response = userService.login(request);
        return ResponseEntity.ok(response);
    }
}
