package com.readora.ai.exception;

import com.readora.sharedcore.exception.ServiceException;
import org.springframework.http.HttpStatus;

public class ConversationNotFoundException extends ServiceException {
    public ConversationNotFoundException() {
        super("CONVERSATION_NOT_FOUND", HttpStatus.NOT_FOUND, "The conversationId does not exist or belongs to another user");
    }
}
