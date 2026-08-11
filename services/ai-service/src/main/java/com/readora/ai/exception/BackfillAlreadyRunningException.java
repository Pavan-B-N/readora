package com.readora.ai.exception;

import com.readora.sharedcore.exception.ServiceException;
import org.springframework.http.HttpStatus;

// Thrown when a backfill is requested while another one is already queued or running.
public class BackfillAlreadyRunningException extends ServiceException {

    // Builds the conflict response for a duplicate backfill request.
    public BackfillAlreadyRunningException() {
        super(
                "BACKFILL_ALREADY_RUNNING",
                HttpStatus.CONFLICT,
                "A backfill is already queued or running. Wait for it to finish."
        );
    }
}
