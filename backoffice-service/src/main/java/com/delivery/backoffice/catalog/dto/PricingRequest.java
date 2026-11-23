package com.delivery.backoffice.catalog.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

public record PricingRequest(@NotEmpty List<@Valid RequestedLine> lines) {
}
