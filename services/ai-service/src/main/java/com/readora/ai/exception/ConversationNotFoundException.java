package com.readora.ai.exception;

import com.readora.sharedcore.exception.ServiceException;
import org.springframework.http.HttpStatus;

// Thrown when a conversationId doesn't exist or belongs to another user.
public class ConversationNotFoundException extends ServiceException {
    // Builds the not-found response for an invalid or foreign conversationId.
    public ConversationNotFoundException() {
        super("CONVERSATION_NOT_FOUND", HttpStatus.NOT_FOUND, "The conversationId does not exist or belongs to another user");
    }
}
