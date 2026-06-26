package br.com.chatbot.chatbot_api.repository;

import br.com.chatbot.chatbot_api.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {
}
