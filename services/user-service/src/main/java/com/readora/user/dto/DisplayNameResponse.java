package com.readora.user.dto;

/** displayName is null if the user has no profile yet, or never set one. */
public record DisplayNameResponse(String displayName) {
}
