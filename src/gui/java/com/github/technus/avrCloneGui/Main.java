package com.github.technus.avrCloneGui;

import com.bulenkov.darcula.DarculaLaf;
import com.github.technus.avrClone.AvrCore;
import com.github.technus.avrClone.compiler.ProgramCompiler;
import com.github.technus.avrClone.compiler.Segment;
import com.github.technus.avrClone.compiler.exceptions.CompilerException;
import com.github.technus.avrClone.instructions.InstructionRegistry;
import com.github.technus.avrClone.registerPackages.GPIO_Registers;

import javax.swing.*;

public class Main {
    private static ProgramCompiler programCompiler=new ProgramCompiler();
    private static AvrCore core=new AvrCore();
    static {
        core.setInstructionRegistry(InstructionRegistry.INSTRUCTION_REGISTRY_OP);
        core.setUsingImmersiveOperands(false);
    }

    public static void main(String[] args) {
        try{
            UIManager.setLookAndFeel(new DarculaLaf());
        }catch (Exception e){
            e.printStackTrace();
        }

        core.simpleInit();
        core.putDataBindings(new GPIO_Registers(0x00),"GPIOR");
        try {
            programCompiler.setSegmentOffset(Segment.ESEG, 4096);
        }catch (CompilerException e){
            e.printStackTrace();
        }
        new AvrTest(core,programCompiler).show();
    }
}
