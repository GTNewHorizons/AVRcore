package com.github.technus.avrClone.instructions;

import java.util.ArrayList;
import java.util.TreeSet;

public class OperandLimit {
    public static final ArrayList<OperandLimit> registry=new ArrayList<>();

    public static final OperandLimit
    //RegisterFileSingles
    R=new OperandLimit(0, 31).setNameAndRegister("R"),
    Rh=new OperandLimit(R, 16, 31).setNameAndRegister("Rh"),
    Rq=new OperandLimit(R, 16, 23).setNameAndRegister("Rq"),
    Rpair=new OperandLimit(0, 30).setNameAndRegister("Rpair"),
    Rpmov=new OperandLimit(Rpair,false,0,2,4,6,8,10,12,14,16,18,20,22,24,26,28,30).setNameAndRegister("Rpmov"),
    Rp=new OperandLimit(Rpair,false, 24, 26, 28, 30).setNameAndRegister("Rp"),
    Z=new OperandLimit(Rpair,true,30).setNameAndRegister("Z"),

    //multiple bits
    mask32=new OperandLimit().setNameAndRegister("mask"),

    //single bits
    b =new OperandLimit(true,mask32).setNameAndRegister("b"),

    //Consts
    K32=new OperandLimit().setNameAndRegister("K32"),
    K16=new OperandLimit(0, 65535).setNameAndRegister("K16"),
    K16pointer=new OperandLimit(0, 65535,true).setNameAndRegister("K16*"),
    K8s=new OperandLimit(K32,-128, 255).setNameAndRegister("K8s"),//for arithmetic
    K8b=new OperandLimit(mask32,0, 255).setNameAndRegister("K8b"),//vinary
    K6=new OperandLimit(K16,0, 63).setNameAndRegister("K6"),
    K6pointer=new OperandLimit(K16pointer,0, 63,true).setNameAndRegister("K6*"),
    K1pointer=new OperandLimit(K16pointer,0,0,true).setNameAndRegister("K1*"),

    //IO
    IO32=new OperandLimit().setNameAndRegister("IO32"),
    IO16=new OperandLimit(IO32,0, 65535).setNameAndRegister("IO16"),
    IO8=new OperandLimit(0, 255).setNameAndRegister("IO8"),
    IO6=new OperandLimit(IO16,0, 63).setNameAndRegister("IO6"),
    IO5=new OperandLimit(IO8,0, 31).setNameAndRegister("IO5"),

    //DATA
    D32=new OperandLimit().setNameAndRegister("D32"),
    D16=new OperandLimit(D32,0, 65535).setNameAndRegister("D16"),

    //Program offset
    S16=new OperandLimit(true, null,-32768, 32767).setRelative().setNameAndRegister("S16"),
    S12=new OperandLimit(true,S16,-2048, 2047).setRelative().setNameAndRegister("S12"),
    S8=new OperandLimit(true, null,-128, 127).setRelative().setNameAndRegister("S8"),
    S7=new OperandLimit(true,S8,-64, 63).setRelative().setNameAndRegister("S7"),

    //Program
    P32=new OperandLimit().setNameAndRegister("P32"),
    P22=new OperandLimit(P32,0, 4194303).setNameAndRegister("P22"),

    //FP
    FP=new OperandLimit().setFP().setNameAndRegister("FP");

    private OperandLimit broader;
    private boolean asOffset,intMaxMin,floatingPointPreffered,relativePreffered;
    private TreeSet<Integer> possibleValues;
    private final int min, max;
    public String name="UNNAMED";

    private OperandLimit() {//no limit
        min = Integer.MIN_VALUE;
        max = Integer.MAX_VALUE;
    }

    private OperandLimit(int value) {
        this(true, value, value);
    }

    private OperandLimit(int min, int max) {
        this(true, min, max);
    }

    private OperandLimit(int min, int max,boolean intMaxMin) {
        this(true, min, max);
        this.intMaxMin=intMaxMin;
    }

    private OperandLimit(boolean generateOnly8,OperandLimit broader) {//no limit
        possibleValues = new TreeSet<>();
        int limit = generateOnly8 ? 8 : 32;
        for (int i = 0; i < limit; i++) {
            possibleValues.add(1 << i);
        }
        this.broader=broader;
        min=possibleValues.first();
        max=possibleValues.last();
    }

    private OperandLimit(boolean asOffset,OperandLimit broader,int min, int max) {
        this(true, min, max);
        this.broader=broader;
        this.asOffset=asOffset;
    }

    private OperandLimit(boolean limitOnly, int... possibleValues) {
        if (limitOnly) {
            this.min = possibleValues[0];
            this.max = possibleValues[possibleValues.length - 1];
        } else {
            this.possibleValues = new TreeSet<>();
            for (int value : possibleValues) {
                this.possibleValues.add(value);
            }
            this.min=this.possibleValues.first();
            this.max=this.possibleValues.last();
        }
    }

    private OperandLimit(OperandLimit broader,int min, int max) {
        this(true, min, max);
        this.broader=broader;
    }

    private OperandLimit(OperandLimit broader,int min, int max,boolean intMaxMin) {
        this(true, min, max);
        this.broader=broader;
        this.intMaxMin=intMaxMin;
    }

    private OperandLimit(OperandLimit broader,boolean limitOnly, int... possibleValues) {
        this(limitOnly,possibleValues);
        this.broader=broader;
    }

    private OperandLimit setFP(){
        floatingPointPreffered=true;
        return this;
    }

    private OperandLimit setRelative(){
        relativePreffered=true;
        return this;
    }

    public int clamp(int value,int programCounter,boolean broader){
        if(asOffset){
            return clampChange(value,programCounter,broader);
        }
        return clampValue(value,broader);
    }

    @SuppressWarnings("ConstantConditions")
    private int clampValue(int value, boolean broader) {
        if(intMaxMin && (value==Integer.MIN_VALUE || value==Integer.MAX_VALUE)){
            return value;
        }
        if (broader && this.broader != null) {
            return clampValue(value, false);
        }
        if (possibleValues == null) {
            return value > max ? max : (value < min ? min : value);
        }
        if (value < possibleValues.first()) {
            return possibleValues.first();
        }
        return possibleValues.floor(value);
    }

    private int clampChange(int newValue, int currentValue, boolean broader) {
        int diff=newValue-currentValue;
        diff=clampValue(diff,broader);
        return currentValue+diff;
    }

    private OperandLimit setNameAndRegister(String name){
        this.name=name;
        registry.add(this);
        return this;
    }

    public String getPossibleValuesString(){
        StringBuilder stringBuilder=new StringBuilder();
        if(possibleValues==null){
            stringBuilder.append(min).append(" to ").append(max).append(' ');
        }else{
            for(int i:possibleValues){
                stringBuilder.append(i).append(' ');
            }
        }
        if(asOffset){
            stringBuilder.append("difference");
        }
        if(intMaxMin){
            stringBuilder.append(", special max min ");
        }
        return stringBuilder.toString();
    }

    public OperandLimit getBroader() {
        return broader;
    }

    public boolean isFloatingPointPreffered() {
        return floatingPointPreffered;
    }

    public boolean isRelativePreffered() {
        return relativePreffered;
    }
}
