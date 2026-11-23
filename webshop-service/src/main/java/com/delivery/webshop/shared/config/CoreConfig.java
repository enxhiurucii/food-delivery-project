package com.delivery.webshop.shared.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;

@Configuration(proxyBeanMethods = false)
public class CoreConfig {

    /** Injected everywhere "now" is needed, so tests can control time. */
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    /**
     * Enables @Transactional on MongoDB (requires a replica set; docker-compose runs a single-node one).
     * Used so that a state change and its outbox event are written atomically.
     */
    @Bean
    MongoTransactionManager transactionManager(MongoDatabaseFactory factory) {
        return new MongoTransactionManager(factory);
    }
}
