package uk.gov.dwp.uc.pairtest.exception;
import java.util.Objects;

public class InvalidPurchaseException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final ErrorCode errorCode;

    public InvalidPurchaseException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = Objects.requireNonNull(errorCode, "errorCode must not be null");
    }

    /** A fixed code for each rule, so callers can handle errors without relying on the message text. */
    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public enum ErrorCode {
        INVALID_ACCOUNT_ID,
        NO_TICKETS_REQUESTED,
        INVALID_TICKET_REQUEST,
        INVALID_TICKET_QUANTITY,
        MAX_TICKETS_EXCEEDED,
        NO_ADULT_TICKET,
        TOO_MANY_INFANTS
    }

}
