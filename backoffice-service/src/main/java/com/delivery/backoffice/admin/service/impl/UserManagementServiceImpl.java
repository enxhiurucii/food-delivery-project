package com.delivery.backoffice.admin.service.impl;

import java.time.Clock;

import com.delivery.backoffice.admin.dto.StaffUserResponse;
import com.delivery.backoffice.admin.keycloak.KeycloakAdminClient;
import com.delivery.backoffice.admin.service.UserManagementService;
import com.delivery.backoffice.restaurant.service.RestaurantService;
import com.delivery.backoffice.shared.security.Roles;
import com.delivery.backoffice.staff.model.StaffMember;
import com.delivery.backoffice.staff.repository.StaffMemberRepository;

import org.springframework.stereotype.Service;

@Service
public class UserManagementServiceImpl implements UserManagementService {

    private final KeycloakAdminClient keycloak;
    private final RestaurantService restaurants;
    private final StaffMemberRepository staffMembers;
    private final Clock clock;

    public UserManagementServiceImpl(KeycloakAdminClient keycloak, RestaurantService restaurants,
                                      StaffMemberRepository staffMembers, Clock clock) {
        this.keycloak = keycloak;
        this.restaurants = restaurants;
        this.staffMembers = staffMembers;
        this.clock = clock;
    }

    @Override
    public StaffUserResponse createManager(String restaurantId, String username, String email,
                                            String firstName, String lastName, String password) {
        restaurants.getRestaurantById(restaurantId); // 404 if restaurant doesn't exist
        String userId = keycloak.createUser(username, email, firstName, lastName, password, Roles.MANAGER);
        staffMembers.save(new StaffMember(userId, restaurantId, firstName + " " + lastName, clock.instant()));
        return new StaffUserResponse(userId, username, email, firstName, lastName, Roles.MANAGER, restaurantId);
    }

    @Override
    public StaffUserResponse createDriver(String username, String email,
                                           String firstName, String lastName, String password) {
        String userId = keycloak.createUser(username, email, firstName, lastName, password, Roles.DRIVER);
        return new StaffUserResponse(userId, username, email, firstName, lastName, Roles.DRIVER, null);
    }
}
