package com.flourishtravel.domain.user.service;

import com.flourishtravel.domain.user.entity.User;
import com.flourishtravel.domain.user.repository.UserRepository;
import com.flourishtravel.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        if (username == null || username.isBlank()) {
            throw new UsernameNotFoundException("User not found");
        }
        User user;
        try {
            user = userRepository.findById(UUID.fromString(username))
                    .orElseGet(() -> userRepository.findByEmail(username).orElse(null));
        } catch (IllegalArgumentException e) {
            user = userRepository.findByEmail(username).orElse(null);
        }
        if (user == null) {
            throw new UsernameNotFoundException("User not found: " + username);
        }
        return new UserPrincipal(user);
    }
}
