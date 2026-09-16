package com.bank.payment.orchestration.api;

import com.bank.payment.orchestration.domain.Payment;
import com.bank.payment.orchestration.domain.WorkflowAuditRecord;
import com.bank.payment.orchestration.repository.PaymentRepository;
import com.bank.payment.orchestration.repository.WorkflowAuditRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Workflow status")
@RestController
@RequestMapping("/api/payments")
public class WorkflowStatusController {

    private final PaymentRepository paymentRepository;
    private final WorkflowAuditRepository auditRepository;

    public WorkflowStatusController(PaymentRepository paymentRepository, WorkflowAuditRepository auditRepository) {
        this.paymentRepository = paymentRepository;
        this.auditRepository = auditRepository;
    }

    @Operation(summary = "Get current payment state by UETR")
    @GetMapping("/{uetr}")
    public ResponseEntity<Payment> getPayment(@PathVariable String uetr) {
        return paymentRepository.findById(uetr)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Operation(summary = "Get the full stage-by-stage audit trail for a payment")
    @GetMapping("/{uetr}/audit")
    public List<WorkflowAuditRecord> getAuditTrail(@PathVariable String uetr) {
        return auditRepository.findByUetrOrderByTimestampAsc(uetr);
    }
}
