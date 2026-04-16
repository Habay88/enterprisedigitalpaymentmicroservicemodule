package com.edpp.ledger.controller;

import com.edpp.ledger.dto.response.request.CreateGLAccountRequest;
import com.edpp.ledger.dto.response.response.GLAccountResponse;
import com.edpp.ledger.entity.GLAccount;
import com.edpp.ledger.mapper.LedgerMapper;
import com.edpp.ledger.repository.GLAccountRepository;
import com.edpp.ledger.util.RequestContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ledger/accounts")
@RequiredArgsConstructor
@Tag(name = "GL Accounts", description = "Chart of Accounts management")
public class GLAccountController {

    private final GLAccountRepository glAccountRepository;
    private final LedgerMapper mapper;

    @PostMapping
    @Operation(summary = "Create a new GL account")
    public ResponseEntity<GLAccountResponse> createAccount(@Valid @RequestBody CreateGLAccountRequest request) {
        String tenantId = RequestContext.getCurrentTenantId();

        GLAccount account = GLAccount.builder()
                .accountCode(request.accountCode())
                .accountName(request.accountName())
                .accountType(request.accountType())
                .normalBalance(request.accountType().getNormalBalance())
                .parentAccountCode(request.parentAccountCode())
                .tenantId(tenantId)
                .active(true)
                .description(request.description())
                .build();

        GLAccount saved = glAccountRepository.save(account);
        return new ResponseEntity<>(mapper.toGLAccountResponse(saved), HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "Get all GL accounts")
    public ResponseEntity<List<GLAccountResponse>> getAllAccounts() {
        String tenantId = RequestContext.getCurrentTenantId();
        List<GLAccount> accounts = glAccountRepository.findByTenantIdOrderByAccountCode(tenantId);
        return ResponseEntity.ok(accounts.stream().map(mapper::toGLAccountResponse).toList());
    }

    @GetMapping("/{accountCode}")
    @Operation(summary = "Get GL account by code")
    public ResponseEntity<GLAccountResponse> getAccount(@PathVariable String accountCode) {
        String tenantId = RequestContext.getCurrentTenantId();
        GLAccount account = glAccountRepository.findByAccountCodeAndTenantId(accountCode, tenantId)
                .orElseThrow(() -> new RuntimeException("Account not found"));
        return ResponseEntity.ok(mapper.toGLAccountResponse(account));
    }
}