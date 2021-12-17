package com.github.technus.avrClone.instructions;

import com.github.technus.avrClone.AvrCore;
import com.github.technus.avrClone.memory.program.exceptions.ProgramException;
import com.github.technus.avrClone.memory.program.ProgramMemory;

public interface IInstruction {
    String name();

    /**
     * must check if the current operands and adresses are valid
     * @param programMemory
     * @param addr
     * @param immersive
     * @param operandsReturn
     * @param values Instruction, Operand 0, Operand 1
     * @return
     */
    void compileInstruction(ProgramMemory programMemory, int addr, boolean immersive, int[] operandsReturn, String[] values) throws ProgramException;

    /**
     * must check if the space that you want to access EXISTS (in case of indirect operations)
     * {#link com.github.technus.avrClone.AvrCore#isDataAddressValid(int) isDataAddressValid}
     * @param core
     */
    ExecutionEvent execute(AvrCore core);
    default int getCost(AvrCore core){
        return 1;
    }
    default boolean isImmersive(){
        return false;
    }
    int getOperandCount();

    OperandLimit getLimit0();
    OperandLimit getLimit1();
    String getNotes();
}
