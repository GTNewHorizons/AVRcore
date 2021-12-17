package com.github.technus.avrClone.registerPackages;

public class GPIO_Registers extends RegisterPackage<GPIO_Registers> {
    public GPIO_Registers(int offset){
        super(offset,0x10);
        addRegisters(Register.values());
    }

    public enum Register implements IRegister<GPIO_Registers> {
        GPIOR0(), GPIOR1(), GPIOR2(), GPIOR3(),
        GPIOR4(), GPIOR5(), GPIOR6(), GPIOR7(),
        GPIOR8(), GPIOR9(), GPIOR10(),GPIOR11(),
        GPIOR12(),GPIOR13(),GPIOR14(),GPIOR15();

        @Override
        public int getAddress(GPIO_Registers registers) {
            return ordinal()+registers.getOffset();
        }
    }
}
