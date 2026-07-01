package br.com.chatbot.chatbot_api.controller;

import br.com.chatbot.chatbot_api.dto.request.KnowledgeRequest;
import br.com.chatbot.chatbot_api.dto.response.KnowledgeResponse;
import br.com.chatbot.chatbot_api.service.KnowledgeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/knowledge")
@RequiredArgsConstructor
@Tag(name = "Knowledge", description = "Adicionar conhecimento direto ao RAG sem upload de arquivo")
public class KnowledgeController {

    private final KnowledgeService knowledgeService;

    @PostMapping("/add")
    @Operation(summary = "Adicionar texto como fonte de conhecimento para o RAG")
    public ResponseEntity<KnowledgeResponse> add(@Valid @RequestBody KnowledgeRequest request) {
        var response = knowledgeService.addKnowledge(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
