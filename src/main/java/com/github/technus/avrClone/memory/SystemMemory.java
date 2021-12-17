package com.github.technus.avrClone.memory;

public class SystemMemory implements IDataMemoryDefinition,Cloneable{
    public final int size;

    public SystemMemory(){
        this(4096);
    }

    public SystemMemory(int ramSize){
        size=8192+ramSize;//init default
    }

    @Override
    public int[] getDataDefault() {
        return new int[size];
    }

    @Override
    public int getOffset() {
        return 0;
    }

    @Override
    public int getSize() {
        return size;
    }
}