package com.bank.payment.orchestration.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/** One row per workflow-stage transition, for auditability (as required by the spec). */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "workflow_audit")
public class WorkflowAuditRecord {

    @Id
    private String id;

    private String uetr;
    private String workflowType;
    private String stage;
    private PaymentStatus status;
    private String details;
    private Instant timestamp;
}
