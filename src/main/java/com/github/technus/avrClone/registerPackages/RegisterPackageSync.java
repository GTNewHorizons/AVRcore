package com.github.technus.avrClone.registerPackages;

public abstract class RegisterPackageSync<OBJ,T extends RegisterPackageSync<OBJ,T>> extends RegisterPackage<T> implements IRegisterPackageSync<OBJ,T> {
    protected RegisterPackageSync(int offset, int size) {
        super(offset, size);
    }
}

