package com.delivery.webshop

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.testcontainers.mongodb.MongoDBContainer
import org.testcontainers.rabbitmq.RabbitMQContainer
import org.testcontainers.utility.DockerImageName

/** Real MongoDB (replica set, so transactions work) and RabbitMQ for integration specs. Requires Docker. */
@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfig {

    @Bean
    @ServiceConnection
    MongoDBContainer mongoDbContainer() {
        new MongoDBContainer(DockerImageName.parse('mongo:8.0'))
    }

    @Bean
    @ServiceConnection
    RabbitMQContainer rabbitMqContainer() {
        new RabbitMQContainer(DockerImageName.parse('rabbitmq:4.1-management'))
    }
}
