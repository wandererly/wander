package com.example.wander.config;

import com.example.wander.user.UserEntity;
import com.example.wander.user.UserRepository;
import com.example.wander.user.UserRole;
import com.example.wander.user.UserStatus;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@EnableConfigurationProperties(AppBootstrapAdminProperties.class)
public class BootstrapAdminConfig {

    @Bean
    public CommandLineRunner bootstrapAdmin(UserRepository userRepository,
                                            PasswordEncoder passwordEncoder,
                                            AppBootstrapAdminProperties props) {
        return args -> {
            if (!props.isEnabled()) {
                return;
            }
            boolean adminExists = userRepository.findAll().stream()
                    .anyMatch(u -> u.getRole() == UserRole.ADMIN);
            if (adminExists) {
                return;
            }
            if (userRepository.findByUsername(props.getUsername()).isPresent()
                    || userRepository.findByEmail(props.getEmail()).isPresent()) {
                return;
            }
            UserEntity admin = new UserEntity();
            admin.setUsername(props.getUsername());
            admin.setEmail(props.getEmail());
            admin.setPassword(passwordEncoder.encode(props.getPassword()));
            admin.setRole(UserRole.ADMIN);
            admin.setStatus(UserStatus.NORMAL);
            userRepository.save(admin);
        };
    }
}
