package br.com.chatbot.chatbot_api.repository;

import br.com.chatbot.chatbot_api.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentRepository extends JpaRepository<Document, Long> {
}