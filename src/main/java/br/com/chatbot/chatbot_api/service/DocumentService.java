package br.com.chatbot.chatbot_api.service;

import br.com.chatbot.chatbot_api.dto.response.DocumentResponse;
import br.com.chatbot.chatbot_api.entity.Document;
import br.com.chatbot.chatbot_api.entity.DocumentChunk;
import br.com.chatbot.chatbot_api.entity.PGvector;
import br.com.chatbot.chatbot_api.exception.DocumentProcessingException;
import br.com.chatbot.chatbot_api.exception.InvalidFileTypeException;
import br.com.chatbot.chatbot_api.mapper.DocumentMapper;
import br.com.chatbot.chatbot_api.repository.DocumentRepository;
import br.com.chatbot.chatbot_api.service.chunking.TextChunker;
import br.com.chatbot.chatbot_api.service.embedding.EmbeddingService;
import br.com.chatbot.chatbot_api.service.integration.N8nWebhookNotifier;
import br.com.chatbot.chatbot_api.service.parser.DocumentParser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class DocumentService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("txt", "pdf", "docx");
    private static final Map<String, String> CONTENT_TYPE_MAP = Map.of(
            "txt", "text/plain",
            "pdf", "application/pdf",
            "docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );

    private final DocumentRepository documentRepository;
    private final DocumentMapper documentMapper;
    private final TextChunker textChunker;
    private final EmbeddingService embeddingService;
    private final N8nWebhookNotifier n8nNotifier;
    private final List<DocumentParser> parsers;

    @Transactional
    public DocumentResponse upload(MultipartFile file) {
        var extension = validateFile(file);

        var parser = selectParser(extension);
        String text;
        try {
            text = parser.parse(file.getInputStream());
        } catch (IOException e) {
            throw new DocumentProcessingException("Falha ao ler arquivo: " + e.getMessage(), e);
        }

        var chunks = textChunker.chunk(text);

        var document = Document.builder()
                .fileName(file.getOriginalFilename())
                .contentType(file.getContentType())
                .fileSize(file.getSize())
                .originalText(text)
                .build();

        var documentChunks = new ArrayList<DocumentChunk>();
        for (int i = 0; i < chunks.size(); i++) {
            var vector = embeddingService.generateEmbedding(chunks.get(i));
            var pgVector = new PGvector(vector.stream().mapToDouble(Float::doubleValue).toArray());
            var chunk = DocumentChunk.builder()
                    .document(document)
                    .content(chunks.get(i))
                    .embedding(pgVector)
                    .chunkIndex(i)
                    .build();
            documentChunks.add(chunk);
        }
        document.setChunks(documentChunks);

        var saved = documentRepository.save(document);

        n8nNotifier.notify(saved.getId());

        return documentMapper.toDocumentResponse(saved);
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

    private DocumentParser selectParser(String extension) {
        for (var parser : parsers) {
            var className = parser.getClass().getSimpleName().toLowerCase();
            if (className.contains(extension)) {
                return parser;
            }
        }
        throw new DocumentProcessingException("Parser não encontrado para extensão: " + extension);
    }
}
