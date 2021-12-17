package com.github.technus.avrClone.compiler.directives;

import com.github.technus.avrClone.compiler.Line;
import com.github.technus.avrClone.compiler.ProgramCompiler;
import com.github.technus.avrClone.compiler.exceptions.CompilerException;

import java.util.HashMap;

public interface IDirective {
    HashMap<String,IDirective> GLOBAL_DIRECTIVES =new HashMap<>();

    void process(ProgramCompiler compiler, Line line) throws CompilerException;//returns evaluated args
    void offsetOriginIfProcessed(ProgramCompiler compiler, Line line) throws CompilerException;

    boolean isUnskippable();//for conditional assembly,changes program/labels
    boolean isRepeatable();//execute every step with first valid values
    boolean onlyFirstPass();
    boolean cannotFail();//if it cannot fail, or if it is impossible to fail
}
