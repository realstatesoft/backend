package com.openroof.openroof.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import com.openroof.openroof.repository.UserRepository;






/*Auth: Enrique Rios
    Desc: loadUserByUsername(String username): 
        Este método es invocado automáticamente por Spring Security cuando un usuario intenta iniciar sesión.
    ultima modif: 21/02/2026
*/
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
    
    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + username));
    }
   
}