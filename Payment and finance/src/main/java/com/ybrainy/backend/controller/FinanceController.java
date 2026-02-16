package com.ybrainy.backend.controller;

import com.ybrainy.backend.dto.finance.*;
import com.ybrainy.backend.service.FinanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/finance")
@RequiredArgsConstructor
public class FinanceController {

    private final FinanceService financeService;

    /* ─── Income Endpoints ─── */
    @PostMapping("/incomes")
    public ResponseEntity<IncomeResponseDTO> createIncome(@Valid @RequestBody CreateIncomeDTO dto) {
        return new ResponseEntity<>(financeService.createIncome(dto), HttpStatus.CREATED);
    }

    @PutMapping("/incomes/{id}")
    public ResponseEntity<IncomeResponseDTO> updateIncome(@PathVariable Long id,
            @Valid @RequestBody UpdateIncomeDTO dto) {
        return ResponseEntity.ok(financeService.updateIncome(id, dto));
    }

    @DeleteMapping("/incomes/{id}")
    public ResponseEntity<Void> deleteIncome(@PathVariable Long id) {
        financeService.deleteIncome(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/incomes")
    public ResponseEntity<List<IncomeResponseDTO>> getAllIncomes() {
        return ResponseEntity.ok(financeService.getAllIncomes());
    }

    /* ─── Expense Endpoints ─── */
    @PostMapping("/expenses")
    public ResponseEntity<ExpenseResponseDTO> createExpense(@Valid @RequestBody CreateExpenseDTO dto) {
        return new ResponseEntity<>(financeService.createExpense(dto), HttpStatus.CREATED);
    }

    @PutMapping("/expenses/{id}")
    public ResponseEntity<ExpenseResponseDTO> updateExpense(@PathVariable Long id,
            @Valid @RequestBody UpdateExpenseDTO dto) {
        return ResponseEntity.ok(financeService.updateExpense(id, dto));
    }

    @DeleteMapping("/expenses/{id}")
    public ResponseEntity<Void> deleteExpense(@PathVariable Long id) {
        financeService.deleteExpense(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/expenses")
    public ResponseEntity<List<ExpenseResponseDTO>> getAllExpenses() {
        return ResponseEntity.ok(financeService.getAllExpenses());
    }
}
