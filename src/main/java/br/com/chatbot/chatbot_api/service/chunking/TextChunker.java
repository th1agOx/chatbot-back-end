package br.com.chatbot.chatbot_api.service.chunking;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

@Component
public class TextChunker {

    @Value("${app.chunking.chunk-size:1000}")
    private int chunkSize;

    @Value("${app.chunking.overlap:200}")
    private int overlap;

    @Value("${app.chunking.delimiters:\n\n|\n|\\.|\\?}")
    private String delimiters;

    @PostConstruct
    public void init() {
        try {
            Pattern.compile("(?<=" + delimiters + ")");
        } catch (PatternSyntaxException e) {
            throw new IllegalArgumentException("Delimitador inválido configurado em app.chunking.delimiters: " + delimiters, e);
        }
    }

    public List<String> chunk(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }

        var cleaned = text
                .replaceAll("\\s+", " ")
                .trim();

        var segments = splitByDelimiters(cleaned);
        var chunks = new ArrayList<String>();
        var current = new StringBuilder();

        for (var segment : segments) {
            if (current.length() + segment.length() > chunkSize && !current.isEmpty()) {
                chunks.add(current.toString().trim());
                current = new StringBuilder(getOverlap(current.toString()));
            }
            current.append(segment).append(" ");
        }

        if (!current.isEmpty()) {
            chunks.add(current.toString().trim());
        }

        return chunks;
    }

    private List<String> splitByDelimiters(String text) {
        var parts = text.split("(?<=" + delimiters + ")");
        var result = new ArrayList<String>();
        for (var part : parts) {
            var trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                result.add(trimmed);
            }
        }
        return result;
    }

    private String getOverlap(String text) {
        if (text.length() <= overlap) {
            return "";
        }
        var candidate = text.substring(text.length() - overlap);
        var firstSpace = candidate.indexOf(' ');
        if (firstSpace > 0 && firstSpace < candidate.length() - 1) {
            return candidate.substring(firstSpace + 1);
        }
        return candidate;
    }
}
