package com.github.technus.avrClone.memory;

public class EepromMemory implements IDataMemoryDefinition {
    private final int size;

    public static EepromMemory make(int size){
        if(size>4096 || size<=0) return null;
        return new EepromMemory(size);
    }

    private EepromMemory(int size) {
        this.size=size;
    }

    @Override
    public int getSize() {
        return size;
    }

    @Override
    public int getOffset() {
        return 4096;
    }

    @Override
    public int[] getDataDefault() {
        return new int[size];
    }

    @Override
    protected EepromMemory clone() {
        return new EepromMemory(size);
    }
}
