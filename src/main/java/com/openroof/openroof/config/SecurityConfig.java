package com.openroof.openroof.config;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.openroof.openroof.security.JwtAuthenticationFilter;
import com.openroof.openroof.security.AuthRateLimitingFilter;
import com.openroof.openroof.security.PropertyViewRateLimitingFilter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Configuración central de Spring Security.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
@Slf4j
public class SecurityConfig {

        @Value("${cors.allowed-origins:http://localhost:3000,http://localhost:4200,http://localhost:5173,http://localhost:5174,http://127.0.0.1:5173,http://127.0.0.1:5174}")
        private String allowedOriginsRaw;

        @Value("${cors.allowed-preview-origins:}")
        private String allowedPreviewOriginsRaw;

        private final JwtAuthenticationFilter jwtAuthFilter;
        private final AuthRateLimitingFilter authRateLimitingFilter;
        private final PropertyViewRateLimitingFilter propertyViewRateLimitingFilter;
        private final SecurityHeadersFilter securityHeadersFilter;
        private final UserDetailsService userDetailsService;
        private final com.openroof.openroof.exception.JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

        /** Rutas que NO requieren autenticación */
        // Only allow explicitly public auth endpoints — keep logout endpoints protected.
        private static final String[] PUBLIC_URLS = {
                        "/auth/login",
                        "/auth/register",
                        "/auth/register-agent",
                        "/auth/refresh-token",
                        "/exchange-rates",
                        "/exchange-rates/**",
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/actuator/health",
                        "/test/**"
        };

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
                http
                                .csrf(csrf -> csrf.ignoringRequestMatchers("/**"))
                                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                                        //Bloque el cerra sesion solo a usuairos autenticados.
                                                .requestMatchers(HttpMethod.POST, "/auth/logout", "/auth/logout-all")
                                                .authenticated()
                                                .requestMatchers(PUBLIC_URLS).permitAll()
                                                .requestMatchers(HttpMethod.GET, "/agents/*/availability").authenticated()
                                                .requestMatchers(HttpMethod.GET, "/agents/**").permitAll()
                                                .requestMatchers(HttpMethod.POST, "/leads/wizard").permitAll()
                                                .requestMatchers(HttpMethod.GET, "/properties/**").permitAll()
                                                .requestMatchers(HttpMethod.POST, "/properties/*/views")
                                                .access((authentication, context) -> new AuthorizationDecision(true))
                                                .requestMatchers(HttpMethod.GET, "/locations/**").permitAll()
                                                .requestMatchers(HttpMethod.GET, "/preferences/options").permitAll()
                                                // Catálogo público de planes de suscripción
                                                .requestMatchers(new org.springframework.security.web.util.matcher.RegexRequestMatcher("^/subscription-plans$|^/subscription-plans/\\d+$", "GET")).permitAll()
                                                // Endpoints públicos de alquileres (catálogo)
                                                .requestMatchers(HttpMethod.GET, "/leases/public/**").permitAll()
                                                .requestMatchers(HttpMethod.POST, "/leases/*/sign").permitAll()
                                                .requestMatchers(HttpMethod.POST, "/api/leases/*/sign").permitAll()
                                                // Endpoints protegidos — autorización fina vía LeaseSecurity
                                                .requestMatchers("/leases/**").authenticated()
                                                .requestMatchers("/rentals/**").authenticated()
                                                .requestMatchers("/lease-payments/**").authenticated()
                                                .anyRequest().authenticated())
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .exceptionHandling(ex -> ex
                                                .authenticationEntryPoint(jwtAuthenticationEntryPoint))
                                .authenticationProvider(authenticationProvider())
                                 .addFilterBefore(securityHeadersFilter, org.springframework.security.web.header.HeaderWriterFilter.class)
                                 .addFilterBefore(jwtAuthFilter, org.springframework.security.web.authentication.logout.LogoutFilter.class)
                                 .addFilterBefore(propertyViewRateLimitingFilter, org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter.class)
                                 .addFilterBefore(authRateLimitingFilter, UsernamePasswordAuthenticationFilter.class)
                                .headers(headers -> headers
                                        .httpStrictTransportSecurity(hsts -> hsts
                                                .includeSubDomains(true)
                                                .maxAgeInSeconds(31536000))
                                        .frameOptions(frame -> frame.sameOrigin())
                                        .contentTypeOptions(content -> {}));

                return http.build();
        }

        @Bean
        public AuthenticationProvider authenticationProvider() {
                DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
                provider.setPasswordEncoder(passwordEncoder());
                return provider;
        }

        @Bean
        public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
                return config.getAuthenticationManager();
        }

        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }

        @Bean
        public CorsConfigurationSource corsConfigurationSource() {
                CorsConfiguration config = new CorsConfiguration();
                List<String> origins = Stream.concat(
                                Arrays.stream(allowedOriginsRaw.split(",")),
                                Arrays.stream(allowedPreviewOriginsRaw.split(",")))
                                .map(String::trim)
                                .filter(s -> !s.isEmpty())
                                .filter(s -> {
                                        boolean isWildcard = s.contains("*");
                                        if (isWildcard) {
                                                log.warn("Se ignora origen CORS con wildcard por seguridad: {}", s);
                                        }
                                        return !isWildcard;
                                })
                                .distinct()
                                .toList();

                config.setAllowedOrigins(origins);
                config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
                config.setAllowedHeaders(List.of("*"));
                config.setAllowCredentials(true);
                config.setMaxAge(3600L);

                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", config);
                return source;
        }
}
