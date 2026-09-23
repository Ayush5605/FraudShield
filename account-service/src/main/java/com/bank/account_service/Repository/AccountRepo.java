package com.bank.account_service.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.bank.account_service.entity.Account;


public interface AccountRepo extends JpaRepository<Account,String> {
}
