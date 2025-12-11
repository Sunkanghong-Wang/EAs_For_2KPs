package com.wskh.classes;

public class EarlyTerminationException extends RuntimeException {
    public EarlyTerminationException() {
    }

    public EarlyTerminationException(String message) {
        super(message);
    }

    public EarlyTerminationException(String message, Throwable cause) {
        super(message, cause);
    }

    public EarlyTerminationException(Throwable cause) {
        super(cause);
    }

    public EarlyTerminationException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
