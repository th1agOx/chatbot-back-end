package br.com.chatbot.chatbot_api.config;

import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.util.LinkedMultiValueMap;

@Configuration
public class OpenAiConfig {

    @Bean
    @Primary
    @ConditionalOnMissingBean
    public OpenAiApi openAiApi(
            @Value("${spring.ai.openai.base-url}") String baseUrl,
            @Value("${spring.ai.openai.api-key}") String apiKey) {
        var headers = new LinkedMultiValueMap<String, String>();
        headers.add("HTTP-Referer", "https://localhost:8080");
        headers.add("X-Title", "Chatbot MVP API");
        return OpenAiApi.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .headers(headers)
                .build();
    }

    @Bean
    public OpenAiApi openAiEmbeddingApi(
            @Value("${spring.ai.openai.embedding.base-url}") String baseUrl,
            @Value("${spring.ai.openai.api-key}") String apiKey) {
        var headers = new LinkedMultiValueMap<String, String>();
        headers.add("HTTP-Referer", "https://localhost:8080");
        headers.add("X-Title", "Chatbot MVP API");
        return OpenAiApi.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .headers(headers)
                .embeddingsPath("/embeddings")
                .build();
    }

    @Bean
    @ConditionalOnMissingBean
    public OpenAiChatModel openAiChatModel(
            OpenAiApi openAiApi,
            @Value("${spring.ai.openai.chat.options.model}") String model) {
        var options = OpenAiChatOptions.builder()
                .model(model)
                .build();
        return OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(options)
                .build();
    }

    @Bean
    @ConditionalOnMissingBean
    public OpenAiEmbeddingModel openAiEmbeddingModel(
            @Qualifier("openAiEmbeddingApi") OpenAiApi openAiEmbeddingApi,
            @Value("${spring.ai.openai.embedding.options.model}") String model) {
        var options = OpenAiEmbeddingOptions.builder()
                .model(model)
                .build();
        return new OpenAiEmbeddingModel(openAiEmbeddingApi, MetadataMode.ALL, options);
    }
}
