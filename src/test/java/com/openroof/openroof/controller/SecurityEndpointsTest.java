package com.openroof.openroof.controller;

import com.openroof.openroof.config.SecurityConfig;
import com.openroof.openroof.repository.ImageRepository;
import com.openroof.openroof.security.JwtAuthenticationFilter;
import com.openroof.openroof.security.PropertyViewRateLimitingFilter;
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
@Import({SecurityConfig.class, com.openroof.openroof.config.JacksonConfig.class})
class SecurityEndpointsTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private org.springframework.web.context.WebApplicationContext context;

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
    private PropertyViewRateLimitingFilter propertyViewRateLimitingFilter;
    @MockitoBean
    private UserDetailsService userDetailsService;
    @MockitoBean
    private com.openroof.openroof.exception.JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

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

        doAnswer(invocation -> {
            ServletRequest request = invocation.getArgument(0);
            ServletResponse response = invocation.getArgument(1);
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(request, response);
            return null;
        }).when(propertyViewRateLimitingFilter).doFilter(any(), any(), any());

        doAnswer(invocation -> {
            jakarta.servlet.http.HttpServletResponse res = invocation.getArgument(1);
            res.setStatus(401);
            return null;
        }).when(jwtAuthenticationEntryPoint).commence(any(), any(), any());

                // Ensure security filters are registered with MockMvc in this @WebMvcTest slice
                mockMvc = org.springframework.test.web.servlet.setup.MockMvcBuilders
                                .webAppContextSetup(context)
                                .apply(org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity())
                                .build();
    }

    @Test
    void authProtectedEndpoints_requireLogin() throws Exception {
        mockMvc.perform(post("/auth/logout")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/auth/logout-all")).andExpect(status().isUnauthorized());
    }

    @Test
    void userEndpoints_requireLogin() throws Exception {
        mockMvc.perform(get("/users/me")).andExpect(status().isUnauthorized());

        mockMvc.perform(put("/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"John Doe\"}"))
                .andExpect(status().isUnauthorized());
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
                .andExpect(status().isUnauthorized());

        mockMvc.perform(put("/properties/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(delete("/properties/1"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(patch("/properties/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newStatus\":\"PUBLISHED\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void propertyViewEndpoint_isPublic() throws Exception {
        org.mockito.Mockito.when(propertyService.registerView(org.mockito.ArgumentMatchers.eq(1L), any(), any()))
                .thenReturn(3L);
        org.mockito.Mockito.when(propertyService.getViewCount(org.mockito.ArgumentMatchers.eq(1L)))
                .thenReturn(5L);

        mockMvc.perform(post("/properties/1/views"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.success").value(true))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.data").value(3));

        mockMvc.perform(get("/properties/1/views/count"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.success").value(true))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.data").value(5));
    }

    @Test
    void propertyImageWriteEndpoints_requireLogin() throws Exception {
        mockMvc.perform(multipartPost("/properties/1/images"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(patch("/properties/1/images/10/primary"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(delete("/properties/1/images/10"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void imageUpload_requiresLogin() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.multipart("/images/upload"))
                .andExpect(status().isUnauthorized());
    }

    private MockMultipartHttpServletRequestBuilder multipartPost(String url) {
        return MockMvcRequestBuilders.multipart(url)
                .file("files", "dummy".getBytes())
                .contentType(MediaType.MULTIPART_FORM_DATA);
    }
}
