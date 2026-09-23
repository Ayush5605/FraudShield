package com.bank.account_service.DTO;

import entity.AccountType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateAccountRequest {

    @NotBlank(message="Account Holder name required *")
    private String accountHolderName;

    @NotBlank(message = "Email is required *")
    @Email(message = "Invalid Email format")
    private String email;

    @NotBlank(message = "Phone no. is required *")
    private String phone;

    @NotNull(message = "Account Type is required *")
    private AccountType accountType;

    @NotNull(message = "Initial deposit is required *")
    @Positive(message="Initial Deposit must be valid ")
    private BigDecimal initialDeposit;
}
