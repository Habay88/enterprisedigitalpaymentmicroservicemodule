package com.edpp.iso8583.repository;

import com.edpp.iso8583.entity.TerminalSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TerminalSessionRepository extends JpaRepository<TerminalSession, String> {

    Optional<TerminalSession> findByTerminalId(String terminalId);

    Optional<TerminalSession> findByTerminalIdAndMerchantId(String terminalId, String merchantId);
}