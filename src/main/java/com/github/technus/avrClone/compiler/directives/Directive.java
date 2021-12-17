package com.github.technus.avrClone.compiler.directives;

import com.github.technus.avrClone.compiler.Binding;
import com.github.technus.avrClone.compiler.ConditionalState;
import com.github.technus.avrClone.compiler.Line;
import com.github.technus.avrClone.compiler.ProgramCompiler;
import com.github.technus.avrClone.compiler.directives.exceptions.InvalidDirective;
import com.github.technus.avrClone.compiler.exceptions.CompilerException;
import com.github.technus.avrClone.compiler.js.exceptions.EvaluationException;

import static com.github.technus.avrClone.compiler.ListingMode.*;
import static com.github.technus.avrClone.compiler.Segment.*;

public abstract class Directive implements IDirective {
    private final boolean unskippable, repeatable, onlyFirst, cannotFail;

    public Directive(boolean unskippable, boolean repeatable, boolean onlyFirst, boolean canotFail) {
        this.unskippable = unskippable;
        this.repeatable = repeatable;
        this.onlyFirst = onlyFirst;
        this.cannotFail = canotFail;
    }

    @Override
    public void offsetOriginIfProcessed(ProgramCompiler compiler, Line line) throws CompilerException {}

    @Override
    public boolean isRepeatable() {
        return repeatable;
    }

    @Override
    public boolean isUnskippable() {
        return unskippable;
    }

    @Override
    public boolean onlyFirstPass() {
        return onlyFirst;
    }

    @Override
    public boolean cannotFail() {
        return cannotFail;
    }

