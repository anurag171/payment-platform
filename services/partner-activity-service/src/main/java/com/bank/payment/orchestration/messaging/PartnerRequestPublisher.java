package com.bank.payment.orchestration.messaging;

import com.bank.payment.orchestration.domain.PartnerRequest;
import com.bank.payment.orchestration.domain.PartnerType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/** Publishes an outbound partner request to that partner's request topic. */
@Component
public class PartnerRequestPublisher {

    private final KafkaTemplate<String, PartnerRequest> kafkaTemplate;
    private final String topicPrefix;

    public PartnerRequestPublisher(KafkaTemplate<String, PartnerRequest> kafkaTemplate,
                                    @Value("${payment-orchestration.kafka-topics.partner-request-prefix}") String topicPrefix) {
        this.kafkaTemplate = kafkaTemplate;
        this.topicPrefix = topicPrefix;
    }

    public void send(PartnerRequest request) {
        String topic = topicPrefix + request.getPartnerType().name().toLowerCase() + ".request";
        kafkaTemplate.send(topic, request.getCorrelationId(), request);
    }
}
