package br.com.chatbot.chatbot_api.service;

import br.com.chatbot.chatbot_api.entity.Document;
import br.com.chatbot.chatbot_api.entity.DocumentChunk;
import br.com.chatbot.chatbot_api.entity.DocumentStatus;
import br.com.chatbot.chatbot_api.entity.PGvector;
import br.com.chatbot.chatbot_api.exception.DocumentProcessingException;
import br.com.chatbot.chatbot_api.repository.DocumentRepository;
import br.com.chatbot.chatbot_api.service.chunking.TextChunker;
import br.com.chatbot.chatbot_api.service.embedding.EmbeddingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class KnowledgeSeeder implements CommandLineRunner {

    private static final int EXPECTED_EMBEDDING_DIMENSION = 768;
    private static final String KNOWLEDGE_LOCATION = "classpath:knowledge/";
    private static final String KNOWLEDGE_PATTERN = KnowledgeSeeder.class.getPackageName().contains("knowledge") ? "" : "knowledge/*.txt";

    private final DocumentRepository documentRepository;
    private final TextChunker textChunker;
    private final EmbeddingService embeddingService;

    @Override
    public void run(String... args) {
        if (isAlreadySeeded()) {
            log.info("KnowledgeSeeder: base de conhecimento ja carregada, pulando.");
            return;
        }
        seed();
    }

    private boolean isAlreadySeeded() {
        var seeded = documentRepository.findAll().stream()
                .anyMatch(d -> d.getFileName() != null && d.getFileName().startsWith("Conhecimento:"));
        return seeded;
    }

    @Transactional
    public void seed() {
        try {
            var resolver = new PathMatchingResourcePatternResolver();
            var resources = resolver.getResources("knowledge/*.txt");

            if (resources.length == 0) {
                log.warn("KnowledgeSeeder: nenhum arquivo .txt encontrado em knowledge/");
                return;
            }

            log.info("KnowledgeSeeder: encontrados {} arquivos de conhecimento", resources.length);

            for (var resource : resources) {
                processResource(resource);
            }

            log.info("KnowledgeSeeder: base de conhecimento carregada com sucesso!");
        } catch (Exception e) {
            log.error("KnowledgeSeeder: erro ao carregar conhecimento", e);
        }
    }

    private void processResource(Resource resource) {
        try {
            var filename = resource.getFilename();
            if (filename == null) return;

            var source = filename
                    .replace(".txt", "")
                    .replaceFirst("^\\d+-", "")
                    .replace("-", " ")
                    .trim();

            String content;
            try (var reader = new BufferedReader(
                    new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
                content = reader.lines().collect(Collectors.joining("\n"));
            }

            if (content.isBlank()) {
                log.warn("KnowledgeSeeder: {} esta vazio, pulando", filename);
                return;
            }

            var chunks = textChunker.chunk(content);

            var document = Document.builder()
                    .fileName("Conhecimento: " + source)
                    .contentType("text/plain")
                    .fileSize((long) content.length())
                    .status(DocumentStatus.PROCESSING)
                    .build();

            var documentChunks = buildChunks(document, chunks);
            document.setChunks(documentChunks);
            document.setStatus(DocumentStatus.COMPLETED);

            documentRepository.save(document);

            log.info("KnowledgeSeeder: '{}' -> {} chunks ({} caracteres)", source, chunks.size(), content.length());
        } catch (Exception e) {
            log.error("KnowledgeSeeder: erro ao processar {}", resource.getFilename(), e);
        }
    }

    private List<DocumentChunk> buildChunks(Document document, List<String> chunks) {
        var documentChunks = new ArrayList<DocumentChunk>();
        for (int i = 0; i < chunks.size(); i++) {
            var vector = embeddingService.generateEmbedding(chunks.get(i));
            if (vector.size() != EXPECTED_EMBEDDING_DIMENSION) {
                throw new DocumentProcessingException(
                        "Dimensão do embedding inesperada: " + vector.size()
                                + " (esperado: " + EXPECTED_EMBEDDING_DIMENSION + ")");
            }
            var pgVector = new PGvector(vector.stream().mapToDouble(Float::doubleValue).toArray());
            var chunk = DocumentChunk.builder()
                    .document(document)
                    .content(chunks.get(i))
                    .embedding(pgVector)
                    .chunkIndex(i)
                    .build();
            documentChunks.add(chunk);
        }
        return documentChunks;
    }
}
