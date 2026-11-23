package com.delivery.backoffice.staff.service;

import java.util.List;
import java.util.Set;

import com.delivery.backoffice.staff.model.StaffMember;

public interface StaffService {

    StaffMember getStaffMemberBySubject(String subject);

    List<StaffMember> getStaffByRestaurant(String restaurantId);

    StaffMember assignStaffMember(String restaurantId, String subject, String displayName);
}
