package br.com.chatbot.chatbot_api.service.parser;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.InputStream;

@Component
public class PdfParser implements DocumentParser {

    @Override
    public String parse(InputStream inputStream) {
        try (var document = Loader.loadPDF(inputStream.readAllBytes())) {
            var stripper = new PDFTextStripper();
            return stripper.getText(document);
        } catch (Exception e) {
            throw new RuntimeException("Falha ao processar PDF: " + e.getMessage(), e);
        }
    }
}
