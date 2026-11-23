package com.delivery.webshop.checkout.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddressRequest(@NotBlank String street, @NotBlank String city, String postalCode,
                             @NotBlank String phone, @Size(max = 300) String instructions) {
}
