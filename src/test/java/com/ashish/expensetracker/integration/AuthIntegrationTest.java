package com.ashish.expensetracker.integration;

import com.ashish.expensetracker.dto.AuthResponse;
import com.ashish.expensetracker.dto.LoginRequest;
import com.ashish.expensetracker.dto.RegisterRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers(disabledWithoutDocker = true)
class AuthIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("expense_tracker_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void register_shouldReturnJwtToken() {
        RegisterRequest request = RegisterRequest.builder()
                .username("testuser")
                .email("test@example.com")
                .password("password123")
                .build();

        ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
                "/api/v1/auth/register", request, AuthResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getToken()).isNotBlank();
        assertThat(response.getBody().getUsername()).isEqualTo("testuser");
        assertThat(response.getBody().getRole()).isNotNull();
    }

    @Test
    void login_shouldReturnJwtToken() {
        RegisterRequest registerRequest = RegisterRequest.builder()
                .username("loginuser")
                .email("login@example.com")
                .password("password123")
                .build();
        restTemplate.postForEntity("/api/v1/auth/register", registerRequest, AuthResponse.class);

        LoginRequest loginRequest = LoginRequest.builder()
                .username("loginuser")
                .password("password123")
                .build();

        ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
                "/api/v1/auth/login", loginRequest, AuthResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getToken()).isNotBlank();
        assertThat(response.getBody().getUsername()).isEqualTo("loginuser");
    }

    @Test
    void login_withInvalidCredentials_shouldReturn401() {
        LoginRequest loginRequest = LoginRequest.builder()
                .username("nonexistent")
                .password("wrongpassword")
                .build();

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/v1/auth/login", loginRequest, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void register_withDuplicateUsername_shouldReturn409() {
        RegisterRequest request = RegisterRequest.builder()
                .username("duplicate")
                .email("dup1@example.com")
                .password("password123")
                .build();
        restTemplate.postForEntity("/api/v1/auth/register", request, AuthResponse.class);

        RegisterRequest duplicateRequest = RegisterRequest.builder()
                .username("duplicate")
                .email("dup2@example.com")
                .password("password123")
                .build();

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/v1/auth/register", duplicateRequest, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }
}
