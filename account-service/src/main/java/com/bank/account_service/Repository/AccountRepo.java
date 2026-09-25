package com.bank.account_service.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.bank.account_service.entity.Account;
import org.springframework.stereotype.Repository;

import java.util.Optional;


@Repository
public interface AccountRepo extends JpaRepository<Account,String> {
    boolean existsByEmail(String email);


    boolean existsByAccountNumber(String accountNumber);

    Optional<Account> findByAccountNumber(String accountNumber);
}
