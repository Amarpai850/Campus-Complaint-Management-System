package com.campus.complaint.exception;

/**
 * Exception thrown when an invalid operation is attempted (e.g., invalid status transition).
 */
public class InvalidOperationException extends RuntimeException {
    public InvalidOperationException(String message) {
        super(message);
    }
}
