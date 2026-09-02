package com.readora.delivery.exception;

import com.readora.sharedcore.exception.ServiceException;
import org.springframework.http.HttpStatus;

/** The caller has the DELIVERY_AGENT role but no agent profile row — shouldn't happen outside manual account setup gone wrong. */
public class AgentNotFoundException extends ServiceException {
    public AgentNotFoundException() {
        super("AGENT_NOT_FOUND", HttpStatus.FORBIDDEN, "Your account isn't set up as a delivery agent yet");
    }
}
