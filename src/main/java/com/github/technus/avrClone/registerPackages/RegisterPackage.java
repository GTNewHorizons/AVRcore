package com.github.technus.avrClone.registerPackages;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class RegisterPackage<T extends RegisterPackage<T>> implements IRegisterPackage<T> {
    protected final Map<String,IRegisterBit<T>> bits=new HashMap<>();
    protected final Map<String,IRegister<T>> registers =new HashMap<>();
    protected final Map<Integer, List<IRegister<T>>> addresses =new HashMap<>();
    protected final Map<Integer, IInterrupt<T>> interrupts=new HashMap<>();

    private final int offset,size;

    @SuppressWarnings("unchecked")
    protected RegisterPackage(int offset, int size){
        T checkCast=(T)this;
        this.offset=offset;
        this.size=size;
    }

    @Override
    public int getOffset() {
        return offset;
    }

    @Override
    public int getSize() {
        return size;
    }

    @Override
    public Map<String,IRegister<T>> registersMap() {
        return registers;
    }

    @Override
    public Map<Integer, List<IRegister<T>>> addressesMap() {
        return addresses;
    }

    @Override
    public Map<String,IRegisterBit<T>> bitsMap() {
        return bits;
    }

    @Override
    public int[] getDataDefault() {
        return new int[getSize()];
    }

    @Override
    public Map<Integer, IInterrupt<T>> interruptsMap() {
        return interrupts;
    }

    @SuppressWarnings("unchecked")
    protected void addRegisters(IRegister<T>... registers){
        for(IRegister<T> register: registers){
            this.registers.put(register.name(),register);
            addresses.computeIfAbsent(register.getAddress((T)this),ArrayList::new).add(register);
        }
    }

    @SuppressWarnings("unchecked")
    protected void addBits(IRegisterBit<T>... bits){
        for(IRegisterBit<T> bit: bits){
            this.bits.put(bit.name(),bit);
        }
    }

    protected void addInterrupts(IInterrupt<T>... interrupts){
        for (IInterrupt<T> interrupt :interrupts) {
            this.interrupts.put(interrupt.getVector(),interrupt);
        }
    }
}
