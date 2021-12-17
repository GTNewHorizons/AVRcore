package com.github.technus.avrClone;

import com.github.technus.avrClone.instructions.ExecutionEvent;
import com.github.technus.avrClone.instructions.IInstruction;
import com.github.technus.avrClone.instructions.InstructionRegistry;
import com.github.technus.avrClone.registerPackages.IInterrupt;
import com.github.technus.avrClone.memory.*;
import com.github.technus.avrClone.memory.program.ProgramMemory;
import com.github.technus.avrClone.registerPackages.CPU_Registers;
import com.github.technus.avrClone.registerPackages.IRegister;
import com.github.technus.avrClone.registerPackages.IRegisterPackage;
import com.github.technus.avrClone.registerFile.RegisterFilePairs;
import com.github.technus.avrClone.registerFile.RegisterFileSingles;

import java.util.*;

public class AvrCore {
    private volatile boolean valid=false;//whoosh

    private InstructionRegistry instructionRegistry;//MCU CORE
    private boolean immersiveOperands;//MCU CURE
    public boolean awoken=true,active=true;

    public int programCounter = 0;//PC register
    public final int[] registerFile=new int[32];
    public int[] dataMemory;
    private BitSet accessibleMemory;

    private ProgramMemory programMemory;//PROGRAM FLASH

    private SystemMemory systemMemory;
    private IoMemory ioMemory;//IO REGION
    private SramMemory sramMemory;//SRAM MEMORY
    private EepromMemory eepromMemory;//EEPROM

    private CPU_Registers cpuRegisters;
    private final HashMap<String, IRegisterPackage<?>> packages = new HashMap<>();

    private final TreeMap<Integer, IInterrupt<?>> interrupts = new TreeMap<>();

    public AvrCore(){}

    //region validity
    public boolean checkValid() {
        return valid = instructionRegistry != null &&
                programMemory != null &&
                ioMemory != null &&
                sramMemory != null &&
                systemMemory != null &&
                dataMemory != null &&
                accessibleMemory != null &&
                cpuRegisters != null;
    }

    public void invalidate(){
        valid=false;
    }

    public void invalidateProgramMemory(){
        programMemory=null;
        programCounter=0;
        invalidate();
    }

    public void invalidateSystemMemory(){
        systemMemory=null;
        ioMemory=null;
        sramMemory=null;
        eepromMemory=null;
        cpuRegisters=null;
        dataMemory=null;
        accessibleMemory =null;
        programCounter=0;
        removeAllPackages();
        clearRegisterFileContent();
        invalidate();
    }
    //endregion

    //region cpu logic
    public AvrCore simpleInit(){
        setDataMemory(0x100,  0x100);//512 mem total
        setCpuRegisters(0x30);
        return this;
    }

    public void reset(){
        clearVolatileDataMemoryContent();
        clearRegisterFileContent();
        programCounter=0;
    }

    public void clearRegisterFileContent(){
        for (int i = 0, len = registerFile.length; i < len; i++) {
            registerFile[i] = 0;
        }
    }

    public void setUsingImmersiveOperands(boolean immersiveOperands) {
        if(this.immersiveOperands!=immersiveOperands){
            this.immersiveOperands=immersiveOperands;
            invalidateProgramMemory();
        }
    }

    public boolean isUsingImmersiveOperands() {
        return immersiveOperands;
    }

    public void setInstructionRegistry(InstructionRegistry instructionRegistry) {
        if(this.instructionRegistry!=instructionRegistry) {
            this.instructionRegistry = instructionRegistry;
            invalidateProgramMemory();
        }
    }

    public InstructionRegistry getInstructionRegistry(){
        return instructionRegistry;
    }
    //endregion

    //region dataMemory
    public boolean setDataMemory(int ioSize,int ramSize) {
        if(ioSize<=0 || ramSize<=0){
            throw new IllegalArgumentException("Invalid memory size! Must be greater than 0!");
        }
        systemMemory=new SystemMemory(ramSize);//IO and SRAM

        dataMemory=new int[systemMemory.size];//memory image
        accessibleMemory =new BitSet(dataMemory.length);

        ioMemory=new IoMemory(ioSize);//in system memory

        sramMemory=new SramMemory(ramSize);//in system memory
        accessibleMemory.set(sramMemory.getOffset(),sramMemory.getOffset()+sramMemory.getSize());

        removeAllPackages();

        clearInterruptsConfiguration();

        cpuRegisters=null;
        eepromMemory=null;

        return checkValid();
    }

