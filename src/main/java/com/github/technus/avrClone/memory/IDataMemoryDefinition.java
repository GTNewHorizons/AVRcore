package com.github.technus.avrClone.memory;

public interface IDataMemoryDefinition {
    int getOffset();
    int getSize();
    int[] getDataDefault();
    default boolean canEncapsulate(IDataMemoryDefinition child){
        return getOffset()<=child.getOffset() && getOffset()+getSize()>=child.getOffset()+child.getSize();
    }
}
