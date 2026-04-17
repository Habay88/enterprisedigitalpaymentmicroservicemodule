package com.edpp.settlement.repository;

import com.edpp.settlement.entity.SettlementBatch;
import com.edpp.settlement.enums.BatchStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SettlementBatchRepository extends JpaRepository<SettlementBatch, String> {

    Optional<SettlementBatch> findByBatchReference(String batchReference);

    List<SettlementBatch> findByBatchDateAndStatus(LocalDate batchDate, BatchStatus status);

    List<SettlementBatch> findByStatus(BatchStatus status);

    @Query("SELECT MAX(sb.batchDate) FROM SettlementBatch sb WHERE sb.status = 'COMPLETED'")
    Optional<LocalDate> findLastCompletedBatchDate();
}