    public void clearVolatileDataMemoryContent(){
        RemovableMemory<EepromMemory> eepromBackup = getEepromMemory();
        dataMemory=new int[dataMemory.length];
        if(eepromBackup!=null) {
            setEepromMemory(eepromBackup);
        }
        initDataMemoryDefaults();
    }

    public void initDataMemoryDefaults(){
        for(IRegisterPackage<?> pack:packages.values()){
            System.arraycopy(pack.getDataDefault(),0,dataMemory,pack.getOffset(),pack.getSize());
        }
    }

    public boolean isDataAddressValid(int address){
        return accessibleMemory.get(address);
    }

    public boolean isDataRangeValid(int fromInclusive,int toExclusive){
        return accessibleMemory.nextClearBit(fromInclusive)>=toExclusive;
    }

    public boolean isDataBlockValid(int offset,int size){
        return accessibleMemory.nextClearBit(offset)>=offset+size;
    }

    public RemovableMemory<EepromMemory> blockEepromMemory(){
        RemovableMemory<EepromMemory> removableMemory = getEepromMemory();
        if(removableMemory!=null) {
            accessibleMemory.clear(eepromMemory.getOffset(), eepromMemory.getOffset() + eepromMemory.getSize());
        }
        return removableMemory;
    }

    public RemovableMemory<EepromMemory> unblockEepromMemory(){
        RemovableMemory<EepromMemory> removableMemory = getEepromMemory();
        if(removableMemory!=null) {
            accessibleMemory.set(eepromMemory.getOffset(), eepromMemory.getOffset() + eepromMemory.getSize());
        }
        return removableMemory;
    }

    public RemovableMemory<EepromMemory> restoreEepromDefinition(EepromMemory eeprom) {
        if(!valid){
            throw new RuntimeException("Cannot set MCU EEPROM!");
        }
        RemovableMemory<EepromMemory> eepromBackup= removeEepromMemory();
        if(eeprom!=null){
            eepromMemory=eeprom;
            accessibleMemory.set(eepromMemory.getOffset(),eepromMemory.getOffset()+eepromMemory.getSize());
        }
        return eepromBackup;
    }

    public RemovableMemory<EepromMemory> setEepromDefinition(EepromMemory eeprom) {
        if(!valid){
            throw new RuntimeException("Cannot set MCU EEPROM!");
        }
        RemovableMemory<EepromMemory> eepromBackup= removeEepromMemory();
        if(eeprom!=null){
            eepromMemory=eeprom;
            accessibleMemory.set(eepromMemory.getOffset(),eepromMemory.getOffset()+eepromMemory.getSize());
            System.arraycopy(eepromMemory.getDataDefault(),0,
                    dataMemory,eepromMemory.getOffset(),eepromMemory.getSize());
        }
        return eepromBackup;
    }

    public RemovableMemory<EepromMemory> setEepromMemory(RemovableMemory<EepromMemory> eeprom) {
        if(!valid){
            throw new RuntimeException("Cannot set MCU EEPROM!");
        }
        RemovableMemory<EepromMemory> eepromBackup=removeEepromMemory();
        if(eeprom!=null){
            eepromMemory=eeprom.getDefinition();
            accessibleMemory.set(eepromMemory.getOffset(),eepromMemory.getOffset()+eepromMemory.getSize());
            System.arraycopy(eepromMemory.getDataDefault(),0,
                    dataMemory,eepromMemory.getOffset(),eepromMemory.getSize());
        }
        return eepromBackup;
    }

    public RemovableMemory<EepromMemory> setEepromContent(RemovableMemory<EepromMemory> eeprom) {
        if(!valid){
            throw new RuntimeException("Cannot set MCU EEPROM!");
        }
        RemovableMemory<EepromMemory> eepromBackup= removeEepromMemory();
        if (eeprom != null) {
            if (eepromMemory != null) {
                int[] data = eeprom.getData();
                System.arraycopy(data, 0,
                        dataMemory, eepromMemory.getOffset(), Math.min(data.length,eepromMemory.getSize()));
            }else {
                throw new RuntimeException("No EEPROM to write to!");
            }
        }
        return eepromBackup;
    }

