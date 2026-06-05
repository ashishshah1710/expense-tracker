package com.ashish.expensetracker.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.admin")
public class AdminBootstrapProperties {

    private boolean enabled = true;
    private String username = "admin";
    private String email = "admin@example.com";
    private String password = "admin123";
}
