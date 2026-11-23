package com.delivery.backoffice.admin.service;

import com.delivery.backoffice.admin.dto.StaffUserResponse;

public interface UserManagementService {

    StaffUserResponse createManager(String restaurantId, String username, String email,
                                     String firstName, String lastName, String password);

    StaffUserResponse createDriver(String username, String email,
                                    String firstName, String lastName, String password);
}
