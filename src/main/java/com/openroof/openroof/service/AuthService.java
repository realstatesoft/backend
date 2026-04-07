package com.openroof.openroof.service;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.openroof.openroof.common.embeddable.RequestMetadata;
import com.openroof.openroof.dto.register.RegisterRequest;
import com.openroof.openroof.dto.security.AuthResponse;
import com.openroof.openroof.dto.security.LoginRequest;
import com.openroof.openroof.exception.BadRequestException;
import com.openroof.openroof.exception.ResourceNotFoundException;
import com.openroof.openroof.model.enums.UserRole;
import com.openroof.openroof.model.user.User;
import com.openroof.openroof.model.user.UserSession;
import com.openroof.openroof.repository.UserRepository;
import com.openroof.openroof.repository.Auth.UserSessionRepository;
import com.openroof.openroof.repository.AgentProfileRepository;
import com.openroof.openroof.model.agent.AgentProfile;
import com.openroof.openroof.security.JwtService;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import java.time.LocalDateTime;

/* * Auth: Enrique Rios
 * Desc: Servicio central de autenticación que gestiona el ciclo de vida de usuarios y sus sesiones.
 */
@Service
@RequiredArgsConstructor
public class AuthService {
        private final AuthenticationManager authenticationManager;
        private final JwtService jwtService;
        private final PasswordEncoder passwordEncoder;
        private final UserRepository userRepository;
        private final UserSessionRepository userSessionRepository;
        private final AgentProfileRepository agentProfileRepository;

        /*
         * * Desc: Autentica al usuario y crea una sesión persistente con Refresh Token.
         */
        @Transactional
        public AuthResponse login(LoginRequest request, HttpServletRequest httpRequest) {
                // Autenticar.
                var auth = authenticationManager.authenticate(
                                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

                var user = (User) auth.getPrincipal();

                return generateFullAuthResponse(user, httpRequest);
        }

        /* * Desc: Registra un nuevo usuario y crea su sesión inicial. */
        @Transactional
        public AuthResponse register(RegisterRequest request, HttpServletRequest httpRequest) {
                if (userRepository.existsByEmail(request.getEmail())) {
                        throw new BadRequestException("El email ya está registrado");
                }

                UserRole selectedRole = UserRole.USER;
                if (request.getRole() != null) {
                        String roleName = request.getRole().toUpperCase();
                        java.util.Set<UserRole> allowedRoles = java.util.Set.of(UserRole.USER, UserRole.AGENT);
                        try {
                                UserRole parsed = UserRole.valueOf(roleName);
                                if (!allowedRoles.contains(parsed)) {
                                        throw new BadRequestException("Rol inválido: " + request.getRole());
                                }
                                selectedRole = parsed;
                        } catch (IllegalArgumentException e) {
                                throw new BadRequestException("Rol inválido: " + request.getRole());
                        }
                }

                var user = User.builder()
                                .email(request.getEmail())
                                .passwordHash(passwordEncoder.encode(request.getPassword()))
                                .name(request.getName())
                                .phone(request.getPhone())
                                .role(selectedRole)
                                .build();

                userRepository.save(user);

                if (selectedRole == UserRole.AGENT) {
                        AgentProfile agentProfile = AgentProfile.builder()
                                        .user(user)
                                        .build();
                        agentProfileRepository.save(agentProfile);
                }

                return generateFullAuthResponse(user, httpRequest);
        }

        /*
         * * Desc: Valida el Refresh Token contra la DB. Si es válido, genera un nuevo
         * Access Token.
         * Este flujo permite invalidar sesiones eliminando el registro de la tabla
         * user_sessions.
         */
        @Transactional
        public AuthResponse refreshToken(String oldRefreshToken, HttpServletRequest httpRequest) {

                // 1. Buscar y bloquear la sesión en una sola transacción
                var session = userSessionRepository.findByTokenHashForUpdate(oldRefreshToken)
                                .orElseThrow(() -> new BadRequestException("Sesión inválida o ya utilizada"));

                // 2. Obtener el usuario desde la sesión bloqueada
                var user = session.getUser();

                // 3. Validar el refresh token
                if (!jwtService.isTokenValid(oldRefreshToken, user)) {
                        userSessionRepository.delete(session); // limpiamos si expiró o es inválido
                        throw new BadRequestException("El token de refresco ha expirado");
                }

                // 4. Consumir la sesión actual
                userSessionRepository.delete(session);

                // 5. Emitir nuevos tokens y guardar nueva sesión
                return generateFullAuthResponse(user, httpRequest);
        }

        private AuthResponse generateFullAuthResponse(User user, HttpServletRequest httpRequest) {
                String accessToken = jwtService.generateToken(user);
                String refreshToken = jwtService.generateRefreshToken(user);

                // Aquí es donde realmente ocurre la magia de guardar la sesión
                saveUserSession(user, refreshToken, httpRequest);

                Long agentProfileId = agentProfileRepository.findByUser_Id(user.getId())
                                .map(ap -> ap.getId())
                                .orElse(null);

                return AuthResponse.builder()
                                .id(user.getId())
                                .accessToken(accessToken)
                                .refreshToken(refreshToken)
                                .email(user.getEmail())
                                .role(user.getRole().name())
                                .agentProfileId(agentProfileId)
                                .build();
        }

        /*
         * * Desc: Invalida la sesión actual eliminando el registro del Refresh Token de
         * la DB.
         */
        @Transactional
        public void logout(String authHeader) {
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                        String refreshToken = authHeader.substring(7);
                        userSessionRepository.deleteByTokenHash(refreshToken);
                }
        }

        /*
         * * Auth: Enrique Rios
         * Desc: Cierra todas las sesiones activas del usuario.
         * Elimina todos los Refresh Tokens asociados al usuario en la base de datos,
         * forzando un nuevo inicio de sesión en todos sus dispositivos.
         */
        @Transactional
        public void logoutAllSessions(String email) {
                var user = userRepository.findByEmail(email)
                                .orElseThrow(() -> new ResourceNotFoundException("Usuario", "email", email));

                userSessionRepository.deleteByUser(user);
        }

        /*
         * * Desc: Método privado para persistir la sesión.
         * Implementa la lógica de "vincular" el token físico con el usuario en la DB.
         */
        private void saveUserSession(User user, String refreshToken, HttpServletRequest request) {
                // Extraer IP
                String ipAddress = request.getRemoteAddr();

                // Proxy la IP
                String xForwardedFor = request.getHeader("X-Forwarded-For");
                if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                        ipAddress = xForwardedFor.split(",")[0];
                }

                // Extraer User-Agent
                String userAgent = request.getHeader("User-Agent");

                RequestMetadata metadata = RequestMetadata.builder()
                                .ipAddress(ipAddress)
                                .userAgent(userAgent)
                                .build();

                // 4. Guardar la sesión con sus metadatos
                var session = UserSession.builder()
                                .user(user)
                                .tokenHash(refreshToken)
                                .expiresAt(LocalDateTime.now().plusDays(7))
                                .requestMetadata(metadata)
                                .build();

                userSessionRepository.save(session);
        }
}