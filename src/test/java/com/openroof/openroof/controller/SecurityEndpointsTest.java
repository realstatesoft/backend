package com.openroof.openroof.controller;

import com.openroof.openroof.config.SecurityConfig;
import com.openroof.openroof.repository.ImageRepository;
import com.openroof.openroof.security.JwtAuthenticationFilter;
import com.openroof.openroof.security.PropertySecurity;
import com.openroof.openroof.service.AuthService;
import com.openroof.openroof.service.PropertyImageService;
import com.openroof.openroof.service.PropertyService;
import com.openroof.openroof.service.StorageService;
import com.openroof.openroof.service.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {
        AuthController.class,
        UserController.class,
        PropertyController.class,
        PropertyImageController.class,
        ImageUploadController.class
})
@Import(SecurityConfig.class)
class SecurityEndpointsTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;
    @MockitoBean
    private UserService userService;
    @MockitoBean
    private PropertyService propertyService;
    @MockitoBean
    private PropertyImageService propertyImageService;
    @MockitoBean
    private StorageService storageService;
    @MockitoBean
    private ImageRepository imageRepository;

    @MockitoBean
    private PropertySecurity propertySecurity;
    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockitoBean
    private UserDetailsService userDetailsService;

    @BeforeEach
    void setUp() throws Exception {
        // Keep security filters active while making the mocked JWT filter transparent.
        doAnswer(invocation -> {
            ServletRequest request = invocation.getArgument(0);
            ServletResponse response = invocation.getArgument(1);
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(request, response);
            return null;
        }).when(jwtAuthenticationFilter).doFilter(any(), any(), any());
    }

    @Test
    void authProtectedEndpoints_requireLogin() throws Exception {
        mockMvc.perform(post("/auth/logout")).andExpect(status().isForbidden());
        mockMvc.perform(post("/auth/logout-all")).andExpect(status().isForbidden());
    }

    @Test
    void userEndpoints_requireLogin() throws Exception {
        mockMvc.perform(get("/users/me")).andExpect(status().isForbidden());

        mockMvc.perform(put("/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"John Doe\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void propertyWriteEndpoints_requireLogin() throws Exception {
        String createPayload = """
                {
                  "title": "Casa test",
                  "propertyType": "HOUSE",
                  "address": "Calle 123",
                  "price": 100000,
                  "ownerId": 1
                }
                """;

        mockMvc.perform(post("/properties")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPayload))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/properties/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/properties/1"))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/properties/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newStatus\":\"PUBLISHED\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void propertyImageWriteEndpoints_requireLogin() throws Exception {
        mockMvc.perform(multipartPost("/properties/1/images"))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/properties/1/images/10/primary"))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/properties/1/images/10"))
                .andExpect(status().isForbidden());
    }

    @Test
    void imageUpload_requiresLogin() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.multipart("/images/upload"))
                .andExpect(status().isForbidden());
    }

    private MockMultipartHttpServletRequestBuilder multipartPost(String url) {
        return MockMvcRequestBuilders.multipart(url)
                .file("files", "dummy".getBytes())
                .contentType(MediaType.MULTIPART_FORM_DATA);
    }
}
