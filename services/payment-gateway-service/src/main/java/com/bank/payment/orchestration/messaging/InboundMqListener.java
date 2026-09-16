package com.bank.payment.orchestration.messaging;

import com.bank.payment.orchestration.domain.MessageFormat;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

/**
 * Bridges the two source MQs into the single Kafka ingestion topic.
 * RabbitMQ carries MX (ISO 20022 XML) traffic; ActiveMQ carries MT (SWIFT FIN) traffic
 * in this deployment — swap the format mapping below if your environment differs.
 */
@Component
public class InboundMqListener {

    private final NormalizedPaymentProducer producer;

    public InboundMqListener(NormalizedPaymentProducer producer) {
        this.producer = producer;
    }

    @RabbitListener(queues = "${payment-orchestration.rabbit.inbound-queue}")
    public void onRabbitMessage(String rawMxMessage) {
        producer.publish(rawMxMessage, MessageFormat.MX, "rabbitmq", null);
    }

    @JmsListener(destination = "${payment-orchestration.activemq.inbound-queue}")
    public void onActiveMqMessage(String rawMtMessage) {
        producer.publish(rawMtMessage, MessageFormat.MT, "activemq", null);
    }
}
