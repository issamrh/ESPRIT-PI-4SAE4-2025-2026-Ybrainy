package com.ybrainy.backend.mapper;

import com.ybrainy.backend.dto.finance.*;
import com.ybrainy.backend.entity.Expense;
import com.ybrainy.backend.entity.Income;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface FinanceMapper {

    /* ─── Income Mappings ─── */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "receivedDate", expression = "java(java.time.LocalDateTime.now())")
    Income toIncomeEntity(CreateIncomeDTO dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "receivedDate", ignore = true)
    void updateIncomeEntity(UpdateIncomeDTO dto, @MappingTarget Income entity);

    IncomeResponseDTO toIncomeResponseDTO(Income entity);

    /* ─── Expense Mappings ─── */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    Expense toExpenseEntity(CreateExpenseDTO dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    void updateExpenseEntity(UpdateExpenseDTO dto, @MappingTarget Expense entity);

    ExpenseResponseDTO toExpenseResponseDTO(Expense entity);
}
