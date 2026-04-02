package com.edpp.wallet.service;



import com.edpp.wallet.client.IdentityServiceClient;
import com.edpp.wallet.dtorequest.CreditRequest;
import com.edpp.wallet.dtorequest.DebitRequest;
import com.edpp.wallet.dtoresponse.BalanceResponse;
import com.edpp.wallet.dtoresponse.WalletResponse;
import com.edpp.wallet.entity.Wallet;
import com.edpp.wallet.entity.WalletTransaction;
import com.edpp.wallet.enums.TransactionStatus;
import com.edpp.wallet.enums.TransactionType;
import com.edpp.wallet.enums.WalletStatus;
import com.edpp.wallet.enums.WalletType;
import com.edpp.wallet.exception.InsufficientBalanceException;
import com.edpp.wallet.exception.WalletException;
import com.edpp.wallet.exception.WalletNotFoundException;
import com.edpp.wallet.mapper.WalletMapper;
import com.edpp.wallet.repository.WalletRepository;
import com.edpp.wallet.repository.WalletTransactionRepository;
import com.edpp.wallet.util.RequestContext;
import com.edpp.wallet.util.WalletNumberGenerator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class WalletService {

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository transactionRepository;
    private final WalletMapper walletMapper;
    private final LimitService limitService;
    private final KafkaProducerService kafkaProducerService;
    private final IdentityServiceClient identityServiceClient;
    private final WalletNumberGenerator walletNumberGenerator;
    private final RequestContext requestContext;

    /**
     * Create a new wallet
     */
    @Transactional
    public WalletResponse createWallet(String customerId, WalletType walletType, String currency) {
        String tenantId = requestContext.getTenantId();
        log.info("Creating wallet for customer: {} in tenant: {}", customerId, tenantId);

        // Validate customer exists with Identity Service
        var customer = identityServiceClient.getCustomer(customerId, tenantId);
        if (customer == null || !customer.active()) {
            throw new WalletException("Customer not found or inactive");
        }

        // Check if wallet already exists
        var existingWallet = walletRepository.findByCustomerIdAndWalletTypeAndTenantId(
                customerId, walletType, tenantId);
        if (existingWallet.isPresent()) {
            throw new WalletException("Wallet of type " + walletType + " already exists");
        }

        // Create wallet
        Wallet wallet = Wallet.builder()
                .walletNumber(walletNumberGenerator.generate())
                .customerId(customerId)
                .tenantId(tenantId)
                .walletType(walletType)
                .status(WalletStatus.ACTIVE)
                .balance(BigDecimal.ZERO)
                .availableBalance(BigDecimal.ZERO)
                .ledgerBalance(BigDecimal.ZERO)
                .currency(currency)
                .createdBy(requestContext.getUserId())
                .build();

        Wallet savedWallet = walletRepository.save(wallet);

        // Publish event
        kafkaProducerService.publishWalletCreated(savedWallet);

        log.info("Wallet created successfully: {}", savedWallet.getWalletNumber());
        return walletMapper.toResponse(savedWallet);
    }

    /**
     * Get wallet by number
     */
    @Cacheable(value = "wallets", key = "#walletNumber + '_' + #tenantId")
    public Wallet getWallet(String walletNumber, String tenantId) {
        return walletRepository.findByWalletNumberAndTenantId(walletNumber, tenantId)
                .orElseThrow(() -> new WalletNotFoundException(walletNumber));
    }

    /**
     * Get wallet response by number
     */
    public WalletResponse getWalletResponse(String walletNumber) {
        String tenantId = requestContext.getTenantId();
        Wallet wallet = getWallet(walletNumber, tenantId);
        return walletMapper.toResponse(wallet);
    }

    /**
     * Get balance
     */
    public BalanceResponse getBalance(String walletNumber) {
        String tenantId = requestContext.getTenantId();
        Wallet wallet = getWallet(walletNumber, tenantId);
        return new BalanceResponse(
                wallet.getWalletNumber(),
                wallet.getBalance(),
                wallet.getAvailableBalance(),
                wallet.getLedgerBalance(),
                wallet.getCurrency()
        );
    }

    /**
     * Credit wallet
     */
    @Transactional
    @CacheEvict(value = "wallets", key = "#request.walletNumber() + '_' + #tenantId")
    public WalletTransaction creditWallet(CreditRequest request, String tenantId) {
        log.info("Crediting wallet: {} with amount: {}", request.walletNumber(), request.amount());

        Wallet wallet = getWallet(request.walletNumber(), tenantId);
        validateWalletStatus(wallet);

        // Check if transaction already processed
        if (transactionRepository.findByReference(request.reference()).isPresent()) {
            throw new WalletException("Duplicate transaction reference: " + request.reference());
        }

        BigDecimal balanceBefore = wallet.getBalance();
        BigDecimal balanceAfter = balanceBefore.add(request.amount());

        // Update wallet balance
        wallet.setBalance(balanceAfter);
        wallet.setAvailableBalance(balanceAfter);
        wallet.setLedgerBalance(balanceAfter);
        wallet.setLastTransactionAt(LocalDateTime.now());
        walletRepository.save(wallet);

        // Create transaction record
        WalletTransaction transaction = WalletTransaction.builder()
                .walletId(wallet.getId())
                .walletNumber(wallet.getWalletNumber())
                .reference(request.reference())
                .type(TransactionType.CREDIT)
                .status(TransactionStatus.COMPLETED)
                .amount(request.amount())
                .balanceBefore(balanceBefore)
                .balanceAfter(balanceAfter)
                .description(request.description())
                .relatedTransactionId(request.relatedTransactionId())
                .tenantId(tenantId)
                .build();

        WalletTransaction savedTransaction = transactionRepository.save(transaction);

        // Publish event
        kafkaProducerService.publishWalletCredited(wallet, request.amount());

        log.info("Wallet credited successfully. New balance: {}", balanceAfter);
        return savedTransaction;
    }

    /**
     * Debit wallet
     */
    @Transactional
    @CacheEvict(value = "wallets", key = "#request.walletNumber() + '_' + #tenantId")
    public WalletTransaction debitWallet(DebitRequest request, String tenantId) {
        log.info("Debiting wallet: {} with amount: {}", request.walletNumber(), request.amount());

        Wallet wallet = getWallet(request.walletNumber(), tenantId);
        validateWalletStatus(wallet);

        // Validate limits
        limitService.validateTransactionLimit(wallet, request.amount());

        // Check sufficient balance
        if (wallet.getAvailableBalance().compareTo(request.amount()) < 0) {
            throw new InsufficientBalanceException(
                    wallet.getWalletNumber(),
                    request.amount(),
                    wallet.getAvailableBalance(),
                    wallet.getCurrency()
            );
        }

        // Check if transaction already processed
        if (transactionRepository.findByReference(request.reference()).isPresent()) {
            throw new WalletException("Duplicate transaction reference: " + request.reference());
        }

        BigDecimal balanceBefore = wallet.getBalance();
        BigDecimal balanceAfter = balanceBefore.subtract(request.amount());

        // Update wallet balance
        wallet.setBalance(balanceAfter);
        wallet.setAvailableBalance(balanceAfter);
        wallet.setLedgerBalance(balanceAfter);
        wallet.setLastTransactionAt(LocalDateTime.now());

        // Update spent limits
        limitService.updateSpentLimits(wallet, request.amount());

        walletRepository.save(wallet);

        // Create transaction record
        WalletTransaction transaction = WalletTransaction.builder()
                .walletId(wallet.getId())
                .walletNumber(wallet.getWalletNumber())
                .reference(request.reference())
                .type(TransactionType.DEBIT)
                .status(TransactionStatus.COMPLETED)
                .amount(request.amount())
                .balanceBefore(balanceBefore)
                .balanceAfter(balanceAfter)
                .description(request.description())
                .relatedTransactionId(request.relatedTransactionId())
                .tenantId(tenantId)
                .build();

        WalletTransaction savedTransaction = transactionRepository.save(transaction);

        // Publish event
        kafkaProducerService.publishWalletDebited(wallet, request.amount());

        log.info("Wallet debited successfully. New balance: {}", balanceAfter);
        return savedTransaction;
    }

    /**
     * Freeze wallet
     */
    @Transactional
    @CacheEvict(value = "wallets", key = "#walletNumber + '_' + #tenantId")
    public WalletResponse freezeWallet(String walletNumber, String reason, String tenantId) {
        log.info("Freezing wallet: {} reason: {}", walletNumber, reason);

        Wallet wallet = getWallet(walletNumber, tenantId);

        if (wallet.getStatus() == WalletStatus.FROZEN) {
            throw new WalletException("Wallet already frozen");
        }

        wallet.setStatus(WalletStatus.FROZEN);
        wallet.setUpdatedBy(requestContext.getUserId());
        wallet = walletRepository.save(wallet);

        kafkaProducerService.publishWalletFrozen(wallet, reason);

        return walletMapper.toResponse(wallet);
    }

    /**
     * Unfreeze wallet
     */
    @Transactional
    @CacheEvict(value = "wallets", key = "#walletNumber + '_' + #tenantId")
    public WalletResponse unfreezeWallet(String walletNumber, String reason, String tenantId) {
        log.info("Unfreezing wallet: {} reason: {}", walletNumber, reason);

        Wallet wallet = getWallet(walletNumber, tenantId);

        if (wallet.getStatus() != WalletStatus.FROZEN) {
            throw new WalletException("Wallet is not frozen");
        }

        wallet.setStatus(WalletStatus.ACTIVE);
        wallet.setUpdatedBy(requestContext.getUserId());
        wallet = walletRepository.save(wallet);

        kafkaProducerService.publishWalletUnfrozen(wallet, reason);

        return walletMapper.toResponse(wallet);
    }

    /**
     * Get all wallets for a customer
     */
    public List<WalletResponse> getCustomerWallets(String customerId) {
        String tenantId = requestContext.getTenantId();
        return walletRepository.findByCustomerIdAndTenantId(customerId, tenantId)
                .stream()
                .map(walletMapper::toResponse)
                .toList();
    }

    /**
     * Validate wallet status
     */
    private void validateWalletStatus(Wallet wallet) {
        if (wallet.getStatus() == WalletStatus.BLOCKED) {
            throw new WalletException("Wallet is blocked");
        }
        if (wallet.getStatus() == WalletStatus.FROZEN) {
            throw new WalletException("Wallet is frozen");
        }
        if (wallet.getStatus() == WalletStatus.CLOSED) {
            throw new WalletException("Wallet is closed");
        }
    }
}