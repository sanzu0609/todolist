package org.example.todolist.exception;

public class AccessDeniedBusinessException extends RuntimeException {

    public AccessDeniedBusinessException(String message) {
        super(message);
    }

    public AccessDeniedBusinessException(String message, Throwable cause) {
        super(message, cause);
    }
}
