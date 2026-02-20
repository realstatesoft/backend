package com.openroof.openroof.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Implementación temporal de UserDetailsService.
 * TODO: Reemplazar con implementación real que consulte la BD cuando se cree la entidad User.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Implementación temporal — será reemplazada al crear el módulo de usuarios
        throw new UsernameNotFoundException("Usuario no encontrado: " + username);
    }
}
