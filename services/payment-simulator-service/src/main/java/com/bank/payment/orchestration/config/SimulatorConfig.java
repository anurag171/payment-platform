package com.bank.payment.orchestration.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Executor backing the fake partner/clearing simulators' delayed responses. Virtual
 * threads (Java 21) are a natural fit here: each "delayed response" is one blocked
 * task waiting out a Duration before publishing to Kafka — cheap to spin up thousands
 * of, no platform-thread pool sizing to think about.
 */
@Configuration
public class SimulatorConfig {

    @Bean
    public Executor simulatorDelayExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
