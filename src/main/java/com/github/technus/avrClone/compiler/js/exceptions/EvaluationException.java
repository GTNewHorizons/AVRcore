package com.github.technus.avrClone.compiler.js.exceptions;

import com.github.technus.avrClone.compiler.exceptions.CompilerException;

public class EvaluationException extends CompilerException {
    public EvaluationException(String s,Throwable c){
        super(s,c);
    }
    public EvaluationException(String s){
        super(s);
    }
}
