package com.example.cdrservice.repository;

import com.example.cdrservice.entity.CdrRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CdrRecordRepository extends JpaRepository<CdrRecord, Long> {
}
