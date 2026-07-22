package com.readora.user.repository;

import com.readora.user.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

// Spring Data repository for addresses.
public interface AddressRepository extends JpaRepository<Address, UUID> {

    // Non-deleted addresses for a user, default first.
    List<Address> findAllByUserIdAndDeletedAtIsNullOrderByIsDefaultDesc(UUID userId);

    // Count of a user's non-deleted addresses, for enforcing the address limit.
    long countByUserIdAndDeletedAtIsNull(UUID userId);

    // Looks up an address, scoped to its owning user.
    Optional<Address> findByIdAndUserId(UUID id, UUID userId);
}
