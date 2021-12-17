package com.github.technus.avrClone.instructions;

import com.github.technus.avrClone.AvrCore;
import com.github.technus.avrClone.compiler.ProgramCompiler;
import com.github.technus.avrClone.instructions.exceptions.*;
import com.github.technus.avrClone.memory.program.*;
import com.github.technus.avrClone.memory.program.exceptions.*;
import com.github.technus.avrClone.registerPackages.CPU_Registers;
import com.github.technus.avrClone.registerFile.RegisterFileSingles;
import jpsam3hklam9.des.DES;

import java.util.ArrayList;
import java.util.Random;

import static com.github.technus.avrClone.instructions.OperandLimit.*;

public abstract class Instruction implements IInstruction {
    public static Random random=new Random();
    public static final ArrayList<Instruction> INSTRUCTIONS_OP = new ArrayList<>();
    public static final ArrayList<Instruction> INSTRUCTIONS_IMMERSIVE = new ArrayList<>();

    public static final Instruction
            NULL = new Instruction("NULL",false) {
        @Override
        public ExecutionEvent execute(AvrCore core) {
            return new ExecutionEvent(core.programCounter++,this,new DebugEvent("NULL!"));
        }
    },
            ADC = new Instruction("ADC",true, R, R) {
                @Override
                public ExecutionEvent execute(AvrCore core) {
                    int Rdi = core.getRegisterValue(core.getOperand0());
                    int Rri = core.getRegisterValue(core.getOperand1());

                    int Cb = core.getStatusBitsAnd(CPU_Registers.C) ? 1 : 0;

                    long R = (Rdi & 0xFFFFFFFFL) + (Rri & 0xFFFFFFFFL) + Cb;
                    core.setRegisterValue(core.getOperand0(), (int) R);
                    boolean C = R > 0xFFFFFFFFL;
                    core.setStatusBits(CPU_Registers.C, C);
                    core.setStatusBits(CPU_Registers.Z, (int) R == 0);
                    boolean N = (int) R < 0;
                    core.setStatusBits(CPU_Registers.N, N);

                    R = (long) Rdi + Rri + Cb;
                    boolean V = R > Integer.MAX_VALUE || R < Integer.MIN_VALUE;
                    core.setStatusBits(CPU_Registers.V, V);
                    core.setStatusBits(CPU_Registers.S, N ^ V);

                    if (core.getStatusBitsAnd(CPU_Registers.U)) {
                        core.setStatusBits(CPU_Registers.H8, C);

                        int mask = 0x0000000F;
                        int Md = Rdi & mask;
                        int Mr = Rri & mask;
                        C = Md + Mr + Cb > mask;
                        core.setStatusBits(CPU_Registers.H, C);
                        core.setStatusBits(CPU_Registers.H1, C);

                        for (int i = 0; i < 6; i++) {
                            mask = 0x0FFFFFFF >> (i << 2);
                            Md = Rdi & mask;
                            Mr = Rri & mask;
                            core.setStatusBits(CPU_Registers.H7 >> i, Md + Mr + Cb > mask);
                        }
                    }
                    core.programCounter++;
                    return null;
                }

                @Override
                public int getCost(AvrCore core) {
                    return core.getStatusBitsAnd(CPU_Registers.U)?4:1;
                }
            },
            ADCI = new Instruction("ADCI",false, Rh, K8s) {
                @Override
                public ExecutionEvent execute(AvrCore core) {
                    int Rdi = core.getRegisterValue(core.getOperand0());
                    int Rri = core.getOperand1();

                    int Cb = core.getStatusBitsAnd(CPU_Registers.C) ? 1 : 0;

                    long R = (Rdi & 0xFFFFFFFFL) + (Rri & 0xFFFFFFFFL) + Cb;
                    core.setRegisterValue(core.getOperand0(), (int) R);
                    boolean C = R > 0xFFFFFFFFL;
                    core.setStatusBits(CPU_Registers.C, C);
                    core.setStatusBits(CPU_Registers.Z, (int) R == 0);
                    boolean N = (int) R < 0;
                    core.setStatusBits(CPU_Registers.N, N);

                    R = (long) Rdi + Rri + Cb;
                    boolean V = R > Integer.MAX_VALUE || R < Integer.MIN_VALUE;
                    core.setStatusBits(CPU_Registers.V, V);
                    core.setStatusBits(CPU_Registers.S, N ^ V);

                    if (core.getStatusBitsAnd(CPU_Registers.U)) {
                        core.setStatusBits(CPU_Registers.H8, C);

                        int mask = 0x0000000F;
                        int Md = Rdi & mask;
                        int Mr = Rri & mask;
                        C = Md + Mr + Cb > mask;
                        core.setStatusBits(CPU_Registers.H, C);
                        core.setStatusBits(CPU_Registers.H1, C);

                        for (int i = 0; i < 6; i++) {
                            mask = 0x0FFFFFFF >> (i << 2);
                            Md = Rdi & mask;
                            Mr = Rri & mask;
                            core.setStatusBits(CPU_Registers.H7 >> i, Md + Mr + Cb > mask);
                        }
                    }
                    core.programCounter++;
                    return null;
                }

                @Override
                public int getCost(AvrCore core) {
                    return core.getStatusBitsAnd(CPU_Registers.U)?4:1;
                }
            },
            ADD = new Instruction("ADD",true, R, R) {
                @Override
                public ExecutionEvent execute(AvrCore core) {
                    int Rdi = core.getRegisterValue(core.getOperand0());
                    int Rri = core.getRegisterValue(core.getOperand1());

                    long R = (Rdi & 0xFFFFFFFFL) + (Rri & 0xFFFFFFFFL);
                    core.setRegisterValue(core.getOperand0(), (int) R);
                    boolean C = R > 0xFFFFFFFFL;
                    core.setStatusBits(CPU_Registers.C, C);
                    core.setStatusBits(CPU_Registers.Z, (int) R == 0);
                    boolean N = (int) R < 0;
                    core.setStatusBits(CPU_Registers.N, N);

                    R = (long) Rdi + Rri;
                    boolean V = R > Integer.MAX_VALUE || R < Integer.MIN_VALUE;
                    core.setStatusBits(CPU_Registers.V, V);
                    core.setStatusBits(CPU_Registers.S, N ^ V);

                    if (core.getStatusBitsAnd(CPU_Registers.U)) {
                        core.setStatusBits(CPU_Registers.H8, C);

                        int mask = 0x0000000F;
                        int Md = Rdi & mask;
                        int Mr = Rri & mask;
                        C = Md + Mr > mask;
                        core.setStatusBits(CPU_Registers.H, C);
                        core.setStatusBits(CPU_Registers.H1, C);

                        for (int i = 0; i < 6; i++) {
                            mask = 0x0FFFFFFF >> (i << 2);
                            Md = Rdi & mask;
                            Mr = Rri & mask;
                            core.setStatusBits(CPU_Registers.H7 >> i, Md + Mr > mask);
                        }
                    }
                    core.programCounter++;
                    return null;
                }

                @Override
                public int getCost(AvrCore core) {
                    return core.getStatusBitsAnd(CPU_Registers.U)?4:1;
                }
            },
            ADDI = new Instruction("ADDI",false, Rh, K8s) {
                @Override
                public ExecutionEvent execute(AvrCore core) {
                    int Rdi = core.getRegisterValue(core.getOperand0());
                    int Rri = core.getOperand1();

                    long R = (Rdi & 0xFFFFFFFFL) + (Rri & 0xFFFFFFFFL);
                    core.setRegisterValue(core.getOperand0(), (int) R);
                    boolean C = R > 0xFFFFFFFFL;
                    core.setStatusBits(CPU_Registers.C, C);
                    core.setStatusBits(CPU_Registers.Z, (int) R == 0);
                    boolean N = (int) R < 0;
                    core.setStatusBits(CPU_Registers.N, N);

                    R = (long) Rdi + Rri;
                    boolean V = R > Integer.MAX_VALUE || R < Integer.MIN_VALUE;
                    core.setStatusBits(CPU_Registers.V, V);
                    core.setStatusBits(CPU_Registers.S, N ^ V);

                    if (core.getStatusBitsAnd(CPU_Registers.U)) {
                        core.setStatusBits(CPU_Registers.H8, C);

                        int mask = 0x0000000F;
                        int Md = Rdi & mask;
                        int Mr = Rri & mask;
                        C = Md + Mr > mask;
                        core.setStatusBits(CPU_Registers.H, C);
                        core.setStatusBits(CPU_Registers.H1, C);

                        for (int i = 0; i < 6; i++) {
                            mask = 0x0FFFFFFF >> (i << 2);
                            Md = Rdi & mask;
                            Mr = Rri & mask;
                            core.setStatusBits(CPU_Registers.H7 >> i, Md + Mr > mask);
                        }
                    }
                    core.programCounter++;
                    return null;
                }

                @Override
                public int getCost(AvrCore core) {
                    return core.getStatusBitsAnd(CPU_Registers.U)?4:1;
                }
            },
    //ADIW = new Instruction("ADIW",true, Rp, K6) {
    //    @Override
    //    public ExecutionEvent execute(AvrCore core) {
    //        long Rdi = core.getRegisterPairValue(core.getOperand0());
    //        int Rri = core.getOperand1();
    //
    //        long R = Rdi + Rri;
    //        core.setRegisterValue(core.getOperand0(), (int) R);
    //        boolean C = Long.MAX_VALUE - Rdi < Rri;
    //        core.setStatusBits(CPU_Registers.C, C);
    //        core.setStatusBits(CPU_Registers.Z, (int) R == 0);
    //        boolean N = (int) R < 0;
    //        core.setStatusBits(CPU_Registers.N, N);
    //
    //        boolean V = C || Long.MAX_VALUE - Rri < Rdi;//todo
    //        core.setStatusBits(CPU_Registers.V, V);
    //        core.setStatusBits(CPU_Registers.S, N ^ V);
    //
    //        core.programCounter++;
    //        return null;
    //    }
    //},
    AND = new Instruction("AND",true, R, R) {
        @Override
        public ExecutionEvent execute(AvrCore core) {
            int R = core.andRegisterValue(core.getOperand0(), core.getRegisterValue(core.getOperand1()));
            core.setStatusBits(CPU_Registers.Z, R == 0);
            core.setStatusBits(CPU_Registers.S | CPU_Registers.N, R < 0);
            core.clearStatusBits(CPU_Registers.V);

            core.programCounter++;
            return null;
        }
    },
            ANDI = new Instruction("ANDI",true, Rh, K8b) {
                @Override
                public ExecutionEvent execute(AvrCore core) {
                    int R = core.andRegisterValue(core.getOperand0(), core.getOperand1());
                    core.setStatusBits(CPU_Registers.Z, R == 0);
                    core.setStatusBits(CPU_Registers.S | CPU_Registers.N, R < 0);
                    core.clearStatusBits(CPU_Registers.V);

                    core.programCounter++;
                    return null;
                }
            },
            ASR = new Instruction("ASR",true, R) {
                @Override
                public ExecutionEvent execute(AvrCore core) {
                    int R = core.getRegisterValue(core.getOperand0());
                    boolean C = (R & 0x1) != 0;
                    core.setStatusBits(CPU_Registers.C, C);//always last bit tho...
                    R >>>= 1;
                    core.setRegisterValue(core.getOperand0(), R);
                    core.setStatusBits(CPU_Registers.Z, R == 0);
                    boolean N = R < 0, V = N ^ C;
                    core.setStatusBits(CPU_Registers.N, N);
                    core.setStatusBits(CPU_Registers.V, V);
                    core.setStatusBits(CPU_Registers.S, N ^ V);
                    core.programCounter++;
                    return null;
                }
            },
            ASRD = new Instruction("ASRD",false, R, R) {
                @Override
                public ExecutionEvent execute(AvrCore core) {
                    int R = core.getRegisterValue(core.getOperand0());
                    R >>>= core.getRegisterValue(core.getOperand1());//for faster divisions hack
                    core.setRegisterValue(core.getOperand0(), R);
                    core.setStatusBits(CPU_Registers.Z, R == 0);
                    core.setStatusBits(CPU_Registers.N, R < 0);
                    core.programCounter++;
                    return null;
                }
            },
            ASRI = new Instruction("ASRI",false, Rh, K8b) {
                @Override
                public ExecutionEvent execute(AvrCore core) {
                    int R = core.getRegisterValue(core.getOperand0());
                    R >>>= core.getOperand1();//for faster divisions hack
                    core.setRegisterValue(core.getOperand0(), R);
                    core.setStatusBits(CPU_Registers.Z, R == 0);
                    core.setStatusBits(CPU_Registers.N, R < 0);
                    core.programCounter++;
                    return null;
                }
            },
            BCLR = new Instruction("BCLR",true, b) {
                @Override
                public ExecutionEvent execute(AvrCore core) {
                    core.clearStatusBits(core.getOperand0());
                    core.programCounter++;
                    return null;
                }
            },
            BLD = new Instruction("BLD",true, R, b) {
                @Override
                public ExecutionEvent execute(AvrCore core) {
                    core.setRegisterBits(core.getOperand0(), core.getOperand1(), core.getStatusBitsAnd(CPU_Registers.T));
                    core.programCounter++;
                    return null;
                }
            },
            BRBC = new Instruction("BRBC",true, b, S7) {
                @Override
                public ExecutionEvent execute(AvrCore core) {
                    if (core.getStatusBitsAnd(core.getOperand0())) {
                        core.programCounter++;
                    } else {
                        core.programCounter += core.getOperand1();
                    }
                    return null;
                }
            },
            BRBS = new Instruction("BRBS",true, b, S7) {
                @Override
                public ExecutionEvent execute(AvrCore core) {
                    if (core.getStatusBitsAnd(core.getOperand0())) {
                        core.programCounter += core.getOperand1();
                    } else {
                        core.programCounter++;
                    }
                    return null;
                }
            },
            BRCC = new Instruction("BRCC",true, S7) {
                @Override
                public ExecutionEvent execute(AvrCore core) {
                    if (core.getStatusBitsAnd(CPU_Registers.C)) {
                        core.programCounter++;
                    } else {
                        core.programCounter += core.getOperand0();
                    }
                    return null;
                }
            },
            BRCS = new Instruction("BRCS",true, S7) {
                @Override
                public ExecutionEvent execute(AvrCore core) {
                    if (core.getStatusBitsAnd(CPU_Registers.C)) {
                        core.programCounter += core.getOperand0();
                    } else {
                        core.programCounter++;
                    }
                    return null;
                }
            },
            BREAK = new Instruction("BREAK",true) {
                @Override
                public ExecutionEvent execute(AvrCore core) {
                    return new ExecutionEvent(core.programCounter++,this,new DebugEvent("BREAK!"));
                }
            },
            BREQ = new Instruction("BREQ",true, S7) {
                @Override
                public ExecutionEvent execute(AvrCore core) {
                    if (core.getStatusBitsAnd(CPU_Registers.Z)) {
                        core.programCounter += core.getOperand0();
                    } else {
                        core.programCounter++;
                    }
                    return null;
                }
            },
            BRGE = new Instruction("BRGE",true, S7) {
                @Override
                public ExecutionEvent execute(AvrCore core) {
                    if (core.getStatusBitsAnd(CPU_Registers.S)) {
                        core.programCounter++;
                    } else {
                        core.programCounter += core.getOperand0();
                    }
                    return null;
                }
            },
            BRHC = new Instruction("BRHC",true, S7) {
                @Override
                public ExecutionEvent execute(AvrCore core) {
                    if (core.getStatusBitsAnd(CPU_Registers.H)) {
                        core.programCounter++;
                    } else {
                        core.programCounter += core.getOperand0();
                    }
                    return null;
                }
            },
            BRHS = new Instruction("BRHS",true, S7) {
                @Override
                public ExecutionEvent execute(AvrCore core) {
                    if (core.getStatusBitsAnd(CPU_Registers.H)) {
                        core.programCounter += core.getOperand0();
                    } else {
                        core.programCounter++;
                    }
                    return null;
                }
            },
            BRID = new Instruction("BRID",true, S7) {
                @Override
                public ExecutionEvent execute(AvrCore core) {
                    if (core.getStatusBitsAnd(CPU_Registers.I)) {
                        core.programCounter++;
                    } else {
                        core.programCounter += core.getOperand0();
                    }
                    return null;
                }
            },
            BRIE = new Instruction("BRIE",true, S7) {
                @Override
                public ExecutionEvent execute(AvrCore core) {
                    if (core.getStatusBitsAnd(CPU_Registers.I)) {
                        core.programCounter += core.getOperand0();
                    } else {
                        core.programCounter++;
                    }
                    return null;
                }
            },
            BRLO = new Instruction("BRLO",true, S7) {
                @Override
                public ExecutionEvent execute(AvrCore core) {
                    if (core.getStatusBitsAnd(CPU_Registers.C)) {
                        core.programCounter += core.getOperand0();
                    } else {
                        core.programCounter++;
                    }
                    return null;
                }
            },
            BRLT = new Instruction("BRLT",true, S7) {
                @Override
                public ExecutionEvent execute(AvrCore core) {
                    if (core.getStatusBitsAnd(CPU_Registers.S)) {
                        core.programCounter += core.getOperand0();
                    } else {
                        core.programCounter++;
                    }
                    return null;
                }
            },
            BRMI = new Instruction("BRMI",true, S7) {
                @Override
                public ExecutionEvent execute(AvrCore core) {
                    if (core.getStatusBitsAnd(CPU_Registers.N)) {
                        core.programCounter += core.getOperand0();
                    } else {
                        core.programCounter++;
                    }
                    return null;
                }
            },
            BRNE = new Instruction("BRNE",true, S7) {
                @Override
                public ExecutionEvent execute(AvrCore core) {
                    if (core.getStatusBitsAnd(CPU_Registers.Z)) {
                        core.programCounter++;
                    } else {
                        core.programCounter += core.getOperand0();
                    }
                    return null;
                }
            },
            BRPL = new Instruction("BRPL",true, S7) {
                @Override
                public ExecutionEvent execute(AvrCore core) {
                    if (core.getStatusBitsAnd(CPU_Registers.N)) {
                        core.programCounter++;
                    } else {
                        core.programCounter += core.getOperand0();
                    }
                    return null;
                }
            },
            BRSH = new Instruction("BRSH",true, S7) {
                @Override
                public ExecutionEvent execute(AvrCore core) {
                    if (core.getStatusBitsAnd(CPU_Registers.C)) {
                        core.programCounter++;
                    } else {
                        core.programCounter += core.getOperand0();
                    }
                    return null;
                }
            },
            BRTC = new Instruction("BRTC",true, S7) {
                @Override
                public ExecutionEvent execute(AvrCore core) {
                    if (core.getStatusBitsAnd(CPU_Registers.T)) {
                        core.programCounter++;
                    } else {
                        core.programCounter += core.getOperand0();
                    }
                    return null;
                }
            },
            BRTS = new Instruction("BRTS",true, S7) {
                @Override
                public ExecutionEvent execute(AvrCore core) {
                    if (core.getStatusBitsAnd(CPU_Registers.T)) {
                        core.programCounter += core.getOperand0();
                    } else {
                        core.programCounter++;
                    }
                    return null;
                }
            },
            BRVC = new Instruction("BRVC",true, S7) {
                @Override
                public ExecutionEvent execute(AvrCore core) {
                    if (core.getStatusBitsAnd(CPU_Registers.V)) {
                        core.programCounter++;
                    } else {
                        core.programCounter += core.getOperand0();
                    }
                    return null;
                }
            },
            BRVS = new Instruction("BRVS",true, S7) {
                @Override
                public ExecutionEvent execute(AvrCore core) {
                    if (core.getStatusBitsAnd(CPU_Registers.V)) {
                        core.programCounter += core.getOperand0();
                    } else {
                        core.programCounter++;
                    }
                    return null;
                }
            },
            BSET = new Instruction("BSET",true, b) {
                @Override
                public ExecutionEvent execute(AvrCore core) {
                    core.setStatusBits(core.getOperand0());
                    core.programCounter++;
                    return null;
                }
            },
            BST = new Instruction("BST",true, R, b) {
                @Override
                public ExecutionEvent execute(AvrCore core) {
                    core.setStatusBits(CPU_Registers.T, core.getRegisterBitsOr(core.getOperand0(), core.getOperand1()));
                    core.programCounter++;
                    return null;
                }
            },
            CALL = new Instruction("CALL",true, P22) {
                @Override
                public ExecutionEvent execute(AvrCore core) {
                    core.pushValue(core.programCounter);
                    core.programCounter = core.getOperand0();
                    return null;
                }
            },
            CBI = new Instruction("CBI",true, IO5, b) {
                @Override
                public ExecutionEvent execute(AvrCore core) {
                    core.clearDataBits(core.getOperand0(), core.getOperand1());
                    core.programCounter++;
                    return null;
                }
            },
            CBR = new Instruction("CBR",true, Rh, K8b) {
                @Override
                public ExecutionEvent execute(AvrCore core) {
                    core.clearRegisterBits(core.getOperand0(), core.getOperand1());
                    core.programCounter++;
                    return null;
                }
            },//ANDI...
            CLC = new Instruction("CLC",true) {
                @Override
                public ExecutionEvent execute(AvrCore core) {
                    core.clearStatusBits(CPU_Registers.C);
                    core.programCounter++;
                    return null;
                }
            },
            CLH = new Instruction("CLH",true) {
                @Override
                public ExecutionEvent execute(AvrCore core) {
                    core.clearStatusBits(CPU_Registers.H);
                    core.programCounter++;
                    return null;
                }
            },
            CLI = new Instruction("CLI",true) {
                @Override
                public ExecutionEvent execute(AvrCore core) {
                    core.clearStatusBits(CPU_Registers.I);
                    core.programCounter++;
                    return null;
                }
            },
            CLN = new Instruction("CLN",true) {
                @Override
                public ExecutionEvent execute(AvrCore core) {
                    core.clearStatusBits(CPU_Registers.N);
                    core.programCounter++;
                    return null;
                }
            },
            CLR = new Instruction("CLR",true, R) {
                @Override
                public ExecutionEvent execute(AvrCore core) {
                    core.setRegisterValue(core.getOperand0(), 0);
                    core.programCounter++;
                    return null;
                }
            },
            CLS = new Instruction("CLS",true) {
                @Override
                public ExecutionEvent execute(AvrCore core) {
                    core.clearStatusBits(CPU_Registers.S);
                    core.programCounter++;
                    return null;
                }
            },
            CLT = new Instruction("CLT",true) {
                @Override
                public ExecutionEvent execute(AvrCore core) {
                    core.clearStatusBits(CPU_Registers.T);
                    core.programCounter++;
                    return null;
                }
            },
            CLV = new Instruction("CLV",true) {
                @Override
                public ExecutionEvent execute(AvrCore core) {
                    core.clearStatusBits(CPU_Registers.V);
                    core.programCounter++;
                    return null;
                }
            },
            CLZ = new Instruction("CLZ",true) {
                @Override
                public ExecutionEvent execute(AvrCore core) {
                    core.clearStatusBits(CPU_Registers.Z);
                    core.programCounter++;
                    return null;
                }
            },
            COM = new Instruction("COM",true, R) {
                @Override
                public ExecutionEvent execute(AvrCore core) {
                    int R = core.notRegisterValue(core.getOperand0());
                    core.setStatusBits(CPU_Registers.V, false);
                    core.setStatusBits(CPU_Registers.C, true);
                    core.setStatusBits(CPU_Registers.Z, R == 0);
                    boolean N = R < 0;
                    core.setStatusBits(CPU_Registers.N, N);
                    core.setStatusBits(CPU_Registers.S, N);
                    core.programCounter++;
                    return null;
                }
            },
            CP = new Instruction("CP",true, R, R) {
                @Override
                public ExecutionEvent execute(AvrCore core) {
                    int Rdi = core.getRegisterValue(core.getOperand0());
                    int Rri = core.getRegisterValue(core.getOperand1());
                    long Rdl = Rdi & 0xFFFFFFFFL;
                    long Rrl = Rri & 0xFFFFFFFFL;

                    long R = Rdl - Rrl;

                    boolean C = Rrl > Rdl;
                    core.setStatusBits(CPU_Registers.C, C);
                    core.setStatusBits(CPU_Registers.Z, (int) R == 0);
                    boolean N = (int) R < 0;
                    core.setStatusBits(CPU_Registers.N, N);

                    R = (long) Rdi - Rri;
                    boolean V = R > Integer.MAX_VALUE || R < Integer.MIN_VALUE;
                    core.setStatusBits(CPU_Registers.V, V);
                    core.setStatusBits(CPU_Registers.S, N ^ V);

                    if (core.getStatusBitsAnd(CPU_Registers.U)) {
                        core.setStatusBits(CPU_Registers.H8, C);

                        int mask = 0x0000000F;
                        int Md = Rdi & mask;
                        int Mr = Rri & mask;
                        C = Mr > Md;
                        core.setStatusBits(CPU_Registers.H, C);
                        core.setStatusBits(CPU_Registers.H1, C);

                        for (int i = 0; i < 6; i++) {
                            mask = 0x0FFFFFFF >> (i << 2);
                            Md = Rdi & mask;
                            Mr = Rri & mask;
                            core.setStatusBits(CPU_Registers.H7 >> i, Mr > Md);
                        }
                    }
                    core.programCounter++;
                    return null;
                }

                @Override
                public int getCost(AvrCore core) {
                    return core.getStatusBitsAnd(CPU_Registers.U)?4:1;
                }
            },
            CPC = new Instruction("CPC",true, R, R) {
                @Override
                public ExecutionEvent execute(AvrCore core) {
                    int Rdi = core.getRegisterValue(core.getOperand0());
                    int Rri = core.getRegisterValue(core.getOperand1());
                    long Rdl = Rdi & 0xFFFFFFFFL;
                    long Rrl = Rri & 0xFFFFFFFFL;

                    int Cb = core.getStatusBitsAnd(CPU_Registers.C) ? 1 : 0;

                    long R = Rdl - Rrl - Cb;

                    boolean C = Rrl + Cb > Rdl;
                    core.setStatusBits(CPU_Registers.C, C);
                    core.setStatusBits(CPU_Registers.Z, (int) R == 0);
                    boolean N = (int) R < 0;
                    core.setStatusBits(CPU_Registers.N, N);

                    R = (long) Rdi - Rri - Cb;
                    boolean V = R > Integer.MAX_VALUE || R < Integer.MIN_VALUE;
                    core.setStatusBits(CPU_Registers.V, V);
                    core.setStatusBits(CPU_Registers.S, N ^ V);

                    if (core.getStatusBitsAnd(CPU_Registers.U)) {
                        core.setStatusBits(CPU_Registers.H8, C);

                        int mask = 0x0000000F;
                        int Md = Rdi & mask;
                        int Mr = Rri & mask;
                        C = Mr + Cb > Md;
                        core.setStatusBits(CPU_Registers.H, C);
                        core.setStatusBits(CPU_Registers.H1, C);

                        for (int i = 0; i < 6; i++) {
                            mask = 0x0FFFFFFF >> (i << 2);
                            Md = Rdi & mask;
                            Mr = Rri & mask;
                            core.setStatusBits(CPU_Registers.H7 >> i, Mr + Cb > Md);
                        }
                    }
                    core.programCounter++;
                    return null;
                }

                @Override
                public int getCost(AvrCore core) {
                    return core.getStatusBitsAnd(CPU_Registers.U)?4:1;
                }
            },
            CPCI = new Instruction("CPCI",false, Rh, K8s) {
                @Override
                public ExecutionEvent execute(AvrCore core) {
                    int Rdi = core.getRegisterValue(core.getOperand0());
                    int Rri = core.getOperand1();
                    long Rdl = Rdi & 0xFFFFFFFFL;
                    long Rrl = Rri & 0xFFFFFFFFL;

                    int Cb = core.getStatusBitsAnd(CPU_Registers.C) ? 1 : 0;

                    long R = Rdl - Rrl - Cb;

                    boolean C = Rrl + Cb > Rdl;
                    core.setStatusBits(CPU_Registers.C, C);
                    core.setStatusBits(CPU_Registers.Z, (int) R == 0);
                    boolean N = (int) R < 0;
                    core.setStatusBits(CPU_Registers.N, N);

                    R = (long) Rdi - Rri - Cb;
                    boolean V = R > Integer.MAX_VALUE || R < Integer.MIN_VALUE;
                    core.setStatusBits(CPU_Registers.V, V);
                    core.setStatusBits(CPU_Registers.S, N ^ V);

                    if (core.getStatusBitsAnd(CPU_Registers.U)) {
                        core.setStatusBits(CPU_Registers.H8, C);

                        int mask = 0x0000000F;
                        int Md = Rdi & mask;
                        int Mr = Rri & mask;
                        C = Mr + Cb > Md;
                        core.setStatusBits(CPU_Registers.H, C);
                        core.setStatusBits(CPU_Registers.H1, C);

                        for (int i = 0; i < 6; i++) {
                            mask = 0x0FFFFFFF >> (i << 2);
                            Md = Rdi & mask;
                            Mr = Rri & mask;
                            core.setStatusBits(CPU_Registers.H7 >> i, Mr + Cb > Md);
                        }
                    }
                    core.programCounter++;
                    return null;
                }

                @Override
                public int getCost(AvrCore core) {
                    return core.getStatusBitsAnd(CPU_Registers.U)?4:1;
                }
            },
            CPI = new Instruction("CPI",true, Rh, K8s) {
                @Override
                public ExecutionEvent execute(AvrCore core) {
                    int Rdi = core.getRegisterValue(core.getOperand0());
                    int Rri = core.getOperand1();
                    long Rdl = Rdi & 0xFFFFFFFFL;
                    long Rrl = Rri & 0xFFFFFFFFL;

                    long R = Rdl - Rrl;

                    boolean C = Rrl > Rdl;
                    core.setStatusBits(CPU_Registers.C, C);
                    core.setStatusBits(CPU_Registers.Z, (int) R == 0);
                    boolean N = (int) R < 0;
                    core.setStatusBits(CPU_Registers.N, N);

                    R = (long) Rdi - Rri;
                    boolean V = R > Integer.MAX_VALUE || R < Integer.MIN_VALUE;
                    core.setStatusBits(CPU_Registers.V, V);
                    core.setStatusBits(CPU_Registers.S, N ^ V);

                    if (core.getStatusBitsAnd(CPU_Registers.U)) {
                        core.setStatusBits(CPU_Registers.H8, C);

                        int mask = 0x0000000F;
                        int Md = Rdi & mask;
                        int Mr = Rri & mask;
                        C = Mr > Md;
                        core.setStatusBits(CPU_Registers.H, C);
                        core.setStatusBits(CPU_Registers.H1, C);

                        for (int i = 0; i < 6; i++) {
                            mask = 0x0FFFFFFF >> (i << 2);
                            Md = Rdi & mask;
                            Mr = Rri & mask;
                            core.setStatusBits(CPU_Registers.H7 >> i, Mr > Md);
                        }
                    }
                    core.programCounter++;
                    return null;
                }

                @Override
                public int getCost(AvrCore core) {
                    return core.getStatusBitsAnd(CPU_Registers.U)?4:1;
                }
            },
            CPSE = new Instruction("CPSE",true, R, R) {
                @Override
                public ExecutionEvent execute(AvrCore core) {
                    if (core.getRegisterValue(core.getOperand0()) == core.getRegisterValue(core.getOperand1())) {
                        core.programCounter += 2;
                    } else {
                        core.programCounter++;
                    }
                    return null;
                }
            },
            CPSNE = new Instruction("CPSNE",false, R, R) {
                @Override
                public ExecutionEvent execute(AvrCore core) {
                    if (core.getRegisterValue(core.getOperand0()) == core.getRegisterValue(core.getOperand1())) {
                        core.programCounter++;
                    } else {
                        core.programCounter += 2;
                    }
                    return null;
                }
            },
            DEC = new Instruction("DEC",true, R) {
                @Override
                public ExecutionEvent execute(AvrCore core) {
                    int Rdi = core.getRegisterValue(core.getOperand0()) - 1;
                    core.setRegisterValue(core.getOperand0(), Rdi);
                    core.setStatusBits(CPU_Registers.Z, Rdi == 0);
                    boolean N = Rdi < 0;
                    core.setStatusBits(CPU_Registers.N, N);
                    boolean V = Rdi == Integer.MAX_VALUE;
                    core.setStatusBits(CPU_Registers.V, V);
                    core.setStatusBits(CPU_Registers.S, N ^ V);
                    core.programCounter++;
                    return null;
                }
            },
            DIVS = new Instruction("DIVS",false, R, R) {
                @Override
                public ExecutionEvent execute(AvrCore core) {
                    long R = (long) core.getRegisterValue(core.getOperand0()) / (long) core.getRegisterValue(core.getOperand1());
                    core.setStatusBits(CPU_Registers.Z, R == 0);
                    core.setStatusBits(CPU_Registers.S | CPU_Registers.N, R < 0);
                    core.clearStatusBits(CPU_Registers.V);
                    core.setRegisterValue(core.getOperand0(),(int)R);
                    core.programCounter++;
                    return null;
                }
            },
            DIV = new Instruction("DIV",false, R, R) {
                @Override
                public ExecutionEvent execute(AvrCore core) {
                    long R = (core.getRegisterValue(core.getOperand0()) & 0xFFFFFFFFL) / (core.getRegisterValue(core.getOperand1()) & 0xFFFFFFFFL);
                    core.setStatusBits(CPU_Registers.Z, R == 0);
                    core.setStatusBits(CPU_Registers.S | CPU_Registers.N, R < 0);//false R<0
                    core.clearStatusBits(CPU_Registers.V);
                    core.setRegisterValue(core.getOperand0(),(int)R);
                    core.programCounter++;
                    return null;
                }
            },
            DIVSU = new Instruction("DIVSU",false, R, R) {
                @Override
                public ExecutionEvent execute(AvrCore core) {
                    long R = (long) core.getRegisterValue(core.getOperand0()) / (core.getRegisterValue(core.getOperand1()) & 0xFFFFFFFFL);
                    core.setStatusBits(CPU_Registers.Z, R == 0);
                    core.setStatusBits(CPU_Registers.S | CPU_Registers.N, R < 0);
                    core.clearStatusBits(CPU_Registers.V);
                    core.setRegisterValue(core.getOperand0(),(int)R);
                    core.programCounter++;
                    return null;
                }
            },
            DIVUS = new Instruction("DIVUS",false, R, R) {
                @Override
                public ExecutionEvent execute(AvrCore core) {
                    long  R = (core.getRegisterValue(core.getOperand0()) & 0xFFFFFFFFL) / (long) core.getRegisterValue(core.getOperand1());
                    core.setStatusBits(CPU_Registers.Z, R == 0);
                    core.setStatusBits(CPU_Registers.S | CPU_Registers.N, R < 0);
                    core.clearStatusBits(CPU_Registers.V);
                    core.setRegisterValue(core.getOperand0(),(int)R);
                    core.programCounter++;
                    return null;
                }
            },
            EOR = new Instruction("EOR",true, R, R) {
                @Override
                public ExecutionEvent execute(AvrCore core) {
                    int R = core.xorRegisterValue(core.getOperand0(), core.getRegisterValue(core.getOperand1()));
                    core.setStatusBits(CPU_Registers.Z, R == 0);
                    core.setStatusBits(CPU_Registers.S | CPU_Registers.N, R < 0);
                    core.clearStatusBits(CPU_Registers.V);

                    core.programCounter++;
                    return null;
                }
            },
            EORI = new Instruction("EORI",false, Rh, K8b) {
                @Override
                public ExecutionEvent execute(AvrCore core) {
                    int R = core.xorRegisterValue(core.getOperand0(), core.getOperand1());
                    core.setStatusBits(CPU_Registers.Z, R == 0);
                    core.setStatusBits(CPU_Registers.S | CPU_Registers.N, R < 0);
                    core.clearStatusBits(CPU_Registers.V);
                    core.programCounter++;
                    return null;
                }
            },
            FMUL = new Instruction("FMUL",true, Rq, Rq) {
                @Override
                public ExecutionEvent execute(AvrCore core) {
                    long R = (core.getRegisterValue(core.getOperand0()) & 0xFFFFFFFFL) *
                            (core.getRegisterValue(core.getOperand1()) & 0xFFFFFFFFL);
                    core.setStatusBits(CPU_Registers.C, (R & Long.MIN_VALUE) != 0);
                    core.setRegisterPairValue(RegisterFileSingles.R0.offset, R <<= 1);
                    core.setStatusBits(CPU_Registers.Z, R == 0);
                    core.programCounter++;
                    return null;
                }
            },
            FMULS = new Instruction("FMULS",true, Rq, Rq) {
                @Override
                public ExecutionEvent execute(AvrCore core) {
                    long R = ((long) (core.getRegisterValue(core.getOperand0()))) *
                            core.getRegisterValue(core.getOperand1());
                    core.setStatusBits(CPU_Registers.C, (R & Long.MIN_VALUE) != 0);
                    core.setRegisterPairValue(RegisterFileSingles.R0.offset, R <<= 1);
                    core.setStatusBits(CPU_Registers.Z, R == 0);
                    core.programCounter++;
                    return null;
                }
            },
            FMULSU = new Instruction("FMULSU",true, Rq, Rq) {
                @Override
                public ExecutionEvent execute(AvrCore core) {
                    long R = ((long) (core.getRegisterValue(core.getOperand0()))) *
                            (core.getRegisterValue(core.getOperand1()) & 0xFFFFFFFFL);
                    core.setStatusBits(CPU_Registers.C, (R & Long.MIN_VALUE) != 0);
                    core.setRegisterPairValue(RegisterFileSingles.R0.offset, R <<= 1);
                    core.setStatusBits(CPU_Registers.Z, R == 0);
                    core.programCounter++;
                    return null;
                }
            },
            ICALL = new Instruction("ICALL",true) {
                @Override
                public ExecutionEvent execute(AvrCore core) {
                    core.pushValue(core.programCounter);
                    core.programCounter = core.getZ();
                    return null;
                }
            },
            IJMP = new Instruction("IJMP",true) {
                @Override
                public ExecutionEvent execute(AvrCore core) {
                    core.programCounter = core.getZ();
                    return null;
                }
            },
            IN = new Instruction("IN",true, R, IO6) {
                @Override
                public ExecutionEvent execute(AvrCore core) {
                    core.setRegisterValue(core.getOperand0(), core.getDataValue(core.getOperand1()));
                    core.programCounter++;
                    return null;
                }
            },
            INC = new Instruction("INC",true, R) {
                @Override
                public ExecutionEvent execute(AvrCore core) {
                    int Rdi = core.getRegisterValue(core.getOperand0()) + 1;
                    core.setRegisterValue(core.getOperand0(), Rdi);
                    core.setStatusBits(CPU_Registers.Z, Rdi == 0);
                    boolean N = Rdi < 0;
                    core.setStatusBits(CPU_Registers.N, N);
                    boolean V = Rdi == Integer.MIN_VALUE;
                    core.setStatusBits(CPU_Registers.V, V);
                    core.setStatusBits(CPU_Registers.S, N ^ V);
                    core.programCounter++;
                    return null;
                }
            },
            JMP = new Instruction("JMP",true, P22) {
                @Override
                public ExecutionEvent execute(AvrCore core) {
                    core.programCounter = core.getOperand0();
                    return null;
                }
            },
            LAC = new Instruction("LAC",true, R) {
                @Override
                public ExecutionEvent execute(AvrCore core) {
                    core.clearDataBits(core.getZ(), core.getOperand0());
                    core.setRegisterValue(core.getOperand0(), core.getDataValue(core.getZ()));
                    core.programCounter++;
                    return null;
                }
            },
            LAS = new Instruction("LAS",true, R) {
                @Override
                public ExecutionEvent execute(AvrCore core) {
                    core.setDataBits(core.getZ(), core.getOperand0());
                    core.setRegisterValue(core.getOperand0(), core.getDataValue(core.getZ()));
                    core.programCounter++;
                    return null;
                }
            },
            LAT = new Instruction("LAT",true, R) {
                @Override
                public ExecutionEvent execute(AvrCore core) {
                    core.xorDataValue(core.getZ(), core.getOperand0());
                    core.setRegisterValue(core.getOperand0(), core.getDataValue(core.getZ()));
                    core.programCounter++;
                    return null;
                }
            },
            LD = new Instruction("LD",true) {
                @Override
                public void compileInstruction(ProgramMemory programMemory, int address, boolean immersive, int[] operandsReturn, String[] values) throws InvalidMnemonic {
                    throw new InvalidMnemonic("This LD is only a dummy!");
                }

                @Override
                public ExecutionEvent execute(AvrCore core) {
                    return new ExecutionEvent(core.programCounter,this,
                            new InvalidMnemonic("This LD is only a dummy! " + core.programCounter));
                }
            },
            LDX = new Instruction("LDX",true, R, K1pointer) {
                @Override
                public ExecutionEvent execute(AvrCore core) {
                    int Xd = core.getOperand1();
                    int X = core.getX();
                    if (Xd == Integer.MIN_VALUE) {
                        core.setX(--X);
                        core.setRegisterValue(core.getOperand0(), core.getDataValue(X));
                    } else {
                        core.setRegisterValue(core.getOperand0(), core.getDataValue(X + Xd));
                    }
                    if (Xd == Integer.MAX_VALUE) {
                        core.setX(X + 1);
                    }
                    core.programCounter++;
                    return null;
                }
            },
            LDY = new Instruction("LDY",true, R, K6pointer) {
                @Override
                public ExecutionEvent execute(AvrCore core) {
                    int Yd = core.getOperand1();
                    int Y = core.getY();
                    if (Yd == Integer.MIN_VALUE) {
                        core.setY(--Y);
                        core.setRegisterValue(core.getOperand0(), core.getDataValue(Y));
                    } else {
                        core.setRegisterValue(core.getOperand0(), core.getDataValue(Y + Yd));
                    }
                    if (Yd == Integer.MAX_VALUE) {
                        core.setY(Y + 1);
                    }
                    core.programCounter++;
                    return null;
                }
            },
            LDZ = new Instruction("LDZ",true, R, K6pointer) {
                @Override
                public ExecutionEvent execute(AvrCore core) {
                    int Zd = core.getOperand1();
                    int Z = core.getZ();
                    if (Zd == Integer.MIN_VALUE) {
                        core.setZ(--Z);
                        core.setRegisterValue(core.getOperand0(), core.getDataValue(Z));
                    } else {
                        core.setRegisterValue(core.getOperand0(), core.getDataValue(Z + Zd));
                    }
                    if (Zd == Integer.MAX_VALUE) {
                        core.setZ(Z + 1);
                    }
                    core.programCounter++;
                    return null;
                }
            },
            LDI = new Instruction("LDI",true, Rh, K8s) {
                @Override
                public ExecutionEvent execute(AvrCore core) {
                    core.setRegisterValue(core.getOperand0(), core.getOperand1());
                    core.programCounter++;
                    return null;
                }
            },
            LDS = new Instruction("LDS",true, R, D16) {
                @Override
                public ExecutionEvent execute(AvrCore core) {
                    core.setRegisterValue(core.getOperand0(), core.getDataValue(core.getOperand1()));
                    core.programCounter++;
                    return null;
                }
            },
            LPM = new Instruction("LPM",true, R) {
                @Override
                public ExecutionEvent execute(AvrCore core) {
                    int Zd = core.getOperand1();
                    int Z = core.getZ();
                    if (Zd < 0) {
                        core.setX(Z += Zd);
                    }
                    core.setRegisterValue(core.getOperand0(), core.getInstructionID(Z));
                    if (Zd > 0) {
                        core.setX(Z + Zd);
                    }
                    core.programCounter++;
                    return null;
                }
            },
            LPO = new Instruction("LPO",true, R) {
                @Override
                public ExecutionEvent execute(AvrCore core) {
                    int Zd = core.getOperand1();
                    int Z = core.getZ();
                    if (Zd < 0) {
                        core.setX(Z += Zd);
                    }
                    core.setRegisterValue(core.getOperand0(), core.getOperand0(Z));
                    if (Zd > 0) {
                        core.setX(Z + Zd);
                    }
                    core.programCounter++;
                    return null;
                }
            },
            LPQ = new Instruction("LPQ",true, R) {
                @Override
                public ExecutionEvent execute(AvrCore core) {
                    int Zd = core.getOperand1();
                    int Z = core.getZ();
                    if (Zd < 0) {
                        core.setX(Z += Zd);
                    }
                    core.setRegisterValue(core.getOperand0(), core.getOperand1(Z));
                    if (Zd > 0) {
                        core.setX(Z + Zd);
                    }
                    core.programCounter++;
                    return null;
                }
            },
            LSL = new Instruction("LSL",true, R) {
                @Override
                public ExecutionEvent execute(AvrCore core) {
                    int R = core.getRegisterValue(core.getOperand0());
                    boolean C = R < 0;
                    core.setStatusBits(CPU_Registers.C, C);//always last bit tho...
                    R <<= 1;
                    core.setRegisterValue(core.getOperand0(), R);
                    core.setStatusBits(CPU_Registers.Z, R == 0);
                    boolean N = R < 0, V = N ^ C;
                    core.setStatusBits(CPU_Registers.N, N);
                    core.setStatusBits(CPU_Registers.V, V);
                    core.setStatusBits(CPU_Registers.S, N ^ V);

                    if (core.getStatusBitsAnd(CPU_Registers.U)) {
                        core.setStatusBits(CPU_Registers.H8, C);
                        core.setStatusBits(CPU_Registers.H7, R >> 27 != 0);
                        core.setStatusBits(CPU_Registers.H6, R >> 23 != 0);
                        core.setStatusBits(CPU_Registers.H5, R >> 19 != 0);
                        core.setStatusBits(CPU_Registers.H4, R >> 15 != 0);
                        core.setStatusBits(CPU_Registers.H3, R >> 11 != 0);
                        core.setStatusBits(CPU_Registers.H2, R >> 7 != 0);
                        C = R >> 3 != 0;
                        core.setStatusBits(CPU_Registers.H1, C);
                        core.setStatusBits(CPU_Registers.H, C);
                    }
                    core.programCounter++;
                    return null;
                }

                @Override
                public int getCost(AvrCore core) {
                    return core.getStatusBitsAnd(CPU_Registers.U)?4:1;
                }
            },
            LSLD = new Instruction("LSLD",false, R, R) {
                @Override
                public ExecutionEvent execute(AvrCore core) {
                    int R = core.getRegisterValue(core.getOperand0());
                    R <<= core.getRegisterValue(core.getOperand1());//for faster divisions hack
                    core.setRegisterValue(core.getOperand0(), R);
                    core.setStatusBits(CPU_Registers.Z, R == 0);
                    core.setStatusBits(CPU_Registers.N, R < 0);
                    core.programCounter++;
                    return null;
                }
            },
            LSLI = new Instruction("LSLI",false, Rh, K8b) {
                @Override
                public ExecutionEvent execute(AvrCore core) {
                    int R = core.getRegisterValue(core.getOperand0());
                    R <<= core.getOperand1();//for faster divisions hack
                    core.setRegisterValue(core.getOperand0(), R);
                    core.setStatusBits(CPU_Registers.Z, R == 0);
                    core.setStatusBits(CPU_Registers.N, R < 0);
                    core.programCounter++;
                    return null;
                }
            },
            LSR = new Instruction("LSR",true, R) {
                @Override
                public ExecutionEvent execute(AvrCore core) {
                    int R = core.getRegisterValue(core.getOperand0());
                    boolean C = (R & 0x1) != 0;
                    core.setStatusBits(CPU_Registers.C, C);//always last bit tho...
                    R >>= 1;
                    core.setRegisterValue(core.getOperand0(), R);
                    core.setStatusBits(CPU_Registers.Z, R == 0);
                    boolean N = R < 0, V = N ^ C;
                    core.setStatusBits(CPU_Registers.N, N);
                    core.setStatusBits(CPU_Registers.V, V);
                    core.setStatusBits(CPU_Registers.S, N ^ V);
                    core.programCounter++;
                    return null;
                }
            },
            LSRD = new Instruction("LSRD",false, R, R) {
                @Override
                public ExecutionEvent execute(AvrCore core) {
                    int R = core.getRegisterValue(core.getOperand0());
                    R >>= core.getRegisterValue(core.getOperand1());//for faster divisions hack
                    core.setRegisterValue(core.getOperand0(), R);
                    core.setStatusBits(CPU_Registers.Z, R == 0);
                    core.setStatusBits(CPU_Registers.N, R < 0);
                    core.programCounter++;
                    return null;
                }
            },
            LSRI = new Instruction("LSRI",false, Rh, K8b) {
                @Override
                public ExecutionEvent execute(AvrCore core) {
                    int R = core.getRegisterValue(core.getOperand0());
                    R >>= core.getOperand1();//for faster divisions hack
                    core.setRegisterValue(core.getOperand0(), R);
                    core.setStatusBits(CPU_Registers.Z, R == 0);
                    core.setStatusBits(CPU_Registers.N, R < 0);
                    core.programCounter++;
                    return null;
                }
            },
            MODS = new Instruction("MODS",false, R, R) {
                @Override
                public ExecutionEvent execute(AvrCore core) {
                    long R = (long) core.getRegisterValue(core.getOperand0()) % (long) core.getRegisterValue(core.getOperand1());
                    core.setStatusBits(CPU_Registers.Z, R == 0);
                    core.setStatusBits(CPU_Registers.S | CPU_Registers.N, R < 0);
                    core.clearStatusBits(CPU_Registers.V);
                    core.setRegisterValue(core.getOperand0(),(int)R);
                    core.programCounter++;
                    return null;
                }
            },
            MODUS = new Instruction("MODUS",false, R, R) {
                @Override
                public ExecutionEvent execute(AvrCore core) {
                    long R = (core.getRegisterValue(core.getOperand0()) & 0xFFFFFFFFL) % core.getRegisterValue(core.getOperand1());
                    core.setStatusBits(CPU_Registers.Z, R == 0);
                    core.clearStatusBits(CPU_Registers.S | CPU_Registers.N | CPU_Registers.V);
                    core.setRegisterValue(core.getOperand0(),(int)R);
                    core.programCounter++;
                    return null;
                }
            },
            MODSU = new Instruction("MODSU",false, R, R) {
                @Override
                public ExecutionEvent execute(AvrCore core) {
                    long R = core.getRegisterValue(core.getOperand0()) % (core.getRegisterValue(core.getOperand1()) & 0xFFFFFFFFL);
                    core.setStatusBits(CPU_Registers.Z, R == 0);
                    core.setStatusBits(CPU_Registers.S | CPU_Registers.N, R < 0);
                    core.clearStatusBits(CPU_Registers.V);
                    core.setRegisterValue(core.getOperand0(),(int)R);
                    core.programCounter++;
                    return null;
                }
            },
            MOD = new Instruction("MOD",false, R, R) {
                @Override
                public ExecutionEvent execute(AvrCore core) {
                    long R = (core.getRegisterValue(core.getOperand0()) & 0xFFFFFFFFL) % (core.getRegisterValue(core.getOperand1()) & 0xFFFFFFFFL);
                    core.setStatusBits(CPU_Registers.Z, R == 0);
                    core.clearStatusBits(CPU_Registers.S | CPU_Registers.N | CPU_Registers.V);
                    core.setRegisterValue(core.getOperand0(),(int)R);
                    core.programCounter++;
                    return null;
                }
            },
            MOV = new Instruction("MOV",true, R, R) {
                @Override
                public ExecutionEvent execute(AvrCore core) {
                    core.setRegisterValue(core.getOperand0(), core.getRegisterValue(core.getOperand1()));
                    core.programCounter++;
                    return null;
                }
            },
            MOVW = new Instruction("MOVW",true, Rpmov, Rpmov) {
                @Override
                public ExecutionEvent execute(AvrCore core) {
                    core.setRegisterValue(core.getOperand0(), core.getRegisterValue(core.getOperand1()));
                    core.setRegisterValue(core.getOperand0()+1, core.getRegisterValue(core.getOperand1()+1));
                    core.programCounter++;
                    return null;
                }
            },
            MUL = new Instruction("MUL",true, R, R) {
                @Override
                public ExecutionEvent execute(AvrCore core) {
                    long R = (core.getRegisterValue(core.getOperand0()) & 0xFFFFFFFFL) *
                            (core.getRegisterValue(core.getOperand1()) & 0xFFFFFFFFL);
                    core.setStatusBits(CPU_Registers.C, (R & Long.MIN_VALUE) != 0);
                    core.setRegisterPairValue(RegisterFileSingles.R0.offset, R);
                    core.setStatusBits(CPU_Registers.Z, R == 0);
                    core.programCounter++;
                    return null;
                }
            },
            MULS = new Instruction("MULS",true, Rh, Rh) {
                @Override
                public ExecutionEvent execute(AvrCore core) {
                    long R = ((long) (core.getRegisterValue(core.getOperand0()))) *
                            core.getRegisterValue(core.getOperand1());
                    core.setStatusBits(CPU_Registers.C, (R & Long.MIN_VALUE) != 0);
                    core.setRegisterPairValue(RegisterFileSingles.R0.offset, R);
                    core.setStatusBits(CPU_Registers.Z, R == 0);
                    core.programCounter++;
                    return null;
                }
            },
            MULSU = new Instruction("MULSU",true, Rq, Rq) {
                @Override
                public ExecutionEvent execute(AvrCore core) {
                    long R = ((long) (core.getRegisterValue(core.getOperand0()))) *
                            (core.getRegisterValue(core.getOperand1()) & 0xFFFFFFFFL);
                    core.setStatusBits(CPU_Registers.C, (R & Long.MIN_VALUE) != 0);
                    core.setRegisterPairValue(RegisterFileSingles.R0.offset, R);
                    core.setStatusBits(CPU_Registers.Z, R == 0);
                    core.programCounter++;
                    return null;
                }
            },
            NEG = new Instruction("NEG",true, R) {
                @Override
                public ExecutionEvent execute(AvrCore core) {
                    int Rdi = core.getRegisterValue(core.getOperand0());
                    int R = core.negRegisterValue(core.getOperand0());
                    core.setStatusBits(CPU_Registers.V, false);
                    boolean C = R != 0;
                    core.setStatusBits(CPU_Registers.Z, !C);
                    core.setStatusBits(CPU_Registers.C, C);
                    boolean N = R < 0;
                    core.setStatusBits(CPU_Registers.N, N);
                    boolean V = R == Integer.MIN_VALUE;
                    core.setStatusBits(CPU_Registers.V, V);
                    core.setStatusBits(CPU_Registers.S, N ^ V);

                    if (core.getStatusBitsAnd(CPU_Registers.U)) {
                        core.setStatusBits(CPU_Registers.H8, C);

                        int mask = 0x0000000F;
                        int Md = Rdi & mask;
                        C = Md > 0;
                        core.setStatusBits(CPU_Registers.H, C);
                        core.setStatusBits(CPU_Registers.H1, C);

                        for (int i = 0; i < 6; i++) {
                            mask = 0x0FFFFFFF >> (i << 2);
                            Md = Rdi & mask;
                            core.setStatusBits(CPU_Registers.H7 >> i, Md > 0);
                        }
                    }
                    core.programCounter++;
                    return null;
                }

                @Override
                public int getCost(AvrCore core) {
                    return core.getStatusBitsAnd(CPU_Registers.U)?4:1;
                }
            },
            NOP = new Instruction("NOP",true) {
                @Override
                public ExecutionEvent execute(AvrCore core) {
                    core.programCounter++;
                    return null;
                }
            },
            NOPT = new Instruction("NOPT",false,R) {//can use sleep and next tick interrupt
                @Override
                public ExecutionEvent execute(AvrCore core) {
                    core.awoken=false;
                    return new ExecutionEvent(core.programCounter++, this,new DelayEvent("NOPT!"), core.getRegisterValue(core.getOperand0()));
                }
            },
            NOPTI = new Instruction("NOPTI",false,K32) {//can use sleep and next tick interrupt
                @Override
                public ExecutionEvent execute(AvrCore core) {
                    core.awoken=false;
                    return new ExecutionEvent(core.programCounter++, this,new DelayEvent("NOPTI!"), core.getOperand0());
                }
            },
            OR = new Instruction("OR",true, R, R) {
                @Override
                public ExecutionEvent execute(AvrCore core) {
                    int R = core.orRegisterValue(core.getOperand0(), core.getRegisterValue(core.getOperand1()));
                    core.setStatusBits(CPU_Registers.Z, R == 0);
                    core.setStatusBits(CPU_Registers.S | CPU_Registers.N, R < 0);
                    core.clearStatusBits(CPU_Registers.V);

                    core.programCounter++;
                    return null;
                }
            },
            ORI = new Instruction("ORI",true, Rh, K8b) {
                @Override
                public ExecutionEvent execute(AvrCore core) {
                    int R = core.orRegisterValue(core.getOperand0(), core.getOperand1());
                    core.setStatusBits(CPU_Registers.Z, R == 0);
                    core.setStatusBits(CPU_Registers.S | CPU_Registers.N, R < 0);
                    core.clearStatusBits(CPU_Registers.V);

                    core.programCounter++;
                    return null;
                }
            },
            OUT = new Instruction("OUT",true, IO6, R) {
                @Override
                public ExecutionEvent execute(AvrCore core) {
                    core.setDataValue(core.getOperand0(), core.getRegisterValue(core.getOperand1()));
                    core.programCounter++;
                    return null;
                }
            },
            POP = new Instruction("POP",true, R) {
                @Override
                public ExecutionEvent execute(AvrCore core) {
                    core.setRegisterValue(core.getOperand0(), core.popValue());
                    core.programCounter++;
                    return null;
                }
            },
            PUSH = new Instruction("PUSH",true, R) {
                @Override
                public ExecutionEvent execute(AvrCore core) {
                    core.pushValue(core.getRegisterValue(core.getOperand0()));
                    core.programCounter++;
                    return null;
                }
            },
            RCALL = new Instruction("RCALL",true, S12) {
                @Override
                public ExecutionEvent execute(AvrCore core) {
                    core.pushValue(core.programCounter);
                    core.programCounter += core.getOperand0();
                    return null;
                }
            },
            RET = new Instruction("RET",true) {
                @Override
                public ExecutionEvent execute(AvrCore core) {
                    core.programCounter = core.popValue() + 1;//next instr
                    return null;
                }
            },
            RETI = new Instruction("RETI",true) {
                @Override
                public ExecutionEvent execute(AvrCore core) {
                    core.setStatusBits(CPU_Registers.I);
                    core.programCounter = core.popValue() + 1;//next instr
                    return null;
                }
            },
            RJMP = new Instruction("RJMP",true, S12) {
                @Override
                public ExecutionEvent execute(AvrCore core) {
                    core.programCounter += core.getOperand0();
                    return null;
                }
            },
            ROL = new Instruction("ROL",true, R) {
                @Override
                public ExecutionEvent execute(AvrCore core) {
                    int R = core.getRegisterValue(core.getOperand0());
                    boolean C = (R & 0x80000000) != 0;
                    boolean C_ = core.getStatusBitsAnd(CPU_Registers.C);
                    R = (R << 1) | (C_ ? 1 : 0);
                    core.setRegisterValue(core.getOperand0(), R);
                    core.setStatusBits(CPU_Registers.C, C);
                    core.setStatusBits(CPU_Registers.Z, R == 0);
                    boolean N = R < 0;
                    core.setStatusBits(CPU_Registers.N, N);
                    boolean V = N ^ C;
                    core.setStatusBits(CPU_Registers.V, V);
                    core.setStatusBits(CPU_Registers.S, N ^ V);

                    if (core.getStatusBitsAnd(CPU_Registers.U)) {
                        core.setStatusBits(CPU_Registers.H8, C);
                        core.setStatusBits(CPU_Registers.H7, R >> 27 != 0);
                        core.setStatusBits(CPU_Registers.H6, R >> 23 != 0);
                        core.setStatusBits(CPU_Registers.H5, R >> 19 != 0);
                        core.setStatusBits(CPU_Registers.H4, R >> 15 != 0);
                        core.setStatusBits(CPU_Registers.H3, R >> 11 != 0);
                        core.setStatusBits(CPU_Registers.H2, R >> 7 != 0);
                        C = R >> 3 != 0;
                        core.setStatusBits(CPU_Registers.H1, C);
                        core.setStatusBits(CPU_Registers.H, C);
                    }
                    core.programCounter++;
                    return null;
                }

                @Override
                public int getCost(AvrCore core) {
                    return core.getStatusBitsAnd(CPU_Registers.U)?4:1;
                }
            },
            ROR = new Instruction("ROR",true, R) {
                @Override
                public ExecutionEvent execute(AvrCore core) {
                    int R = core.getRegisterValue(core.getOperand0());
                    boolean C = (R & 0x1) != 0;
                    boolean C_ = core.getStatusBitsAnd(CPU_Registers.C);
                    R = (R >> 1) | (C_ ? 0x80000000 : 0);
                    core.setRegisterValue(core.getOperand0(), R);
                    core.setStatusBits(CPU_Registers.C, C);
                    core.setStatusBits(CPU_Registers.Z, R == 0);
                    boolean N = R < 0;
                    core.setStatusBits(CPU_Registers.N, N);
                    boolean V = N ^ C;
                    core.setStatusBits(CPU_Registers.V, V);
                    core.setStatusBits(CPU_Registers.S, N ^ V);

                    core.programCounter++;
                    return null;
                }
            },
            SBC = new Instruction("SBC",true, R, R) {
                @Override
                public ExecutionEvent execute(AvrCore core) {
                    int Rdi = core.getRegisterValue(core.getOperand0());
                    int Rri = core.getRegisterValue(core.getOperand1());
                    long Rdl = Rdi & 0xFFFFFFFFL;
                    long Rrl = Rri & 0xFFFFFFFFL;

                    int Cb = core.getStatusBitsAnd(CPU_Registers.C) ? 1 : 0;

                    long R = Rdl - Rrl - Cb;
                    core.setRegisterValue(core.getOperand0(), (int) R);
                    boolean C = Rrl + Cb > Rdl;
                    core.setStatusBits(CPU_Registers.C, C);
                    core.setStatusBits(CPU_Registers.Z, (int) R == 0);
                    boolean N = (int) R < 0;
                    core.setStatusBits(CPU_Registers.N, N);

                    R = (long) Rdi - Rri - Cb;
                    boolean V = R > Integer.MAX_VALUE || R < Integer.MIN_VALUE;
                    core.setStatusBits(CPU_Registers.V, V);
                    core.setStatusBits(CPU_Registers.S, N ^ V);

                    if (core.getStatusBitsAnd(CPU_Registers.U)) {
                        core.setStatusBits(CPU_Registers.H8, C);

                        int mask = 0x0000000F;
                        int Md = Rdi & mask;
                        int Mr = Rri & mask;
                        C = Mr + Cb > Md;
                        core.setStatusBits(CPU_Registers.H, C);
                        core.setStatusBits(CPU_Registers.H1, C);

                        for (int i = 0; i < 6; i++) {
                            mask = 0x0FFFFFFF >> (i << 2);
                            Md = Rdi & mask;
                            Mr = Rri & mask;
                            core.setStatusBits(CPU_Registers.H7 >> i, Mr + Cb > Md);
                        }
                    }
                    core.programCounter++;
                    return null;
                }

                @Override
                public int getCost(AvrCore core) {
                    return core.getStatusBitsAnd(CPU_Registers.U)?4:1;
                }
            },
            SBCI = new Instruction("SBCI",true, Rh, K8s) {
                @Override
                public ExecutionEvent execute(AvrCore core) {
                    int Rdi = core.getRegisterValue(core.getOperand0());
                    int Rri = core.getOperand1();
                    long Rdl = Rdi & 0xFFFFFFFFL;
                    long Rrl = Rri & 0xFFFFFFFFL;

                    int Cb = core.getStatusBitsAnd(CPU_Registers.C) ? 1 : 0;

                    long R = Rdl - Rrl - Cb;
                    core.setRegisterValue(core.getOperand0(), (int) R);
                    boolean C = Rrl + Cb > Rdl;
                    core.setStatusBits(CPU_Registers.C, C);
                    core.setStatusBits(CPU_Registers.Z, (int) R == 0);
                    boolean N = (int) R < 0;
                    core.setStatusBits(CPU_Registers.N, N);

                    R = (long) Rdi - Rri - Cb;
                    boolean V = R > Integer.MAX_VALUE || R < Integer.MIN_VALUE;
                    core.setStatusBits(CPU_Registers.V, V);
                    core.setStatusBits(CPU_Registers.S, N ^ V);

                    if (core.getStatusBitsAnd(CPU_Registers.U)) {
                        core.setStatusBits(CPU_Registers.H8, C);

                        int mask = 0x0000000F;
                        int Md = Rdi & mask;
                        int Mr = Rri & mask;
                        C = Mr + Cb > Md;
                        core.setStatusBits(CPU_Registers.H, C);
                        core.setStatusBits(CPU_Registers.H1, C);

                        for (int i = 0; i < 6; i++) {
                            mask = 0x0FFFFFFF >> (i << 2);
                            Md = Rdi & mask;
                            Mr = Rri & mask;
                            core.setStatusBits(CPU_Registers.H7 >> i, Mr + Cb > Md);
                        }
                    }
                    core.programCounter++;
                    return null;
                }

                @Override
                public int getCost(AvrCore core) {
                    return core.getStatusBitsAnd(CPU_Registers.U)?4:1;
                }
            },
            SBI = new Instruction("SBI",true, IO5, b) {
                @Override
                public ExecutionEvent execute(AvrCore core) {
                    core.setDataBits(core.getOperand0(), core.getOperand1());
                    core.programCounter++;
                    return null;
                }
            },
            SBIC = new Instruction("SBIC",true, IO5, b) {
                @Override
                public ExecutionEvent execute(AvrCore core) {
                    if (core.getDataBitsAnd(core.getOperand0(), core.getOperand1())) {//if all set
                        core.programCounter++;
                    } else {//if not all set
                        core.programCounter += 2;
                    }
                    return null;
                }
            },//if any clear
            SBIS = new Instruction("SBIS",true, IO5, b) {
                @Override
                public ExecutionEvent execute(AvrCore core) {
                    if (core.getDataBitsOr(core.getOperand0(), core.getOperand1())) {//if any set
                        core.programCounter += 2;
                    } else {//if not any set
                        core.programCounter++;
                    }
                    return null;
                }
            },//if any set
            SBICC = new Instruction("SBICC",false, IO5, b) {
                @Override
                public ExecutionEvent execute(AvrCore core) {
                    if (core.getDataBitsOr(core.getOperand0(), core.getOperand1())) {//if any set
                        core.programCounter++;
                    } else {//if not any set
                        core.programCounter += 2;
                    }
                    return null;
                }
            },//if all clear
            SBISS = new Instruction("SBISS",false, IO5, b) {
                @Override
                public ExecutionEvent execute(AvrCore core) {
                    if (core.getDataBitsAnd(core.getOperand0(), core.getOperand1())) {//if all set
                        core.programCounter += 2;
                    } else {//if not all set
                        core.programCounter++;
                    }
                    return null;
                }
            },//if all set
            SBR = new Instruction("SBR",true, Rh, K8b) {
                @Override
                public ExecutionEvent execute(AvrCore core) {
                    core.setRegisterBits(core.getOperand0(), core.getOperand1());
                    core.programCounter++;
                    return null;
                }
            },//ORI....
            SBRC = new Instruction("SBRC",true, R, b) {
                @Override
                public ExecutionEvent execute(AvrCore core) {
                    if (core.getRegisterBitsAnd(core.getOperand0(), core.getOperand1())) {
                        core.programCounter++;
                    } else {
                        core.programCounter += 2;
                    }
                    return null;
                }
            },
            SBRS = new Instruction("SBRS",true, R, b) {
                @Override
                public ExecutionEvent execute(AvrCore core) {
                    if (core.getRegisterBitsOr(core.getOperand0(), core.getOperand1())) {
                        core.programCounter += 2;
                    } else {
                        core.programCounter++;
                    }
                    return null;
                }
            },
            SBRCC = new Instruction("SBRCC",false, R, b) {
                @Override
                public ExecutionEvent execute(AvrCore core) {
                    if (core.getRegisterBitsOr(core.getOperand0(), core.getOperand1())) {
                        core.programCounter++;
                    } else {
                        core.programCounter += 2;
                    }
                    return null;
                }
            },
            SBRSS = new Instruction("SBRSS",false, R, b) {
                @Override
                public ExecutionEvent execute(AvrCore core) {
                    if (core.getRegisterBitsAnd(core.getOperand0(), core.getOperand1())) {
                        core.programCounter += 2;
                    } else {
                        core.programCounter++;
                    }
                    return null;
                }
            },
            SEC = new Instruction("SEC",true) {
                @Override
                public ExecutionEvent execute(AvrCore core) {
                    core.setStatusBits(CPU_Registers.C);
                    core.programCounter++;
                    return null;
                }
            },
            SEH = new Instruction("SEH",true) {
                @Override
                public ExecutionEvent execute(AvrCore core) {
                    core.setStatusBits(CPU_Registers.H);
                    core.programCounter++;
                    return null;
                }
            },
            SEI = new Instruction("SEI",true) {
                @Override
                public ExecutionEvent execute(AvrCore core) {
                    core.setStatusBits(CPU_Registers.I);
                    core.programCounter++;
                    return null;
                }
            },
            SEN = new Instruction("SEN",true) {
                @Override
                public ExecutionEvent execute(AvrCore core) {
                    core.setStatusBits(CPU_Registers.N);
                    core.programCounter++;
                    return null;
                }
            },
            SER = new Instruction("SER",true, Rh) {
                @Override
                public ExecutionEvent execute(AvrCore core) {
                    core.setRegisterValue(core.getOperand0(), 0xFFFFFFFF);
                    core.programCounter++;
                    return null;
                }
            },
            SES = new Instruction("SES",true) {
                @Override
                public ExecutionEvent execute(AvrCore core) {
                    core.setStatusBits(CPU_Registers.S);
                    core.programCounter++;
                    return null;
                }
            },
            SET = new Instruction("SET",true) {
                @Override
                public ExecutionEvent execute(AvrCore core) {
                    core.setStatusBits(CPU_Registers.T);
                    core.programCounter++;
                    return null;
                }
            },
            SEV = new Instruction("SEV",true) {
                @Override
                public ExecutionEvent execute(AvrCore core) {
                    core.setStatusBits(CPU_Registers.V);
                    core.programCounter++;
                    return null;
                }
            },
            SEZ = new Instruction("SEZ",true) {
                @Override
                public ExecutionEvent execute(AvrCore core) {
                    core.setStatusBits(CPU_Registers.Z);
                    core.programCounter++;
                    return null;
                }
            },
            SLEEP = new Instruction("SLEEP",true) {
                public ExecutionEvent execute(AvrCore core) {
                    core.awoken =false;
                    core.programCounter++;
                    return null;
                }
            },
            SPM = new Instruction("SPM",true) {
                @Override
                public void compileInstruction(ProgramMemory programMemory, int address, boolean immersive, int[] operandsReturn, String[] values) throws InvalidMnemonic{
                    throw new InvalidMnemonic("This SPM is only a dummy!");
                }

                public ExecutionEvent execute(AvrCore core) {
                    return new ExecutionEvent(core.programCounter,this,
                            new InvalidMnemonic("This SPM is only a dummy! " + (core.programCounter++)));
                }
            },
            ST = new Instruction("ST",true) {
                @Override
                public void compileInstruction(ProgramMemory programMemory, int address, boolean immersive, int[] operandsReturn, String[] values)  throws InvalidMnemonic{
                    throw new InvalidMnemonic("This ST is only a dummy!");
                }

                @Override
                public ExecutionEvent execute(AvrCore core) {
                    return new ExecutionEvent(core.programCounter,this,
                            new InvalidMnemonic("This ST is only a dummy! " + (core.programCounter++)));
                }
            },
            STX = new Instruction("STX",true, K1pointer, R) {
                @Override
                public ExecutionEvent execute(AvrCore core) {
                    int Xd = core.getOperand0();
                    int X = core.getX();
                    if (Xd == Integer.MIN_VALUE) {
                        core.setX(--X);
                        core.setDataValue(X, core.getRegisterValue(core.getOperand1()));
                    } else {
                        core.setDataValue(X + Xd, core.getRegisterValue(core.getOperand1()));
                    }
                    if (Xd == Integer.MAX_VALUE) {
                        core.setX(X + 1);
                    }
                    core.programCounter++;
                    return null;
                }
            },
            STY = new Instruction("STY",true, K6pointer, R) {
                @Override
                public ExecutionEvent execute(AvrCore core) {
                    int Yd = core.getOperand0();
                    int Y = core.getY();
                    if (Yd == Integer.MIN_VALUE) {
                        core.setY(--Y);
                        core.setDataValue(Y, core.getRegisterValue(core.getOperand1()));
                    } else {
                        core.setDataValue(Y + Yd, core.getRegisterValue(core.getOperand1()));
                    }
                    if (Yd == Integer.MAX_VALUE) {
                        core.setY(Y + 1);
                    }
                    core.programCounter++;
                    return null;
                }
            },
            STZ = new Instruction("STZ",true, K6pointer, R) {
                @Override
                public ExecutionEvent execute(AvrCore core) {
                    int Zd = core.getOperand0();
                    int Z = core.getZ();
                    if (Zd == Integer.MIN_VALUE) {
                        core.setZ(--Z);
                        core.setDataValue(Z, core.getRegisterValue(core.getOperand1()));
                    } else {
                        core.setDataValue(Z + Zd, core.getRegisterValue(core.getOperand1()));
                    }
                    if (Zd == Integer.MAX_VALUE) {
                        core.setZ(Z + 1);
                    }
                    core.programCounter++;
                    return null;
                }
            },
            STS = new Instruction("STS",true, D16, R) {
                public ExecutionEvent execute(AvrCore core) {
                    core.setDataValue(core.getOperand0(), core.getRegisterValue(core.getOperand1()));
                    core.programCounter++;
                    return null;
                }
            },
            SUB = new Instruction("SUB",true, R, R) {
                @Override
                public ExecutionEvent execute(AvrCore core) {
                    int Rdi = core.getRegisterValue(core.getOperand0());
                    int Rri = core.getRegisterValue(core.getOperand1());
                    long Rdl = Rdi & 0xFFFFFFFFL;
                    long Rrl = Rri & 0xFFFFFFFFL;

                    long R = Rdl - Rrl;
                    core.setRegisterValue(core.getOperand0(), (int) R);
                    boolean C = Rrl > Rdl;
                    core.setStatusBits(CPU_Registers.C, C);
                    core.setStatusBits(CPU_Registers.Z, (int) R == 0);
                    boolean N = (int) R < 0;
                    core.setStatusBits(CPU_Registers.N, N);

                    R = (long) Rdi - Rri;
                    boolean V = R > Integer.MAX_VALUE || R < Integer.MIN_VALUE;
                    core.setStatusBits(CPU_Registers.V, V);
                    core.setStatusBits(CPU_Registers.S, N ^ V);

                    if (core.getStatusBitsAnd(CPU_Registers.U)) {
                        core.setStatusBits(CPU_Registers.H8, C);

                        int mask = 0x0000000F;
                        int Md = Rdi & mask;
                        int Mr = Rri & mask;
                        C = Mr > Md;
                        core.setStatusBits(CPU_Registers.H, C);
                        core.setStatusBits(CPU_Registers.H1, C);

                        for (int i = 0; i < 6; i++) {
                            mask = 0x0FFFFFFF >> (i << 2);
                            Md = Rdi & mask;
                            Mr = Rri & mask;
                            core.setStatusBits(CPU_Registers.H7 >> i, Mr > Md);
                        }
                    }
                    core.programCounter++;
                    return null;
                }

                @Override
                public int getCost(AvrCore core) {
                    return core.getStatusBitsAnd(CPU_Registers.U)?4:1;
                }
            },
            SUBI = new Instruction("SUBI",true, Rh, K8s) {
                @Override
                public ExecutionEvent execute(AvrCore core) {
                    int Rdi = core.getRegisterValue(core.getOperand0());
                    int Rri = core.getOperand1();
                    long Rdl = Rdi & 0xFFFFFFFFL;
                    long Rrl = Rri & 0xFFFFFFFFL;

                    long R = Rdl - Rrl;
                    core.setRegisterValue(core.getOperand0(), (int) R);
                    boolean C = Rrl > Rdl;
                    core.setStatusBits(CPU_Registers.C, C);
                    core.setStatusBits(CPU_Registers.Z, (int) R == 0);
                    boolean N = (int) R < 0;
                    core.setStatusBits(CPU_Registers.N, N);

                    R = (long) Rdi - Rri;
                    boolean V = R > Integer.MAX_VALUE || R < Integer.MIN_VALUE;
                    core.setStatusBits(CPU_Registers.V, V);
                    core.setStatusBits(CPU_Registers.S, N ^ V);

                    if (core.getStatusBitsAnd(CPU_Registers.U)) {
                        core.setStatusBits(CPU_Registers.H8, C);

                        int mask = 0x0000000F;
                        int Md = Rdi & mask;
                        int Mr = Rri & mask;
                        C = Mr > Md;
                        core.setStatusBits(CPU_Registers.H, C);
                        core.setStatusBits(CPU_Registers.H1, C);

                        for (int i = 0; i < 6; i++) {
                            mask = 0x0FFFFFFF >> (i << 2);
                            Md = Rdi & mask;
                            Mr = Rri & mask;
                            core.setStatusBits(CPU_Registers.H7 >> i, Mr > Md);
                        }
                    }
                    core.programCounter++;
                    return null;
                }

                @Override
                public int getCost(AvrCore core) {
                    return core.getStatusBitsAnd(CPU_Registers.U)?4:1;
                }
            },
    //SBIW = new Instruction("SBIW",true, Rp, K6) {
    //    @Override
    //    public ExecutionEvent execute(AvrCore core) {//todo
    //        int Rdi = core.getRegisterValue(core.getOperand0());
    //        int Rri = core.getOperand1();
    //        long Rdl = Rdi & 0xFFFFFFFFL;
    //        long Rrl = Rri & 0xFFFFFFFFL;
    //
    //        long R = Rdl - Rrl;
    //        core.setRegisterValue(core.getOperand0(), (int) R);
    //        boolean C = Rrl > Rdl;
    //        core.setStatusBits(CPU_Registers.C, C);
    //        core.setStatusBits(CPU_Registers.Z, (int) R == 0);
    //        boolean N = (int) R < 0;
    //        core.setStatusBits(CPU_Registers.N, N);
    //
    //        R = (long) Rdi - Rri;
    //        boolean V = R > Integer.MAX_VALUE || R < Integer.MIN_VALUE;
    //        core.setStatusBits(CPU_Registers.V, V);
    //        core.setStatusBits(CPU_Registers.S, N ^ V);
    //
    //        if (core.getStatusBitsAnd(CPU_Registers.U)) {
    //            core.setStatusBits(CPU_Registers.H8, C);
    //
    //            int mask = 0x0000000F;
    //            int Md = Rdi & mask;
    //            int Mr = Rri & mask;
    //            C = Mr > Md;
    //            core.setStatusBits(CPU_Registers.H, C);
    //            core.setStatusBits(CPU_Registers.H1, C);
    //
    //            for (int i = 0; i < 6; i++) {
    //                mask = 0x0FFFFFFF >> (i << 2);
    //                Md = Rdi & mask;
    //                Mr = Rri & mask;
    //                core.setStatusBits(CPU_Registers.H7 >> i, Mr > Md);
    //            }
    //        }
    //        core.programCounter++;
    //        return null;
    //    }
    //},
    SWAP = new Instruction("SWAP",true, R) {
        @Override
        public ExecutionEvent execute(AvrCore core) {
            int R = core.getRegisterValue(core.getOperand0());
            int R_ = R >> 4;
            R <<= 4;
            R = (R & 0xf0f0f0f0) | (R_ & 0x0f0f0f0f);
            core.setRegisterValue(core.getOperand0(), R);
            core.programCounter++;
            return null;
        }
    },
            SWAPP = new Instruction("SWAPP",false, R) {
                @Override
                public ExecutionEvent execute(AvrCore core) {
                    int R = core.getRegisterValue(core.getOperand0());
                    int R_ = R >> 2;
                    R <<= 2;
                    R = (R & 0xcccccccc) | (R_ & 0x033333333);
                    core.setRegisterValue(core.getOperand0(), R);
                    core.programCounter++;
                    return null;
                }
            },
            SWAPB = new Instruction("SWAPB",false, R) {
                @Override
                public ExecutionEvent execute(AvrCore core) {
                    int R = core.getRegisterValue(core.getOperand0());
                    int R_ = R >> 8;
                    R <<= 8;
                    R = (R & 0xff00ff00) | (R_ & 0x00ff00ff);
                    core.setRegisterValue(core.getOperand0(), R);
                    core.programCounter++;
                    return null;
                }
            },
            SWAPS = new Instruction("SWAPS",false, R) {
                @Override
                public ExecutionEvent execute(AvrCore core) {
                    int R = core.getRegisterValue(core.getOperand0());
                    R = (R >>> 16) | (R << 16);
                    core.setRegisterValue(core.getOperand0(), R);
                    core.programCounter++;
                    return null;
                }
            },
            TST = new Instruction("TST",true, R) {
                @Override
                public ExecutionEvent execute(AvrCore core) {
                    int R = core.getRegisterValue(core.getOperand0());
                    core.setStatusBits(CPU_Registers.Z, R == 0);
                    core.setStatusBits(CPU_Registers.S | CPU_Registers.N, R < 0);
                    core.clearStatusBits(CPU_Registers.V);
                    core.programCounter++;
                    return null;
                }
            },
            WDR = new Instruction("WDR",true) {
                @Override
                public ExecutionEvent execute(AvrCore core) {
                    return new ExecutionEvent(core.programCounter++,this,new WatchdogEvent("WDR!"));
                }
            },
            XCH = new Instruction("XCH",true,Z, R) {
                @Override
                public ExecutionEvent execute(AvrCore core) {
                    int T = core.getRegisterValue(core.getOperand1());
                    core.setRegisterValue(core.getOperand1(), core.getDataValue(core.getZ()));
                    core.setDataValue(core.getZ(), T);
                    core.programCounter++;
                    return null;
                }
            },
            FPADD = new Instruction("FPADD",false, Rq, Rq) {
                @Override
                public ExecutionEvent execute(AvrCore core) {
                    float Rd = Float.intBitsToFloat(core.getRegisterValue(core.getOperand0()));
                    float Rr = Float.intBitsToFloat(core.getRegisterValue(core.getOperand1()));
                    Rd += Rr;
                    core.setStatusBits(CPU_Registers.Z, Rd == 0);
                    core.setStatusBits(CPU_Registers.N | CPU_Registers.S, Rd < 0);
                    core.setRegisterValue(core.getOperand0(), Float.floatToIntBits(Rd));
                    core.programCounter++;
                    return null;
                }
            },
            FPSUB = new Instruction("FPSUB",false, Rq, Rq) {
                @Override
                public ExecutionEvent execute(AvrCore core) {
                    float Rd = Float.intBitsToFloat(core.getRegisterValue(core.getOperand0()));
                    float Rr = Float.intBitsToFloat(core.getRegisterValue(core.getOperand1()));
                    Rd -= Rr;
                    core.setStatusBits(CPU_Registers.Z, Rd == 0);
                    core.setStatusBits(CPU_Registers.N | CPU_Registers.S, Rd < 0);
                    core.setRegisterValue(core.getOperand0(), Float.floatToIntBits(Rd));
                    core.programCounter++;
                    return null;
                }
            },
            FPMUL = new Instruction("FPMUL",false, Rq, Rq) {
                @Override
                public ExecutionEvent execute(AvrCore core) {
                    float Rd = Float.intBitsToFloat(core.getRegisterValue(core.getOperand0()));
                    float Rr = Float.intBitsToFloat(core.getRegisterValue(core.getOperand1()));
                    Rd *= Rr;
                    core.setStatusBits(CPU_Registers.Z, Rd == 0);
                    core.setStatusBits(CPU_Registers.N | CPU_Registers.S, Rd < 0);
                    core.setRegisterValue(core.getOperand0(), Float.floatToIntBits(Rd));
                    core.programCounter++;
                    return null;
                }
            },
            FPDIV = new Instruction("FPDIV",false, Rq, Rq) {
                @Override
                public ExecutionEvent execute(AvrCore core) {
                    float Rd = Float.intBitsToFloat(core.getRegisterValue(core.getOperand0()));
                    float Rr = Float.intBitsToFloat(core.getRegisterValue(core.getOperand1()));
                    Rd /= Rr;
                    core.setStatusBits(CPU_Registers.Z, Rd == 0);
                    core.setStatusBits(CPU_Registers.N | CPU_Registers.S, Rd < 0);
                    core.setRegisterValue(core.getOperand0(), Float.floatToIntBits(Rd));
                    core.programCounter++;
                    return null;
                }
            },
            FPMOD = new Instruction("FPMOD",false, Rq, Rq) {
                @Override
                public ExecutionEvent execute(AvrCore core) {
                    float Rd = Float.intBitsToFloat(core.getRegisterValue(core.getOperand0()));
                    float Rr = Float.intBitsToFloat(core.getRegisterValue(core.getOperand1()));
                    Rd %= Rr;
                    core.setStatusBits(CPU_Registers.Z, Rd == 0);
                    core.setStatusBits(CPU_Registers.N | CPU_Registers.S, Rd < 0);
                    core.setRegisterValue(core.getOperand0(), Float.floatToIntBits(Rd));
                    core.programCounter++;
                    return null;
                }
            },
            FPCP = new Instruction("FPCP",false, Rq, Rq) {
                @Override
                public ExecutionEvent execute(AvrCore core) {
                    double Rd = Float.intBitsToFloat(core.getRegisterValue(core.getOperand0()));
                    float Rr = Float.intBitsToFloat(core.getRegisterValue(core.getOperand1()));
                    Rd -= Rr;
                    core.setStatusBits(CPU_Registers.Z, Rd == 0);
                    core.setStatusBits(CPU_Registers.N | CPU_Registers.S, Rd < 0);
                    core.programCounter++;
                    return null;
                }
            },
            FPCPI = new Instruction("FPCPI",false, Rq, K32) {
                @Override
                public ExecutionEvent execute(AvrCore core) {
                    double Rd = Float.intBitsToFloat(core.getRegisterValue(core.getOperand0()));
                    float Rr = Float.intBitsToFloat(core.getOperand1());
                    Rd -= Rr;
                    core.setStatusBits(CPU_Registers.Z, Rd == 0);
                    core.setStatusBits(CPU_Registers.N | CPU_Registers.S, Rd < 0);
                    core.programCounter++;
                    return null;
                }
            },
            FPCPC = new Instruction("FPCPC",false, Rq, Rq) {
                @Override
                public ExecutionEvent execute(AvrCore core) {
                    double Rd = Float.intBitsToFloat(core.getRegisterValue(core.getOperand0()));
                    float Rr = Float.intBitsToFloat(core.getRegisterValue(core.getOperand1()));
                    Rd = Rd - Rr - (core.getStatusBitsAnd(CPU_Registers.C) ? 1 : 0);
                    core.setStatusBits(CPU_Registers.Z, Rd == 0);
                    core.setStatusBits(CPU_Registers.N | CPU_Registers.S, Rd < 0);
                    core.programCounter++;
                    return null;
                }
            },
            FPCPCI = new Instruction("FPCPCI",false, Rq, K32) {
                @Override
                public ExecutionEvent execute(AvrCore core) {
                    double Rd = Float.intBitsToFloat(core.getRegisterValue(core.getOperand0()));
                    float Rr = Float.intBitsToFloat(core.getOperand1());
                    Rd = Rd - Rr - (core.getStatusBitsAnd(CPU_Registers.C) ? 1 : 0);
                    core.setStatusBits(CPU_Registers.Z, Rd == 0);
                    core.setStatusBits(CPU_Registers.N | CPU_Registers.S, Rd < 0);
                    core.programCounter++;
                    return null;
                }
            },
            FPINT = new Instruction("FPINT",false, Rq) {
                @Override
                public ExecutionEvent execute(AvrCore core) {
                    core.setRegisterValue(core.getOperand1(), (int) Float.intBitsToFloat(core.getRegisterValue(core.getOperand0())));
                    core.programCounter++;
                    return null;
                }
            },
            INTFP = new Instruction("INTFP",false, Rq) {
                @Override
                public ExecutionEvent execute(AvrCore core) {
                    core.setRegisterValue(core.getOperand1(), Float.floatToIntBits(core.getRegisterValue(core.getOperand0())));
                    core.programCounter++;
                    return null;
                }
            },
            COPYD = new Instruction("COPYD",false) {
                @Override
                public ExecutionEvent execute(AvrCore core) {
                    if(core.isDataBlockValid(core.getX(),core.getY()) && core.isDataBlockValid(core.getZ(),core.getY())) {
                        System.arraycopy(core.dataMemory, core.getX(), core.dataMemory, core.getZ(), core.getY());
                    }
                    core.programCounter++;
                    return null;
                }

                @Override
                public int getCost(AvrCore core) {
                    return 16;
                }
            },
            COPYP = new Instruction("COPYP",false) {
                @Override
                public ExecutionEvent execute(AvrCore core) {
                    if (core.isDataBlockValid(core.getZ(), core.getY())) {
                        System.arraycopy(core.getProgramMemory().instructions, core.getX(), core.dataMemory, core.getZ(), core.getY());
                    }
                    core.programCounter++;
                    return null;
                }

                @Override
                public int getCost(AvrCore core) {
                    return 16;
                }
            },
            COPYO = new Instruction("COPYO",false) {
                @Override
                public ExecutionEvent execute(AvrCore core) {
                    if(core.isDataBlockValid(core.getZ(),core.getY())) {
                        System.arraycopy(core.getProgramMemory().param0, core.getX(), core.dataMemory, core.getZ(), core.getY());
                    }
                    core.programCounter++;
                    return null;
                }

                @Override
                public int getCost(AvrCore core) {
                    return 16;
                }
            },
            COPYQ = new Instruction("COPYQ",false) {
                @Override
                public ExecutionEvent execute(AvrCore core) {
                    if (core.isDataBlockValid(core.getZ(), core.getY())) {
                        System.arraycopy(core.getProgramMemory().param1, core.getX(), core.dataMemory, core.getZ(), core.getY());
                    }
                    core.programCounter++;
                    return null;
                }

                @Override
                public int getCost(AvrCore core) {
                    return 16;
                }
            },
            CLEAR = new Instruction("CLEAR",false) {
                @Override
                public ExecutionEvent execute(AvrCore core) {
                    if (core.isDataBlockValid(core.getZ(), core.getY())) {
                        System.arraycopy(new int[core.getOperand1()], 0, core.dataMemory, core.getOperand0(), core.getOperand1());
                    }
                    core.programCounter++;
                    return null;
                }

                @Override
                public int getCost(AvrCore core) {
                    return 8;
                }
            },
            RAND = new Instruction("RAND",false, Rq, Rq) {
                @Override
                public ExecutionEvent execute(AvrCore core) {
                    int bound = core.getOperand1();
                    if (bound > 0) {
                        core.setRegisterValue(core.getOperand0(), random.nextInt(bound));
                    } else if (bound == 0) {
                        core.setRegisterValue(core.getOperand0(), random.nextInt());
                    } else {
                        core.setRegisterValue(core.getOperand0(), Float.floatToIntBits((float) random.nextGaussian()));
                    }
                    core.programCounter++;
                    return null;
                }

                @Override
                public int getCost(AvrCore core) {
                    return 4;
                }
            },
            RANDI = new Instruction("RANDI",false, Rq, K32) {
                @Override
                public ExecutionEvent execute(AvrCore core) {
                    int bound = core.getRegisterValue(core.getOperand1());
                    if (bound > 0) {
                        core.setRegisterValue(core.getOperand0(), random.nextInt(bound));
                    } else if (bound == 0) {
                        core.setRegisterValue(core.getOperand0(), random.nextInt());
                    } else {
                        core.setRegisterValue(core.getOperand0(), Float.floatToIntBits(random.nextFloat()));
                    }
                    core.programCounter++;
                    return null;
                }

                @Override
                public int getCost(AvrCore core) {
                    return 4;
                }
            },
            WRITE = new Instruction("WRITE",false) {
                @Override
                public ExecutionEvent execute(AvrCore core) {
                    return new ExecutionEvent(core.programCounter++,this,new WriteEvent("WRITE!"));
                }

                @Override
                public int getCost(AvrCore core) {
                    return 32;
                }
            },
            READ = new Instruction("READ",false) {
                @Override
                public ExecutionEvent execute(AvrCore core) {
                    return new ExecutionEvent(core.programCounter++, this,new ReadEvent("READ!"));
                }

                @Override
                public int getCost(AvrCore core) {
                    return 32;
                }
            },
            DES = new Instruction("DES",false) {
                private final jpsam3hklam9.des.DES des=new DES();

                @Override
                public ExecutionEvent execute(AvrCore core) {
                    core.setRegisterPairValue(core.getOperand0(), des.cipher(
                            core.getRegisterPairValue(0),
                            core.getRegisterPairValue(8),
                            core.getStatusBitsNotAnd(CPU_Registers.H)));
                    core.programCounter++;
                    return null;
                }
            },
            DESR = new Instruction("DESR",false,Rpair,Rpair) {
                private final jpsam3hklam9.des.DES des=new DES();

                @Override
                public ExecutionEvent execute(AvrCore core) {
                    core.setRegisterPairValue(core.getOperand0(), des.cipher(
                            core.getRegisterPairValue(core.getOperand0()),
                            core.getRegisterPairValue(core.getOperand1()),
                            core.getStatusBitsNotAnd(CPU_Registers.H)));
                    core.programCounter++;
                    return null;
                }
            };

