package com.bank.payment.orchestration.repository;

import com.bank.payment.orchestration.domain.WorkflowAuditRecord;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkflowAuditRepository extends MongoRepository<WorkflowAuditRecord, String> {
    List<WorkflowAuditRecord> findByUetrOrderByTimestampAsc(String uetr);
}
