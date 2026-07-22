package com.readora.user.dto;

import java.util.UUID;

// Response after creating an address.
public record CreateAddressResponse(UUID id, boolean isDefault) {
}