    public RemovableMemory<EepromMemory> setEepromContent(int[] data) {
        if(!valid){
            throw new RuntimeException("Cannot set MCU EEPROM!");
        }
        RemovableMemory<EepromMemory> eepromBackup= removeEepromMemory();
        if (data != null) {
            if (eepromMemory != null) {
                System.arraycopy(data, 0,
                        dataMemory, eepromMemory.getOffset(), Math.min(data.length,eepromMemory.getSize()));
            }else {
                throw new RuntimeException("No EEPROM to write to!");
            }
        }
        return eepromBackup;
    }

    public RemovableMemory<EepromMemory> clearEepromContent() {
        if(!valid){
            throw new RuntimeException("Cannot set MCU EEPROM!");
        }
        RemovableMemory<EepromMemory> eepromBackup= removeEepromMemory();
        if (eepromMemory != null) {
            System.arraycopy(eepromMemory.getDataDefault(), 0,
                    dataMemory, eepromMemory.getOffset(), eepromMemory.getSize());
        }else {
            throw new RuntimeException("No EEPROM to clear!");
        }
        return eepromBackup;
    }

    public RemovableMemory<EepromMemory> removeEepromMemory(){
        RemovableMemory<EepromMemory> removableMemory = getEepromMemory();
        if(removableMemory!=null) {
            accessibleMemory.clear(eepromMemory.getOffset(), eepromMemory.getOffset() + eepromMemory.getSize());
            eepromMemory = null;
        }
        return removableMemory;
    }

    public RemovableMemory<EepromMemory> getEepromMemory(){
        if(!valid || eepromMemory==null){
            return null;
        }
        int[] eeprom=new int[eepromMemory.getSize()];
        System.arraycopy(dataMemory,eepromMemory.getOffset(),eeprom,0,eeprom.length);
        return RemovableMemory.makeWithoutCloning(eepromMemory,eeprom);
    }
    //endregion

    //region cpu registers
    public void setCpuRegisters(int offset) {
        if(offset<0 || dataMemory==null){
            throw new RuntimeException("Cannot set MCU CPU Registers!");
        }
        if(cpuRegisters!=null) {
            removeDataBindings(cpuRegisters);
        }

        CPU_Registers cpu=new CPU_Registers(offset,dataMemory.length-1);
        if (putDataBindings(cpu, "CPU")) {
            cpuRegisters = cpu;
            checkValid();
        } else {
            throw new RuntimeException("Cannot set MCU CPU Registers, outside of range!");
        }
    }

    public CPU_Registers getCpuRegisters() {
        return cpuRegisters;
    }
    //endregion

    //region programMemory
    public void setProgramMemory(ProgramMemory programMemory) {
        this.programMemory = programMemory;
        checkValid();
    }

    public ProgramMemory getProgramMemory() {
        return programMemory;
    }

    public int getProgramSize(){
        return programMemory.instructions.length;
    }

    public IInstruction getInstruction(){
        return instructionRegistry.getInstruction(programMemory.instructions[programCounter]);
    }

    public IInstruction getInstruction(int addr){
        return instructionRegistry.getInstruction(programMemory.instructions[addr]);
    }

    public int getInstructionID(){
        return programMemory.instructions[programCounter];
    }

    public int getInstructionID(int addr){
        return programMemory.instructions[addr];
    }

    public int getOperand0(){
        return programMemory.param0[programCounter];
    }

    public int getOperand0(int addr){
        return programMemory.param0[addr];
    }

    public int getOperand1(){
        return programMemory.param1[programCounter];
    }

    public int getOperand1(int addr){
        return programMemory.param1[addr];
    }
    //endregion

