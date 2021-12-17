package com.github.technus.avrClone.memory;

public class IoMemory implements IDataMemoryDefinition {
    private final int size;

    public IoMemory(){
        this(4096);
    }

    public IoMemory(int available){
        size=available;
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
