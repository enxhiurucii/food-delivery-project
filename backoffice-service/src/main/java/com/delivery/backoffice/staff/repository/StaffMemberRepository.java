package com.delivery.backoffice.staff.repository;

import java.util.List;

import com.delivery.backoffice.staff.model.StaffMember;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface StaffMemberRepository extends MongoRepository<StaffMember, String> {

    List<StaffMember> findByRestaurantId(String restaurantId);
}
