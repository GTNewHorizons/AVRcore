package com.github.technus.avrClone.registerPackages;

import com.github.technus.avrClone.AvrCore;

public interface IRegisterPackageSync<OBJ,T extends IRegisterPackageSync<OBJ,T>> extends IRegisterPackage<T> {
    /**
     * Sync before cpu cycle
     * @param core
     * @param objToSync
     */
    void preSync(AvrCore core,OBJ objToSync);

    /**
     * Sync after cpu cycle
     * @param core
     * @param objToSync
     */
    void postSync(AvrCore core,OBJ objToSync);
}
