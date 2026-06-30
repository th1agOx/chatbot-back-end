package br.com.chatbot.chatbot_api.config;

import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenAiConfig {

    @Bean
    @ConditionalOnMissingBean
    public OpenAiChatModel openAiChatModel(
            @Value("${spring.ai.openai.base-url}") String baseUrl,
            @Value("${spring.ai.openai.api-key}") String apiKey,
            @Value("${spring.ai.openai.chat.options.model}") String model) {
        var api = new OpenAiApi(baseUrl, apiKey);
        var options = OpenAiChatOptions.builder()
                .model(model)
                .build();
        return new OpenAiChatModel(api, options);
    }

    @Bean
    @ConditionalOnMissingBean
    public OpenAiEmbeddingModel openAiEmbeddingModel(
            @Value("${spring.ai.openai.base-url}") String baseUrl,
            @Value("${spring.ai.openai.api-key}") String apiKey,
            @Value("${spring.ai.openai.embedding.options.model}") String model) {
        var api = new OpenAiApi(baseUrl, apiKey);
        var options = OpenAiEmbeddingOptions.builder()
                .model(model)
                .build();
        return new OpenAiEmbeddingModel(api, MetadataMode.ALL, options);
    }
}
