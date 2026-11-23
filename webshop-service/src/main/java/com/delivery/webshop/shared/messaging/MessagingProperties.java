package com.delivery.webshop.shared.messaging;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.messaging")
public record MessagingProperties(String exchange, String orderStatusQueue) {

    public String deadLetterExchange() {
        return exchange + ".dlx";
    }
}
