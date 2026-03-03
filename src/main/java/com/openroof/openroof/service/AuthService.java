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
import com.openroof.openroof.security.JwtService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
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

                // Convertimos el String que viene del frontend al Enum de Java
                UserRole selectedRole = UserRole.valueOf(request.getRole().toUpperCase());

                var user = User.builder()
                                .email(request.getEmail())
                                .passwordHash(passwordEncoder.encode(request.getPassword()))
                                .name(request.getName())
                                .phone(request.getPhone())
                                .role(selectedRole)
                                .build();

                userRepository.save(user);
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

                String userEmail = jwtService.extractUsername(oldRefreshToken);

                var user = userRepository.findByEmail(userEmail)
                                .orElseThrow(() -> new BadRequestException("Usuario no encontrado"));

                // 2. BUSCAR LA SESIÓN: Validar que el token existe en nuestra DB
                var session = userSessionRepository.findByTokenHash(oldRefreshToken)
                                .orElseThrow(() -> new BadRequestException("Sesión inválida o ya utilizada"));

                // 3. VALIDAR EL JWT: Verificar expiración y firma
                if (!jwtService.isTokenValid(oldRefreshToken, user)) {
                        userSessionRepository.delete(session); // Limpiamos la DB si expiró
                        throw new BadRequestException("El token de refresco ha expirado");
                }

                userSessionRepository.delete(session);

                return generateFullAuthResponse(user, httpRequest);
        }

        private AuthResponse generateFullAuthResponse(User user, HttpServletRequest httpRequest) {
                String accessToken = jwtService.generateToken(user);
                String refreshToken = jwtService.generateRefreshToken(user);

                // Aquí es donde realmente ocurre la magia de guardar la sesión
                saveUserSession(user, refreshToken, httpRequest);

                return AuthResponse.builder()
                                .accessToken(accessToken)
                                .refreshToken(refreshToken)
                                .email(user.getEmail())
                                .role(user.getRole().name())
                                .build();
        }

        private String parseToken(String header) {
                if (header == null || !header.startsWith("Bearer ")) {
                        throw new BadRequestException("Token no proporcionado");
                }
                return header.substring(7);
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