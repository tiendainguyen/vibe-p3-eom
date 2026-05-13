package com.example.eom.exception;

public class SelfDeactivationException extends RuntimeException {

    public SelfDeactivationException() {
        super("Admin cannot deactivate their own account");
    }
}
