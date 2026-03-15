package com.ashish.expensetracker.service;

import com.ashish.expensetracker.dto.ExpenseRequest;
import com.ashish.expensetracker.dto.ExpenseResponse;
import com.ashish.expensetracker.dto.PagedResponse;
import com.ashish.expensetracker.exception.ResourceNotFoundException;
import com.ashish.expensetracker.exception.UnauthorizedException;
import com.ashish.expensetracker.model.Category;
import com.ashish.expensetracker.model.Expense;
import com.ashish.expensetracker.model.Role;
import com.ashish.expensetracker.model.User;
import com.ashish.expensetracker.repository.ExpenseRepository;
import com.ashish.expensetracker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final UserRepository userRepository;

    public ExpenseResponse create(ExpenseRequest request) {
        User user = getCurrentUser();
        Expense expense = Expense.builder()
                .user(user)
                .amount(request.getAmount())
                .category(request.getCategory())
                .description(request.getDescription())
                .date(request.getDate())
                .build();
        expense = expenseRepository.save(expense);
        return toResponse(expense);
    }

    public ExpenseResponse getById(Long id) {
        Expense expense = findExpenseWithAccessCheck(id);
        return toResponse(expense);
    }

    public PagedResponse<ExpenseResponse> getAll(
            int page, int size,
            Category category,
            LocalDate startDate, LocalDate endDate,
            BigDecimal minAmount, BigDecimal maxAmount) {
        User user = getCurrentUser();
        User filterUser = user.getRole() == Role.ROLE_ADMIN ? null : user;
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "date"));

        Page<Expense> expensePage = expenseRepository.findByUserWithFilters(
                filterUser, category, startDate, endDate, minAmount, maxAmount, pageable);

        List<ExpenseResponse> content = expensePage.getContent().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return PagedResponse.<ExpenseResponse>builder()
                .content(content)
                .page(expensePage.getNumber())
                .size(expensePage.getSize())
                .totalElements(expensePage.getTotalElements())
                .totalPages(expensePage.getTotalPages())
                .build();
    }

    public ExpenseResponse update(Long id, ExpenseRequest request) {
        Expense expense = findExpenseWithAccessCheck(id);
        expense.setAmount(request.getAmount());
        expense.setCategory(request.getCategory());
        expense.setDescription(request.getDescription());
        expense.setDate(request.getDate());
        expense = expenseRepository.save(expense);
        return toResponse(expense);
    }

    public void delete(Long id) {
        Expense expense = findExpenseWithAccessCheck(id);
        expenseRepository.delete(expense);
    }

    private Expense findExpenseWithAccessCheck(Long id) {
        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found with id: " + id));

        User currentUser = getCurrentUser();
        if (!expense.getUser().getId().equals(currentUser.getId()) && currentUser.getRole() != Role.ROLE_ADMIN) {
            throw new UnauthorizedException("Not authorized to access this expense");
        }
        return expense;
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private ExpenseResponse toResponse(Expense expense) {
        return ExpenseResponse.builder()
                .id(expense.getId())
                .userId(expense.getUser().getId())
                .amount(expense.getAmount())
                .category(expense.getCategory())
                .description(expense.getDescription())
                .date(expense.getDate())
                .createdAt(expense.getCreatedAt())
                .build();
    }
}
