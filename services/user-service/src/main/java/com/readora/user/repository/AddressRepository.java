package com.readora.user.repository;

import com.readora.user.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AddressRepository extends JpaRepository<Address, UUID> {

    List<Address> findAllByUserIdAndDeletedAtIsNullOrderByIsDefaultDesc(UUID userId);

    long countByUserIdAndDeletedAtIsNull(UUID userId);

    Optional<Address> findByIdAndUserId(UUID id, UUID userId);
}
