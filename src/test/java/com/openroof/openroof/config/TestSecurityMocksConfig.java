package com.openroof.openroof.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.core.userdetails.UserDetailsService;
import com.openroof.openroof.exception.JwtAuthenticationEntryPoint;
import org.mockito.Mockito;

@TestConfiguration
public class TestSecurityMocksConfig {

    @Bean
    public UserDetailsService userDetailsService() {
        return Mockito.mock(UserDetailsService.class);
    }

    @Bean
    public JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint() {
        return Mockito.mock(JwtAuthenticationEntryPoint.class);
    }

    // AuthRateLimitingFilter (dependencia de SecurityConfig) lo requiere; los
    // slices @WebMvcTest no escanean @Component planos.
    @Bean
    public com.openroof.openroof.security.AuthRateLimiter authRateLimiter() {
        return Mockito.mock(com.openroof.openroof.security.AuthRateLimiter.class);
    }
}
