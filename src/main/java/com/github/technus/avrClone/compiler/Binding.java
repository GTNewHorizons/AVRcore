package com.github.technus.avrClone.compiler;

public class Binding {
    public final NameType type;
    public Object value;

    public Binding(NameType type){
        this.type=type;
    }

    public Binding(NameType type, Object value){
        this(type);
        this.value=value;
    }

    public enum NameType{
        SET,EQU,DEF,LABEL,POINTER//is a label but for dseg and eseg
    }

    @Override
    public int hashCode() {
        if(value instanceof Long) {
            long v=(long)value;
            return type.ordinal()^(int)v^(int)(v>>>32);
        } else if(value instanceof Number) {
            long v = Double.doubleToLongBits(((Number) value).doubleValue());
            return type.ordinal()^(int)v^(int)(v>>>32);
        } else if (value instanceof Boolean) {
            return (Boolean) value ?type.ordinal():~type.ordinal();
        }
        return type.ordinal()^value.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Binding && type == ((Binding) obj).type && value.equals(((Binding) obj).value);
    }
}

