package com.github.technus.avrClone.instructions.exceptions;

public class WriteEvent extends EventException {
    public WriteEvent(String message) {
        super(message);
    }

    public WriteEvent(String message, Throwable cause) {
        super(message, cause);
    }
}
