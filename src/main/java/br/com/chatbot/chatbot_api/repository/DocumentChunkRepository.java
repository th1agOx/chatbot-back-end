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
            SELECT c.id, c.document_id AS documentId, c.content, c.chunk_index AS chunkIndex,
                   c.created_at AS createdAt, d.file_name AS documentName,
                   1 - (c.embedding <=> :queryVector) AS similarity
            FROM document_chunks c
            JOIN documents d ON d.id = c.document_id
            WHERE 1 - (c.embedding <=> :queryVector) >= :minSimilarity
            ORDER BY c.embedding <=> :queryVector
            LIMIT :topK
            """, nativeQuery = true)
    List<ChunkSimilarityProjection> findSimilarChunks(
            @Param("queryVector") PGvector queryVector,
            @Param("topK") int topK,
            @Param("minSimilarity") double minSimilarity);
}