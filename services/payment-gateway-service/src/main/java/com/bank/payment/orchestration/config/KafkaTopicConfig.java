package com.bank.payment.orchestration.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Declares the topics this service owns. Partner/clearing response topics are created
 * by their respective producing systems in a real deployment — declared here too so
 * local/dev environments (single-broker, auto-create-friendly) work out of the box.
 */
@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic normalizedPaymentsTopic(
            @Value("${payment-orchestration.kafka-topics.normalized-payments}") String topic) {
        return TopicBuilder.name(topic).partitions(6).replicas(3).build();
    }

    @Bean
    public NewTopic sanctionsRequestTopic() {
        return TopicBuilder.name("partner.sanctions.request").partitions(3).replicas(3).build();
    }

    @Bean
    public NewTopic sanctionsResponseTopic() {
        return TopicBuilder.name("partner.sanctions.response").partitions(3).replicas(3).build();
    }

    @Bean
    public NewTopic fraudRequestTopic() {
        return TopicBuilder.name("partner.fraud.request").partitions(3).replicas(3).build();
    }

    @Bean
    public NewTopic fraudResponseTopic() {
        return TopicBuilder.name("partner.fraud.response").partitions(3).replicas(3).build();
    }

    @Bean
    public NewTopic fxRequestTopic() {
        return TopicBuilder.name("partner.fx.request").partitions(3).replicas(3).build();
    }

    @Bean
    public NewTopic fxResponseTopic() {
        return TopicBuilder.name("partner.fx.response").partitions(3).replicas(3).build();
    }

    @Bean
    public NewTopic bookingRequestTopic() {
        return TopicBuilder.name("partner.booking.request").partitions(3).replicas(3).build();
    }

    @Bean
    public NewTopic bookingResponseTopic() {
        return TopicBuilder.name("partner.booking.response").partitions(3).replicas(3).build();
    }
}