    private final String name;
    private final OperandLimit limit0, limit1;
    private final boolean immersive;
    private final int operandCount;

    protected Instruction(String name, boolean immersive) {
        this(name,immersive, null, null);
    }

    protected Instruction(String name, boolean immersive, OperandLimit operand0) {
        this(name,immersive, operand0, null);
    }

    protected Instruction(String name, boolean immersive, OperandLimit operand0, OperandLimit operand1) {
        limit0 = operand0;
        limit1 = operand1;
        this.name = name;
        this.immersive=immersive;
        if (isImmersive()) {
            INSTRUCTIONS_IMMERSIVE.add(this);
        }
        INSTRUCTIONS_OP.add(this);
        operandCount=limit0 == null ? 0 : (limit1 == null ? 1 : 2);
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public boolean isImmersive() {
        return immersive;
    }


    /**
     * Default compileInstruction implementation. works well for 2x Register number
     *
     * @param programMemory  target prog mem
     * @param address           current prog address
     * @param immersive      be realistic
     * @param operandsReturn put data0 and data1 here
     * @param values         OPNAME DATA0 DATA1
     * @return is instruction valid
     */
    @Override
    public void compileInstruction(ProgramMemory programMemory, int address, boolean immersive, int[] operandsReturn, String[] values) throws ProgramException {
        Number temp=0;
        InvalidOperand0 err0=null;
        if (values.length >0) {
            if(limit0 == null){
                err0 = new InvalidOperand0("Does not require operand 0");
            }
            if(err0==null) {
                try {
                    temp=ProgramCompiler.parseNumberAdvanced(values[0]);
                    if(limit0.isRelativePreffered()){
                        temp=temp.doubleValue()-address;
                    }
                    if(limit0.isFloatingPointPreffered()){
                        temp=Float.floatToIntBits(temp.floatValue());
                    }
                } catch (Exception e) {
                    err0 = new InvalidOperand0("Cannot Parse " + values[0]);
                }
                if (err0 == null) {
                    operandsReturn[0] = limit0.clamp(temp.intValue(), address, immersive);
                    if (temp.intValue() != operandsReturn[0]) {
                        err0 = new InvalidOperand0("Out of range " + temp + " clamped to: " + operandsReturn[0]);
                    }
                }
            }
        }
        if (values.length > 1) {
            if(limit1 == null){
                if(err0==null){
                    throw new InvalidOperand1("Instruction " +name+ " At line "+address+" Does not require operand 1");
                }else {
                    throw new InvalidOperands("Instruction " +name+ " At line "+address+"    OP0: "+err0.getMessage()+"    OP1: Does not require operand 1");
                }
            }
            try {
                temp=ProgramCompiler.parseNumberAdvanced(values[1]);
                if(limit1.isRelativePreffered()){
                    temp=temp.doubleValue()-address;
                }
                if(limit1.isFloatingPointPreffered()){
                    temp=Float.floatToIntBits(temp.floatValue());
                }
            }catch (Exception e){
                if(err0==null){
                    throw new InvalidOperand1("Instruction " +name+ " At line "+address+" Cannot Parse "+values[1]);
                }else {
                    throw new InvalidOperands("Instruction " +name+ " At line "+address+"    OP0: "+err0.getMessage()+"    OP1: Cannot Parse "+values[1]);
                }
            }
            operandsReturn[1] = limit1.clamp(temp.intValue(), address, immersive);
            if(temp.intValue()!=operandsReturn[1]){
                if(err0==null){
                    throw new InvalidOperand1("Instruction " +name+ " At line "+address+" Out of range "+temp+" clamped to: "+operandsReturn[1]);
                }else {
                    throw new InvalidOperands("Instruction " +name+ " At line "+address+"    OP0: "+err0.getMessage()+"    OP1: Out of range "+temp+" clamped to: "+operandsReturn[1]);
                }
            }
        }
        if(err0!=null){
            throw new InvalidOperand0("Instruction " +name+ " At line "+address+' '+err0.getMessage());
        }
    }

    @Override
    public int getOperandCount() {
        return operandCount;
    }

    @Override
    public OperandLimit getLimit0() {
        return limit0;
    }

    @Override
    public OperandLimit getLimit1() {
        return limit1;
    }

    @Override
    public String getNotes() {
        return "";
    }
}
