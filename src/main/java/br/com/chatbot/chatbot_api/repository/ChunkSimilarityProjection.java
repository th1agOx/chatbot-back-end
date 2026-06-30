package br.com.chatbot.chatbot_api.repository;

import java.time.LocalDateTime;
import java.util.UUID;

public interface ChunkSimilarityProjection {

    UUID getId();

    Long getDocumentId();

    String getDocumentName();

    String getContent();

    Integer getChunkIndex();

    LocalDateTime getCreatedAt();

    Double getSimilarity();
}