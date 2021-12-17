package com.github.technus.avrClone.registerPackages;

import com.github.technus.avrClone.memory.IDataMemoryDefinition;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public interface IRegisterPackage<T extends IRegisterPackage<T>> extends IDataMemoryDefinition {
    /**
     * Register raw address to name
     * @return
     */
    Map<String,IRegister<T>> registersMap();

    /**
     * Register name to raw address
     * @return, single register, then register pair (optional)
     */
    Map<Integer, List<IRegister<T>>> addressesMap();

    /**
     * Register bit name to Definition
     * @return
     */
    Map<String,IRegisterBit<T>> bitsMap();

    /**
     * Interrupt vector to Definiton
     * @return
     */
    Map<Integer,IInterrupt<T>> interruptsMap();
}
