package com.github.technus.avrClone.registerPackages;

public class CPU_Registers extends RegisterPackage<CPU_Registers> {
    public final int SREG,SP,defaultSP;
    public final static int
            I=RegisterBit.I.getBitMask(),
            _I=~RegisterBit.I.getBitMask(),
            T=RegisterBit.T.getBitMask(),
            H=RegisterBit.H.getBitMask(),
            S=RegisterBit.S.getBitMask(),
            V=RegisterBit.V.getBitMask(),
            N=RegisterBit.N.getBitMask(),
            Z=RegisterBit.Z.getBitMask(),
            C=RegisterBit.C.getBitMask(),
            U=RegisterBit.U.getBitMask(),//cheeky boye disables H status compute
            H1=RegisterBit.H1.getBitMask(),//is H
            H2=RegisterBit.H2.getBitMask(),
            H3=RegisterBit.H3.getBitMask(),
            H4=RegisterBit.H4.getBitMask(),
            H5=RegisterBit.H5.getBitMask(),
            H6=RegisterBit.H6.getBitMask(),
            H7=RegisterBit.H7.getBitMask(),
            H8=RegisterBit.H8.getBitMask();//is C
    
    public CPU_Registers(int offset,int defaultSP){
        super(offset,0x10);
        this.defaultSP=defaultSP;
        addRegisters(Register.values());
        addBits(RegisterBit.values());

        SREG=Register.SREG.getAddress(this);
        SP=Register.SP.getAddress(this);
    }

    @Override
    public int[] getDataDefault() {
        int[] dataDefault=new int[getSize()];
        dataDefault[Register.SP.relativeOffset]=defaultSP;
        return dataDefault;
    }

    public enum Register implements IRegister<CPU_Registers> {
        //lets assume the core doesn't need more than 64bit addressing space...
        //RAMPD(0x08),RAMPX(0x09),RAMPY(0x0a),RAMPZ(0x0b),
        //EIND(0x0c),
        CCP(0x04),
        SP(0x0d),
        SREG(0x0f);

        public final int relativeOffset;

        Register(int relativeOffset){
            this.relativeOffset =relativeOffset;
        }

        @Override
        public int getAddress(CPU_Registers registers) {
            return relativeOffset +registers.getOffset();
        }
    }

    public enum RegisterBit implements IRegisterBit<CPU_Registers> {
        C,Z,N,V,S,H,T,I,H1,H2,H3,H4,H5,H6,H7,H8,U;
        public final int bit,mask;

        RegisterBit(){
            this.bit=ordinal();
            this.mask=1<<bit;
        }

        @Override
        public int getOffset(CPU_Registers registers) {
            return Register.SREG.getAddress(registers);
        }

        @Override
        public int getBitPosition() {
            return bit;
        }

        @Override
        public int getBitMask() {
            return mask;
        }
    }
}
