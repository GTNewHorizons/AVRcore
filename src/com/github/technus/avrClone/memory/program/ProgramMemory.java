package com.github.technus.avrClone.memory.program;

import com.github.technus.avrClone.AvrCore;
import com.github.technus.avrClone.instructions.I_Instruction;
import com.github.technus.avrClone.instructions.InstructionRegistry;

import java.io.PrintStream;
import java.util.HashMap;

public class ProgramMemory implements Cloneable{
    public final int[] instructions, param0, param1;
    public final InstructionRegistry registry;

    public ProgramMemory(AvrCore core, String... lines) throws Exception{
        this.registry=core.getInstructionRegistry();
        int size=countPC(lines);
        instructions=new int[size];
        param0 =new int[size];
        param1 =new int[size];

        compile(core, core.isUsingImmersiveOperands(), lines);
    }

    public ProgramMemory(AvrCore core, int size) {
        this.registry=core.getInstructionRegistry();
        instructions=new int[size];
        param0 =new int[size];
        param1 =new int[size];
    }

    private ProgramMemory(InstructionRegistry registry,int size){
        this.registry=registry;
        instructions=new int[size];
        param0 =new int[size];
        param1 =new int[size];
    }

    public static int countPC(String... lines){
        int pc=0;
        for(String s:lines){
            s=s.replaceAll("[\\n\\r]","").replaceAll("\\s*;.*$","").replaceAll("^.*:\\s*","").replaceAll("^\\s*","");
            if(
                    s.startsWith(".") ||
                    s.startsWith(";") ||
                    s.length()==0){
                continue;
            }
            pc++;
        }
        return pc;
    }

    public void print(PrintStream printStream){
        for(int i=0;i<instructions.length;i++){
            I_Instruction instruction= registry.getInstruction(instructions[i]);
            switch (instruction.getOperandCount()){
                case 0:
                    printStream.println(i+" : " +instruction.name());
                    break;
                case 1:
                    printStream.println(i+" : " +instruction.name()+" "+param0[i]);
                    break;
                case 2:
                    printStream.println(i+" : " +instruction.name()+" "+param0[i]+","+ param1[i]);
                    break;
            }
        }
    }

    public String getProgram(int radix){
        String prefix="";
        switch (radix){
            case 2:prefix="0b";break;
            case 8:prefix="0";break;
            case 16:prefix="0x";break;
        }
        StringBuilder stringBuilder=new StringBuilder();
        for(int i=0;i<instructions.length;i++){
            I_Instruction instruction= registry.getInstruction(instructions[i]);
            switch (instruction.getOperandCount()){
                case 0:
                    stringBuilder.append(instruction.name());
                    break;
                case 1:
                    stringBuilder.append(instruction.name()).append(' ');
                    stringBuilder.append(prefix).append(Integer.toString(param0[i],radix));
                    break;
                case 2:
                    stringBuilder.append(instruction.name()).append(' ');
                    stringBuilder.append(prefix).append(Integer.toString(param0[i],radix)).append(',');
                    stringBuilder.append(prefix).append(Integer.toString(param1[i],radix));
                    break;
            }
            stringBuilder.append('\n');
        }
        return stringBuilder.toString();
    }

    public String getProgramWithLineNumbers(int radix,int radixLines){
        String prefix="";
        switch (radix){
            case 2:prefix="0b";break;
            case 8:prefix="0";break;
            case 16:prefix="0x";break;
        }
        String prefixLines="";
        switch (radixLines){
            case 2:prefixLines="0b";break;
            case 8:prefixLines="0";break;
            case 16:prefixLines="0x";break;
        }
        StringBuilder stringBuilder=new StringBuilder();
        int lenOfLineNumbers=Integer.toString(instructions.length-1,radixLines).length();
        for(int i=0;i<instructions.length;i++){
            I_Instruction instruction= registry.getInstruction(instructions[i]);
            stringBuilder.append(prefixLines).append(String.format("%1$-"+lenOfLineNumbers+"s",Integer.toString(i,radixLines))).append(" : ");
            switch (instruction.getOperandCount()){
                case 0:
                    stringBuilder.append(instruction.name());
                    break;
                case 1:
                    stringBuilder.append(instruction.name()).append(' ');
                    stringBuilder.append(prefix).append(Integer.toString(param0[i],radix));
                    break;
                case 2:
                    stringBuilder.append(instruction.name()).append(' ');
                    stringBuilder.append(prefix).append(Integer.toString(param0[i],radix)).append(',');
                    stringBuilder.append(prefix).append(Integer.toString(param1[i],radix));
                    break;
            }
            stringBuilder.append('\n');
        }
        return stringBuilder.toString();
    }

