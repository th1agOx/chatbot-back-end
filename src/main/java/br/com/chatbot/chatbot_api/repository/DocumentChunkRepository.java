package br.com.chatbot.chatbot_api.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.chatbot.chatbot_api.entity.DocumentChunk;
import br.com.chatbot.chatbot_api.entity.PGvector;

public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, UUID> {

    @Query(value = """
            SELECT id, document_id AS documentId, content, chunk_index AS chunkIndex, created_at AS createdAt,
                   1 - (embedding <=> :queryVector) AS similarity
            FROM document_chunks
            WHERE 1 - (embedding <=> :queryVector) >= :minSimilarity
            ORDER BY embedding <=> :queryVector
            LIMIT :topK
            """, nativeQuery = true)
    List<ChunkSimilarityProjection> findSimilarChunks(
            @Param("queryVector") PGvector queryVector,
            @Param("topK") int topK,
            @Param("minSimilarity") double minSimilarity);
}