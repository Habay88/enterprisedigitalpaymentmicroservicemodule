package com.edpp.iso8583.repository;

import com.edpp.iso8583.entity.IsoMessageLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface IsoMessageLogRepository extends JpaRepository<IsoMessageLog, String> {

    List<IsoMessageLog> findByStan(String stan);

    Page<IsoMessageLog> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end, Pageable pageable);

    long countByMtiAndCreatedAtBetween(String mti, LocalDateTime start, LocalDateTime end);
}