package com.github.technus.avrClone.registerPackages;

import com.github.technus.avrClone.AvrCore;

public interface IInterrupt<T extends IRegisterPackage<T>> extends IRegister<T> {
    @Override
    @Deprecated
    default int getAddress(T registerPackage) {
        return getVector();
    }

    int getVector();
    boolean getTrigger(AvrCore core,T registerPackage);
    void setTrigger(AvrCore core,T registerPackage, boolean value);

    /**
     * Assuming that SREG.I is always SET!, otherwise it is not called, should clear it's interrupt request flag
     * @param core
     * @return
     */
    default boolean tryInterrupt(AvrCore core,T registerPackage){
        if(getTrigger(core,registerPackage)){
            setTrigger(core,registerPackage,false);
            return true;
        }
        return false;
    }
}
