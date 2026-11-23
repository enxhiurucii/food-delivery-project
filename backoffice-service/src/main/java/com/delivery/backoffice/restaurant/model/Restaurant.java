package com.delivery.backoffice.restaurant.model;

import java.time.Instant;
import java.time.ZoneId;

import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

/** Restaurant and its configuration ("how the restaurant works"). Money in cents. */
@Getter
@Document("restaurants")
public class Restaurant {

    @Id
    private String id;
    private String name;
    private String description;
    private String address;
    private String phone;
    private String imageUrl;
    private String timezone;
    private boolean acceptingOrders;
    private boolean pickupEnabled;
    private boolean deliveryEnabled;
    private long deliveryFeeCents;
    private long minimumOrderCents;
    private int estimatedPrepMinutes;
    private Instant createdAt;
    private Instant updatedAt;
    @Version
    private Long version;

    protected Restaurant() {
    }

    /** @param id optional fixed id (used by demo data); null lets MongoDB generate one. */
    public static Restaurant create(String id, RestaurantDetails details, Instant now) {
        Restaurant restaurant = new Restaurant();
        restaurant.id = id;
        restaurant.createdAt = now;
        restaurant.apply(details, now);
        return restaurant;
    }

    public void update(RestaurantDetails details, Instant now) {
        apply(details, now);
    }

    public ZoneId zone() {
        return ZoneId.of(timezone);
    }

    private void apply(RestaurantDetails details, Instant now) {
        details.validate();
        this.name = details.name();
        this.description = details.description();
        this.address = details.address();
        this.phone = details.phone();
        this.imageUrl = details.imageUrl();
        this.timezone = details.timezone();
        this.acceptingOrders = details.acceptingOrders();
        this.pickupEnabled = details.pickupEnabled();
        this.deliveryEnabled = details.deliveryEnabled();
        this.deliveryFeeCents = details.deliveryFeeCents();
        this.minimumOrderCents = details.minimumOrderCents();
        this.estimatedPrepMinutes = details.estimatedPrepMinutes();
        this.updatedAt = now;
    }
}
