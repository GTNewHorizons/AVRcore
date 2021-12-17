package com.github.technus.avrCloneGui.presentation;

import java.nio.charset.Charset;

public interface Presentation<T> {
    Charset UTF_32=Charset.forName("UTF-32");
    String present(T... t);
}
