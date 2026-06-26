package br.com.chatbot.chatbot_api.service;

import br.com.chatbot.chatbot_api.dto.response.AttachmentResponse;
import org.springframework.web.multipart.MultipartFile;

public interface FileService {
    AttachmentResponse upload(Long conversationId, MultipartFile file);
}
