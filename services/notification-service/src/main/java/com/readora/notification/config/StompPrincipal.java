package com.readora.notification.config;

import java.security.Principal;

// A minimal Principal wrapping the authenticated user id, bound to a STOMP session.
public record StompPrincipal(String name) implements Principal {
    // The principal's name, i.e. the user id.
    @Override
    public String getName() {
        return name;
    }
}
