package com.openroof.openroof.service;

import com.openroof.openroof.dto.register.RegisterRequest;
import com.openroof.openroof.dto.security.AuthResponse;
import com.openroof.openroof.model.enums.UserRole;
import com.openroof.openroof.model.user.User;
import com.openroof.openroof.repository.Auth.UserSessionRepository;
import com.openroof.openroof.repository.UserRepository;
import com.openroof.openroof.repository.AgentProfileRepository;
import com.openroof.openroof.model.agent.AgentProfile;
import com.openroof.openroof.security.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtService jwtService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserSessionRepository userSessionRepository;
    @Mock
    private AgentProfileRepository agentProfileRepository;
    @Mock
    private HttpServletRequest httpRequest;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                authenticationManager,
                jwtService,
                passwordEncoder,
                userRepository,
                userSessionRepository,
                agentProfileRepository
        );

        when(passwordEncoder.encode(any())).thenReturn("encoded");
        when(jwtService.generateToken(any(User.class))).thenReturn("access-token");
        when(jwtService.generateRefreshToken(any(User.class))).thenReturn("refresh-token");
        when(httpRequest.getRemoteAddr()).thenReturn("127.0.0.1");
        when(httpRequest.getHeader("X-Forwarded-For")).thenReturn(null);
        when(httpRequest.getHeader("User-Agent")).thenReturn("JUnit");
    }

    @Test
    void register_createsValidUserRoleUser() {
        RegisterRequest request = RegisterRequest.builder()
                .name("User One")
                .email("user1@test.com")
                .password("123456")
                .phone("+595981000001")
                .role("USER")
                .build();

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);

        AuthResponse response = authService.register(request, httpRequest);

        assertNotNull(response);
        assertEquals("user1@test.com", response.getEmail());
        assertEquals("USER", response.getRole());

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertEquals(UserRole.USER, captor.getValue().getRole());
    }

    @Test
    void register_createsValidUserRoleAgent() {
        RegisterRequest request = RegisterRequest.builder()
                .name("Agent One")
                .email("agent1@test.com")
                .password("123456")
                .phone("+595981000002")
                .role("AGENT")
                .build();

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);

        AuthResponse response = authService.register(request, httpRequest);

        assertNotNull(response);
        assertEquals("agent1@test.com", response.getEmail());
        assertEquals("AGENT", response.getRole());

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertEquals(UserRole.AGENT, captor.getValue().getRole());
        ArgumentCaptor<AgentProfile> profileCaptor = ArgumentCaptor.forClass(AgentProfile.class);
        verify(agentProfileRepository).save(profileCaptor.capture());
        assertEquals(captor.getValue(), profileCaptor.getValue().getUser());
    }
}
