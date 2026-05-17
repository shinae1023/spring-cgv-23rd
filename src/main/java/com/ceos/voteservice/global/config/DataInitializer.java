package com.ceos.voteservice.global.config;

import com.ceos.voteservice.user.entity.Part;
import com.ceos.voteservice.user.entity.Team;
import com.ceos.voteservice.user.entity.User;
import com.ceos.voteservice.user.entity.UserRole;
import com.ceos.voteservice.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
public class DataInitializer {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    CommandLineRunner seedUsers() {
        return args -> {
            if (userRepository.existsByLoginId("ceos")) {
                return;
            }

            userRepository.save(User.builder()
                    .loginId("ceos")
                    .password(passwordEncoder.encode("password123!"))
                    .email("ceos@example.com")
                    .name("CEOS")
                    .part(Part.BACKEND)
                    .team(Team.IPX)
                    .role(UserRole.ADMIN)
                    .build());
        };
    }
}
