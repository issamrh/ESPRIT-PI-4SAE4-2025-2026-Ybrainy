package com.ybrainy.finance.service;

import com.ybrainy.finance.dto.finance.*;
import com.ybrainy.finance.entity.Expense;
import com.ybrainy.finance.entity.Income;
import com.ybrainy.finance.exception.ResourceNotFoundException;
import com.ybrainy.finance.mapper.FinanceMapper;
import com.ybrainy.finance.repository.ExpenseRepository;
import com.ybrainy.finance.repository.IncomeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class FinanceService {

    private final IncomeRepository incomeRepository;
    private final ExpenseRepository expenseRepository;
    private final FinanceMapper financeMapper;

    /* ─── Income CRUD ─── */
    public IncomeResponseDTO createIncome(CreateIncomeDTO dto) {
        Income entity = financeMapper.toIncomeEntity(dto);
        Income saved = incomeRepository.save(entity);
        return financeMapper.toIncomeResponseDTO(saved);
    }

    public IncomeResponseDTO updateIncome(Long id, UpdateIncomeDTO dto) {
        Income entity = incomeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Income not found with id: " + id));
        financeMapper.updateIncomeEntity(dto, entity);
        Income updated = incomeRepository.save(entity);
        return financeMapper.toIncomeResponseDTO(updated);
    }

    public void deleteIncome(Long id) {
        if (!incomeRepository.existsById(id)) {
            throw new ResourceNotFoundException("Income not found with id: " + id);
        }
        incomeRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<IncomeResponseDTO> getAllIncomes() {
        return incomeRepository.findAll().stream()
                .map(financeMapper::toIncomeResponseDTO)
                .collect(Collectors.toList());
    }

    /* ─── Expense CRUD ─── */
    public ExpenseResponseDTO createExpense(CreateExpenseDTO dto) {
        Expense entity = financeMapper.toExpenseEntity(dto);
        Expense saved = expenseRepository.save(entity);
        return financeMapper.toExpenseResponseDTO(saved);
    }

    public ExpenseResponseDTO updateExpense(Long id, UpdateExpenseDTO dto) {
        Expense entity = expenseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found with id: " + id));
        financeMapper.updateExpenseEntity(dto, entity);
        Expense updated = expenseRepository.save(entity);
        return financeMapper.toExpenseResponseDTO(updated);
    }

    public void deleteExpense(Long id) {
        if (!expenseRepository.existsById(id)) {
            throw new ResourceNotFoundException("Expense not found with id: " + id);
        }
        expenseRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<ExpenseResponseDTO> getAllExpenses() {
        return expenseRepository.findAll().stream()
                .map(financeMapper::toExpenseResponseDTO)
                .collect(Collectors.toList());
    }
}