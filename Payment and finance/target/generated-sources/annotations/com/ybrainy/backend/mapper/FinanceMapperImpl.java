package com.ybrainy.backend.mapper;

import com.ybrainy.backend.dto.finance.CreateExpenseDTO;
import com.ybrainy.backend.dto.finance.CreateIncomeDTO;
import com.ybrainy.backend.dto.finance.ExpenseResponseDTO;
import com.ybrainy.backend.dto.finance.IncomeResponseDTO;
import com.ybrainy.backend.dto.finance.UpdateExpenseDTO;
import com.ybrainy.backend.dto.finance.UpdateIncomeDTO;
import com.ybrainy.backend.entity.Expense;
import com.ybrainy.backend.entity.Income;
import com.ybrainy.backend.entity.enums.ExpenseCategory;
import com.ybrainy.backend.entity.enums.ExpenseStatus;
import com.ybrainy.backend.entity.enums.PaymentMethod;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-02-17T03:06:36+0100",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.45.0.v20260128-0750, environment: Java 21.0.9 (Eclipse Adoptium)"
)
@Component
public class FinanceMapperImpl implements FinanceMapper {

    @Override
    public Income toIncomeEntity(CreateIncomeDTO dto) {
        if ( dto == null ) {
            return null;
        }

        Income.IncomeBuilder income = Income.builder();

        income.amount( dto.getAmount() );
        income.currency( dto.getCurrency() );
        income.description( dto.getDescription() );
        if ( dto.getPaymentMethod() != null ) {
            income.paymentMethod( Enum.valueOf( PaymentMethod.class, dto.getPaymentMethod() ) );
        }
        income.referenceId( dto.getReferenceId() );
        income.sourceType( dto.getSourceType() );

        income.receivedDate( java.time.LocalDateTime.now() );

        return income.build();
    }

    @Override
    public void updateIncomeEntity(UpdateIncomeDTO dto, Income entity) {
        if ( dto == null ) {
            return;
        }

        entity.setAmount( dto.getAmount() );
        entity.setCurrency( dto.getCurrency() );
        entity.setDescription( dto.getDescription() );
        if ( dto.getPaymentMethod() != null ) {
            entity.setPaymentMethod( Enum.valueOf( PaymentMethod.class, dto.getPaymentMethod() ) );
        }
        else {
            entity.setPaymentMethod( null );
        }
        entity.setReferenceId( dto.getReferenceId() );
        entity.setSourceType( dto.getSourceType() );
    }

    @Override
    public IncomeResponseDTO toIncomeResponseDTO(Income entity) {
        if ( entity == null ) {
            return null;
        }

        IncomeResponseDTO incomeResponseDTO = new IncomeResponseDTO();

        incomeResponseDTO.setAmount( entity.getAmount() );
        incomeResponseDTO.setCreatedAt( entity.getCreatedAt() );
        incomeResponseDTO.setCurrency( entity.getCurrency() );
        incomeResponseDTO.setDescription( entity.getDescription() );
        incomeResponseDTO.setId( entity.getId() );
        if ( entity.getPaymentMethod() != null ) {
            incomeResponseDTO.setPaymentMethod( entity.getPaymentMethod().name() );
        }
        incomeResponseDTO.setReceivedDate( entity.getReceivedDate() );
        incomeResponseDTO.setReferenceId( entity.getReferenceId() );
        incomeResponseDTO.setSourceType( entity.getSourceType() );

        return incomeResponseDTO;
    }

    @Override
    public Expense toExpenseEntity(CreateExpenseDTO dto) {
        if ( dto == null ) {
            return null;
        }

        Expense.ExpenseBuilder expense = Expense.builder();

        expense.amount( dto.getAmount() );
        if ( dto.getCategory() != null ) {
            expense.category( Enum.valueOf( ExpenseCategory.class, dto.getCategory() ) );
        }
        expense.currency( dto.getCurrency() );
        expense.description( dto.getDescription() );
        expense.expenseDate( dto.getExpenseDate() );
        if ( dto.getStatus() != null ) {
            expense.status( Enum.valueOf( ExpenseStatus.class, dto.getStatus() ) );
        }
        expense.title( dto.getTitle() );

        return expense.build();
    }

    @Override
    public void updateExpenseEntity(UpdateExpenseDTO dto, Expense entity) {
        if ( dto == null ) {
            return;
        }

        entity.setAmount( dto.getAmount() );
        if ( dto.getCategory() != null ) {
            entity.setCategory( Enum.valueOf( ExpenseCategory.class, dto.getCategory() ) );
        }
        else {
            entity.setCategory( null );
        }
        entity.setCurrency( dto.getCurrency() );
        entity.setDescription( dto.getDescription() );
        entity.setExpenseDate( dto.getExpenseDate() );
        if ( dto.getStatus() != null ) {
            entity.setStatus( Enum.valueOf( ExpenseStatus.class, dto.getStatus() ) );
        }
        else {
            entity.setStatus( null );
        }
        entity.setTitle( dto.getTitle() );
    }

    @Override
    public ExpenseResponseDTO toExpenseResponseDTO(Expense entity) {
        if ( entity == null ) {
            return null;
        }

        ExpenseResponseDTO expenseResponseDTO = new ExpenseResponseDTO();

        expenseResponseDTO.setAmount( entity.getAmount() );
        if ( entity.getCategory() != null ) {
            expenseResponseDTO.setCategory( entity.getCategory().name() );
        }
        expenseResponseDTO.setCreatedAt( entity.getCreatedAt() );
        expenseResponseDTO.setCurrency( entity.getCurrency() );
        expenseResponseDTO.setDescription( entity.getDescription() );
        expenseResponseDTO.setExpenseDate( entity.getExpenseDate() );
        expenseResponseDTO.setId( entity.getId() );
        if ( entity.getStatus() != null ) {
            expenseResponseDTO.setStatus( entity.getStatus().name() );
        }
        expenseResponseDTO.setTitle( entity.getTitle() );

        return expenseResponseDTO;
    }
}
