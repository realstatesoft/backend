package com.openroof.openroof.controller;

import com.openroof.openroof.config.SecurityConfig;
import com.openroof.openroof.dto.property.PropertyMediaResponse;
import com.openroof.openroof.model.enums.MediaType;
import com.openroof.openroof.security.JwtAuthenticationFilter;
import com.openroof.openroof.security.JwtService;
import com.openroof.openroof.service.PropertyFloorPlanService;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests de controller slice para PropertyFloorPlanController.
 *
 * Verifica el mapeo de rutas, la delegación al servicio y el formato
 * de respuesta (ApiResponse). La seguridad de los endpoints con
 * {@code @propertySecurity.canModify()} se cubre en los tests de integración.
 */
@WebMvcTest(PropertyFloorPlanController.class)
@Import({SecurityConfig.class, com.openroof.openroof.config.JacksonConfig.class, com.openroof.openroof.test.SliceSecurityBeans.class})
@DisplayName("PropertyFloorPlanController")
class PropertyFloorPlanControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private PropertyFloorPlanService         floorPlanService;
    @MockitoBean private JwtAuthenticationFilter          jwtAuthFilter;
    @MockitoBean private JwtService                       jwtService;
    @MockitoBean private UserDetailsService               userDetailsService;
    @MockitoBean private com.openroof.openroof.exception.JwtAuthenticationEntryPoint jwtEntryPoint;

    @BeforeEach
    void setupJwtPassThrough() throws Exception {
        doAnswer(inv -> {
            FilterChain chain = inv.getArgument(2);
            chain.doFilter(inv.getArgument(0), inv.getArgument(1));
            return null;
        }).when(jwtAuthFilter).doFilter(
                any(ServletRequest.class), any(ServletResponse.class), any(FilterChain.class));
    }

    private PropertyMediaResponse response(Long id) {
        return PropertyMediaResponse.builder()
                .id(id)
                .propertyId(1L)
                .url("https://storage.test/plano.pdf")
                .storageKey("properties/1/floor-plans/plano.pdf")
                .type(MediaType.FLOOR_PLAN)
                .isPrimary(false)
                .orderIndex(0)
                .title("plano.pdf")
                .build();
    }

    private MockMultipartFile pdf() {
        return new MockMultipartFile("file", "plano.pdf", "application/pdf", "pdf".getBytes());
    }

    // ─── GET /properties/{id}/floor-plans ────────────────────────────────────

    @Nested
    @DisplayName("GET /properties/{id}/floor-plans")
    class GetFloorPlans {

        @Test
        @DisplayName("Devuelve lista de planos → 200 con datos")
        void get_returns200WithList() throws Exception {
            when(floorPlanService.getByPropertyId(1L))
                    .thenReturn(List.of(response(1L), response(2L)));

            mockMvc.perform(get("/properties/1/floor-plans"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data", hasSize(2)))
                    .andExpect(jsonPath("$.data[0].type").value("FLOOR_PLAN"))
                    .andExpect(jsonPath("$.data[1].id").value(2));
        }

        @Test
        @DisplayName("Sin planos → lista vacía con 200")
        void get_noPlans_returnsEmpty() throws Exception {
            when(floorPlanService.getByPropertyId(1L)).thenReturn(List.of());

            mockMvc.perform(get("/properties/1/floor-plans"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data", hasSize(0)));
        }

        @Test
        @DisplayName("Propiedad inexistente → 404")
        void get_propertyNotFound_returns404() throws Exception {
            when(floorPlanService.getByPropertyId(99L))
                    .thenThrow(new com.openroof.openroof.exception.ResourceNotFoundException("No encontrada"));

            mockMvc.perform(get("/properties/99/floor-plans"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }

    // ─── POST /properties/floor-plans/upload ─────────────────────────────────

    @Nested
    @DisplayName("POST /properties/floor-plans/upload")
    class UploadGeneric {

        @Test
        @DisplayName("PDF válido con usuario autenticado → 200")
        void upload_authenticated_returns200() throws Exception {
            when(floorPlanService.uploadGeneric(any())).thenReturn(response(null));

            mockMvc.perform(multipart("/properties/floor-plans/upload")
                            .file(pdf())
                            .with(user("agent@test.com").roles("AGENT")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.url").value("https://storage.test/plano.pdf"))
                    .andExpect(jsonPath("$.data.type").value("FLOOR_PLAN"));

            verify(floorPlanService).uploadGeneric(any());
        }

        @Test
        @DisplayName("Servicio lanza BadRequestException → 400")
        void upload_badType_returns400() throws Exception {
            when(floorPlanService.uploadGeneric(any()))
                    .thenThrow(new com.openroof.openroof.exception.BadRequestException("Tipo no válido"));

            mockMvc.perform(multipart("/properties/floor-plans/upload")
                            .file(new MockMultipartFile("file", "v.exe", "application/octet-stream", "x".getBytes()))
                            .with(user("agent@test.com").roles("AGENT")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }
}
