package com.github.technus.avrClone.compiler.directives.exceptions;

import com.github.technus.avrClone.compiler.exceptions.CompilerException;

public class InvalidDirective extends CompilerException {
    public InvalidDirective(String s){
        super(s);
    }
}
