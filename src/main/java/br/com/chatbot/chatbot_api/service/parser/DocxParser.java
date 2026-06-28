package br.com.chatbot.chatbot_api.service.parser;

import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Component;

import java.io.InputStream;

@Component
public class DocxParser implements DocumentParser {

    @Override
    public String parse(InputStream inputStream) {
        try (var document = new XWPFDocument(inputStream);
             var extractor = new XWPFWordExtractor(document)) {
            return extractor.getText();
        } catch (Exception e) {
            throw new RuntimeException("Falha ao processar DOCX: " + e.getMessage(), e);
        }
    }
}
