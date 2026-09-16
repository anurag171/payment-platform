package com.bank.payment.orchestration.workflow;

import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;

import java.time.Duration;

/**
 * Shared activity-stub options builder. Every workflow impl across every
 * workflow-service calls this instead of each having its own copy — in the monolith
 * this lived as a static method on InboundNormalizationWorkflowImpl and was
 * static-imported by the others, which only worked because they were all in the same
 * package/JAR. Now that each workflow impl lives alone in its own service, the shared
 * bit has to live in the common library instead.
 */
public final class WorkflowSupport {

    public static ActivityOptions activityOptions(String taskQueue) {
        return ActivityOptions.newBuilder()
                .setTaskQueue(taskQueue)
                .setStartToCloseTimeout(Duration.ofSeconds(30))
                .setRetryOptions(RetryOptions.newBuilder()
                        .setMaximumAttempts(3)
                        .setInitialInterval(Duration.ofSeconds(1))
                        .setBackoffCoefficient(2.0)
                        .build())
                .build();
    }

    private WorkflowSupport() {}
}
