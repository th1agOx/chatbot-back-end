package br.com.chatbot.chatbot_api.controller;

import br.com.chatbot.chatbot_api.dto.request.ConversationRequest;
import br.com.chatbot.chatbot_api.dto.response.ConversationResponse;
import br.com.chatbot.chatbot_api.service.ConversationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/conversations")
@RequiredArgsConstructor
@Tag(name = "Conversations", description = "End-point dedicado para o gerenciamento de conversação")
public class ConversationController {

    private final ConversationService conversationService;

    @PostMapping
    @Operation(summary = "Post para criar um novo campo de conversação")
    public ResponseEntity<ConversationResponse> create(@Valid @RequestBody ConversationRequest request) {
        var response = conversationService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "Get para listar todas as conversas")
    public ResponseEntity<List<ConversationResponse>> findAll() {
        var responses = conversationService.findAll();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get para encontrar a conversa pelo ID")
    public ResponseEntity<ConversationResponse> findById(@PathVariable Long id) {
        var response = conversationService.findById(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar título da conversa")
    public ResponseEntity<ConversationResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody ConversationRequest request) {
        var response = conversationService.update(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete do campo da conversar")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        conversationService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
