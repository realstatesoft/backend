package com.openroof.openroof.service;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

import com.openroof.openroof.dto.security.AuthResponse;
import com.openroof.openroof.dto.security.LoginRequest;
import com.openroof.openroof.model.user.User;
import com.openroof.openroof.repository.UserRepository;
import com.openroof.openroof.security.JwtService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

/* */
@Service
@RequiredArgsConstructor
public class AuthService {
        private final AuthenticationManager authenticationManager;
        //private final UserRepository userRepository;
        private final JwtService jwtService;

        @Transactional
        public AuthResponse login(LoginRequest request) {

                // 1. Autenticar y capturar el resultado
                var auth = authenticationManager.authenticate(
                                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

            
                // var user = userRepository.findByEmail(request.getEmail())
                //                 .orElseThrow();

                
                // 2. RECUPERAR EL USUARIO DEL RESULTADO DE LA AUTENTICACIÓN
                // En lugar de ir a la DB con userRepository, lo tomamos del principal
                var user = (User) auth.getPrincipal();

                // 3. Generar el token
                var jwtToken = jwtService.generateToken(user);

                return AuthResponse.builder()
                                .accessToken(jwtToken)
                                .email(user.getEmail())
                                .role(user.getRole().name())
                                .build();
        }

}
