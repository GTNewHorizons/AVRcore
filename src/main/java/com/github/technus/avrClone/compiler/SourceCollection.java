package com.github.technus.avrClone.compiler;

import com.github.technus.avrClone.compiler.exceptions.CompilerException;
import com.github.technus.avrClone.compiler.exceptions.InvalidInclude;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class SourceCollection {
    public static final char INCLUDE_SEPARATOR_CHAR ='\034';
    public static final char MACRO_SEPARATOR_CHAR ='\035';
    public static final char LINE_NUMBER_SEPARATOR_CHAR ='\036';

    private HashSet<String> systemDirectories=new HashSet<>();
    private HashSet<String> userDirectories=new HashSet<>();
    private HashMap<String,String> paths=new HashMap<>();

    private IIncludeProcessor currentIncludeProcessor;
    private HashMap<String,ArrayList<String>> includedFiles;

    public SourceCollection(){
        includedFiles = new HashMap<>(8);
    }

    public void clear(){
        includedFiles=new HashMap<>(8);
    }

    public void setCurrentIncludeProcessor(IIncludeProcessor include) {
        this.currentIncludeProcessor = include;
    }

    public IIncludeProcessor getCurrentIncludeProcessor() {
        return currentIncludeProcessor;
    }

    public ArrayList<Line> projectRootInclude(String absoluteIncludeName,boolean shouldList) throws CompilerException {
        paths.clear();
        return getInclude("","",absoluteIncludeName,1,shouldList);
    }

    public ArrayList<Line> getInclude(String includePath,String parentIncludeName, String includeName,int currentLine,boolean shouldList) throws CompilerException {
        if (includePath == null || includeName==null) {
            throw new InvalidInclude("Invalid include parameters! "+includePath+" "+includeName);
        }
        String includePathNew=includePath + includeName + INCLUDE_SEPARATOR_CHAR;
        ArrayList<String> contents = includedFiles.get(includePathNew);
        if (contents==null) {
            if (currentIncludeProcessor == null) {
                throw new InvalidInclude("No include processor set!");
            }
            contents = currentIncludeProcessor.include(includePath, includeName,includePathNew,systemDirectories,userDirectories,paths);
            if (contents == null) {
                throw new InvalidInclude("Unable to include file! "+includeName);
            } else {
                includedFiles.put(includePathNew, contents);
            }
        }

        ArrayList<Line> inc=new ArrayList<>(contents.size());
        String lineName=parentIncludeName+includeName+ LINE_NUMBER_SEPARATOR_CHAR +currentLine+ INCLUDE_SEPARATOR_CHAR;
        for(int i=0;i<contents.size();i++){
            inc.add(new Line(includePathNew,lineName,i+1,contents.get(i),shouldList));
        }
        return inc;
    }

    public HashSet<String> getSystemDirectories() {
        return systemDirectories;
    }

    public void setSystemDirectories(HashSet<String> systemDirectories) throws InvalidInclude{
        if(systemDirectories==null){
            throw new InvalidInclude("System directories cannot be null!");
        }
        this.systemDirectories = systemDirectories;
    }

    public HashSet<String> getUserDirectories() {
        return userDirectories;
    }

    public void setUserDirectories(HashSet<String> userDirectories) throws InvalidInclude{
        if(userDirectories==null){
            throw new InvalidInclude("System directories cannot be null!");
        }
        this.userDirectories = userDirectories;
    }
}
