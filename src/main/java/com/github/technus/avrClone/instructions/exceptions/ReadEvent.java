package com.github.technus.avrClone.instructions.exceptions;

import com.github.technus.avrClone.instructions.exceptions.EventException;

public class ReadEvent extends EventException {
    public ReadEvent(String message) {
        super(message);
    }

    public ReadEvent(String message, Throwable cause) {
        super(message, cause);
    }
}
