package com.github.technus.avrClone.memory;

public class RemovableMemory<T extends IDataMemoryDefinition> implements IDataMemoryInstance {
    private final int[] data;
    private final int offset;
    private T definition;

    public static RemovableMemory make(int offset,int size){
        if(size<=0 || offset<0) return null;
        return new RemovableMemory(size,offset);
    }

    private RemovableMemory(int size,int offset) {
        data=new int[size];
        this.offset=offset;
    }

    public static RemovableMemory make(int offset,int[] dataDefault){
        if(dataDefault==null || dataDefault.length==0 || offset<0) return null;
        return new RemovableMemory(dataDefault,offset);
    }

    private RemovableMemory(int[] dataDefault,int offset) {
        data=dataDefault.clone();
        this.offset=offset;
    }

    public static <T extends IDataMemoryDefinition> RemovableMemory<T> makeWithoutCloning(T definition, int... data){
        if(definition==null || data==null || data.length!=definition.getSize()){
            return null;
        }
        return new RemovableMemory<>(definition,data,true);
    }

    private RemovableMemory (T definition,int[] data,boolean exact){
        this.definition=definition;
        offset=definition.getOffset();
        if(exact){
            this.data=data;
        }else {
            this.data=new int[definition.getSize()];
            System.arraycopy(data,0,this.data,0,Math.min(data.length,definition.getSize()));
        }
    }

    public static <T extends IDataMemoryDefinition> RemovableMemory<T> make(T definition, int... data){
        if(definition==null || data==null || data.length>definition.getSize()){
            return null;
        }
        return new RemovableMemory<>(definition,data);
    }

    private RemovableMemory (T definition,int[] data){
        this(definition,data,false);
    }

    @Override
    public int getSize() {
        return data.length;
    }

    @Override
    public int getOffset() {
        return offset;
    }

    @Override
    public int[] getDataDefault() {
        return new int[data.length];
    }

    @Override
    public int[] getData() {
        return data;
    }

    @Override
    protected RemovableMemory<T> clone() {
        return new RemovableMemory<>(data,offset);
    }

    public T getDefinition() {
        return definition;
    }
}
