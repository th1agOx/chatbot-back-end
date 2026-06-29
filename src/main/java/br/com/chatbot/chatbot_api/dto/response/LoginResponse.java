package br.com.chatbot.chatbot_api.dto.response;

public record LoginResponse(
        String token,
        String type
) {}
