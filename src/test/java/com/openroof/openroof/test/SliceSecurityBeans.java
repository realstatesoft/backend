package com.openroof.openroof.test;

import com.openroof.openroof.exception.JwtAuthenticationEntryPoint;
import com.openroof.openroof.security.PropertyViewRateLimiter;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * Provides bean dependencies of SecurityConfig that @WebMvcTest slices do not
 * auto-scan (PropertyViewRateLimiter is a plain @Component). Import in slice
 * tests via @Import(SliceSecurityBeans.class) alongside SecurityConfig.
 */
@TestConfiguration(proxyBeanMethods = false)
public class SliceSecurityBeans {

    @Bean
    public PropertyViewRateLimiter propertyViewRateLimiter() {
        return Mockito.mock(PropertyViewRateLimiter.class);
    }

    @Bean
    public JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint() {
        return Mockito.mock(JwtAuthenticationEntryPoint.class);
    }
}
