package com.ceos.voteservice.global.config;

import com.ceos.voteservice.user.entity.Part;
import com.ceos.voteservice.user.entity.Team;
import com.ceos.voteservice.user.entity.User;
import com.ceos.voteservice.user.entity.UserRole;
import com.ceos.voteservice.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;

@Configuration
@RequiredArgsConstructor
public class DataInitializer {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.seed-enabled:false}")
    private boolean adminSeedEnabled;

    @Value("${app.admin.login-id:}")
    private String adminLoginId;

    @Value("${app.admin.password:}")
    private String adminPassword;

    @Value("${app.admin.email:}")
    private String adminEmail;

    @Value("${app.admin.name:}")
    private String adminName;

    @Value("${app.admin.part:BACKEND}")
    private Part adminPart;

    @Value("${app.admin.team:IPX}")
    private Team adminTeam;

    @Bean
    CommandLineRunner seedUsers() {
        return args -> {
            if (!adminSeedEnabled) {
                return;
            }

            validateAdminSeedProperties();

            if (userRepository.existsByLoginId(adminLoginId)) {
                return;
            }

            userRepository.save(User.builder()
                    .loginId(adminLoginId)
                    .password(passwordEncoder.encode(adminPassword))
                    .email(adminEmail)
                    .name(adminName)
                    .part(adminPart)
                    .team(adminTeam)
                    .role(UserRole.ADMIN)
                    .build());
        };
    }

    private void validateAdminSeedProperties() {
        if (!StringUtils.hasText(adminLoginId)
                || !StringUtils.hasText(adminPassword)
                || !StringUtils.hasText(adminEmail)
                || !StringUtils.hasText(adminName)) {
            throw new IllegalStateException("관리자 초기 계정을 생성하려면 ADMIN_LOGIN_ID, ADMIN_PASSWORD, ADMIN_EMAIL, ADMIN_NAME 값을 모두 설정해야 합니다.");
        }
    }
}
