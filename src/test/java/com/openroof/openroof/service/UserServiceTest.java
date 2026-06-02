package com.openroof.openroof.service;

import com.openroof.openroof.dto.user.UpdateUserRequest;
import com.openroof.openroof.dto.user.UserProfileResponse;
import com.openroof.openroof.exception.BadRequestException;
import com.openroof.openroof.exception.ResourceNotFoundException;
import com.openroof.openroof.model.user.User;
import com.openroof.openroof.repository.UserRepository;
import com.openroof.openroof.service.StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Tests")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private StorageService storageService;

    @InjectMocks
    private UserService userService;

    private User mockUser;

    @BeforeEach
    void setUp() {
        mockUser = new User();
        mockUser.setEmail("test@openroof.com");
        mockUser.setName("Juan Pérez");
        mockUser.setPhone("+595981000000");
        mockUser.setAvatarUrl("https://example.com/avatar.jpg");
    }

    // ─── getProfile ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("getProfile: retorna perfil cuando el usuario existe")
    void getProfile_shouldReturnProfile_whenUserExists() {
        when(userRepository.findByEmail("test@openroof.com"))
                .thenReturn(Optional.of(mockUser));

        UserProfileResponse response = userService.getProfile("test@openroof.com");

        assertThat(response).isNotNull();
        assertThat(response.getEmail()).isEqualTo("test@openroof.com");
        assertThat(response.getName()).isEqualTo("Juan Pérez");

        verify(userRepository, times(1)).findByEmail("test@openroof.com");
    }

    @Test
    @DisplayName("getProfile: lanza ResourceNotFoundException cuando el usuario no existe")
    void getProfile_shouldThrowException_whenUserNotFound() {
        when(userRepository.findByEmail("noexiste@openroof.com"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getProfile("noexiste@openroof.com"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("noexiste@openroof.com");

        verify(userRepository, times(1)).findByEmail("noexiste@openroof.com");
    }

    // ─── updatePersonalData ───────────────────────────────────────────────────

    @Test
    @DisplayName("updatePersonalData: actualiza todos los campos cuando todos vienen en el request")
    void updatePersonalData_shouldUpdateAllFields_whenAllFieldsProvided() {
        when(userRepository.findByEmail("test@openroof.com"))
                .thenReturn(Optional.of(mockUser));
        when(userRepository.save(any(User.class)))
                .thenReturn(mockUser);

        UpdateUserRequest request = new UpdateUserRequest();
        request.setName("María García");
        request.setPhone("+595982111111");
        request.setAvatarUrl("https://example.com/new-avatar.jpg");

        UserProfileResponse response = userService.updatePersonalData("test@openroof.com", request);

        assertThat(response).isNotNull();
        assertThat(mockUser.getName()).isEqualTo("María García");
        assertThat(mockUser.getPhone()).isEqualTo("+595982111111");
        assertThat(mockUser.getAvatarUrl()).isEqualTo("https://example.com/new-avatar.jpg");

        verify(userRepository, times(1)).save(mockUser);
    }

    @Test
    @DisplayName("updatePersonalData: no sobreescribe campos cuando vienen null")
    void updatePersonalData_shouldNotOverwrite_whenFieldsAreNull() {
        when(userRepository.findByEmail("test@openroof.com"))
                .thenReturn(Optional.of(mockUser));
        when(userRepository.save(any(User.class)))
                .thenReturn(mockUser);

        UpdateUserRequest request = new UpdateUserRequest();
        // Todos los campos null — no debe cambiar nada

        userService.updatePersonalData("test@openroof.com", request);

        // Los valores originales se mantienen
        assertThat(mockUser.getName()).isEqualTo("Juan Pérez");
        assertThat(mockUser.getPhone()).isEqualTo("+595981000000");
        assertThat(mockUser.getAvatarUrl()).isEqualTo("https://example.com/avatar.jpg");

        verify(userRepository, times(1)).save(mockUser);
    }

    @Test
    @DisplayName("updatePersonalData: actualiza solo el nombre cuando solo ese campo viene")
    void updatePersonalData_shouldUpdateOnlyName_whenOnlyNameProvided() {
        when(userRepository.findByEmail("test@openroof.com"))
                .thenReturn(Optional.of(mockUser));
        when(userRepository.save(any(User.class)))
                .thenReturn(mockUser);

        UpdateUserRequest request = new UpdateUserRequest();
        request.setName("Nuevo Nombre");

        userService.updatePersonalData("test@openroof.com", request);

        assertThat(mockUser.getName()).isEqualTo("Nuevo Nombre");
        assertThat(mockUser.getPhone()).isEqualTo("+595981000000"); // sin cambio
        assertThat(mockUser.getAvatarUrl()).isEqualTo("https://example.com/avatar.jpg"); // sin cambio
    }

    @Test
    @DisplayName("updatePersonalData: lanza excepción cuando el usuario no existe")
    void updatePersonalData_shouldThrowException_whenUserNotFound() {
        when(userRepository.findByEmail("fantasma@openroof.com"))
                .thenReturn(Optional.empty());

        UpdateUserRequest request = new UpdateUserRequest();
        request.setName("Cualquier nombre");

        assertThatThrownBy(() ->
                userService.updatePersonalData("fantasma@openroof.com", request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("fantasma@openroof.com");

        verify(userRepository, never()).save(any());
    }

    // ─── uploadAvatar ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("uploadAvatar: llama al storage, actualiza URL y retorna perfil")
    void uploadAvatar_shouldUploadAndUpdateAvatarUrl() {
        when(userRepository.findByEmail("test@openroof.com"))
                .thenReturn(Optional.of(mockUser));
        when(storageService.upload(any(), eq("avatars")))
                .thenReturn(new StorageService.UploadResult("https://storage.example.com/new.jpg", "avatars/new.jpg", 100L, "image/jpeg"));
        when(userRepository.save(any(User.class))).thenReturn(mockUser);

        MockMultipartFile file = new MockMultipartFile(
                "avatar", "photo.jpg", "image/jpeg", new byte[100]
        );

        UserProfileResponse response = userService.uploadAvatar("test@openroof.com", file);

        assertThat(mockUser.getAvatarUrl()).isEqualTo("https://storage.example.com/new.jpg");
        verify(storageService).upload(file, "avatars");
        verify(userRepository).save(mockUser);
        assertThat(response).isNotNull();
    }

    @Test
    @DisplayName("uploadAvatar: lanza BadRequestException cuando el archivo está vacío")
    void uploadAvatar_shouldThrow_whenFileIsEmpty() {
        MockMultipartFile empty = new MockMultipartFile(
                "avatar", "photo.jpg", "image/jpeg", new byte[0]
        );

        assertThatThrownBy(() -> userService.uploadAvatar("test@openroof.com", empty))
                .isInstanceOf(BadRequestException.class);

        verifyNoInteractions(storageService);
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("uploadAvatar: lanza BadRequestException cuando el tipo MIME no está permitido")
    void uploadAvatar_shouldThrow_whenContentTypeNotAllowed() {
        MockMultipartFile pdf = new MockMultipartFile(
                "avatar", "doc.pdf", "application/pdf", new byte[100]
        );

        assertThatThrownBy(() -> userService.uploadAvatar("test@openroof.com", pdf))
                .isInstanceOf(BadRequestException.class);

        verifyNoInteractions(storageService);
    }

    @Test
    @DisplayName("uploadAvatar: lanza BadRequestException cuando el archivo supera 5 MB")
    void uploadAvatar_shouldThrow_whenFileTooLarge() {
        byte[] big = new byte[6 * 1024 * 1024]; // 6 MB
        MockMultipartFile file = new MockMultipartFile(
                "avatar", "big.jpg", "image/jpeg", big
        );

        assertThatThrownBy(() -> userService.uploadAvatar("test@openroof.com", file))
                .isInstanceOf(BadRequestException.class);

        verifyNoInteractions(storageService);
    }

    @Test
    @DisplayName("uploadAvatar: no llama al storage cuando el usuario no existe")
    void uploadAvatar_shouldThrow_whenUserNotFound() {
        when(userRepository.findByEmail("noexiste@openroof.com"))
                .thenReturn(Optional.empty());

        MockMultipartFile file = new MockMultipartFile(
                "avatar", "photo.png", "image/png", new byte[100]
        );

        assertThatThrownBy(() -> userService.uploadAvatar("noexiste@openroof.com", file))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(storageService, never()).upload(any(), any());
        verify(userRepository, never()).save(any());
    }
}