    //region register package handlers
    public HashMap<String,Integer> getRegisterNames(){
        HashMap<String,Integer> map=new HashMap<>();
        for(int i=0;i<registerFile.length;i++){
            map.put("R"+i,i);
            map.put("r"+i,i);
        }
        map.put("X",RegisterFilePairs.X.offset);
        map.put("x",RegisterFilePairs.X.offset);
        map.put("Y",RegisterFilePairs.Y.offset);
        map.put("y",RegisterFilePairs.Y.offset);
        map.put("Z",RegisterFilePairs.Z.offset);
        map.put("z",RegisterFilePairs.Z.offset);
        map.put("W",RegisterFilePairs.W.offset);
        map.put("w",RegisterFilePairs.W.offset);
        map.put("Xl",RegisterFileSingles.Xl.offset);
        map.put("xl",RegisterFileSingles.Xl.offset);
        map.put("Yl",RegisterFileSingles.Yl.offset);
        map.put("yl",RegisterFileSingles.Yl.offset);
        map.put("Zl",RegisterFileSingles.Zl.offset);
        map.put("zl",RegisterFileSingles.Zl.offset);
        map.put("Wl",RegisterFileSingles.Wl.offset);
        map.put("wl",RegisterFileSingles.Wl.offset);
        map.put("Xh",RegisterFileSingles.Xh.offset);
        map.put("xh",RegisterFileSingles.Xh.offset);
        map.put("Yh",RegisterFileSingles.Yh.offset);
        map.put("yh",RegisterFileSingles.Yh.offset);
        map.put("Zh",RegisterFileSingles.Zh.offset);
        map.put("zh",RegisterFileSingles.Zh.offset);
        map.put("Wh",RegisterFileSingles.Wh.offset);
        map.put("wh",RegisterFileSingles.Wh.offset);
        return map;
    }
    
    public void removeAllPackages(){
        for (Map.Entry<String, IRegisterPackage<?>> entry:packages.entrySet()) {
            removeDataBindings(entry.getValue(),entry.getKey());
        }
        clearInterruptsConfiguration();//to be sure...
    }

    public String getPackageName(int i){
        if(accessibleMemory.get(i)){
            for(Map.Entry<String, IRegisterPackage<?>> entry:packages.entrySet()){
                IRegisterPackage<?> registerPackage=entry.getValue();
                if(i<registerPackage.getOffset()+registerPackage.getSize() && i>=registerPackage.getOffset()){
                    return entry.getKey();
                }
            }
        }
        return null;
    }

    public List<? extends IRegister<?>> getDataDefinitions(int i){
        if(accessibleMemory.get(i)){
            for(IRegisterPackage<?> registerPackage:packages.values()){
                List<? extends IRegister<?>> definitions=registerPackage.addressesMap().get(i);
                if(definitions!=null){
                    return definitions;
                }
            }
        }
        return null;
    }

    public HashMap<String,Integer> getDataNames(){
        HashMap<String,Integer> map=new HashMap<>();
        for(int i=0;i>=0;i=accessibleMemory.nextSetBit(i)){
            List<? extends IRegister<?>> definitions=getDataDefinitions(i);
            if(definitions!=null) {
                for (IRegister<?> definition : definitions) {
                    map.put(definition.name(),i);
                }
            }
            i++;
        }
        return map;
    }

    public boolean restoreDataBindings(IRegisterPackage<?> registerPackage) {
        return restoreDataBindings(registerPackage,registerPackage.getClass().getSimpleName()+registerPackage.hashCode());
    }

    public boolean restoreDataBindings(IRegisterPackage<?> registerPackage, String prefix, String postfix) {
        return restoreDataBindings(registerPackage,prefix + registerPackage.getClass().getSimpleName() + postfix);
    }

    public boolean restoreDataBindings(IRegisterPackage<?> registerPackage, String name) {
        if(accessibleMemory.get(registerPackage.getOffset(),registerPackage.getOffset()+registerPackage.getSize()).isEmpty()) {
            Map<Integer, ? extends IInterrupt<?>> interrupts=registerPackage.interruptsMap();
            if(interrupts!=null) {
                for (Integer key : interrupts.keySet()) {
                    if(this.interrupts.containsKey(key)){
                        throw new IllegalArgumentException("Overlapping interrupt vector");
                    }
                }
                this.interrupts.putAll(interrupts);
            }
            packages.put(name,registerPackage);
            accessibleMemory.set(registerPackage.getOffset(), registerPackage.getOffset() + registerPackage.getSize());
            return true;
        }
        return false;
    }

