package br.com.chatbot.chatbot_api.controller;

import br.com.chatbot.chatbot_api.dto.request.ChatRequest;
import br.com.chatbot.chatbot_api.dto.response.ChatResponse;
import br.com.chatbot.chatbot_api.dto.response.MessageResponse;
import br.com.chatbot.chatbot_api.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Tag(name = "Chat", description = "Chat mapeia os end-points")
public class ChatController {

    private final ChatService chatService;

    @PostMapping("/send")
    @Operation(summary = "Post para Enviar mensagem para o chatbot")
    public ResponseEntity<ChatResponse> send(@Valid @RequestBody ChatRequest request) {
        var response = chatService.sendMessage(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/history/{conversationId}")
    @Operation(summary = "Get no historico de conversa no chat")
    public ResponseEntity<List<MessageResponse>> history(@PathVariable Long conversationId) {
        var messages = chatService.getHistory(conversationId);
        return ResponseEntity.ok(messages);
    }
}
