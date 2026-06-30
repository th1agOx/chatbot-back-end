package br.com.chatbot.chatbot_api.dto.response;

@Deprecated(since = "2.0", forRemoval = true)
public record ChatResponse(
        MessageResponse userMessage,
        MessageResponse botMessage
) {}
