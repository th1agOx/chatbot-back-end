package br.com.chatbot.chatbot_api.service;

import br.com.chatbot.chatbot_api.dto.response.DocumentResponse;
import br.com.chatbot.chatbot_api.entity.DocumentStatus;
import br.com.chatbot.chatbot_api.exception.DocumentProcessingException;
import br.com.chatbot.chatbot_api.exception.InvalidFileTypeException;
import br.com.chatbot.chatbot_api.exception.ResourceNotFoundException;
import br.com.chatbot.chatbot_api.mapper.DocumentMapper;
import br.com.chatbot.chatbot_api.repository.DocumentRepository;
import br.com.chatbot.chatbot_api.service.ingestion.DocumentIngestionService;
import br.com.chatbot.chatbot_api.service.integration.N8nWebhookNotifier;
import br.com.chatbot.chatbot_api.service.parser.DocumentFailureRecorder;
import br.com.chatbot.chatbot_api.service.parser.DocumentParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("txt", "pdf", "docx");
    private static final Map<String, String> CONTENT_TYPE_MAP = Map.of(
            "txt", "text/plain",
            "pdf", "application/pdf",
            "docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");

    private final DocumentRepository documentRepository;
    private final DocumentMapper documentMapper;
    private final DocumentIngestionService ingestionService;
    private final N8nWebhookNotifier n8nNotifier;
    private final DocumentFailureRecorder failureRecorder;
    private final Map<String, DocumentParser> parserMap = new HashMap<>();

    private final List<DocumentParser> parsers;

    @PostConstruct
    public void initParserMap() {
        for (var parser : parsers) {
            var key = parser.getClass().getSimpleName()
                    .replace("Parser", "")
                    .toLowerCase();
            parserMap.put(key, parser);
        }
    }

    @Transactional
    public DocumentResponse upload(MultipartFile file) {
        var extension = validateFile(file);

        var parser = selectParser(extension);
        String text;
        try {
            text = parser.parse(file.getInputStream());
        } catch (IOException e) {
            throw new DocumentProcessingException("Falha ao ler arquivo: " + e.getMessage(), e);
        } catch (RuntimeException e) {
            throw new DocumentProcessingException("Falha ao processar conteudo do arquivo: " + e.getMessage(), e);
        }

        log.info("DocumentService: '{}' extraiu {} caracteres", file.getOriginalFilename(), text.length());

        try {
            var document = ingestionService.ingestContent(text, file.getOriginalFilename(), file.getContentType());
            n8nNotifier.notify(document.getId());
            return documentMapper.toDocumentResponse(document);
        } catch (RuntimeException e) {
            failureRecorder.recordFailure(file.getOriginalFilename(), file.getContentType(), file.getSize());
            throw new DocumentProcessingException(
                    "Falha ao gerar embeddings para o documento: " + e.getMessage(), e);
        }
    }

    private String validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidFileTypeException("Arquivo vazio ou não enviado");
        }

        var originalName = file.getOriginalFilename();
        if (originalName == null || !originalName.contains(".")) {
            throw new InvalidFileTypeException("Arquivo sem extensão");
        }

        var extension = originalName.substring(originalName.lastIndexOf('.') + 1).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new InvalidFileTypeException(
                    "Tipo de arquivo não suportado: " + extension +
                            ". Tipos aceitos: .txt, .pdf, .docx");
        }

        return extension;
    }

    public DocumentStatus getStatus(Long id) {
        var document = documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Documento não encontrado: " + id));
        return document.getStatus();
    }

    private DocumentParser selectParser(String extension) {
        var parser = parserMap.get(extension);
        if (parser == null) {
            throw new DocumentProcessingException("Parser não encontrado para extensão: " + extension);
        }
        return parser;
    }
}
