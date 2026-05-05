package com.openroof.openroof.service;

import com.openroof.openroof.dto.register.RegisterRequest;
import com.openroof.openroof.dto.register.AgentSignupRequest;
import com.openroof.openroof.dto.security.AuthResponse;
import com.openroof.openroof.model.enums.UserRole;
import com.openroof.openroof.model.user.User;
import com.openroof.openroof.model.user.UserSession;
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
    private EmailService emailService;
    @Mock
    private AuditService auditService;
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
                agentProfileRepository,
                emailService,
                auditService
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

    @Test
    void register_fallsBackToRemoteAddrWhenForwardedIpIsInvalid() {
        RegisterRequest request = RegisterRequest.builder()
                .name("User Two")
                .email("user2@test.com")
                .password("123456")
                .phone("+595981000003")
                .role("USER")
                .build();

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(httpRequest.getRemoteAddr()).thenReturn("127.0.0.1");
        when(httpRequest.getHeader("X-Forwarded-For"))
                .thenReturn("1234567890123456789012345678901234567890123456, 10.0.0.2");

        authService.register(request, httpRequest);

        ArgumentCaptor<UserSession> sessionCaptor = ArgumentCaptor.forClass(UserSession.class);
        verify(userSessionRepository).save(sessionCaptor.capture());
        assertEquals("127.0.0.1", sessionCaptor.getValue().getRequestMetadata().getIpAddress());
    }

    @Test
    void registerAgent_createsAgentUserWithAdditionalFields() {
        AgentSignupRequest request = AgentSignupRequest.builder()
                .name("Agent Professional")
                .email("agent.pro@test.com")
                .password("123456")
                .phone("+595981000004")
                .companyName("Pro Real Estate")
                .licenseNumber("LIC-2024-001")
                .experienceYears(5)
                .build();

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);

        AuthResponse response = authService.registerAgent(request, httpRequest);

        assertNotNull(response);
        assertEquals("agent.pro@test.com", response.getEmail());
        assertEquals("AGENT", response.getRole());

        // Verificar que se guarde el usuario con role AGENT
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertEquals(UserRole.AGENT, savedUser.getRole());
        assertEquals("Agent Professional", savedUser.getName());
        assertEquals("agent.pro@test.com", savedUser.getEmail());
        assertEquals("+595981000004", savedUser.getPhone());

        // Verificar que se cree el AgentProfile con campos adicionales
        ArgumentCaptor<AgentProfile> profileCaptor = ArgumentCaptor.forClass(AgentProfile.class);
        verify(agentProfileRepository).save(profileCaptor.capture());
        AgentProfile savedProfile = profileCaptor.getValue();
        assertEquals(savedUser, savedProfile.getUser());
        assertEquals("Pro Real Estate", savedProfile.getCompanyName());
        assertEquals("LIC-2024-001", savedProfile.getLicenseNumber());
        assertEquals(5, savedProfile.getExperienceYears());
    }

    @Test
    void registerAgent_createsAgentUserWithMinimalFields() {
        AgentSignupRequest request = AgentSignupRequest.builder()
                .name("Simple Agent")
                .email("simple@test.com")
                .password("123456")
                .phone("+595981000005")
                // Sin campos adicionales opcionales
                .build();

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);

        AuthResponse response = authService.registerAgent(request, httpRequest);

        assertNotNull(response);
        assertEquals("simple@test.com", response.getEmail());
        assertEquals("AGENT", response.getRole());

        // Verificar que se guarde el usuario
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertEquals(UserRole.AGENT, userCaptor.getValue().getRole());

        // Verificar que se cree el AgentProfile básico
        ArgumentCaptor<AgentProfile> profileCaptor = ArgumentCaptor.forClass(AgentProfile.class);
        verify(agentProfileRepository).save(profileCaptor.capture());
        AgentProfile savedProfile = profileCaptor.getValue();
        assertNotNull(savedProfile.getUser());
        // Los campos opcionales deben ser null/default
    }
}
