package br.com.chatbot.chatbot_api.service.integration;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
public class N8nWebhookNotifier {

    private static final Logger log = LoggerFactory.getLogger(N8nWebhookNotifier.class);
    private static final int MAX_RETRIES = 3;
    private static final long BASE_DELAY_MS = 1000;

    @Value("${app.n8n.enabled:true}")
    private boolean enabled;

    @Value("${app.n8n.webhook-url:#{null}}")
    private String webhookUrl;

    private final RestTemplate restTemplate;

    @Async
    public void notify(Long documentId) {
        if (!enabled) {
            log.debug("n8n notificador desabilitado — notificação ignorada para documento {}", documentId);
            return;
        }
        if (webhookUrl == null || webhookUrl.isBlank()) {
            log.debug("n8n webhook não configurado — notificação ignorada para documento {}", documentId);
            return;
        }
        notifyWithRetry(documentId, 0);
    }

    private void notifyWithRetry(Long documentId, int attempt) {
        try {
            var payload = new N8nPayload(documentId);
            restTemplate.postForEntity(webhookUrl, payload, String.class);
            log.info("n8n notificado para documento {}", documentId);
        } catch (ResourceAccessException e) {
            if (attempt < MAX_RETRIES - 1) {
                long delay = BASE_DELAY_MS * (1L << attempt);
                log.warn("Falha de conexão ao notificar n8n para documento {} (tentativa {}), "
                        + "reintentando em {}ms: {}", documentId, attempt + 1, delay, e.getMessage());
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return;
                }
                notifyWithRetry(documentId, attempt + 1);
            } else {
                log.warn("Falha ao notificar n8n para documento {} após {} tentativas: {}",
                        documentId, MAX_RETRIES, e.getMessage());
            }
        } catch (Exception e) {
            log.warn("Falha ao notificar n8n para documento {}: {}", documentId, e.getMessage());
        }
    }

    private record N8nPayload(Long documentId) {}
}
