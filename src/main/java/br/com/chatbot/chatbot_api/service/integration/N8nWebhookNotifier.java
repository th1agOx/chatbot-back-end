package br.com.chatbot.chatbot_api.service.integration;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
public class N8nWebhookNotifier {

    private static final Logger log = LoggerFactory.getLogger(N8nWebhookNotifier.class);

    @Value("${app.n8n.webhook-url:#{null}}")
    private String webhookUrl;

    private final RestTemplate restTemplate;

    @Async
    public void notify(Long documentId) {
        if (webhookUrl == null || webhookUrl.isBlank()) {
            log.debug("n8n webhook não configurado — notificação ignorada para documento {}", documentId);
            return;
        }
        try {
            var payload = new N8nPayload(documentId);
            restTemplate.postForEntity(webhookUrl, payload, String.class);
            log.info("n8n notificado para documento {}", documentId);
        } catch (Exception e) {
            log.warn("Falha ao notificar n8n para documento {}: {}", documentId, e.getMessage());
        }
    }

    private record N8nPayload(Long documentId) {}
}