    private void compile(AvrCore core, boolean immersiveOperands, String[] lines) throws Exception {
        for (int i=0;i<lines.length;i++){
            //remove comments and cleanup space between operands
            lines[i]=lines[i]
                    .replaceAll("[\\n\\r]","")
                    .replaceAll("^\\s*","")
                    .replaceAll("\\s*:\\s*",":")
                    .replaceAll("\\s*\\.\\s*",".")
                    .replaceAll("\\s*,\\s*"," ")
                    .replaceAll("\\s*;.*$","");
        }
        compileDefinitions(lines,core,compileLabels(readLabels(lines),lines));

        int i = 0;
        int[] operandsReturn = new int[2];
        try {
            for (int pc=0; i < lines.length; i++) {
                if(lines[i].length()==0){
                    continue;
                }

                if(lines[i].startsWith(".")){
                    throw new InvalidDirective("Unknown directive "+lines[i]+ " At line "+i);
                }

                String[] values = lines[i].split(" ");
                Integer id=registry.getId(values[0].toUpperCase());
                if (id == null) {
                    throw new InvalidMnemonic("Instruction " +values[0].toUpperCase()+ " At line "+i+" Mnemonic does not exist");
                }
                instructions[pc] = id;
                operandsReturn[0]=operandsReturn[1]=0;

                registry.getInstruction(instructions[pc]).compileInstruction(core, this,pc, immersiveOperands, operandsReturn, values);

                param0[pc] = operandsReturn[0];
                param1[pc] = operandsReturn[1];
                pc++;
            }
        }catch (ProgramException e){
            throw e;
        }catch (Exception e){
            throw new ProgramException("Program compilation failed! At line "+i,e);
        }
    }

    private HashMap<String,Integer> readLabels(String[] lines) throws InvalidLabel{
        HashMap<String,Integer> labels=new HashMap<>();

        for(int i=0,pc=0;i<lines.length;i++){
            if(lines[i].length()>0 && !lines[i].startsWith(".")){
                if(lines[i].contains(":")){
                    String label=lines[i].replaceAll(":.*$","");
                    if(label.replaceAll("[0-9a-zA-Z]","").length()!=0){
                        throw new InvalidLabel("Invalid label "+label+", required: \"[0-9a-zA-Z]\" later you can use -name for relative label! At line "+i);
                    }
                    if(labels.containsKey(label)){
                        throw new InvalidLabel("Invalid label "+label+", already defined as label! At line "+i);
                    }
                    labels.put(label,pc);
                    lines[i]=lines[i].replaceAll("^.*:\\s*","");
                }
                if(lines[i].length()==0){
                    continue;
                }
                pc++;
            }

        }
        return labels;
    }

    private HashMap<String,Integer> compileLabels(HashMap<String,Integer> labels, String[] lines){

        for(int i=0,pc=0;i<lines.length;i++){
            if(lines[i].length()>0 && !lines[i].startsWith(".")){
                String[] values = lines[i].split(" ");

                if(values.length>1){
                    if(labels.containsKey(values[1].replaceAll("-",""))){
                        if(values[1].contains("-")){
                            lines[i]=lines[i].replaceAll(values[1],Integer.toString(labels.get(values[1].replaceAll("-",""))-pc));
                        }else {
                            lines[i]=lines[i].replaceAll(values[1],labels.get(values[1]).toString());
                        }
                    }
                }
                if(values.length>2){
                    if(labels.containsKey(values[2].replaceAll("-",""))){
                        if(values[2].contains("-")){
                            lines[i]=lines[i].replaceAll(values[2],Integer.toString(labels.get(values[2].replaceAll("-",""))-pc));
                        }else {
                            lines[i]=lines[i].replaceAll(values[2],labels.get(values[2]).toString());
                        }
                    }
                }
                pc++;
            }

        }
        return labels;
    }

