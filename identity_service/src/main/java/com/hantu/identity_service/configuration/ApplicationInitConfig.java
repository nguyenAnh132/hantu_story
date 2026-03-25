package com.hantu.identity_service.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.AccessLevel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.boot.ApplicationRunner;
import com.hantu.identity_service.repository.UserRepository;
import com.hantu.identity_service.repository.RoleRepository;
import com.hantu.identity_service.entity.User;
import com.hantu.identity_service.entity.Role;
import java.time.LocalDateTime;
import java.util.Set;
import com.hantu.identity_service.exception.AppException;
import com.hantu.identity_service.exception.ErrorCode;

@Configuration
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class ApplicationInitConfig {

    PasswordEncoder passwordEncoder;

    @Bean
    ApplicationRunner applicationRunner(UserRepository userRepository, RoleRepository roleRepository) {
        return args -> {
            if (!userRepository.existsByUsername("admin")
                    && !roleRepository.existsByName("ADMIN")
                    && !roleRepository.existsByName("USER")) {

                roleRepository.save(Role.builder()
                        .name("ADMIN")
                        .description("Admin role")
                        .build());

                roleRepository.save(Role.builder()
                        .name("USER")
                        .description("User role")
                        .build());

                userRepository.save(User.builder()
                        .username("admin")
                        .password(passwordEncoder.encode("12345678"))
                        .roles(Set.of(roleRepository.findById("ADMIN")
                                .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND))))
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .lastLoginAt(LocalDateTime.now())
                        .active(true)
                        .email("admin@gmail.com")
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .lastLoginAt(LocalDateTime.now())
                        .build());

            }

        };
    }

}
