package com.bank.payment.orchestration.config;

import jakarta.jms.ConnectionFactory;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.support.converter.MappingJackson2MessageConverter;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.converter.MessageType;

/** Classic ActiveMQ 5.x connectivity for the MT inbound queue. */
@EnableJms
@Configuration
public class JmsConfig {

    @Bean
    public ConnectionFactory connectionFactory(
            @Value("${payment-orchestration.activemq.broker-url}") String brokerUrl,
            @Value("${payment-orchestration.activemq.username}") String username,
            @Value("${payment-orchestration.activemq.password}") String password) {
        ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(brokerUrl);
        factory.setUserName(username);
        factory.setPassword(password);
        return factory;
    }

    @Bean
    public DefaultJmsListenerContainerFactory jmsListenerContainerFactory(ConnectionFactory connectionFactory) {
        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setConcurrency("3-10");
        return factory;
    }

    @Bean
    public MessageConverter jmsMessageConverter() {
        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        converter.setTargetType(MessageType.TEXT);
        converter.setTypeIdPropertyName("_type");
        return converter;
    }
}
