package org.omnaphade.auth_service.security;

import lombok.RequiredArgsConstructor;
import org.omnaphade.auth_service.entities.User;
import org.omnaphade.auth_service.repository.AuthRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final AuthRepository authRepository;

    @Override
    public UserDetails loadUserByUsername(String usernameOrId) throws UsernameNotFoundException {
        // We store users by email or id (JWT uses id)
        User user;
        try {
            Long userId = Long.parseLong(usernameOrId);
            user = authRepository.findById(userId)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + userId));
        } catch (NumberFormatException e) {
            user = authRepository.findByEmail(usernameOrId)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + usernameOrId));
        }

        // Convert Role to GrantedAuthority
        GrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + user.getRole().name());

        return new org.springframework.security.core.userdetails.User(
                String.valueOf(user.getId()), // username = userId for JWT
                user.getPasswordHash(),
                Collections.singleton(authority)
        );
    }
}
