package br.com.chatbot.chatbot_api.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserCreateRequest(
        @NotBlank @Email(message = "Email deve ser válido") String email,
        @NotBlank @Size(min = 6, message = "Senha deve ter no mínimo 6 caracteres") String password,
        @NotBlank(message = "Nome de exibição é obrigatório") String displayName
) {}
