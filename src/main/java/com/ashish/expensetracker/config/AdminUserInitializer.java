package com.ashish.expensetracker.config;

import com.ashish.expensetracker.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminUserInitializer implements ApplicationRunner {

    private final AdminBootstrapProperties adminProperties;
    private final AuthService authService;

    @Override
    public void run(ApplicationArguments args) {
        if (!adminProperties.isEnabled()) {
            return;
        }

        authService.bootstrapAdmin(
                adminProperties.getUsername(),
                adminProperties.getEmail(),
                adminProperties.getPassword());
        log.info("Admin bootstrap check completed for user '{}'", adminProperties.getUsername());
    }
}
