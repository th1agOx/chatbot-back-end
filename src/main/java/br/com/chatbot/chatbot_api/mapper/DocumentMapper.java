package br.com.chatbot.chatbot_api.mapper;

import br.com.chatbot.chatbot_api.dto.response.DocumentResponse;
import br.com.chatbot.chatbot_api.entity.Document;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface DocumentMapper {

    @Mapping(target = "chunkCount", expression = "java(document.getChunks() != null ? document.getChunks().size() : 0)")
    DocumentResponse toDocumentResponse(Document document);
}
