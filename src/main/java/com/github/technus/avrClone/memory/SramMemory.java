package com.github.technus.avrClone.memory;

public class SramMemory implements IDataMemoryDefinition {
    private final int size;

    public SramMemory(int ramSize){
        size=ramSize;
    }

    @Override
    public int[] getDataDefault() {
        return new int[size];
    }

    @Override
    public int getOffset() {
        return 8192;
    }

    @Override
    public int getSize() {
        return size;
    }
}
