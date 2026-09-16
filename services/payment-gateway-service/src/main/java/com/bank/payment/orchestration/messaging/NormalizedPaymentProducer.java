package com.bank.payment.orchestration.messaging;

import com.bank.payment.orchestration.domain.MessageFormat;
import com.bank.payment.orchestration.domain.PaymentEnvelope;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/** Normalizes a raw inbound message into a PaymentEnvelope and publishes to the single ingestion topic. */
@Component
public class NormalizedPaymentProducer {

    private final KafkaTemplate<String, PaymentEnvelope> kafkaTemplate;
    private final String topic;

    public NormalizedPaymentProducer(KafkaTemplate<String, PaymentEnvelope> kafkaTemplate,
                                      @Value("${payment-orchestration.kafka-topics.normalized-payments}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    public void publish(String rawMessage, MessageFormat format, String sourceQueue, String senderReferenceHint) {
        PaymentEnvelope envelope = new PaymentEnvelope(
                UUID.randomUUID().toString(),   // UETR generated here if the source can't supply one;
                                                 // the translator/qualification stage will keep it if a
                                                 // real UETR is later found embedded in the message body.
                senderReferenceHint,
                format,
                rawMessage,
                sourceQueue,
                Instant.now());
        kafkaTemplate.send(topic, envelope.getUetr(), envelope);
    }
}
