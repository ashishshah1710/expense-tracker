package com.ashish.expensetracker.dto;

import com.ashish.expensetracker.model.Category;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseResponse {

    private Long id;
    private Long userId;
    private BigDecimal amount;
    private Category category;
    private String description;
    private LocalDate date;
    private Instant createdAt;
}