    public boolean putDataBindings(IRegisterPackage<?> registerPackage) {
        return putDataBindings(registerPackage,registerPackage.getClass().getSimpleName()+registerPackage.hashCode());
    }

    public boolean putDataBindings(IRegisterPackage<?> registerPackage, String prefix, String postfix) {
        return putDataBindings(registerPackage,prefix + registerPackage.getClass().getSimpleName() + postfix);
    }

    public boolean putDataBindings(IRegisterPackage<?> registerPackage, String name) {
        if(accessibleMemory.get(registerPackage.getOffset(),registerPackage.getOffset()+registerPackage.getSize()).isEmpty() && ioMemory.canEncapsulate(registerPackage)) {
            Map<Integer,? extends IInterrupt<?>> interrupts=registerPackage.interruptsMap();
            if(interrupts!=null) {
                for (Integer key : interrupts.keySet()) {
                    if(this.interrupts.containsKey(key)){
                        throw new IllegalArgumentException("Overlapping interrupt vector");
                    }
                }
                this.interrupts.putAll(interrupts);
            }
            packages.put(name,registerPackage);
            accessibleMemory.set(registerPackage.getOffset(), registerPackage.getOffset() + registerPackage.getSize());
            System.arraycopy(registerPackage.getDataDefault(),0,dataMemory,registerPackage.getOffset(),registerPackage.getSize());
            return true;
        }
        return false;
    }

    public boolean removeDataBindings(IRegisterPackage<?> registerPackage) {
        return removeDataBindings(registerPackage,registerPackage.getClass().getSimpleName()+registerPackage.hashCode());
    }

    public boolean removeDataBindings(IRegisterPackage<?> registerPackage, String prefix, String postfix){
        String name=prefix + registerPackage.getClass().getSimpleName() + postfix;
        return removeDataBindings(registerPackage,name);
    }

    public boolean removeDataBindings(IRegisterPackage<?> registerPackage, String name){
        if(packages.containsKey(name)) {
            Map<Integer,? extends IInterrupt<?>> i=registerPackage.interruptsMap();
            if(i!=null) {
                for (Integer key : i.keySet()) {
                    interrupts.remove(key);
                }
            }
            packages.remove(name);
            accessibleMemory.clear(registerPackage.getOffset(), registerPackage.getOffset() + registerPackage.getSize());
            //no cleanup required, data inaccessible, just do it?
            return true;
        }
        return false;
    }
    //endregion

    //region register bits
    public void setRegisterBits(int addr,int bitMask,boolean value){
        registerFile[addr] = value ? registerFile[addr] | bitMask : registerFile[addr] & ~bitMask;
    }

    public void setRegisterBits(int addr,int bitMask){
        registerFile[addr]|=bitMask;
    }

    public void clearRegisterBits(int addr,int bitMask){
        registerFile[addr]&=~bitMask;
    }

    public boolean getRegisterBitsOr(int addr, int bitMask) {
        return (registerFile[addr] & bitMask) != 0;
    }

    public boolean getRegisterBitsNotOr(int addr, int bitMask) {
        return (registerFile[addr] & bitMask) == 0;
    }

    public boolean getRegisterBitsNotAnd(int addr, int bitMask) {
        return (registerFile[addr] & bitMask) != bitMask;
    }

    public boolean getRegisterBitsAnd(int addr, int bitMask) {
        return (registerFile[addr] & bitMask) == bitMask;
    }
    //endregion

    //region data bits
    public void setDataBits(int addr,int bitMask,boolean value){
        if(accessibleMemory.get(addr)) {
            dataMemory[addr] = value ? dataMemory[addr] | bitMask : dataMemory[addr] & ~bitMask;
        }
    }

    public void setDataBits(int addr,int bitMask){
        if(accessibleMemory.get(addr)) {
            dataMemory[addr] |= bitMask;
        }
    }

    public void clearDataBits(int addr,int bitMask){
        if(accessibleMemory.get(addr)) {
            dataMemory[addr] &= ~bitMask;
        }
    }

