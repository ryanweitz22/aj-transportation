package com.ajtransportation.app.service;

import com.ajtransportation.app.model.User;
import com.ajtransportation.app.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
        String value = identifier == null ? "" : identifier.trim();

        User user = userRepository.findByEmailIgnoreCase(value)
            .or(() -> userRepository.findByUsernameIgnoreCase(value))
            .orElseThrow(() -> new UsernameNotFoundException("No account found for: " + identifier));

        boolean enabled = user.isEmailVerified() && !user.isBlocked();
        SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + user.getRole());

        return new org.springframework.security.core.userdetails.User(
            user.getEmail(),
            user.getPassword(),
            enabled,
            true,
            true,
            !user.isBlocked(),
            List.of(authority)
        );
    }
}