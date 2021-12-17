package com.github.technus.avrClone.registerPackages;

public interface IRegisterBit<T extends IRegisterPackage<T>>{
    int getBitPosition();
    int getBitMask();
    String name();
    int getOffset(T registerPackage);
}
