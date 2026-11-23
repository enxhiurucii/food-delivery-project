package com.delivery.backoffice.order.model;

public record DeliveryAddress(String street, String city, String postalCode, String phone, String instructions) {
}
