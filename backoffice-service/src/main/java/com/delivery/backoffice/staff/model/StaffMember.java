package com.delivery.backoffice.staff.model;

import java.time.Instant;

import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Back-office profile of a Keycloak user. Credentials live only in Keycloak; this service stores the
 * domain relationship "user X works at restaurant Y", keyed by the token subject (never by email,
 * which can change).
 */
@Getter
@Document("staff_members")
public class StaffMember {

    @Id
    private String subject;
    @Indexed
    private String restaurantId;
    private String displayName;
    private Instant createdAt;

    protected StaffMember() {
    }

    public StaffMember(String subject, String restaurantId, String displayName, Instant createdAt) {
        this.subject = subject;
        this.restaurantId = restaurantId;
        this.displayName = displayName;
        this.createdAt = createdAt;
    }
}
