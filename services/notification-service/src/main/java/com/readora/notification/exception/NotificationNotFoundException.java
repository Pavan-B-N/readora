package com.readora.notification.exception;

import com.readora.sharedcore.exception.ServiceException;
import org.springframework.http.HttpStatus;

// Thrown when a notification id doesn't exist or doesn't belong to the caller.
public class NotificationNotFoundException extends ServiceException {
    // Builds the 404 response for a missing notification.
    public NotificationNotFoundException() {
        super("NOTIFICATION_NOT_FOUND", HttpStatus.NOT_FOUND, "No such notification");
    }
}
