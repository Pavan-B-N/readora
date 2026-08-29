package com.readora.delivery.exception;

import org.springframework.http.HttpStatus;

/** Going off duty mid-job would let the agent walk away from a claimed delivery or pickup — must finish or the order gets stranded. */
public class CannotGoOffDutyException extends ServiceException {
    public CannotGoOffDutyException() {
        super("CANNOT_GO_OFF_DUTY", HttpStatus.CONFLICT, "Finish your active delivery or pickup before going off duty");
    }
}
