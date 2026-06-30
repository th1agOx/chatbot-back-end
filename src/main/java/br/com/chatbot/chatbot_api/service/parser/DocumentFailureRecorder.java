package br.com.chatbot.chatbot_api.service.parser;

import br.com.chatbot.chatbot_api.entity.Document;
import br.com.chatbot.chatbot_api.entity.DocumentStatus;
import br.com.chatbot.chatbot_api.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registra a falha de processamento de um documento em uma transação própria
 * (REQUIRES_NEW), garantindo que o registro com status FAILED seja persistido
 * mesmo que a transação principal de upload (que falhou) seja revertida.
 */
@Component
@RequiredArgsConstructor
public class DocumentFailureRecorder {

    private final DocumentRepository documentRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Long recordFailure(String fileName, String contentType, long fileSize) {
        var document = Document.builder()
                .fileName(fileName)
                .contentType(contentType)
                .fileSize(fileSize)
                .status(DocumentStatus.FAILED)
                .build();
        return documentRepository.save(document).getId();
    }
}
