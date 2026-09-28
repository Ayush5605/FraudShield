package com.bank.account_service.Service;

import com.bank.account_service.DTO.AccountResponse;
import com.bank.account_service.DTO.CreateAccountRequest;
import com.bank.account_service.Repository.AccountRepo;
import com.bank.account_service.entity.Account;
import com.bank.account_service.entity.AccountStatus;
import com.bank.account_service.entity.AccountType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.security.SecureRandom;

import static com.bank.account_service.entity.AccountStatus.BLOCKED;

@Service
@Slf4j
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepo accountRepo;
    private static SecureRandom secureRandom=new SecureRandom();
    public AccountResponse createAccount(CreateAccountRequest request){
        log.info("Creating account for :{}",request.getEmail());

        if(accountRepo.existsByEmail(request.getEmail())){
            throw new RuntimeException("Account already exists for this email :"+request.getEmail());

        }

        Account account=new Account();
        account.setAccountHolderName(request.getAccountHolderName());
        account.setEmail(request.getEmail());
        account.setPhone(request.getPhone());
        account.setAccountType(request.getAccountType());
        account.setStatus(AccountStatus.ACTIVE);
        account.setBalance(request.getInitialDeposit());
        account.setAccountNumber(generateAccountNumber());
        account.setDailyTransactionLimit(request.getAccountType()== AccountType.SAVINGS?
                new BigDecimal("100000"):new BigDecimal("500000"));
        Account savedAccount=accountRepo.save(account);
        log.info("Account created:{}",savedAccount.getAccountNumber());

        return mapToResponse(savedAccount);


    };

    private AccountResponse mapToResponse(Account account){
        AccountResponse response=new AccountResponse();

        response.setId(account.getId());
        response.setAccountNumber(account.getAccountNumber());
        response.setAccountHolderName(account.getAccountHolderName());
        response.setEmail(account.getEmail());
        response.setPhone(account.getPhone());
        response.setAccountType(account.getAccountType());
        response.setStatus(account.getStatus());
        response.setBalance(account.getBalance());
        response.setDailyTransactionLimit(account.getDailyTransactionLimit());
        response.setCreatedAt(account.getCreatedAt());

        return response;

    }

    private String generateAccountNumber(){

        String accountNumber;

        do{
            long number = secureRandom.nextLong(1_000_000_000_000L);

            accountNumber=String.format("%012d",number);

        }while(accountRepo.existsByAccountNumber(accountNumber));

        return accountNumber;


    }

    public AccountResponse getAccount(String accountNumber) {
        Account account=accountRepo.findByAccountNumber(accountNumber)
                .orElseThrow(()-> new RuntimeException("Account Not Found !"));

        return mapToResponse(account);
    }

    public BigDecimal getBalance(String accountNumber) {
        Account account=accountRepo.findByAccountNumber(accountNumber)
                .orElseThrow(()-> new RuntimeException("Account Not Found !"));

        return account.getBalance();
    }

    /** called by fraud-detection servie via kafka **/
    public void blockAccount(String accountNumber) {
        log.info("Blocking account :"+accountNumber);

        Account account=accountRepo.findByAccountNumber(accountNumber)
                .orElseThrow(()->new RuntimeException("Account not found !"));

        account.setStatus(AccountStatus.BLOCKED);
        accountRepo.save(account);

        log.info(accountNumber+"Account Blocked !");

    }

    /** called by transaction-service **/
    public void deductBalance(String accountNumber,BigDecimal amount){
        log.info("Deducting Balance :"+accountNumber);
        Account account=accountRepo.findByAccountNumber(accountNumber)
                .orElseThrow(()->new RuntimeException("Account not found !"));

        if(account.getStatus() !=AccountStatus.ACTIVE){
            throw new RuntimeException("Account not active !!");

        }else{
            if(account.getBalance().compareTo(amount)<0){
                throw new RuntimeException("Insufficient Balance !");
            }

            account.setBalance(account.getBalance().subtract(amount));
            accountRepo.save(account);

            log.info("Amount debited successfully");


        }

    }

    public void credit_balance(String accountNumber,BigDecimal amount){
        Account account=accountRepo.findByAccountNumber(accountNumber)
                .orElseThrow(()->new RuntimeException("Account not found !"));

        if(account.getStatus()!=AccountStatus.ACTIVE){
            throw  new RuntimeException("Account is not active !");
        }else{
            account.setBalance(account.getBalance().add(amount));

            accountRepo.save(account);
            log.info("Amount {} credited successfully!", amount);
        }
    }
}
