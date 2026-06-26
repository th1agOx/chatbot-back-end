package br.com.chatbot.chatbot_api.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

public record HealthResponse(
        @JsonProperty("status") String status,
        @JsonProperty("timestamp") LocalDateTime timestamp
) {}
