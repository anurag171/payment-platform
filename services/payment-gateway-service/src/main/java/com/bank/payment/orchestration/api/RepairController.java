package com.bank.payment.orchestration.api;

import com.bank.payment.orchestration.domain.RepairSubmission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowStub;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** What a real operator-worklist UI would call after fixing a repaired payment. */
@Tag(name = "Repair")
@RestController
@RequestMapping("/api/repair")
public class RepairController {

    private final WorkflowClient workflowClient;

    public RepairController(WorkflowClient workflowClient) {
        this.workflowClient = workflowClient;
    }

    @Operation(summary = "Submit an operator correction for a payment currently in repair (attempt N)")
    @PostMapping("/{uetr}/attempt/{attempt}/submit")
    public ResponseEntity<Void> submitRepair(@PathVariable String uetr, @PathVariable int attempt,
                                              @RequestBody RepairSubmission submission) {
        WorkflowStub stub = workflowClient.newUntypedWorkflowStub(uetr + "-repair-" + attempt);
        stub.signal("submitRepair", submission);
        return ResponseEntity.accepted().build();
    }
}
