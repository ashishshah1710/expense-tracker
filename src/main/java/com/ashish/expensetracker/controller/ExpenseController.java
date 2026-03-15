package com.ashish.expensetracker.controller;

import com.ashish.expensetracker.dto.ExpenseRequest;
import com.ashish.expensetracker.dto.ExpenseResponse;
import com.ashish.expensetracker.dto.PagedResponse;
import com.ashish.expensetracker.model.Category;
import com.ashish.expensetracker.service.ExpenseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/expenses")
@RequiredArgsConstructor
@Tag(name = "Expenses", description = "Expense CRUD with pagination and filtering")
@SecurityRequirement(name = "bearerAuth")
public class ExpenseController {

    private final ExpenseService expenseService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new expense")
    public ExpenseResponse create(@Valid @RequestBody ExpenseRequest request) {
        return expenseService.create(request);
    }

    @GetMapping
    @Operation(summary = "Get all expenses with pagination and filters")
    public PagedResponse<ExpenseResponse> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Category category,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount) {
        return expenseService.getAll(page, size, category, startDate, endDate, minAmount, maxAmount);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get expense by ID")
    public ExpenseResponse getById(@PathVariable Long id) {
        return expenseService.getById(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an expense")
    public ExpenseResponse update(@PathVariable Long id, @Valid @RequestBody ExpenseRequest request) {
        return expenseService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete an expense")
    public void delete(@PathVariable Long id) {
        expenseService.delete(id);
    }
}
