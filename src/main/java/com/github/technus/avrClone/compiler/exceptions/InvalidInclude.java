package com.github.technus.avrClone.compiler.exceptions;

public class InvalidInclude extends CompilerException {
    public InvalidInclude(String s){
        super(s);
    }
    public InvalidInclude(String s,Throwable cause){
        super(s,cause);
    }
}
