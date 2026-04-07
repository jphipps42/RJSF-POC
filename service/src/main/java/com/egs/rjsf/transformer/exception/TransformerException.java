package com.egs.rjsf.transformer.exception;

public class TransformerException extends RuntimeException {
    private final String errorCode;

    public TransformerException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public TransformerException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
