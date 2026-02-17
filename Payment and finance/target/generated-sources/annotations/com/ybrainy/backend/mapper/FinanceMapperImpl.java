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
    date = "2026-02-17T02:07:25+0100",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 17.0.8 (Oracle Corporation)"
)
@Component
public class FinanceMapperImpl implements FinanceMapper {

    @Override
    public Income toIncomeEntity(CreateIncomeDTO dto) {
        if ( dto == null ) {
            return null;
        }

        Income.IncomeBuilder income = Income.builder();

        income.sourceType( dto.getSourceType() );
        income.referenceId( dto.getReferenceId() );
        income.description( dto.getDescription() );
        income.amount( dto.getAmount() );
        income.currency( dto.getCurrency() );
        if ( dto.getPaymentMethod() != null ) {
            income.paymentMethod( Enum.valueOf( PaymentMethod.class, dto.getPaymentMethod() ) );
        }

        income.receivedDate( java.time.LocalDateTime.now() );

        return income.build();
    }

    @Override
    public void updateIncomeEntity(UpdateIncomeDTO dto, Income entity) {
        if ( dto == null ) {
            return;
        }

        entity.setSourceType( dto.getSourceType() );
        entity.setReferenceId( dto.getReferenceId() );
        entity.setDescription( dto.getDescription() );
        entity.setAmount( dto.getAmount() );
        entity.setCurrency( dto.getCurrency() );
        if ( dto.getPaymentMethod() != null ) {
            entity.setPaymentMethod( Enum.valueOf( PaymentMethod.class, dto.getPaymentMethod() ) );
        }
        else {
            entity.setPaymentMethod( null );
        }
    }

    @Override
    public IncomeResponseDTO toIncomeResponseDTO(Income entity) {
        if ( entity == null ) {
            return null;
        }

        IncomeResponseDTO incomeResponseDTO = new IncomeResponseDTO();

        incomeResponseDTO.setId( entity.getId() );
        incomeResponseDTO.setSourceType( entity.getSourceType() );
        incomeResponseDTO.setReferenceId( entity.getReferenceId() );
        incomeResponseDTO.setDescription( entity.getDescription() );
        incomeResponseDTO.setAmount( entity.getAmount() );
        incomeResponseDTO.setCurrency( entity.getCurrency() );
        if ( entity.getPaymentMethod() != null ) {
            incomeResponseDTO.setPaymentMethod( entity.getPaymentMethod().name() );
        }
        incomeResponseDTO.setReceivedDate( entity.getReceivedDate() );
        incomeResponseDTO.setCreatedAt( entity.getCreatedAt() );

        return incomeResponseDTO;
    }

    @Override
    public Expense toExpenseEntity(CreateExpenseDTO dto) {
        if ( dto == null ) {
            return null;
        }

        Expense.ExpenseBuilder expense = Expense.builder();

        expense.title( dto.getTitle() );
        expense.description( dto.getDescription() );
        if ( dto.getCategory() != null ) {
            expense.category( Enum.valueOf( ExpenseCategory.class, dto.getCategory() ) );
        }
        expense.amount( dto.getAmount() );
        expense.currency( dto.getCurrency() );
        expense.expenseDate( dto.getExpenseDate() );
        if ( dto.getStatus() != null ) {
            expense.status( Enum.valueOf( ExpenseStatus.class, dto.getStatus() ) );
        }

        return expense.build();
    }

    @Override
    public void updateExpenseEntity(UpdateExpenseDTO dto, Expense entity) {
        if ( dto == null ) {
            return;
        }

        entity.setTitle( dto.getTitle() );
        entity.setDescription( dto.getDescription() );
        if ( dto.getCategory() != null ) {
            entity.setCategory( Enum.valueOf( ExpenseCategory.class, dto.getCategory() ) );
        }
        else {
            entity.setCategory( null );
        }
        entity.setAmount( dto.getAmount() );
        entity.setCurrency( dto.getCurrency() );
        entity.setExpenseDate( dto.getExpenseDate() );
        if ( dto.getStatus() != null ) {
            entity.setStatus( Enum.valueOf( ExpenseStatus.class, dto.getStatus() ) );
        }
        else {
            entity.setStatus( null );
        }
    }

    @Override
    public ExpenseResponseDTO toExpenseResponseDTO(Expense entity) {
        if ( entity == null ) {
            return null;
        }

        ExpenseResponseDTO expenseResponseDTO = new ExpenseResponseDTO();

        expenseResponseDTO.setId( entity.getId() );
        expenseResponseDTO.setTitle( entity.getTitle() );
        expenseResponseDTO.setDescription( entity.getDescription() );
        expenseResponseDTO.setAmount( entity.getAmount() );
        expenseResponseDTO.setCurrency( entity.getCurrency() );
        if ( entity.getCategory() != null ) {
            expenseResponseDTO.setCategory( entity.getCategory().name() );
        }
        if ( entity.getStatus() != null ) {
            expenseResponseDTO.setStatus( entity.getStatus().name() );
        }
        expenseResponseDTO.setExpenseDate( entity.getExpenseDate() );
        expenseResponseDTO.setCreatedAt( entity.getCreatedAt() );

        return expenseResponseDTO;
    }
}
