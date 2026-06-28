package br.com.chatbot.chatbot_api.service.parser;

import java.io.InputStream;

public interface DocumentParser {

    String parse(InputStream inputStream);
}
