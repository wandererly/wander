package com.example.wander.auth;

import com.example.wander.user.UserEntity;
import com.example.wander.user.UserRepository;
import com.example.wander.user.UserStatus;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserEntity entity = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("用户不存在"));

        boolean disabled = entity.getStatus() == UserStatus.BANNED;

        return User.withUsername(entity.getUsername())
                .password(entity.getPassword())
                .roles(entity.getRole().name())
                .disabled(disabled)
                .build();
    }
}

