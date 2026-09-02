package com.readora.ai.exception;

import com.readora.sharedcore.exception.ServiceException;
import org.springframework.http.HttpStatus;

public class BackfillAlreadyRunningException extends ServiceException {

    public BackfillAlreadyRunningException() {
        super(
                "BACKFILL_ALREADY_RUNNING",
                HttpStatus.CONFLICT,
                "A backfill is already queued or running. Wait for it to finish."
        );
    }
}
