package com.github.technus.avrClone.compiler;

import java.util.ArrayList;

public class Segment{
    private static final ArrayList<Segment> segments=new ArrayList<>();
    public static final Segment
            CSEG=new Segment("CSEG",true,false),
            DSEG=new Segment("DSEG",false,true),
            ESEG=new Segment("ESEG",true,true);

    public static Segment[] values(){
        return segments.toArray(new Segment[0]);
    }

    public static int count(){
        return segments.size();
    }

    private final boolean allowingConstants, allowingVariables;
    private final int ordinal;
    private final String name;

    public Segment(String name,boolean allowingConstants, boolean allowingVariables){
        this.allowingConstants = allowingConstants;
        this.allowingVariables = allowingVariables;
        this.name=name;
        ordinal=segments.size();
        segments.add(this);
    }

    public boolean isAllowingVariables() {
        return allowingVariables;
    }

    public boolean isAllowingConstants() {
        return allowingConstants;
    }

    public int ordinal() {
        return ordinal;
    }

    public String name() {
        return name;
    }
}
