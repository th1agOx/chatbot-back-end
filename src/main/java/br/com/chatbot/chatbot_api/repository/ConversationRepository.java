package br.com.chatbot.chatbot_api.repository;

import br.com.chatbot.chatbot_api.entity.Conversation;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    @EntityGraph(attributePaths = "messages")
    List<Conversation> findAllWithMessages();

    @EntityGraph(attributePaths = "messages")
    Optional<Conversation> findWithMessagesById(Long id);
}
