package br.com.chatbot.chatbot_api.dto.response;

public record ChatResponse(
        MessageResponse userMessage,
        MessageResponse botMessage
) {}
