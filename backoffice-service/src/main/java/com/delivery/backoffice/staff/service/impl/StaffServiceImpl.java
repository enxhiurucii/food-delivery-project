package com.delivery.backoffice.staff.service.impl;

import java.time.Clock;
import java.util.List;

import com.delivery.backoffice.restaurant.service.RestaurantService;
import com.delivery.backoffice.staff.model.StaffMember;
import com.delivery.backoffice.staff.repository.StaffMemberRepository;
import com.delivery.backoffice.staff.service.StaffService;

import org.springframework.stereotype.Service;

@Service
public class StaffServiceImpl implements StaffService {

    private final StaffMemberRepository staffMembers;
    private final RestaurantService restaurants;
    private final Clock clock;

    public StaffServiceImpl(StaffMemberRepository staffMembers, RestaurantService restaurants, Clock clock) {
        this.staffMembers = staffMembers;
        this.restaurants = restaurants;
        this.clock = clock;
    }

    @Override
    public StaffMember getStaffMemberBySubject(String subject) {
        return staffMembers.findById(subject).orElse(null);
    }

    @Override
    public List<StaffMember> getStaffByRestaurant(String restaurantId) {
        return staffMembers.findByRestaurantId(restaurantId);
    }

    @Override
    public StaffMember assignStaffMember(String restaurantId, String subject, String displayName) {
        restaurants.getRestaurantById(restaurantId); // 404 if the restaurant does not exist
        return staffMembers.save(new StaffMember(subject, restaurantId, displayName, clock.instant()));
    }
}
