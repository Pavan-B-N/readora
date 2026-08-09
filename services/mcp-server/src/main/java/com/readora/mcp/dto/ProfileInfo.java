package com.readora.mcp.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// A user's profile info.
@JsonIgnoreProperties(ignoreUnknown = true)
public record ProfileInfo(String userId, String email, String displayName, String locale) {
}
