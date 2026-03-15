package com.ashish.expensetracker.repository;

import com.ashish.expensetracker.model.Category;
import com.ashish.expensetracker.model.Expense;
import com.ashish.expensetracker.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    Page<Expense> findByUser(User user, Pageable pageable);

    Optional<Expense> findByIdAndUser(Long id, User user);

    @Query("SELECT e FROM Expense e WHERE (:user IS NULL OR e.user = :user) " +
            "AND (:category IS NULL OR e.category = :category) " +
            "AND (:startDate IS NULL OR e.date >= :startDate) " +
            "AND (:endDate IS NULL OR e.date <= :endDate) " +
            "AND (:minAmount IS NULL OR e.amount >= :minAmount) " +
            "AND (:maxAmount IS NULL OR e.amount <= :maxAmount)")
    Page<Expense> findByUserWithFilters(
            @Param("user") User user,
            @Param("category") Category category,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("minAmount") BigDecimal minAmount,
            @Param("maxAmount") BigDecimal maxAmount,
            Pageable pageable);
}
