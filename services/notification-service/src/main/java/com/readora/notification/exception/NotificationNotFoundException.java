package com.readora.notification.exception;

import com.readora.sharedcore.exception.ServiceException;
import org.springframework.http.HttpStatus;

public class NotificationNotFoundException extends ServiceException {
    public NotificationNotFoundException() {
        super("NOTIFICATION_NOT_FOUND", HttpStatus.NOT_FOUND, "No such notification");
    }
}
