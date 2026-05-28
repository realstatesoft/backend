package com.openroof.openroof.service;

import com.openroof.openroof.exception.BadRequestException;
import com.openroof.openroof.model.enums.DocumentType;
import com.openroof.openroof.model.user.User;
import com.openroof.openroof.repository.UserDocumentRepository;
import com.openroof.openroof.repository.UserRepository;
import com.openroof.openroof.upload.FileUploadValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static com.openroof.openroof.upload.UploadTestFixtures.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserDocumentService upload validation")
class UserDocumentServiceTest {

    @Mock private UserDocumentRepository documentRepository;
    @Mock private UserRepository userRepository;
    @Mock private StorageService storageService;
    @Mock private EmailService emailService;

    @InjectMocks
    private UserDocumentService userDocumentService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(userDocumentService, "maxFileSizeRaw", "10MB");
        ReflectionTestUtils.setField(userDocumentService, "allowedTypesRaw",
                "application/pdf,image/jpeg,image/png,image/webp");
        ReflectionTestUtils.setField(userDocumentService, "fileUploadValidator",
                new FileUploadValidator());
        userDocumentService.initConfig();
    }

    @Test
    void upload_rejectsEmptyFile() {
        assertThrows(BadRequestException.class, () ->
                userDocumentService.upload("u@test.com", emptyPdf(), DocumentType.ID_FRONT));
        verify(storageService, never()).upload(any(), any());
        verifyNoInteractions(userRepository);
    }

    @Test
    void upload_rejectsSpoofedExtension() {
        assertThrows(BadRequestException.class, () ->
                userDocumentService.upload("u@test.com", spoofedPdfAsJpg(), DocumentType.ID_FRONT));
        verify(storageService, never()).upload(any(), any());
        verifyNoInteractions(userRepository);
    }

    @Test
    void upload_acceptsValidPdf() {
        try (var mockedStatic = org.mockito.Mockito.mockStatic(
                org.springframework.transaction.support.TransactionSynchronizationManager.class)) {
            mockedStatic.when(org.springframework.transaction.support.TransactionSynchronizationManager::isSynchronizationActive)
                    .thenReturn(true);

            User user = User.builder().email("u@test.com").build();
            user.setId(1L);
            when(userRepository.findByEmail("u@test.com")).thenReturn(Optional.of(user));
            when(storageService.upload(any(), anyString())).thenReturn(
                    new StorageService.UploadResult("https://x/doc.pdf", "documents/1/u.pdf", 100L, "application/pdf"));
            when(documentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            userDocumentService.upload("u@test.com", pdf("dni.pdf"), DocumentType.ID_FRONT);

            verify(storageService).upload(any(), eq("documents/1"));
        }
    }
}
