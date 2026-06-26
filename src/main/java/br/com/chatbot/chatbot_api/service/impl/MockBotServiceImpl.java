package br.com.chatbot.chatbot_api.service.impl;

import br.com.chatbot.chatbot_api.service.BotService;
import org.springframework.stereotype.Service;

@Service
public class MockBotServiceImpl implements BotService {
    @Override
    public String responseGenerate(String userMessage) {
        return "Você disse: " + userMessage;
    }
}