    public boolean getDataBitsOr(int addr, int bitMask) {
        if(accessibleMemory.get(addr)) {
            return (dataMemory[addr] & bitMask) != 0;
        }
        return false;
    }

    public boolean getDataBitsNotOr(int addr, int bitMask) {
        if(accessibleMemory.get(addr)) {
            return (dataMemory[addr] & bitMask) == 0;
        }
        return true;
    }

    public boolean getDataBitsNotAnd(int addr, int bitMask) {
        if(accessibleMemory.get(addr)) {
            return (dataMemory[addr] & bitMask) != bitMask;
        }
        return true;
    }

    public boolean getDataBitsAnd(int addr, int bitMask) {
        if (accessibleMemory.get(addr)) {
            return (dataMemory[addr] & bitMask) == bitMask;
        }
        return false;
    }
    //endregion

    //region register value
    public void setRegisterValue(int addr,int value) {
        registerFile[addr]=value;
    }

    public int orRegisterValue(int addr, int bits) {
        return registerFile[addr]|=bits;
    }

    public int andRegisterValue(int addr, int bits) {
        return registerFile[addr]&=bits;
    }

    public int xorRegisterValue(int addr, int bits) {
        return registerFile[addr]^=bits;
    }

    public int notRegisterValue(int addr){
        return registerFile[addr]=~registerFile[addr];
    }

    public int negRegisterValue(int addr){
        return registerFile[addr]=-registerFile[addr];
    }

    public int getRegisterValue(int addr) {
        return registerFile[addr];
    }
    //endregion

    //region register pair
    public long getRegisterPairValue(int addr) {
        return (((long) registerFile[addr + 1]) << 32) | (registerFile[addr]);
    }

    public void setRegisterPairValue(int addr,long value) {
        registerFile[addr+1]=(int)(value>>32);
        registerFile[addr]=(int)value;
    }
    //endregion

    //region data
    public void setDataValue(int addr,int value) {
        dataMemory[addr]=value;
    }

    public int orDataValue(int addr, int bits) {
        return dataMemory[addr]|=bits;
    }

    public int andDataValue(int addr, int bits) {
        return dataMemory[addr]&=bits;
    }

    public int xorDataValue(int addr, int bits) {
        return dataMemory[addr]^=bits;
    }

    public int notDataValue(int addr){
        return dataMemory[addr]=~dataMemory[addr];
    }

    public int negDataValue(int addr){
        return dataMemory[addr]=-dataMemory[addr];
    }

    public int getDataValue(int addr) {
        return dataMemory[addr];
    }
    //endregion

    //region status bits
    public void setStatusBits(int bitMask,boolean desiredValue){
        dataMemory[cpuRegisters.SREG]=desiredValue? dataMemory[cpuRegisters.SREG]|bitMask: dataMemory[cpuRegisters.SREG]&~bitMask;
    }

    public void setStatusBits(int bitMask){
        dataMemory[cpuRegisters.SREG]|=bitMask;
    }

    public void clearStatusBits(int bitMask){
        dataMemory[cpuRegisters.SREG]&=~bitMask;
    }

    public boolean getStatusBitsOr(int bitMask){
        return (dataMemory[cpuRegisters.SREG] & bitMask) != 0;
    }

    public boolean getStatusBitsNotOr(int bitMask){
        return (dataMemory[cpuRegisters.SREG] & bitMask) == 0;
    }

    public boolean getStatusBitsNotAnd(int bitMask){
        return (dataMemory[cpuRegisters.SREG] & bitMask) != bitMask;
    }

    public boolean getStatusBitsAnd(int bitMask){
        return (dataMemory[cpuRegisters.SREG] & bitMask) == bitMask;
    }
    //endregion

    //region status value
    public int getStatusValue(){
        return dataMemory[cpuRegisters.SREG];
    }

    public void setStatusValue(int value){
        dataMemory[cpuRegisters.SREG]=value;
    }
    //endregion

    //region stack
    public int getStackPointer(){
        return dataMemory[cpuRegisters.SP];
    }

    public void setStackPointer(int stackPosition){
        dataMemory[cpuRegisters.SP]=stackPosition;
    }

    public void incStackPointer(){
        dataMemory[cpuRegisters.SP]++;
    }

