package com.github.technus.avrClone.compiler;


import com.github.technus.avrClone.compiler.exceptions.CompilerException;
import com.github.technus.avrClone.compiler.exceptions.InvalidInclude;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public interface IIncludeProcessor {
    IIncludeProcessor DUMB_FILE_SYSTEM_INCLUDE_PROCESSOR = (parentIncludePath, includeName, includePath, systemDirectories, userDirectories, includedFilePaths) -> {
        File f = new File(includeName);
        if (!f.isFile()) {
            throw new InvalidInclude("File does not exist! " + includeName);
        }
        if (!f.canRead()) {
            throw new InvalidInclude("File is not readable! " + includeName);
        }
        try {
            includedFilePaths.put(includePath, f.getCanonicalPath());
            return (ArrayList<String>) Files.readAllLines(f.toPath());
        } catch (Exception e) {
            throw new InvalidInclude("Failed to read file! " + includeName, e);
        }
    };
    IIncludeProcessor ABSOLUTE_FILE_SYSTEM_INCLUDE_PROCESSOR = (parentIncludePath, includeName, includePath, systemDirectories, userDirectories, includedFilePaths) -> {
        File f=new File(includeName);
        if(!f.getAbsolutePath().equals(includeName)){
            throw new InvalidInclude("Cannot resolve as absolute path! "+includeName);
        }
        if(!f.isFile()){
            throw new InvalidInclude("File does not exist! "+includeName);
        }
        if(!f.canRead()){
            throw new InvalidInclude("File is not readable! "+includeName);
        }
        try{
            includedFilePaths.put(includePath,f.getCanonicalPath());
            return (ArrayList<String>) Files.readAllLines(f.toPath());
        }catch (Exception e){
            throw new InvalidInclude("Failed to read file! "+includeName,e);
        }
    };
    IIncludeProcessor RELATIVE_FILE_SYSTEM_INCLUDE_PROCESSOR = (parentIncludePath, includeName, includePath, systemDirectories, userDirectories, includedFilePaths) -> {
        if(parentIncludePath.length()==0){
            return ABSOLUTE_FILE_SYSTEM_INCLUDE_PROCESSOR.include(parentIncludePath, includeName, includePath, systemDirectories, userDirectories, includedFilePaths);
        }else {
            String parentFilePath=includedFilePaths.get(parentIncludePath);
            if(parentFilePath==null){
                throw new InvalidInclude("Cannot resolve parent! "+includeName);
            }
            File d=new File(parentFilePath);
            if(d.isFile()){
                d=d.getAbsoluteFile().getParentFile();
            }
            if(!d.isDirectory()){
                throw new InvalidInclude("Cannot resolve! "+includeName);
            }
            File f=new File(d.getAbsolutePath()+File.separator+includeName);
            if(!f.isFile()){
                throw new InvalidInclude("File does not exist! "+includeName);
            }
            if(!f.canRead()){
                throw new InvalidInclude("File is not readable! "+includeName);
            }
            try{
                includedFilePaths.put(includePath,f.getCanonicalPath());
                return (ArrayList<String>) Files.readAllLines(f.toPath());
            }catch (Exception e){
                throw new InvalidInclude("Failed to read file! "+includeName,e);
            }
        }
    };
    IIncludeProcessor SYSTEM_FILE_SYSTEM_INCLUDE_PROCESSOR = (parentIncludePath, includeName, includePath, systemDirectories, userDirectories, includedFilePaths) -> {
        for (String dir : systemDirectories) {
            File d=new File(dir);
            if(d.isFile()){
                d=d.getAbsoluteFile().getParentFile();
            }
            if(!d.isDirectory()){
                throw new InvalidInclude("Cannot resolve! "+includeName);
            }
            File f = new File(d.getAbsolutePath()+File.separator+includeName);
            if (!f.isFile()) {
                continue;
            }
            if (!f.canRead()) {
                throw new InvalidInclude("File is not readable! " + includeName);
            }
            try {
                includedFilePaths.put(includePath,f.getCanonicalPath());
                return (ArrayList<String>) Files.readAllLines(f.toPath());
            } catch (Exception e) {
                throw new InvalidInclude("Failed to read file! " + includeName, e);
            }
        }
        return ABSOLUTE_FILE_SYSTEM_INCLUDE_PROCESSOR.include(parentIncludePath, includeName, includePath, systemDirectories, userDirectories, includedFilePaths);
    };
    IIncludeProcessor GLOBAL_FILE_SYSTEM_INCLUDE_PROCESSOR = (parentIncludePath, includeName, includePath, systemDirectories, userDirectories, includedFilePaths) -> {
        for (String dir : userDirectories) {
            File d=new File(dir);
            if(d.isFile()){
                d=d.getAbsoluteFile().getParentFile();
            }
            if(!d.isDirectory()){
                throw new InvalidInclude("Cannot resolve! "+includeName);
            }
            File f = new File(d.getAbsolutePath()+File.separator+includeName);
            if (!f.isFile()) {
                continue;
            }
            if (!f.canRead()) {
                throw new InvalidInclude("File is not readable! " + includeName);
            }
            try {
                includedFilePaths.put(includePath,f.getCanonicalPath());
                return (ArrayList<String>) Files.readAllLines(f.toPath());
            } catch (Exception e) {
                throw new InvalidInclude("Failed to read file! " + includeName, e);
            }
        }
        for (String dir : systemDirectories) {
            File d=new File(dir);
            if(d.isFile()){
                d=d.getAbsoluteFile().getParentFile();
            }
            if(!d.isDirectory()){
                throw new InvalidInclude("Cannot resolve! "+includeName);
            }
            File f = new File(d.getAbsolutePath()+File.separator+includeName);
            if (!f.isFile()) {
                continue;
            }
            if (!f.canRead()) {
                throw new InvalidInclude("File is not readable! " + includeName);
            }
            try {
                includedFilePaths.put(includePath,f.getCanonicalPath());
                return (ArrayList<String>) Files.readAllLines(f.toPath());
            } catch (Exception e) {
                throw new InvalidInclude("Failed to read file! " + includeName, e);
            }
        }
        return ABSOLUTE_FILE_SYSTEM_INCLUDE_PROCESSOR.include(parentIncludePath, includeName, includePath, systemDirectories, userDirectories, includedFilePaths);
    };
    IIncludeProcessor FILE_SYSTEM_INCLUDE_PROCESSOR = (parentIncludePath, includeName, includePath, systemDirectories, userDirectories, includedFilePaths) -> {
        if(includeName.startsWith("\"") && includeName.endsWith("\"")){
            includeName=includeName.replaceFirst("^\"(.*)\"$","$1");
            try{
                return RELATIVE_FILE_SYSTEM_INCLUDE_PROCESSOR.include(parentIncludePath, includeName, includePath, systemDirectories, userDirectories, includedFilePaths);
            }catch (InvalidInclude e){
                return GLOBAL_FILE_SYSTEM_INCLUDE_PROCESSOR.include(parentIncludePath, includeName, includePath, systemDirectories, userDirectories, includedFilePaths);
            }
        }else if(includeName.startsWith("<") && includeName.endsWith(">")){
            includeName=includeName.replaceFirst("^<(.*)>$","$1");
            return SYSTEM_FILE_SYSTEM_INCLUDE_PROCESSOR.include(parentIncludePath, includeName, includePath, systemDirectories, userDirectories, includedFilePaths);
        }else if(parentIncludePath.length()==0){
            return ABSOLUTE_FILE_SYSTEM_INCLUDE_PROCESSOR.include(parentIncludePath, includeName, includePath, systemDirectories, userDirectories, includedFilePaths);
        }
        throw new InvalidInclude("Invalid inclusion type! "+includeName);
    };

    ArrayList<String> include(String parentIncludePath, String includeName, String includePath, HashSet<String> systemDirectories, HashSet<String> userDirectories, HashMap<String,String> includedFilePaths) throws CompilerException;
}
