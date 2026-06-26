package br.com.chatbot.chatbot_api.service.impl;

import br.com.chatbot.chatbot_api.dto.response.AttachmentResponse;
import br.com.chatbot.chatbot_api.entity.Attachment;
import br.com.chatbot.chatbot_api.exception.InvalidFileTypeException;
import br.com.chatbot.chatbot_api.mapper.EntityMapper;
import br.com.chatbot.chatbot_api.repository.AttachmentRepository;
import br.com.chatbot.chatbot_api.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    private static final Set<String> ALLOWED_TYPES = Set.of("text/plain", "application/pdf");
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024L;

    private final AttachmentRepository attachmentRepository;
    private final ConversationServiceImpl conversationService;
    private final EntityMapper entityMapper;

    @Override
    public AttachmentResponse upload(Long conversationId, MultipartFile file) {
        var conversation = conversationService.findConversationOrThrow(conversationId);

        validateFile(file);

        var attachment = Attachment.builder()
                .conversation(conversation)
                .fileName(file.getOriginalFilename())
                .contentType(file.getContentType())
                .size(file.getSize())
                .uploadDate(LocalDateTime.now())
                .build();

        var saved = attachmentRepository.save(attachment);
        return entityMapper.toAttachmentResponse(saved);
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new InvalidFileTypeException("File is empty");
        }

        var contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            throw new InvalidFileTypeException(
                    "Invalid file type: " + contentType + ". Only TXT and PDF files are allowed."
            );
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new InvalidFileTypeException(
                    "File size exceeds the maximum limit of 10MB"
            );
        }
    }
}