    public void decStackPointer(){
        dataMemory[cpuRegisters.SP]--;
    }

    public void pushValue(int value){
        if(accessibleMemory.get(dataMemory[cpuRegisters.SP])){
            dataMemory[dataMemory[cpuRegisters.SP]--]=value;
        }
    }

    public int popValue(){
        if(accessibleMemory.get(++dataMemory[cpuRegisters.SP])) {
            return dataMemory[dataMemory[cpuRegisters.SP]];
        }
        return 0;
    }
    //endregion

    //region pointers
    public int getW(){
        return registerFile[RegisterFileSingles.Wl.offset];
    }

    public int getX(){
        return registerFile[RegisterFileSingles.Xl.offset];
    }

    public int getY(){
        return registerFile[RegisterFileSingles.Yl.offset];
    }

    public int getZ(){
        return registerFile[RegisterFileSingles.Zl.offset];
    }

    public int setW(int value){
        return registerFile[RegisterFileSingles.Wl.offset]=value;
    }

    public int setX(int value){
        return registerFile[RegisterFileSingles.Xl.offset]=value;
    }

    public int setY(int value){
        return registerFile[RegisterFileSingles.Yl.offset]=value;
    }

    public int setZ(int value){
        return registerFile[RegisterFileSingles.Zl.offset]=value;
    }

    public long getWpair(){
        return registerFile[RegisterFileSingles.Wl.offset]&((long) registerFile[RegisterFileSingles.Wh.offset]<<32);
    }

    public long getXpair(){
        return registerFile[RegisterFileSingles.Xl.offset]&((long) registerFile[RegisterFileSingles.Xh.offset]<<32);
    }

    public long getYpair(){
        return registerFile[RegisterFileSingles.Yl.offset]&((long) registerFile[RegisterFileSingles.Yh.offset]<<32);
    }

    public long getZpair(){
        return registerFile[RegisterFileSingles.Zl.offset]&((long) registerFile[RegisterFileSingles.Zh.offset]<<32);
    }

    public long setWpair(long value){
        setRegisterPairValue(RegisterFilePairs.W.offset,value);
        return getRegisterPairValue(RegisterFilePairs.W.offset);
    }

    public long setXpair(long value){
        setRegisterPairValue(RegisterFilePairs.X.offset,value);
        return getRegisterPairValue(RegisterFilePairs.X.offset);
    }

    public long setYpair(long value){
        setRegisterPairValue(RegisterFilePairs.Y.offset,value);
        return getRegisterPairValue(RegisterFilePairs.Y.offset);
    }

    public long setZpair(long value){
        setRegisterPairValue(RegisterFilePairs.Z.offset,value);
        return getRegisterPairValue(RegisterFilePairs.Z.offset);
    }
    //endregion

    //region interrupt handling
    public boolean getInterruptEnable(){
        return (dataMemory[cpuRegisters.SREG] & CPU_Registers.I) != 0;
    }

    public void interruptsHandle() throws IndexOutOfBoundsException,NullPointerException{
        if (getInterruptEnable()) {
            interruptsCycleForce();
        }
    }

    private void clearInterruptsConfiguration(){
        interrupts.clear();
    }

    @SuppressWarnings({"unchecked","rawtypes"})
    public void interruptsCycleForce() throws IndexOutOfBoundsException,NullPointerException{
        for(IRegisterPackage<?> registerPackage:packages.values()){
            for (IInterrupt interrupt:registerPackage.interruptsMap().values()){
                if (interrupt.tryInterrupt(this,registerPackage)) {//if cool and good
                    awoken =true;
                    pushValue(programCounter);
                    programCounter = interrupt.getVector();
                    dataMemory[cpuRegisters.SREG] &= CPU_Registers._I;//disable global interrupts!
                }
            }
        }
    }
    //endregion

    public ExecutionEvent cpuCycleForce() throws IndexOutOfBoundsException,NullPointerException{
        return instructionRegistry.getInstruction(programMemory.instructions[programCounter]).execute(this);
    }

    public ExecutionEvent cycle() throws IndexOutOfBoundsException,NullPointerException{
        if(active) {
            interruptsHandle();
            return awoken ? cpuCycleForce() : null;
        }
        return null;
    }
}
