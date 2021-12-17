package com.github.technus.avrClone.instructions.exceptions;

public class DebugEvent extends EventException {
    public DebugEvent(String message) {
        super(message);
    }

    public DebugEvent(String message, Throwable cause) {
        super(message, cause);
    }
}
