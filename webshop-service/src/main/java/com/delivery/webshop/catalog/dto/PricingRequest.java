package com.delivery.webshop.catalog.dto;

import java.util.List;

public record PricingRequest(List<RequestedLine> lines) {
}
