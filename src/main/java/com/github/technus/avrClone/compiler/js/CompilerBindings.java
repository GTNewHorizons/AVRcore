package com.github.technus.avrClone.compiler.js;

import com.github.technus.avrClone.compiler.Binding;

import javax.script.Bindings;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

public class CompilerBindings extends HashMap<String,Object> implements Bindings {
    public CompilerBindings(int initialCapacity){
        super(initialCapacity);
    }

    @Override
    public Object get(Object key) {
        if(containsKey(key)){
            return getBinding(key).value;
        }
        return null;
    }

    @Override
    public Object getOrDefault(Object key, Object defaultValue) {
        Object v=get(key);
        return v==null?defaultValue:v;
    }

    public Binding getBinding(Object key) {
        return (Binding) super.get(key);
    }

    @Override
    @Deprecated
    public Object put(String key, Object value) {
        return null;
    }

    @Override
    @Deprecated
    public void putAll(Map<? extends String, ?> toMerge) {
        throw new NoSuchMethodError();
    }

    @Override
    @Deprecated
    public Binding putIfAbsent(String key, Object value) {
        throw new NoSuchMethodError();
    }

    public Binding putBinding(String name, Binding value) {
        return (Binding) super.put(name,value);
    }

    public void putAllBindings(Map<String,Binding> toMerge) {
        super.putAll(toMerge);
    }

    @Override
    @Deprecated
    public boolean replace(String key, Object oldValue, Object newValue) {
        throw new NoSuchMethodError();
    }

    @Override
    @Deprecated
    public Binding replace(String key, Object value) {
        throw new NoSuchMethodError();
    }

    @Override
    @Deprecated
    public void replaceAll(BiFunction<? super String, ? super Object, ?> function) {
        throw new NoSuchMethodError();
    }

    @Override
    @Deprecated
    public Binding merge(String key, Object value, BiFunction<? super Object, ? super Object, ?> remappingFunction) {
        throw new NoSuchMethodError();
    }

    @Override
    @Deprecated
    public boolean remove(Object key, Object value) {
        throw new NoSuchMethodError();
    }

    @Override
    @Deprecated
    public Binding remove(Object key) {
        return null;
    }

    public Binding removeBinding(String key) {
        return (Binding) super.remove(key);
    }

    public Binding removeAllBindings(Binding.NameType... type) {
        nextKey:
        for(String key:keySet().toArray(new String[0])){
            Binding v= getBinding(key);
            for (int i = 0; i < type.length; i++) {
                if(v.type==type[i]){
                    super.remove(key);
                    continue nextKey;
                }
            }
        }
        return null;
    }

    @Override
    @Deprecated
    public boolean containsValue(Object value) {
        throw new NoSuchMethodError();
    }

    public boolean containsBinding(Binding binding){
        return super.containsValue(binding);
    }

    public boolean containsNotDefinitions(String... keys) {
        for(Object key:keys){
            if(!containsKey(key) || getBinding(key).type==Binding.NameType.DEF){
                return false;
            }
        }
        return true;
    }

    public boolean lacksNotDefinitions(String... keys) {
        for(Object key:keys){
            if(containsKey(key) && getBinding(key).type!=Binding.NameType.DEF){
                return false;
            }
        }
        return true;
    }

    @Override
    @Deprecated
    public Object computeIfAbsent(String key, Function<? super String, ?> mappingFunction) {
        throw new NoSuchMethodError();
    }

    @Override
    @Deprecated
    public Object computeIfPresent(String key, BiFunction<? super String, ? super Object, ?> remappingFunction) {
        throw new NoSuchMethodError();
    }
}
