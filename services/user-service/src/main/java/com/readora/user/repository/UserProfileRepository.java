package com.readora.user.repository;

import com.readora.user.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

// Spring Data repository for user profiles.
public interface UserProfileRepository extends JpaRepository<UserProfile, UUID> {

    // Looks up the admin profile assigned to a given store, if any.
    Optional<UserProfile> findFirstByAdminStoreId(UUID storeId);
}