    //.equ const
    //.set var
    //.def reg
    //.undef reg
    private void compileDefinitions(String[] lines,AvrCore core,HashMap<String,Integer> labels) throws InvalidDirective{
        HashMap<String,Integer> mapD=new HashMap<>();
        HashMap<String,Integer> mapE=new HashMap<>();
        HashMap<String,Integer> mapS=new HashMap<>();
        for (int i = 0;i<lines.length;i++) {
            if(lines[i].toLowerCase().contains(".def")){
                String[] temp=lines[i].replaceAll("^\\.[dD][eE][fF]\\s+","").replaceAll("\\s*=\\s*[rR]*","=").split("=");
                if(temp.length!=2){
                    throw new InvalidDirective("Invalid format "+lines[i]+", required: \".def name = number\"! At line "+i);
                }
                if(temp[0].replaceAll("[0-9a-zA-Z]","").length()!=0){
                    throw new InvalidDirective("Invalid name "+temp[0]+", required: \"[0-9a-zA-Z]\"! At line "+i);
                }
                try {
                    int r = parseAdvanced(temp[1]);
                    if(r<0 || r>=core.registerFile.length){
                        throw new InvalidDirective("Invalid register address "+r+"! At line "+i);
                    }
                    if(labels.containsKey(temp[0])){
                        throw new InvalidDirective("Invalid name "+temp[0]+", already defined as label! At line "+i);
                    }
                    if(mapE.containsKey(temp[0])){
                        throw new InvalidDirective("Invalid name "+temp[0]+", already defined as constant! At line "+i);
                    }
                    if(mapS.containsKey(temp[0])){
                        throw new InvalidDirective("Invalid name "+temp[0]+", is already defined as variable! At line "+i);
                    }
                    if(mapD.containsKey(temp[0])){
                        throw new InvalidDirective("Invalid name "+temp[0]+", is already defined as register name! At line "+i);
                    }else {
                        mapD.put(temp[0],r);
                    }
                }catch (Exception e){
                    throw new InvalidDirective("Invalid register address! At line "+i+" Cannot parse "+temp[1]);
                }
            }else if(lines[i].toLowerCase().startsWith(".undef")){
                String[] temp=lines[i].replaceAll("^\\.[uU][nN][dD][eE][fF]\\s+","").split("=");
                if(temp.length!=1){
                    throw new InvalidDirective("Invalid format "+lines[i]+", required: \".undef name\"! At line "+i);
                }
                if(temp[0].replaceAll("[0-9a-zA-Z]","").length()!=0){
                    throw new InvalidDirective("Invalid name "+temp[0]+", required: \"[0-9a-zA-Z]\"! At line "+i);
                }
                if(mapD.containsKey(temp[0])){
                    mapD.remove(temp[0]);
                }else {
                    throw new InvalidDirective("Invalid name "+temp[0]+", is not defined as register name! At line "+i);
                }
            }else if(lines[i].toLowerCase().startsWith(".equ")){
                String[] temp=lines[i].replaceAll("^\\.[eE][qQ][uU]\\s+","").replaceAll("\\s*=\\s*[rR]*","=").split("=");
                if(temp.length!=2){
                    throw new InvalidDirective("Invalid format "+lines[i]+", required: \".equ name = number\"! At line "+i);
                }
                if(temp[0].replaceAll("[0-9a-zA-Z]","").length()!=0){
                    throw new InvalidDirective("Invalid name "+temp[0]+", required: \"[0-9a-zA-Z]\"! At line "+i);
                }
                try {
                    int r = parseAdvanced(temp[1]);
                    if(labels.containsKey(temp[0])){
                        throw new InvalidDirective("Invalid name "+temp[0]+", already defined as label! At line "+i);
                    }
                    if(mapE.containsKey(temp[0])){
                        throw new InvalidDirective("Invalid name "+temp[0]+", already defined as constant! At line "+i);
                    }
                    if(mapS.containsKey(temp[0])){
                        throw new InvalidDirective("Invalid name "+temp[0]+", is already defined as variable! At line "+i);
                    }
                    if(mapD.containsKey(temp[0])){
                        throw new InvalidDirective("Invalid name "+temp[0]+", already defined as register name! At line "+i);
                    }
                    mapE.put(temp[0],r);
                }catch (Exception e){
                    throw new InvalidDirective("Invalid constant! At line "+i+" Cannot parse "+temp[1]);
                }
            }else if(lines[i].toLowerCase().startsWith(".set")){
                String[] temp=lines[i].replaceAll("^\\.[sS][eE][tT]\\s+","").replaceAll("\\s*=\\s*[rR]*","=").split("=");
                if(temp.length!=2 && temp.length!=1){
                    throw new InvalidDirective("Invalid format "+lines[i]+", required: \".set name (= (number))\" () means optional! At line "+i);
                }
                if(temp[0].replaceAll("[0-9a-zA-Z]","").length()!=0){
                    throw new InvalidDirective("Invalid name "+temp[0]+", required: \"[0-9a-zA-Z]\"! At line "+i);
                }
                try {
                    if(labels.containsKey(temp[0])){
                        throw new InvalidDirective("Invalid name "+temp[0]+", already defined as label! At line "+i);
                    }
                    if(mapE.containsKey(temp[0])){
                        throw new InvalidDirective("Invalid name "+temp[0]+", already defined as constant! At line "+i);
                    }
                    if(mapD.containsKey(temp[0])){
                        throw new InvalidDirective("Invalid name "+temp[0]+", already defined as register name! At line "+i);
                    }
                    if(temp.length>1) {
                        if (temp[1].length() == 0) {
                            mapS.remove(temp[0]);
                        } else {
                            int r = parseAdvanced(temp[1]);
                            mapS.put(temp[0], r);
                        }
                    }else {
                        mapS.remove(temp[0]);
                    }
                }catch (Exception e){
                    throw new InvalidDirective("Invalid variable! At line "+i+" Cannot parse "+temp[1]);
                }
            }else if(lines[i].startsWith(".")) {
                continue;
            }else if(lines[i].length()>0){
                String[] values = lines[i].split(" ");
                if(values.length>1){
                    if(mapD.containsKey(values[1])){
                        lines[i]=lines[i].replaceAll(values[1],mapD.get(values[1]).toString());
                    } else if(mapE.containsKey(values[1])){
                        lines[i]=lines[i].replaceAll(values[1],mapE.get(values[1]).toString());
                    } else if(mapS.containsKey(values[1])){
                        lines[i]=lines[i].replaceAll(values[1],mapS.get(values[1]).toString());
                    }
                }
                if(values.length>2){
                    if(mapD.containsKey(values[2])){
                        lines[i]=lines[i].replaceAll(values[2],mapD.get(values[2]).toString());
                    } else if(mapE.containsKey(values[2])){
                        lines[i]=lines[i].replaceAll(values[2],mapE.get(values[2]).toString());
                    } else if(mapS.containsKey(values[2])){
                        lines[i]=lines[i].replaceAll(values[2],mapS.get(values[2]).toString());
                    }
                }
                continue;
            }
            lines[i]="";
        }
    }

    @Override
    public ProgramMemory clone() {
        ProgramMemory programMemory=new ProgramMemory(registry,instructions.length);
        System.arraycopy(instructions,0,programMemory.instructions,0,instructions.length);
        System.arraycopy(param0,0,programMemory.param0,0, param0.length);
        System.arraycopy(param1,0,programMemory.param1,0, param1.length);
        return programMemory;
    }
    
    public static int parseAdvanced(String str){
        if (str.contains("0x") | str.contains("0X")) {
            str = str.replaceAll("0[xX]", "");
            return Integer.parseInt(str, 16);
        } else if (str.contains("0b") | str.contains("0B")) {
            str = str.replaceAll("0[bB]", "");
            return Integer.parseInt(str, 2);
        } else if (str.startsWith("-0") | str.startsWith("0")) {
            return Integer.parseInt(str, 8);
        } else {
            return Integer.parseInt(str, 10);
        }
    }
}
