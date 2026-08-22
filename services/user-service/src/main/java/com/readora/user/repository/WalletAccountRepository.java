package com.readora.user.repository;

import com.readora.user.entity.WalletAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface WalletAccountRepository extends JpaRepository<WalletAccount, UUID> {
}
