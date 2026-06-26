package br.com.chatbot.chatbot_api.service.impl;

import br.com.chatbot.chatbot_api.dto.response.AttachmentResponse;
import br.com.chatbot.chatbot_api.entity.Attachment;
import br.com.chatbot.chatbot_api.exception.InvalidFileTypeException;
import br.com.chatbot.chatbot_api.mapper.EntityMapper;
import br.com.chatbot.chatbot_api.repository.AttachmentRepository;
import br.com.chatbot.chatbot_api.service.ConversationService;
import br.com.chatbot.chatbot_api.service.FileService;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".txt", ".pdf");
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("text/plain", "application/pdf");

    @Value("${spring.servlet.multipart.max-file-size}")
    private DataSize maxFileSize;

    private final AttachmentRepository attachmentRepository;
    private final ConversationService conversationService;
    private final EntityMapper entityMapper;

    @Override
    public AttachmentResponse upload(Long conversationId, MultipartFile file) {
        var fileName = file.getOriginalFilename();
        if (fileName == null || fileName.isBlank()) {
            throw new InvalidFileTypeException("O arquivo precisa de um nome");
        }

        validateFile(file);

        var conversation = conversationService.findConversationOrThrow(conversationId);

        var attachment = Attachment.builder()
                .conversation(conversation)
                .fileName(fileName)
                .contentType(file.getContentType())
                .size(file.getSize())
                .uploadDate(LocalDateTime.now())
                .build();

        var saved = attachmentRepository.save(attachment);

        return entityMapper.toAttachmentResponse(saved);
    }

    private void validateFile(MultipartFile file) {
        var originalName = file.getOriginalFilename();

        if (originalName != null) {
            var extension = originalName.toLowerCase();

            if (ALLOWED_EXTENSIONS.stream().noneMatch(extension::endsWith)) {
                throw new InvalidFileTypeException(
                        "Extensão inválida: " + originalName + ". Somente .txt e .pdf são aceitos.");
            }
        }

        var contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new InvalidFileTypeException(
                    "Tipo de arquivo inválido: " + contentType + ". Somente arquivos .txt e .pdf são aceitos.");
        }

        if (file.getSize() > maxFileSize.toBytes()) {
            throw new InvalidFileTypeException(
                    "Arquivo excede o limite de " + maxFileSize);
        }
    }
}
