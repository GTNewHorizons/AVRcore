package com.github.technus.avrClone.compiler.exceptions;

public class CompilerException extends Exception {
    public CompilerException(String s){
        super(s);
    }
    public CompilerException(String s,Throwable cause){
        super(s,cause);
    }
    public CompilerException(Throwable cause){
        super(cause.getMessage(),cause);
    }
}
