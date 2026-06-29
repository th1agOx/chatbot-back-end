package br.com.chatbot.chatbot_api.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank @Email(message = "Email deve ser válido") String email,
        @NotBlank(message = "Senha é obrigatória") String password
) {}
