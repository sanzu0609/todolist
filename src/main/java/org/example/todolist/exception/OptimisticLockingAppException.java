package org.example.todolist.exception;

public class OptimisticLockingAppException extends RuntimeException {

    public OptimisticLockingAppException(String message) {
        super(message);
    }

    public OptimisticLockingAppException(String message, Throwable cause) {
        super(message, cause);
    }
}
