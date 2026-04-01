package com.openroof.openroof.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.openroof.openroof.config.SecurityConfig;
import com.openroof.openroof.dto.notification.CreateNotificationRequest;
import com.openroof.openroof.dto.notification.NotificationResponse;
import com.openroof.openroof.exception.ForbiddenException;
import com.openroof.openroof.exception.JwtAuthenticationEntryPoint;
import com.openroof.openroof.model.enums.NotificationType;
import com.openroof.openroof.security.JwtAuthenticationFilter;
import com.openroof.openroof.security.JwtService;
import com.openroof.openroof.service.NotificationService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = NotificationController.class,
        properties = "spring.config.location=classpath:/notification-controller-test.yml"
)
@Import(SecurityConfig.class)
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @MockitoBean
    private NotificationService notificationService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthFilter;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @MockitoBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    private static final String BASE = "/notifications";

    @BeforeEach
    void setupJwtFilterPassThrough() throws Exception {
        doAnswer(invocation -> {
            ServletRequest req = invocation.getArgument(0);
            ServletResponse res = invocation.getArgument(1);
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(req, res);
            return null;
        }).when(jwtAuthFilter).doFilter(
                any(ServletRequest.class), any(ServletResponse.class), any(FilterChain.class));
    }

    @Nested
    @DisplayName("POST /notifications")
    class CreateTests {

        @Test
        @DisplayName("Crea una notificación para el usuario actual")
        void create_returns201() throws Exception {
            CreateNotificationRequest request = new CreateNotificationRequest(
                    null,
                    NotificationType.VISIT,
                    "Nueva visita",
                    "Tenés una solicitud pendiente",
                    Map.of("visitRequestId", 8),
                    "/visit-requests"
            );

            when(notificationService.create(any(CreateNotificationRequest.class), eq("user@test.com")))
                    .thenReturn(sampleResponse(1L));

            mockMvc.perform(post(BASE)
                            .with(user("user@test.com").roles("USER"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(1))
                    .andExpect(jsonPath("$.data.type").value("VISIT"));
        }

        @Test
        @DisplayName("Devuelve 403 si el servicio rechaza crear para otro usuario")
        void createForAnotherUser_returns403() throws Exception {
            CreateNotificationRequest request = new CreateNotificationRequest(
                    44L,
                    NotificationType.ALERT,
                    "Recordatorio",
                    "No podés crear para otro usuario",
                    null,
                    null
            );

            when(notificationService.create(any(CreateNotificationRequest.class), eq("user@test.com")))
                    .thenThrow(new ForbiddenException("Solo un ADMIN puede crear notificaciones para otros usuarios"));

            mockMvc.perform(post(BASE)
                            .with(user("user@test.com").roles("USER"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message", containsString("ADMIN")));
        }
    }

    @Nested
    @DisplayName("GET /notifications/me")
    class GetMyNotificationsTests {

        @Test
        @DisplayName("Lista las notificaciones del usuario autenticado")
        void getMyNotifications_returns200() throws Exception {
            when(notificationService.getMyNotifications("user@test.com"))
                    .thenReturn(List.of(sampleResponse(1L), sampleResponse(2L)));

            mockMvc.perform(get(BASE + "/me")
                            .with(user("user@test.com").roles("USER")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data", hasSize(2)));
        }
    }

    @Nested
    @DisplayName("GET /notifications/{id}")
    class GetByIdTests {

        @Test
        @DisplayName("Obtiene una notificación propia por ID")
        void getById_returns200() throws Exception {
            when(notificationService.getByIdForCurrentUser(1L, "user@test.com"))
                    .thenReturn(sampleResponse(1L));

            mockMvc.perform(get(BASE + "/1")
                            .with(user("user@test.com").roles("USER")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.id").value(1));
        }
    }

    @Nested
    @DisplayName("PUT /notifications/{id}/read")
    class MarkAsReadTests {

        @Test
        @DisplayName("Marca una notificación como leída")
        void markAsRead_returns200() throws Exception {
            NotificationResponse response = new NotificationResponse(
                    1L,
                    10L,
                    NotificationType.VISIT,
                    "Nueva visita",
                    "Tenés una solicitud pendiente",
                    Map.of("visitRequestId", 8),
                    "/visit-requests",
                    LocalDateTime.of(2026, 3, 29, 14, 0),
                    true,
                    LocalDateTime.of(2026, 3, 29, 13, 0),
                    LocalDateTime.of(2026, 3, 29, 14, 0)
            );
            when(notificationService.markAsRead(1L, "user@test.com")).thenReturn(response);

            mockMvc.perform(put(BASE + "/1/read")
                            .with(user("user@test.com").roles("USER")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.read").value(true));
        }
    }

    @Nested
    @DisplayName("PUT /notifications/me/read-all")
    class MarkAllAsReadTests {

        @Test
        @DisplayName("Marca todas las notificaciones como leídas")
        void markAllAsRead_returns200() throws Exception {
            when(notificationService.markAllAsRead("user@test.com")).thenReturn(5L);

            mockMvc.perform(put(BASE + "/me/read-all")
                            .with(user("user@test.com").roles("USER")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").value(5));
        }
    }

    @Nested
    @DisplayName("GET /notifications/me/unread-count")
    class GetUnreadCountTests {

        @Test
        @DisplayName("Retorna la cantidad de no leídas")
        void getUnreadCount_returns200() throws Exception {
            when(notificationService.getUnreadCount("user@test.com")).thenReturn(3L);

            mockMvc.perform(get(BASE + "/me/unread-count")
                            .with(user("user@test.com").roles("USER")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").value(3));
        }
    }

    @Nested
    @DisplayName("DELETE /notifications/{id}")
    class DeleteTests {

        @Test
        @DisplayName("Elimina una notificación propia")
        void delete_returns204() throws Exception {
            doNothing().when(notificationService).delete(1L, "user@test.com");

            mockMvc.perform(delete(BASE + "/1")
                            .with(user("user@test.com").roles("USER")))
                    .andExpect(status().isNoContent());
        }
    }

    private NotificationResponse sampleResponse(Long id) {
        return new NotificationResponse(
                id,
                10L,
                NotificationType.VISIT,
                "Nueva visita",
                "Tenés una solicitud pendiente",
                Map.of("visitRequestId", 8),
                "/visit-requests",
                null,
                false,
                LocalDateTime.of(2026, 3, 29, 13, 0),
                LocalDateTime.of(2026, 3, 29, 13, 0)
        );
    }
}
