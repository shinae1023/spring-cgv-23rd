package cgv_23rd.ceos.global.config;

import cgv_23rd.ceos.user.entity.Part;
import cgv_23rd.ceos.user.entity.Team;
import cgv_23rd.ceos.user.entity.User;
import cgv_23rd.ceos.user.entity.UserRole;
import cgv_23rd.ceos.user.repository.UserRepository;
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
