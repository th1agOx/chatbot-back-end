package br.com.chatbot.chatbot_api.controller;

import br.com.chatbot.chatbot_api.dto.response.AttachmentResponse;
import br.com.chatbot.chatbot_api.service.FileService;
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
@RequestMapping("/api/files")
@RequiredArgsConstructor
@Tag(name = "Files", description = "End-point dedicado aos arquivos de upload")
public class FileController {

    private final FileService fileService;

    @PostMapping("/upload")
    @Operation(summary = "Post de Upload para arquivos .txt e .pdf")
    public ResponseEntity<AttachmentResponse> upload(
            @RequestParam Long conversationId,
            @RequestParam MultipartFile file) {
        var response = fileService.upload(conversationId, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
