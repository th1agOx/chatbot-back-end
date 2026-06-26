package br.com.chatbot.chatbot_api.service.impl;

import br.com.chatbot.chatbot_api.entity.Conversation;
import br.com.chatbot.chatbot_api.exception.InvalidFileTypeException;
import br.com.chatbot.chatbot_api.mapper.EntityMapper;
import br.com.chatbot.chatbot_api.repository.AttachmentRepository;
import br.com.chatbot.chatbot_api.service.ConversationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.unit.DataSize;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileServiceImplTest {

    @Mock
    private AttachmentRepository attachmentRepository;

    @Mock
    private ConversationService conversationService;

    @Mock
    private EntityMapper entityMapper;

    @InjectMocks
    private FileServiceImpl fileService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(fileService, "maxFileSize", DataSize.ofMegabytes(10));
    }

    @Test
    void upload_WithValidTxtFile_ShouldSucceed() {
        var conversation = new Conversation();
        conversation.setId(1L);

        when(conversationService.findConversationOrThrow(1L)).thenReturn(conversation);
        when(attachmentRepository.save(any())).thenAnswer(invocation -> {
            var att = invocation.getArgument(0, br.com.chatbot.chatbot_api.entity.Attachment.class);
            att.setId(1L);
            return att;
        });
        when(entityMapper.toAttachmentResponse(any())).thenAnswer(invocation -> {
            var att = invocation.getArgument(0, br.com.chatbot.chatbot_api.entity.Attachment.class);
            return new br.com.chatbot.chatbot_api.dto.response.AttachmentResponse(
                    att.getId(), att.getFileName(), att.getContentType(), att.getSize(), att.getUploadDate());
        });

        var file = new MockMultipartFile(
                "file", "test.txt", "text/plain", "Hello World".getBytes());

        var response = fileService.upload(1L, file);
        assertNotNull(response);
    }

    @Test
    void upload_WithInvalidFileType_ShouldThrow() {
        var file = new MockMultipartFile(
                "file", "test.exe", "application/x-msdownload", "data".getBytes());

        assertThrows(InvalidFileTypeException.class, () -> fileService.upload(1L, file));
    }
}
