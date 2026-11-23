package com.delivery.backoffice.shared.messaging;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.messaging")
public record MessagingProperties(String exchange, String orderPlacedQueue) {

    public String deadLetterExchange() {
        return exchange + ".dlx";
    }
}
