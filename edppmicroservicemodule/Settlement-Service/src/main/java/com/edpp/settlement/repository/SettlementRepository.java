package com.edpp.settlement.repository;

import com.edpp.settlement.entity.Settlement;
import com.edpp.settlement.enums.SettlementStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SettlementRepository extends JpaRepository<Settlement, String> {

    Optional<Settlement> findBySettlementReference(String reference);

    List<Settlement> findByMerchantIdAndTenantId(String merchantId, String tenantId);

    Page<Settlement> findByMerchantIdAndTenantIdOrderBySettlementDateDesc(String merchantId, 
                                                                          String tenantId, 
                                                                          Pageable pageable);

    List<Settlement> findByStatusAndSettlementDateBefore(SettlementStatus status, LocalDate date);

    List<Settlement> findByStatusAndSettlementDateBetween(SettlementStatus status, 
                                                           LocalDate start, 
                                                           LocalDate end);

    @Query("SELECT COALESCE(SUM(s.netAmount), 0) FROM Settlement s " +
           "WHERE s.merchantId = :merchantId AND s.status = 'COMPLETED' " +
           "AND s.settlementDate BETWEEN :startDate AND :endDate")
    BigDecimal getTotalSettledAmountForMerchant(@Param("merchantId") String merchantId,
                                                 @Param("startDate") LocalDate startDate,
                                                 @Param("endDate") LocalDate endDate);

    @Modifying
    @Query("UPDATE Settlement s SET s.status = :status WHERE s.id = :id")
    int updateStatus(@Param("id") String id, @Param("status") SettlementStatus status);
}