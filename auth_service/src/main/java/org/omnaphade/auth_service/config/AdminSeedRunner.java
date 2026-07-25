package org.omnaphade.auth_service.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.omnaphade.auth_service.entities.Role;
import org.omnaphade.auth_service.entities.User;
import org.omnaphade.auth_service.repository.AuthRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminSeedRunner implements CommandLineRunner {

    private final AuthRepository authRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${admin.seed.enabled:false}")
    private boolean adminSeedEnabled;

    @Value("${admin.seed.email:admin@jobportal.local}")
    private String adminEmail;

    @Value("${admin.seed.password:Pass123!}")
    private String adminPassword;

    @Override
    public void run(String... args) {
        if (!adminSeedEnabled) {
            return;
        }

        if (authRepository.existsByEmail(adminEmail)) {
            return;
        }

        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        User admin = new User();
        admin.setEmail(adminEmail);
        admin.setPasswordHash(passwordEncoder.encode(adminPassword));
        admin.setRole(Role.ADMIN);
        admin.setStatus("ACTIVE");
        admin.setCreatedAt(now);
        admin.setUpdatedAt(now);
        authRepository.save(admin);

        log.info("Seeded deterministic admin account: {}", adminEmail);
    }
}
