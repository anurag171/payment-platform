package com.bank.payment.orchestration.workflow;

/**
 * Single source of truth for every Temporal task queue name in the platform. Each
 * microservice's worker registers against exactly one of these; every cross-service
 * child-workflow call or activity-stub creation targets one of these explicitly (via
 * ChildWorkflowOptions.setTaskQueue / ActivityOptions.setTaskQueue) since, unlike the
 * monolith, workflow and activity implementations no longer share a single worker
 * process or task queue.
 *
 * These names are also what each service's application.yml / k8s ConfigMap sets as
 * `payment-orchestration.temporal.task-queue` — keep this file and the generator
 * manifest (_generator/services.yaml) in sync if you rename one.
 */
public final class TaskQueues {

    // Workflow services
    public static final String INBOUND_NORMALIZATION = "INBOUND_NORMALIZATION_TQ";
    public static final String QUALIFICATION = "QUALIFICATION_TQ";
    public static final String WAREHOUSE = "WAREHOUSE_TQ";
    public static final String MOP_DERIVATION = "MOP_DERIVATION_TQ";
    public static final String REPAIR_WORKFLOW = "REPAIR_WORKFLOW_TQ";
    public static final String PAYMENT_PROCESSING = "PAYMENT_PROCESSING_TQ";

    // Activity services
    public static final String TRANSLATION_ACTIVITY = "TRANSLATION_ACTIVITY_TQ";
    public static final String ENRICHMENT_ACTIVITY = "ENRICHMENT_ACTIVITY_TQ";
    public static final String VALUE_DATE_ACTIVITY = "VALUE_DATE_ACTIVITY_TQ";
    public static final String MOP_ACTIVITY = "MOP_ACTIVITY_TQ";
    public static final String REPAIR_ACTIVITY = "REPAIR_ACTIVITY_TQ";
    public static final String PARTNER_ACTIVITY = "PARTNER_ACTIVITY_TQ";
    public static final String CLEARING_ACTIVITY = "CLEARING_ACTIVITY_TQ";
    public static final String AUDIT_ACTIVITY = "AUDIT_ACTIVITY_TQ";
    public static final String REVERSAL_ACTIVITY = "REVERSAL_ACTIVITY_TQ";

    private TaskQueues() {}
}
