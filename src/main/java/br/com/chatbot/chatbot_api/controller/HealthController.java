package br.com.chatbot.chatbot_api.controller;

import br.com.chatbot.chatbot_api.dto.response.HealthResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@Tag(name = "Health", description = "Health check endpoint")
public class HealthController {

    @GetMapping("/health")
    @Operation(summary = "Checagem dos status da saúde da API")
    public ResponseEntity<HealthResponse> health() {
        var response = new HealthResponse("UP", LocalDateTime.now());
        return ResponseEntity.ok(response);
    }
}
