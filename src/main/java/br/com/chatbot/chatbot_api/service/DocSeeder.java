package br.com.chatbot.chatbot_api.service;

import br.com.chatbot.chatbot_api.repository.DocumentRepository;
import br.com.chatbot.chatbot_api.service.ingestion.DocumentIngestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class DocSeeder implements CommandLineRunner {

    private static final List<String> DOC_PATTERNS = List.of("docs/*.txt", "docs/*.md");

    private final DocumentRepository documentRepository;
    private final DocumentIngestionService ingestionService;

    @Override
    public void run(String... args) {
        if (isAlreadySeeded()) {
            log.info("DocSeeder: base de conhecimento ja carregada, pulando.");
            return;
        }
        seed();
    }

    private boolean isAlreadySeeded() {
        return documentRepository.count() > 0;
    }

    public void seed() {
        try {
            var resolver = new PathMatchingResourcePatternResolver();
            var seeded = false;

            for (var pattern : DOC_PATTERNS) {
                var resources = resolver.getResources(pattern);
                for (var resource : resources) {
                    processResource(resource);
                    seeded = true;
                }
            }

            if (!seeded) {
                log.warn("DocSeeder: nenhum arquivo .txt ou .md encontrado em docs/");
                return;
            }

            log.info("DocSeeder: base de conhecimento carregada com sucesso!");
        } catch (Exception e) {
            log.error("DocSeeder: erro ao carregar conhecimento", e);
        }
    }

    private void processResource(Resource resource) {
        try {
            var filename = resource.getFilename();
            if (filename == null) return;

            var source = extractSourceName(filename);

            String content;
            try (var reader = new BufferedReader(
                    new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
                content = reader.lines().collect(Collectors.joining("\n"));
            }

            if (content.isBlank()) {
                log.warn("DocSeeder: {} esta vazio, pulando", filename);
                return;
            }

            var cleaned = cleanContent(content, filename);

            ingestionService.ingestContent(cleaned, "Conhecimento: " + source, "text/plain");

            log.info("DocSeeder: '{}' ingerido ({} caracteres)", source, cleaned.length());
        } catch (Exception e) {
            log.error("DocSeeder: erro ao processar {}", resource.getFilename(), e);
        }
    }

    private String extractSourceName(String filename) {
        var name = filename.contains(".")
                ? filename.substring(0, filename.lastIndexOf('.'))
                : filename;
        return name
                .replaceFirst("^\\d+-", "")
                .replace("-", " ")
                .replace("_", " ")
                .trim();
    }

    private String cleanContent(String content, String filename) {
        if (filename.endsWith(".md")) {
            return stripMarkdown(content);
        }
        return content;
    }

    private String stripMarkdown(String markdown) {
        var lines = markdown.lines().collect(Collectors.toList());
        var result = new StringBuilder();
        for (var line : lines) {
            var stripped = line.stripLeading();
            if (stripped.startsWith("```")) continue;
            if (stripped.startsWith("#")) {
                result.append(stripped.replaceAll("^#+\\s*", "")).append("\n");
            } else {
                result.append(line).append("\n");
            }
        }
        return result.toString().trim();
    }}
