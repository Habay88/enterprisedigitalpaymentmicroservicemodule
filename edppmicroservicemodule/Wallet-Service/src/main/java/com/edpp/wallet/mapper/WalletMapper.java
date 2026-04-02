package com.edpp.wallet.mapper;

import com.edpp.wallet.dtoresponse.TransactionResponse;
import com.edpp.wallet.dtoresponse.WalletResponse;
import com.edpp.wallet.entity.Wallet;
import com.edpp.wallet.entity.WalletTransaction;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class WalletMapper {

    public WalletResponse toResponse(Wallet wallet) {
        if (wallet == null) {
            return null;
        }
        
        return new WalletResponse(
                wallet.getId(),
                wallet.getWalletNumber(),
                wallet.getCustomerId(),
                wallet.getWalletType(),
                wallet.getStatus(),
                wallet.getBalance(),
                wallet.getAvailableBalance(),
                wallet.getCurrency(),
                wallet.getDailyTransactionLimit(),
                wallet.getMonthlyTransactionLimit(),
                wallet.getPerTransactionLimit(),
                wallet.getTenantId()
        );
    }

    public List<WalletResponse> toResponseList(List<Wallet> wallets) {
        if (wallets == null) {
            return List.of();
        }
        return wallets.stream()
                .map(this::toResponse)
                .toList();
    }

    public TransactionResponse toTransactionResponse(WalletTransaction transaction) {
        if (transaction == null) {
            return null;
        }
        
        return new TransactionResponse(
                transaction.getId(),
                transaction.getWalletId(),
                transaction.getWalletNumber(),
                transaction.getReference(),
                transaction.getType(),
                transaction.getStatus(),
                transaction.getAmount(),
                transaction.getBalanceBefore(),
                transaction.getBalanceAfter(),
                transaction.getDescription(),
                transaction.getCreatedAt()
        );
    }

    public List<TransactionResponse> toTransactionResponseList(List<WalletTransaction> transactions) {
        if (transactions == null) {
            return List.of();
        }
        return transactions.stream()
                .map(this::toTransactionResponse)
                .toList();
    }
}