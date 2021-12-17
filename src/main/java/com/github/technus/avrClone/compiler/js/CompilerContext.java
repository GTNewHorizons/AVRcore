package com.github.technus.avrClone.compiler.js;

import javax.script.Bindings;
import javax.script.ScriptContext;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.github.technus.avrClone.compiler.Line.NAME_FORMAT;

public class CompilerContext implements ScriptContext {
    private Writer writer,errorWriter;
    private Reader reader;
    private CompilerBindings bindings;
    private Bindings global;

    public CompilerContext(CompilerBindings compilerBindings,Bindings globalScope){
        bindings = compilerBindings;
        global = globalScope;
        reader = new InputStreamReader(System.in);
        writer = new PrintWriter(System.out , true);
        errorWriter = new PrintWriter(System.err, true);
    }

    @Override
    public void setBindings(Bindings bindings, int scope) {
        if(scope==GLOBAL_SCOPE){
            global=bindings;
        } else {
            if (scope == ENGINE_SCOPE && bindings instanceof CompilerBindings) {
                this.bindings = (CompilerBindings) bindings;
            } else {
                throw new IllegalArgumentException("Requires " + CompilerBindings.class.getCanonicalName());
            }
        }
    }

    @Override
    public Bindings getBindings(int scope) {
        switch (scope){
            case GLOBAL_SCOPE: return global;
            case ENGINE_SCOPE: return bindings;
            default: return null;
        }
    }

    @Override
    public void setAttribute(String name, Object value, int scope) {
        if(!checkName(name)){
            return;
        }
        switch (scope){
            case GLOBAL_SCOPE: global.put(name,value); return;
            case ENGINE_SCOPE: bindings.put(name,value); return;
            default:
        }
    }

    @Override
    public Object getAttribute(String name, int scope) {
        switch (scope){
            case GLOBAL_SCOPE: return global.get(name);
            case ENGINE_SCOPE: return bindings.get(name);
            default: return null;
        }
    }

    @Override
    public Object removeAttribute(String name, int scope) {
        switch (scope){
            case GLOBAL_SCOPE: return global.remove(name);
            case ENGINE_SCOPE: return bindings.remove(name);
            default: return null;
        }
    }

    @Override
    public Object getAttribute(String name) {
        Object value=bindings.get(name);
        return value != null ? value : global.get(name);
    }

    @Override
    public int getAttributesScope(String name) {
        if(bindings.containsKey(name)){
            return ENGINE_SCOPE;
        }else if(global.containsKey(name)){
            return GLOBAL_SCOPE;
        }
        return -1;
    }

    //region read write
    @Override
    public Writer getWriter() {
        return writer;
    }

    @Override
    public Writer getErrorWriter() {
        return errorWriter;
    }

    @Override
    public void setWriter(Writer writer) {
        this.writer=writer;
    }

    @Override
    public void setErrorWriter(Writer writer) {
        this.errorWriter=writer;
    }

    @Override
    public Reader getReader() {
        return reader;
    }

    @Override
    public void setReader(Reader reader) {
        this.reader=reader;
    }
    //endregion

    private static List<Integer> scopes;
    static {
        scopes = new ArrayList<>(2);
        scopes.add(ENGINE_SCOPE);
        scopes.add(GLOBAL_SCOPE);
        scopes = Collections.unmodifiableList(scopes);
    }

    @Override
    @Deprecated
    public List<Integer> getScopes() {
        return scopes;
    }

    private boolean checkName(String name) {
        if(     name==null ||
                name.isEmpty() ||
                !name.matches(NAME_FORMAT)){
            return false;
        }
        return true;
    }

}