    public static void makeDirectives() {
        GLOBAL_DIRECTIVES.put("OVERLAP", new Directive(false, true, false, true) {
            @Override
            public void process(ProgramCompiler compiler, Line line) throws CompilerException {
                compiler.setCurrentOverlap(true);
            }
        });
        GLOBAL_DIRECTIVES.put("NOOVERLAP", new Directive(false, true, false, true) {
            @Override
            public void process(ProgramCompiler compiler, Line line) throws CompilerException {
                compiler.setCurrentOverlap(false);
            }
        });

        GLOBAL_DIRECTIVES.put("CSEG", new Directive(false, true, false, true) {
            @Override
            public void process(ProgramCompiler compiler, Line line) throws CompilerException {
                compiler.setCurrentSegment(CSEG);
            }
        });
        GLOBAL_DIRECTIVES.put("DSEG", new Directive(false, true, false, true) {
            @Override
            public void process(ProgramCompiler compiler, Line line) throws CompilerException {
                compiler.setCurrentSegment(DSEG);
            }
        });
        GLOBAL_DIRECTIVES.put("ESEG", new Directive(false, true, false, true) {
            @Override
            public void process(ProgramCompiler compiler, Line line) throws CompilerException {
                compiler.setCurrentSegment(ESEG);
            }
        });

        GLOBAL_DIRECTIVES.put("ORG", new Directive(false, false, true, true) {
            @Override
            public void process(ProgramCompiler compiler, Line line) throws CompilerException {
                int value = compiler.computeNumber(line.getLatestArguments()).intValue();
                compiler.setCurrentOrigin(value);
                line.setEvaluatedArguments(Integer.toString(value));
            }
        });

        //malloc
        GLOBAL_DIRECTIVES.put("INT", new Directive(false, false, true, true) {
            @Override
            public void process(ProgramCompiler compiler, Line line) throws CompilerException {
                compiler.reserveMemory(compiler.computeNumber(line.getLatestArguments()).intValue());
            }

            @Override
            public void offsetOriginIfProcessed(ProgramCompiler compiler, Line line) throws CompilerException{
                compiler.offsetCurrentOrigin(1);
            }
        });
        GLOBAL_DIRECTIVES.put("FLOAT", new Directive(false, false, true, true) {
            @Override
            public void process(ProgramCompiler compiler, Line line) throws CompilerException {
                compiler.reserveMemory(compiler.computeNumber(line.getLatestArguments()).intValue());
            }

            @Override
            public void offsetOriginIfProcessed(ProgramCompiler compiler, Line line) throws CompilerException{
                compiler.offsetCurrentOrigin(1);
            }
        });
        GLOBAL_DIRECTIVES.put("LONG", new Directive(false, false, true, true) {
            @Override
            public void process(ProgramCompiler compiler, Line line) throws CompilerException {
                compiler.reserveMemory(compiler.computeNumber(line.getLatestArguments()).intValue() * 2);
            }

            @Override
            public void offsetOriginIfProcessed(ProgramCompiler compiler, Line line) throws CompilerException{
                compiler.offsetCurrentOrigin(2);
            }
        });

        //consts
        GLOBAL_DIRECTIVES.put("STRING", new Directive(false, false, true, true) {
            @Override
            public void process(ProgramCompiler compiler, Line line) throws CompilerException {
                if(line.getEvaluatedArguments()==null){
                    line.setEvaluatedArguments(compiler.computeString(line.getLatestArguments()));
                }
                compiler.putConstant(line.getLatestArguments());
            }

            @Override
            public void offsetOriginIfProcessed(ProgramCompiler compiler, Line line) throws CompilerException{
                compiler.offsetCurrentOrigin(line.getLatestArguments().length());
            }
        });
        GLOBAL_DIRECTIVES.put("DINT", new Directive(false, false, true, true) {
            @Override
            public void process(ProgramCompiler compiler, Line line) throws CompilerException {
                for (String anArg : line.getLatestArgumentArray()) {
                    compiler.putConstant(compiler.computeNumber(anArg).intValue());
                }
            }

            @Override
            public void offsetOriginIfProcessed(ProgramCompiler compiler, Line line) throws CompilerException{
                compiler.offsetCurrentOrigin(line.getLatestArgumentArray().length);
            }
        });
        GLOBAL_DIRECTIVES.put("DLONG", new Directive(false, false, true, true) {
            @Override
            public void process(ProgramCompiler compiler, Line line) throws CompilerException {
                for (String anArg : line.getLatestArgumentArray()) {
                    compiler.putConstant(compiler.computeNumber(anArg).longValue());
                }
            }

            @Override
            public void offsetOriginIfProcessed(ProgramCompiler compiler, Line line) throws CompilerException{
                compiler.offsetCurrentOrigin(line.getLatestArgumentArray().length*2);
            }
        });
        GLOBAL_DIRECTIVES.put("DFLOAT", new Directive(false, false, true, true) {
            @Override
            public void process(ProgramCompiler compiler, Line line) throws CompilerException {
                String[] arg = line.getLatestArgumentArray();
                for (String anArg : arg) {
                    compiler.putConstant(compiler.computeNumber(anArg).floatValue());
                }
            }

            @Override
            public void offsetOriginIfProcessed(ProgramCompiler compiler, Line line) throws CompilerException{
                compiler.offsetCurrentOrigin(line.getLatestArgumentArray().length);
            }
        });

        GLOBAL_DIRECTIVES.put("MESSAGE", new Directive(false, false, false, false) {
            @Override
            public void process(ProgramCompiler compiler, Line line) throws CompilerException {
                compiler.write(compiler.computeString(line.getLatestArguments()));
            }
        });
        GLOBAL_DIRECTIVES.put("WARNING", new Directive(false, false, false, false) {
            @Override
            public void process(ProgramCompiler compiler, Line line) throws CompilerException {
                compiler.writeError("WARNING: " + compiler.computeString(line.getLatestArguments()));
            }
        });
        GLOBAL_DIRECTIVES.put("ERROR", new Directive(false, false, false, false) {
            @Override
            public void process(ProgramCompiler compiler, Line line) throws CompilerException {
                compiler.writeError("ERROR: " + compiler.computeString(line.getLatestArguments()));
            }
        });

        GLOBAL_DIRECTIVES.put("EQU", new Directive(false, false, false, false) {
            @Override
            public void process(ProgramCompiler compiler, Line line) throws CompilerException {
                String[] argArr = line.getLatestArguments().replaceFirst(" *= *", "\0").split("\\x00");
                if (argArr.length != 2) {
                    throw new InvalidDirective("Malformed directive! " + line.getLatestArguments());
                }
                compiler.putBinding(argArr[0], new Binding(Binding.NameType.EQU, compiler.computeNumber(argArr[1])));
            }
        });
        GLOBAL_DIRECTIVES.put("SET", new Directive(false, true, false, false) {
            @Override
            public void process(ProgramCompiler compiler, Line line) throws CompilerException {
                String[] argArr = line.getLatestArguments().replaceFirst(" *= *", "\0").split("\\x00");
                if (argArr.length != 2) {
                    throw new InvalidDirective("Malformed directive! " + line.getLatestArguments());
                }
                Number no = compiler.computeNumber(argArr[1]);
                compiler.putBinding(argArr[0], new Binding(Binding.NameType.SET, no));
                line.setEvaluatedArguments(argArr[0]+'='+no.toString());
            }
        });
        GLOBAL_DIRECTIVES.put("DEF", new Directive(false, true, false, false) {
            @Override
            public void process(ProgramCompiler compiler, Line line) throws CompilerException {
                String[] argArr = line.getLatestArguments().replaceFirst(" *= *", "\0").split("\\x00");
                if (argArr.length != 2) {
                    throw new InvalidDirective("Malformed directive! " + line.getLatestArguments());
                }
                Number no = compiler.computeNumber(argArr[1]);
                compiler.putBinding(argArr[0], new Binding(Binding.NameType.DEF, no));
                line.setEvaluatedArguments(argArr[0]+'='+no.toString());
            }
        });
        GLOBAL_DIRECTIVES.put("UNDEF", new Directive(false, true, false, false) {
            @Override
            public void process(ProgramCompiler compiler, Line line) throws CompilerException {
                for (String def : line.getLatestArgumentArray()) {
                    compiler.removeBinding(def);
                }
            }
        });

        GLOBAL_DIRECTIVES.put("MACRO", new Directive(false, false, true, true) {
            @Override
            public void process(ProgramCompiler compiler, Line line) throws CompilerException {
                for (String name : line.getLatestArgumentArray()) {
                    compiler.addMacro(name);
                }
            }
        });
        GLOBAL_DIRECTIVES.put("ENDMACRO", new Directive(false, false, true, true) {
            @Override
            public void process(ProgramCompiler compiler, Line line) throws CompilerException {
                for (String name : line.getLatestArgumentArray()) {
                    compiler.finishMacro(name);
                }
            }
        });
        GLOBAL_DIRECTIVES.put("ENDM", new Directive(false, false, true, true) {
            @Override
            public void process(ProgramCompiler compiler, Line line) throws CompilerException {
                for (String name : line.getLatestArgumentArray()) {
                    compiler.finishMacro(name);
                }
            }
        });

        GLOBAL_DIRECTIVES.put("IF", new Directive(true, false, true, true) {
            @Override
            public void process(ProgramCompiler compiler, Line line) throws CompilerException {
                try {
                    compiler.openIf(compiler.computeBoolean(line.getLatestArguments()));
                } catch (EvaluationException e) {
                    compiler.openIf(false);
                    compiler.setConditionalState(ConditionalState.DISABLED);
                    throw e;
                }
            }
        });
        GLOBAL_DIRECTIVES.put("IFDEF", new Directive(true, false, true, true) {
            @Override
            public void process(ProgramCompiler compiler, Line line) throws CompilerException {
                //all def
                compiler.openIf(compiler.containsNotDefs(line.getLatestArgumentArray()));
            }
        });
        GLOBAL_DIRECTIVES.put("IFNDEF", new Directive(true, false, true, true) {
            @Override
            public void process(ProgramCompiler compiler, Line line) throws CompilerException {
                //all undef
                compiler.openIf(compiler.lacksNotDefs(line.getLatestArgumentArray()));
            }
        });
        GLOBAL_DIRECTIVES.put("ENDIF", new Directive(true, false, true, true) {
            @Override
            public void process(ProgramCompiler compiler, Line line) throws CompilerException {
                compiler.endIf();
            }
        });

        GLOBAL_DIRECTIVES.put("ELIF", new Directive(true, false, true, true) {
            @Override
            public void process(ProgramCompiler compiler, Line line) throws CompilerException {
                try {
                    compiler.elseIf(compiler.computeBoolean(line.getLatestArguments()));
                } catch (EvaluationException e) {
                    compiler.setConditionalState(ConditionalState.DISABLED);
                    throw e;
                }
            }
        });
        GLOBAL_DIRECTIVES.put("ELDEF", new Directive(true, false, true, true) {
            @Override
            public void process(ProgramCompiler compiler, Line line) throws CompilerException {
                //all def
                compiler.elseIf(compiler.containsNotDefs(line.getLatestArgumentArray()));
            }
        });
        GLOBAL_DIRECTIVES.put("ELNDEF", new Directive(true, false, true, true) {
            @Override
            public void process(ProgramCompiler compiler, Line line) throws CompilerException {
                //all undef
                compiler.elseIf(compiler.lacksNotDefs(line.getLatestArgumentArray()));
            }
        });
        GLOBAL_DIRECTIVES.put("ELSE", new Directive(true, false, true, true) {
            @Override
            public void process(ProgramCompiler compiler, Line line) throws CompilerException {
                compiler.elseIf(true);
            }
        });

        GLOBAL_DIRECTIVES.put("NOLIST", new Directive(false, true, false, true) {
            @Override
            public void process(ProgramCompiler compiler, Line line) throws CompilerException {
                compiler.setCurrentListing(NO_LIST);
            }
        });
        GLOBAL_DIRECTIVES.put("LIST", new Directive(false, true, false, true) {
            @Override
            public void process(ProgramCompiler compiler, Line line) throws CompilerException {
                compiler.setCurrentListing(LIST);
            }
        });
        GLOBAL_DIRECTIVES.put("LISTMAC", new Directive(false, true, false, true) {
            @Override
            public void process(ProgramCompiler compiler, Line line) throws CompilerException {
                compiler.setCurrentListing(LIST_MACRO);
            }
        });

        GLOBAL_DIRECTIVES.put("INCLUDE", new Directive(false, false, true, true) {
            @Override
            public void process(ProgramCompiler compiler, Line line) throws CompilerException {
                compiler.include(line.getLatestArguments());
            }
        });

        GLOBAL_DIRECTIVES.put("EXIT", new Directive(false, false, true, true) {
            @Override
            public void process(ProgramCompiler compiler, Line line) throws CompilerException {
                compiler.exitCurrentFile();
            }
        });
    }
}
