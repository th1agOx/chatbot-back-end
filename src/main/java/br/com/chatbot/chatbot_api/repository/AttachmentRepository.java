package br.com.chatbot.chatbot_api.repository;

import br.com.chatbot.chatbot_api.entity.Attachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AttachmentRepository extends JpaRepository<Attachment, Long> {
}
