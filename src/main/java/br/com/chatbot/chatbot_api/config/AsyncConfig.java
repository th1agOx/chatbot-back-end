package br.com.chatbot.chatbot_api.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplateBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .readTimeout(Duration.ofSeconds(5))
                .build();
    }
}
