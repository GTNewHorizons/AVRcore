package com.github.technus.avrClone.instructions.exceptions;

import com.github.technus.avrClone.instructions.exceptions.EventException;

public class WatchdogEvent extends EventException {
    public WatchdogEvent(String message) {
        super(message);
    }

    public WatchdogEvent(String message, Throwable cause) {
        super(message, cause);
    }
}
