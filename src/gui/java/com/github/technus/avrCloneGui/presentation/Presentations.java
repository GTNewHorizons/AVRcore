package com.github.technus.avrCloneGui.presentation;

import java.nio.charset.StandardCharsets;

public enum Presentations implements Presentation {
    FLOAT(i ->  Float.toString(Float.intBitsToFloat(i[0]))),
    UINT_HEX(i -> "0x" + Integer.toHexString(i[0])),
    UINT_OCT(i -> "0" + Integer.toOctalString(i[0])),
    UINT_BIN(i -> "0b" + Integer.toBinaryString(i[0])),
    UINT_DEC(i -> Integer.toUnsignedString(i[0], 10)),
    INT_DEC(i -> Integer.toString(i[0])),

    DOUBLE(i -> {
        if(i.length==2){
            return Double.toString(Double.longBitsToDouble((i[0]&0xffffffffL)|((long)i[1]<<32)));
        }
        return null;
    }),
    ULONG_HEX(i -> {
        if(i.length==2){
            return "0x" + Long.toHexString((i[0]&0xffffffffL)|((long)i[1]<<32));
        }
        return null;
    }),
    ULONG_OCT(i -> {
        if(i.length==2){
            return "0" + Long.toOctalString((i[0]&0xffffffffL)|((long)i[1]<<32));
        }
        return null;
    }),
    ULONG_BIN(i -> {
        if(i.length==2){
            return "0b" + Long.toBinaryString((i[0]&0xffffffffL)|((long)i[1]<<32));
        }
        return null;
    }),
    ULONG_DEC(i -> {
        if(i.length==2){
            return Long.toUnsignedString((i[0]&0xffffffffL)|((long)i[1]<<32), 10);
        }
        return null;
    }),
    LONG_DEC(i -> {
        if(i.length==2){
            return Long.toString((i[0]&0xffffffffL)|((long)i[1]<<32));
        }
        return null;
    }),
    UTF8(i -> {
        int i0=i[0];
        return new String(new byte[]{(byte)(i0>>24),(byte)(i0>>16),(byte)(i0>>8),(byte)i0},StandardCharsets.UTF_8);
    }),
    UTF16(i -> {
        int i0=i[0];
        return new String(new byte[]{(byte)(i0>>24),(byte)(i0>>16),(byte)(i0>>8),(byte)i0},StandardCharsets.UTF_16);
    }),
    UTF32(i -> {
        int i0=i[0];
        return new String(new byte[]{(byte)(i0>>24),(byte)(i0>>16),(byte)(i0>>8),(byte)i0},UTF_32);
    });


    private final Presentation<Integer> presentation;

    Presentations(Presentation<Integer> integerPresentation) {
        presentation = integerPresentation;
    }

    @Override
    public String present(Object... i) {
        if (i == null || i.length == 0 ) {
            return null;
        }
        if(i[0] instanceof Integer){
            if(i.length>1 && i[1] instanceof Integer){
                i=new Integer[]{(Integer)i[0],(Integer)i[1]};
            }else {
                i=new Integer[]{(Integer)i[0]};
            }
            return presentation.present((Integer[]) i);
        }
        return null;
    }
}
