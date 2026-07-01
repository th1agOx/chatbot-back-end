package br.com.chatbot.chatbot_api.dto.response;

@Deprecated(since = "2.0", forRemoval = true)
public record ChatResponse(
        Long conversationId,
        MessageResponse userMessage,
        MessageResponse botMessage
) {}
