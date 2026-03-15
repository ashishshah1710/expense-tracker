package com.ashish.expensetracker.integration;

import com.ashish.expensetracker.dto.AuthResponse;
import com.ashish.expensetracker.dto.ExpenseRequest;
import com.ashish.expensetracker.dto.ExpenseResponse;
import com.ashish.expensetracker.dto.LoginRequest;
import com.ashish.expensetracker.dto.PagedResponse;
import com.ashish.expensetracker.dto.RegisterRequest;
import com.ashish.expensetracker.model.Category;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers(disabledWithoutDocker = true)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ExpenseIntegrationTest {

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

    private String authToken;

    @BeforeAll
    void setUp() {
        RegisterRequest registerRequest = RegisterRequest.builder()
                .username("expenseuser")
                .email("expense@example.com")
                .password("password123")
                .build();
        restTemplate.postForEntity("/api/v1/auth/register", registerRequest, AuthResponse.class);

        LoginRequest loginRequest = LoginRequest.builder()
                .username("expenseuser")
                .password("password123")
                .build();
        ResponseEntity<AuthResponse> loginResponse = restTemplate.postForEntity(
                "/api/v1/auth/login", loginRequest, AuthResponse.class);
        authToken = loginResponse.getBody().getToken();
    }

    private HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(authToken);
        return headers;
    }

    @Test
    void createExpense_shouldReturn201() {
        ExpenseRequest request = ExpenseRequest.builder()
                .amount(new BigDecimal("49.99"))
                .category(Category.FOOD)
                .description("Lunch")
                .date(LocalDate.now())
                .build();

        ResponseEntity<ExpenseResponse> response = restTemplate.exchange(
                "/api/v1/expenses",
                HttpMethod.POST,
                new HttpEntity<>(request, authHeaders()),
                ExpenseResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isNotNull();
        assertThat(response.getBody().getAmount()).isEqualByComparingTo("49.99");
        assertThat(response.getBody().getCategory()).isEqualTo(Category.FOOD);
        assertThat(response.getBody().getDescription()).isEqualTo("Lunch");
    }

    @Test
    void getExpenseById_shouldReturnExpense() {
        ExpenseRequest createRequest = ExpenseRequest.builder()
                .amount(new BigDecimal("25.00"))
                .category(Category.TRANSPORT)
                .date(LocalDate.now())
                .build();
        ResponseEntity<ExpenseResponse> createResponse = restTemplate.exchange(
                "/api/v1/expenses", HttpMethod.POST,
                new HttpEntity<>(createRequest, authHeaders()),
                ExpenseResponse.class);
        Long expenseId = createResponse.getBody().getId();

        ResponseEntity<ExpenseResponse> response = restTemplate.exchange(
                "/api/v1/expenses/" + expenseId,
                HttpMethod.GET,
                new HttpEntity<>(authHeaders()),
                ExpenseResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo(expenseId);
        assertThat(response.getBody().getAmount()).isEqualByComparingTo("25.00");
    }

    @Test
    void getAllExpenses_shouldReturnPaginatedResults() {
        restTemplate.exchange("/api/v1/expenses", HttpMethod.POST,
                new HttpEntity<>(ExpenseRequest.builder()
                        .amount(new BigDecimal("10"))
                        .category(Category.OTHER)
                        .date(LocalDate.now())
                        .build(), authHeaders()),
                ExpenseResponse.class);

        ResponseEntity<PagedResponse<ExpenseResponse>> response = restTemplate.exchange(
                "/api/v1/expenses?page=0&size=5",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders()),
                new ParameterizedTypeReference<PagedResponse<ExpenseResponse>>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getContent()).isNotEmpty();
        assertThat(response.getBody().getPage()).isZero();
        assertThat(response.getBody().getSize()).isEqualTo(5);
        assertThat(response.getBody().getTotalElements()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void getAllExpenses_withCategoryFilter_shouldReturnFilteredResults() {
        restTemplate.exchange("/api/v1/expenses", HttpMethod.POST,
                new HttpEntity<>(ExpenseRequest.builder()
                        .amount(new BigDecimal("15"))
                        .category(Category.ENTERTAINMENT)
                        .date(LocalDate.now())
                        .build(), authHeaders()),
                ExpenseResponse.class);

        ResponseEntity<PagedResponse<ExpenseResponse>> response = restTemplate.exchange(
                "/api/v1/expenses?page=0&size=10&category=ENTERTAINMENT",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders()),
                new ParameterizedTypeReference<PagedResponse<ExpenseResponse>>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        response.getBody().getContent().forEach(expense ->
                assertThat(expense.getCategory()).isEqualTo(Category.ENTERTAINMENT));
    }

    @Test
    void updateExpense_shouldReturnUpdatedExpense() {
        ExpenseRequest createRequest = ExpenseRequest.builder()
                .amount(new BigDecimal("30"))
                .category(Category.SHOPPING)
                .description("Original")
                .date(LocalDate.now())
                .build();
        ResponseEntity<ExpenseResponse> createResponse = restTemplate.exchange(
                "/api/v1/expenses", HttpMethod.POST,
                new HttpEntity<>(createRequest, authHeaders()),
                ExpenseResponse.class);
        Long expenseId = createResponse.getBody().getId();

        ExpenseRequest updateRequest = ExpenseRequest.builder()
                .amount(new BigDecimal("45"))
                .category(Category.HEALTHCARE)
                .description("Updated")
                .date(LocalDate.now())
                .build();

        ResponseEntity<ExpenseResponse> response = restTemplate.exchange(
                "/api/v1/expenses/" + expenseId,
                HttpMethod.PUT,
                new HttpEntity<>(updateRequest, authHeaders()),
                ExpenseResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getAmount()).isEqualByComparingTo("45");
        assertThat(response.getBody().getCategory()).isEqualTo(Category.HEALTHCARE);
        assertThat(response.getBody().getDescription()).isEqualTo("Updated");
    }

    @Test
    void deleteExpense_shouldReturn204() {
        ExpenseRequest createRequest = ExpenseRequest.builder()
                .amount(new BigDecimal("5"))
                .category(Category.UTILITIES)
                .date(LocalDate.now())
                .build();
        ResponseEntity<ExpenseResponse> createResponse = restTemplate.exchange(
                "/api/v1/expenses", HttpMethod.POST,
                new HttpEntity<>(createRequest, authHeaders()),
                ExpenseResponse.class);
        Long expenseId = createResponse.getBody().getId();

        ResponseEntity<Void> response = restTemplate.exchange(
                "/api/v1/expenses/" + expenseId,
                HttpMethod.DELETE,
                new HttpEntity<>(authHeaders()),
                Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void getExpenseWithoutAuth_shouldReturn401() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/v1/expenses/1", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
