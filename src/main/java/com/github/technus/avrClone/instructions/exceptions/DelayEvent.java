package com.github.technus.avrClone.instructions.exceptions;

public class DelayEvent extends EventException {
    public DelayEvent(String message) {
        super(message);
    }

    public DelayEvent(String message, Throwable cause) {
        super(message, cause);
    }
}
