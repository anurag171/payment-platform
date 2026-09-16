package com.bank.payment.orchestration.correlation;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Redis-backed pending-correlation registry for async partner/clearing round trips.
 *
 * The correlation id itself is self-describing (it embeds the target Temporal workflow
 * id, see {@link #buildCorrelationId}), so signalling the workflow never needs a lookup
 * here. Redis's job is narrower: guard against duplicate/late signal delivery if a
 * partner or clearing rail redelivers a response (common with at-least-once MQ/Kafka
 * semantics), and give ops a place to see what's currently in flight.
 */
@Service
public class PartnerCorrelationService {

    private final StringRedisTemplate redisTemplate;
    private final long pendingTtlSeconds;

    public PartnerCorrelationService(StringRedisTemplate redisTemplate,
                                      @Value("${payment-orchestration.correlation.pending-ttl-seconds:900}") long pendingTtlSeconds) {
        this.redisTemplate = redisTemplate;
        this.pendingTtlSeconds = pendingTtlSeconds;
    }

    public String buildCorrelationId(String workflowId, String suffix) {
        return workflowId + "::" + suffix;
    }

    public void registerPending(String correlationId) {
        redisTemplate.opsForValue().set(key(correlationId), "PENDING", Duration.ofSeconds(pendingTtlSeconds));
    }

    /** Returns true and marks resolved iff this correlation id was pending (i.e. this is the first delivery). */
    public boolean markResolvedIfPending(String correlationId) {
        Boolean deleted = redisTemplate.delete(key(correlationId));
        return Boolean.TRUE.equals(deleted);
    }

    public boolean isPending(String correlationId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key(correlationId)));
    }

    private String key(String correlationId) {
        return "partner-correlation:" + correlationId;
    }
}
