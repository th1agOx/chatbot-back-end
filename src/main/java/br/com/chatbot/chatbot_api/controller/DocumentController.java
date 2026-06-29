package br.com.chatbot.chatbot_api.controller;

import br.com.chatbot.chatbot_api.dto.response.DocumentResponse;
import br.com.chatbot.chatbot_api.service.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
@Tag(name = "Documents", description = "Upload e processamento de documentos com pipeline de IA")
public class DocumentController {

    private final DocumentService documentService;

    @PostMapping("/upload")
    @Operation(summary = "Upload de documento com pipeline completo (parse + chunk + embed + persist)")
    public ResponseEntity<DocumentResponse> upload(@RequestParam MultipartFile file) {
        var response = documentService.upload(file);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
