package com.github.technus.avrClone.compiler;

import com.github.technus.avrClone.compiler.directives.IDirective;
import com.github.technus.avrClone.compiler.directives.Directive;
import com.github.technus.avrClone.compiler.directives.exceptions.InvalidDirective;
import com.github.technus.avrClone.compiler.exceptions.*;
import com.github.technus.avrClone.compiler.js.*;
import com.github.technus.avrClone.compiler.js.exceptions.EvaluationException;
import com.github.technus.avrClone.compiler.js.exceptions.InvalidConditionalEvaluation;
import com.github.technus.avrClone.compiler.js.exceptions.PrintingException;
import jdk.nashorn.api.scripting.NashornScriptEngine;
import jdk.nashorn.api.scripting.NashornScriptEngineFactory;

import javax.script.ScriptException;
import javax.script.SimpleBindings;
import java.io.IOException;
import java.util.*;

import static com.github.technus.avrClone.compiler.Line.NAME_FORMAT;
import static com.github.technus.avrClone.compiler.Line.NAME_TERMINATOR;
import static com.github.technus.avrClone.compiler.SourceCollection.LINE_NUMBER_SEPARATOR_CHAR;
import static com.github.technus.avrClone.compiler.SourceCollection.MACRO_SEPARATOR_CHAR;

public class ProgramCompiler {
    static {
        Directive.makeDirectives();
    }

    private static final String SANDBOX =
            "var eval=function(){};var uneval=function(){};" +
                    "var decodeURI=function(){};var decodeURIComponent=function(){};" +
                    "var encodeURI=function(){};var encodeURIComponent=function(){};" +
                    "var escape=function(){};var unescape=function(){};" +
                    "var quit=function(){};var exit=function(){};" +
                    "var print=function(){};var echo = function(){};" +
                    "var readFully=function(){};" + "var readLine=function(){};" +
                    "var load=function(){};var loadWithNewGlobal=function(){};\n";

    //region fields
    private HashMap<String, IDirective> instanceDirectives = new HashMap<>();

    public final SourceCollection sources = new SourceCollection();

    private int currentLine;
    private ArrayList<Line> lines;

    private Segment currentSegment;
    private ListingMode currentListing;

    private int[] startOffset, origins;
    private boolean[] overlap;
    private HashMap<Integer, Integer>[] constants;
    private BitSet[] constantRanges;

    private CompilerBindings compilerBindings=new CompilerBindings(64);
    private CompilerContext scriptContext;
    private NashornScriptEngine scriptEngine;

    private ArrayList<ConditionalState> compilationEnabled = new ArrayList<>();

    private HashMap<String, ArrayList<Line>> macros;
    private LinkedHashSet<String> editingMacros;

    private TreeMap<Integer,String> madeFile;
    //endregion

    public ProgramCompiler() {
        reset();
    }

    @SuppressWarnings("unchecked")
    private void reset() {
        lines = null;

        sources.clear();

        currentLine = 0;

        currentSegment = Segment.CSEG;
        currentListing = ListingMode.LIST;

        startOffset = new int[Segment.count()];
        origins = new int[Segment.count()];

        overlap = new boolean[Segment.count()];

        constants = new HashMap[Segment.count()];
        for (int i = 0; i < constants.length; i++) {
            constants[i] = new HashMap<>();
        }
        constantRanges = new BitSet[Segment.count()];
        for (int i = 0; i < constantRanges.length; i++) {
            constantRanges[i] = new BitSet(4096);
        }

        //compilerBindings = new CompilerBindings(64);
        SimpleBindings globalBindings = new SimpleBindings();
        scriptContext = new CompilerContext(compilerBindings, globalBindings);
        ClassLoader ccl = Thread.currentThread().getContextClassLoader();
        ccl = ccl == null ? NashornScriptEngineFactory.class.getClassLoader() : ccl;
        scriptEngine = (NashornScriptEngine) new NashornScriptEngineFactory()
                .getScriptEngine(new String[]{"--no-java"}, ccl, s -> false);
        scriptEngine.setContext(scriptContext);

        resetConditionalState();

        macros = new HashMap<>();
        editingMacros = new LinkedHashSet<>();

        madeFile = null;
    }

    public CompilerBindings getCompilerBindings() {
        return compilerBindings;
    }

    public void setCompilerBindings(CompilerBindings compilerBindings) {
        if(compilerBindings==null) {
            this.compilerBindings=new CompilerBindings(64);
            return;
        }
        this.compilerBindings = compilerBindings;
    }

    private void softReset() throws CompilerException {
        resetConditionalState();
        setCurrentListing(ListingMode.LIST);
        setCurrentSegment(Segment.CSEG);
        Segment[] segments = Segment.values();
        for (Segment segment : segments) {
            setOrigin(0, segment);
            setOverlap(false, segment);
        }
        compilerBindings.removeAllBindings(Binding.NameType.DEF);
    }

    //region input file
    public void compile(String includeName) throws Exception {
        reset();

        lines=sources.projectRootInclude(includeName, isListing());

        if (lines == null) {
            throw new CompilerException("Cannot compile null program!");
        }

        boolean didSomething;
        {
            do {
                if(Thread.currentThread().isInterrupted()){
                    throw new InterruptedException("INTERUPTED!");
                }
                didSomething = false;
                softReset();

                for (currentLine = 0; currentLine < lines.size(); currentLine++) {
                    Line lineObj = lines.get(currentLine);

                    if (!lineObj.isProcessed()) {
                        lineObj.setEnabled(isCompilationEnabled());

                        if (lineObj.getDirectiveName() != null) {
                            IDirective directive = getDirective(lineObj);
                            didSomething = processDirectiveInternal(didSomething, lineObj, directive);
                        } else if (lineObj.getMnemonic() != null) {
                            if (currentSegment != Segment.CSEG) {
                                throw new CompilerException("Invalid mnemonic use! " + lineObj.getLine());
                            }
                            if (lineObj.isEnabled() && isEditingMacros()){
                                addToMacros(lineObj);
                                lineObj.setEnabled(false);
                            }
                            lineObj.setProcessed(true);
                            didSomething=true;
                        } else {
                            lineObj.setProcessed(true);
                            didSomething=true;
                        }
                    }else{
                        if(lineObj.getDirectiveName()!=null){
                            IDirective directive=getDirective(lineObj);
                            if(directive!=null) {
                                directive.offsetOriginIfProcessed(this, lineObj);
                            }
                        }
                    }
                }
            } while (didSomething);
        }

        if (isEditingMacros()) {
            throw new CompilerException("Is still trying to edit macros! " + Arrays.toString(editingMacros.toArray(new String[0])));
        }

        compilerBindings.removeAllBindings(Binding.NameType.SET);

        softReset();

        for (currentLine = 0; currentLine < lines.size(); currentLine++) {
            Line lineObj = lines.get(currentLine);
            if (lineObj.getDirectiveName() != null) {
                IDirective directive = getDirective(lineObj);
                if (directive == null) {
                    lineObj.setProcessed(true);
                } else {
                    if (lineObj.isProcessed()) {
                        directive.offsetOriginIfProcessed(this, lineObj);
                    } else {
                        if(directive.onlyFirstPass()){
                            if(directive.isRepeatable()) {
                                lineObj.setProcessed(true);
                                directive.offsetOriginIfProcessed(this, lineObj);
                            }else {
                                throw new CompilerException("Cannot process in first pass! "+lineObj.getLine());
                            }
                        }else{
                            try {
                                if(lineObj.getEvaluatedArguments()==null) {
                                    lineObj.setArguments(lineObj.getLatestArguments().replaceAll("\\$", "(" + (getOrigin(Segment.CSEG) + getSegmentOffset(Segment.CSEG)) + ')')
                                            .replaceAll(NAME_FORMAT + "#", "($1-" + (getOrigin(Segment.CSEG) + getSegmentOffset(Segment.CSEG)) + ')'));
                                }
                                if (lineObj.getLatestArguments().contains("#")) {
                                    throw new CompilerException("Unable to replace with PC difference! " + lineObj.getLine());
                                }
                                directive.process(this, lineObj);
                                if (!directive.isRepeatable()) {
                                    lineObj.setProcessed(true);
                                }
                            } catch (EvaluationException e) {
                                if (directive.cannotFail()) {
                                    throw new EvaluationException("Directive failed! " + lineObj.getLine(), e);
                                }
                            }
                        }
                    }
                }
            } else if (lineObj.isEnabled() && lineObj.getMnemonic() != null) {
                if (currentSegment != Segment.CSEG) {
                    throw new CompilerException("Invalid mnemonic use! " + lineObj.getLine());
                }
                lineObj.setProcessed(false);

                lineObj.setArguments(lineObj.getLatestArguments().replaceAll("\\$", "(" + (getOrigin(Segment.CSEG) + getSegmentOffset(Segment.CSEG)) + ')')
                        .replaceAll(NAME_FORMAT + "#", "($1-" + (getOrigin(Segment.CSEG) + getSegmentOffset(Segment.CSEG)) + ')'));
                if (lineObj.getLatestArguments().contains("#")) {
                    throw new CompilerException("Unable to replace with PC difference! " + lineObj.getLine());
                }

                putProgramLabels();
                if (isMacroDefined(lineObj.getMnemonic())) {
                    lineObj.setListing(!isMacroListing() && isListing());
                    injectMacro(lineObj.getMnemonic(), lineObj.getLatestArgumentArray());
                    lineObj.setProcessed(true);
                    lineObj.setEnabled(false);
                } else {
                    lineObj.setListing(isListing());
                    offsetCurrentOrigin(1);
                }
            }
        }

        HashMap<Integer,String> made=new HashMap<>();

        compilerBindings.removeAllBindings(Binding.NameType.SET);

        {
            do {
                if(Thread.currentThread().isInterrupted()){
                    throw new InterruptedException("INTERUPTED!");
                }

                didSomething = false;
                softReset();

                for (currentLine = 0; currentLine < lines.size(); currentLine++) {
                    Line lineObj = lines.get(currentLine);
                    if (!lineObj.isProcessed()) {
                        if (lineObj.getDirectiveName() != null) {
                            IDirective directive = getDirective(lineObj);
                            didSomething = processDirectiveInternal(didSomething, lineObj, directive);
                        } else if (lineObj.getMnemonic() != null && lineObj.isEnabled()) {
                            if (currentSegment != Segment.CSEG) {
                                throw new CompilerException("Invalid mnemonic use! " + lineObj.getLine());
                            }
                            String[] expressions = lineObj.getLatestArgumentArray();
                            StringBuilder sb = new StringBuilder(lineObj.getMnemonic()).append(' ');
                            try {
                                for (String expression : expressions) {
                                    sb.append(computeString(expression)).append('`');
                                }
                                sb.deleteCharAt(sb.length()-1);
                            }catch (EvaluationException e){
                                throw new EvaluationException("Mnemonic failed! "+lineObj.getLine(),e);
                            }
                            made.put(getCurrentOrigin(), sb.toString());
                            putCodeLine();
                            lineObj.setProcessed(true);
                            didSomething = true;
                        }
                    }else if(lineObj.getDirectiveName()!=null){
                        IDirective directive=getDirective(lineObj);
                        if(directive!=null) {
                            directive.offsetOriginIfProcessed(this, lineObj);
                        }
                    }else if(lineObj.getMnemonic()!=null && lineObj.isEnabled()){
                        if (currentSegment != Segment.CSEG) {
                            throw new CompilerException("Invalid mnemonic use! " + lineObj.getLine());
                        }
                        offsetCurrentOrigin(1);
                    }
                }
            } while (didSomething);
        }

        if(Thread.currentThread().isInterrupted()){
            throw new InterruptedException("INTERUPTED!");
        }
        madeFile = new TreeMap<>(made);
    }

    private boolean processDirectiveInternal(boolean didSomething, Line lineObj, IDirective directive) throws CompilerException {
        if (directive == null) {
            lineObj.setProcessed(true);
            didSomething=true;
        } else {
            try {
                directive.process(this, lineObj);
                if (!directive.isRepeatable()) {
                    lineObj.setProcessed(true);
                    didSomething = true;
                }
            } catch (EvaluationException e){
                if (directive.cannotFail()) {
                    throw new EvaluationException("Directive failed! " + lineObj.getLine(), e);
                }
            }
        }
        return didSomething;
    }

    public TreeMap<Integer,String> getMadeFile() {
        return madeFile;
    }

    public ArrayList<String> getProgram(){
        ArrayList<String> cseg=new ArrayList<>(madeFile.size());
        for (int i = 0,len=madeFile.size(); i < len; i++) {
            cseg.add("");
        }
        for(Map.Entry<Integer,String> entry:madeFile.entrySet()){
            cseg.set(entry.getKey(),entry.getValue());
        }
        return cseg;
    }
    public ArrayList<String> getDataCSEG(){
        ArrayList<String> cseg=new ArrayList<>(getMemorySize(Segment.CSEG));
        for (int i = 0,len=getMemorySize(Segment.CSEG); i < len; i++) {
            cseg.add("");
        }
        for(Map.Entry<Integer,Integer> entry:constants[Segment.CSEG.ordinal()].entrySet()){
            cseg.set(entry.getKey(),entry.getValue().toString());
        }
        return cseg;
    }
    public ArrayList<String> getDataDSEG(){
        ArrayList<String> dseg=new ArrayList<>(getMemorySize(Segment.DSEG));
        for (int i = 0,len=getMemorySize(Segment.DSEG); i < len; i++) {
            dseg.add("");
        }
        for(Map.Entry<Integer,Integer> entry:constants[Segment.ESEG.ordinal()].entrySet()){
            dseg.set(entry.getKey(),entry.getValue().toString());
        }
        return dseg;
    }
    public ArrayList<String> getDataESEG(){
        ArrayList<String> eseg=new ArrayList<>(getMemorySize(Segment.ESEG));
        for (int i = 0,len=getMemorySize(Segment.ESEG); i < len; i++) {
            eseg.add("");
        }
        for(Map.Entry<Integer,Integer> entry:constants[Segment.ESEG.ordinal()].entrySet()){
            eseg.set(entry.getKey(),entry.getValue().toString());
        }
        return eseg;
    }
    //endregion

    //region include
    public void include(String includeName) throws CompilerException {
        if (includeName == null) {
            throw new InvalidInclude("Cannot include null!");
        }
        Line line = lines.get(currentLine);
        lines.addAll(currentLine + 1,
                sources.getInclude(line.getIncludePath(), line.getIncludeName(), includeName, currentLine, isListing()));
    }

    public void exitCurrentFile() {
        Line l = lines.get(currentLine);
        String file = l.getIncludePath();
        for (int i = currentLine + 1, size = lines.size(); i < size; ) {
            l = lines.get(currentLine);
            if (l.getIncludePath().startsWith(file)) {
                l.setProcessed(true);
                i++;
            } else {
                break;
            }
        }
    }
    //endregion

    //region segment
    public void setCurrentSegment(Segment currentSegment) throws InvalidMemorySegment {
        if (currentSegment == null) {
            throw new InvalidMemorySegment("Segment cannot be null!");
        }
        this.currentSegment = currentSegment;
    }

    public Segment getCurrentSegment() {
        return currentSegment;
    }
    //endregion

    //region start offsets
    public int getCurrentSegmentOffset() {
        return startOffset[currentSegment.ordinal()];
    }

    public int getSegmentOffset(Segment segment) {
        return startOffset[segment.ordinal()];
    }

    public void setSegmentOffset(Segment segment, int offset) throws InvalidStartOffset {
        if (offset < 0) {
            throw new InvalidStartOffset("Start offset must be not negative! " + offset);
        }
        startOffset[segment.ordinal()] = offset;
    }
    //endregion

    //region overlap policy
    public void setCurrentOverlap(boolean value) {
        overlap[currentSegment.ordinal()] = value;
    }

    public void setOverlap(boolean value, Segment segment) {
        overlap[segment.ordinal()] = value;
    }

    public boolean getCurrentOverlap() {
        return overlap[currentSegment.ordinal()];
    }

    public boolean getOverlap(Segment segment) {
        return overlap[segment.ordinal()];
    }
    //endregion

    //region origin
    public void offsetCurrentOrigin(int value) throws InvalidOrigin {
        if (origins[currentSegment.ordinal()] + value < 0) {
            throw new InvalidOrigin("Origin must be not negative! " + origins[currentSegment.ordinal()] + "+" + value);
        }
        origins[currentSegment.ordinal()] += value;
    }

    public void setCurrentOrigin(int value) throws InvalidOrigin {
        if (value < 0) {
            throw new InvalidOrigin("Origin must be not negative! " + value);
        }
        origins[currentSegment.ordinal()] = value;
    }

    public void setOrigin(int value, Segment segment) throws InvalidOrigin {
        if (value < 0) {
            throw new InvalidOrigin("Origin must be not negative! " + value);
        }
        origins[segment.ordinal()] = value;
    }

    public int getCurrentOrigin() {
        return origins[currentSegment.ordinal()];
    }

    public int getOrigin(Segment segment) {
        return origins[segment.ordinal()];
    }
    //endregion

    //region mem use - for CSEG after instructions!
    public int getCurrentMemorySize() {
        return constantRanges[currentSegment.ordinal()].length();
    }

    public int getMemorySize(Segment segment) {
        return constantRanges[segment.ordinal()].length();
    }

    public boolean isCurrentMemoryCellFree() {
        return !constantRanges[currentSegment.ordinal()].get(getCurrentOrigin());
    }

    public boolean isCurrentMemoryCellFree(int address) throws InvalidMemoryAccess {
        if (address < 0) {
            throw new InvalidMemoryAccess("Memory address must be not negative! " + address);
        }
        return !constantRanges[currentSegment.ordinal()].get(address);
    }

    public boolean isMemoryCellFree(Segment segment, int address) throws InvalidMemoryAccess {
        if (address < 0) {
            throw new InvalidMemoryAccess("Memory address must be not negative! " + address);
        }
        return !constantRanges[segment.ordinal()].get(address);
    }

    public boolean isCurrentMemoryRangeFree(int toExclusive) throws InvalidMemoryAccess {
        if (getCurrentOrigin() >= toExclusive) {
            throw new InvalidMemoryAccess("Range end must be greater than origin! " + getCurrentOrigin() + " !< " + toExclusive);
        }
        return constantRanges[currentSegment.ordinal()].nextClearBit(getCurrentOrigin()) >= toExclusive;
    }

    public boolean isCurrentMemoryRangeFree(int fromInclusive, int toExclusive) throws InvalidMemoryAccess {
        if (fromInclusive < 0) {
            throw new InvalidMemoryAccess("Range start must be not negative! " + fromInclusive);
        }
        if (fromInclusive >= toExclusive) {
            throw new InvalidMemoryAccess("Range end must be greater than range start! " + fromInclusive + " !< " + toExclusive);
        }
        return constantRanges[currentSegment.ordinal()].nextClearBit(fromInclusive) >= toExclusive;
    }

    public boolean isMemoryRangeFree(Segment segment, int fromInclusive, int toExclusive) throws InvalidMemoryAccess {
        if (fromInclusive < 0) {
            throw new InvalidMemoryAccess("Range start must be not negative! " + fromInclusive);
        }
        if (fromInclusive >= toExclusive) {
            throw new InvalidMemoryAccess("Range end must be greater than range start! " + fromInclusive + " !< " + toExclusive);
        }
        return constantRanges[segment.ordinal()].nextClearBit(fromInclusive) >= toExclusive;
    }

    public boolean isCurrentMemoryBlockFree(int size) throws InvalidMemoryAccess {
        if (size <= 0) {
            throw new InvalidMemoryAccess("Block size must be positive! " + size);
        }
        int offset = getCurrentOrigin();
        return constantRanges[currentSegment.ordinal()].nextClearBit(offset) >= offset + size;
    }

    public boolean isCurrentMemoryBlockFree(int start, int size) throws InvalidMemoryAccess {
        if (start < 0) {
            throw new InvalidMemoryAccess("Block start must be not negative! " + start);
        }
        if (size <= 0) {
            throw new InvalidMemoryAccess("Block size must be positive! " + size);
        }
        return constantRanges[currentSegment.ordinal()].nextClearBit(start) >= start + size;
    }

    public boolean isMemoryBlockFree(Segment segment, int start, int size) throws InvalidMemoryAccess {
        if (start < 0) {
            throw new InvalidMemoryAccess("Block start must be not negative! " + start);
        }
        if (size <= 0) {
            throw new InvalidMemoryAccess("Block size must be positive! " + size);
        }
        return constantRanges[segment.ordinal()].nextClearBit(start) >= start + size;
    }
    //endregion

    //region constants
    public void putConstant(int constant) throws InvalidOrigin, InvalidMemoryAllocation, InvalidBinding {
        if (!currentSegment.isAllowingConstants()) {
            throw new InvalidMemoryAllocation("Cannot store constants in volatile memory! " + constant);
        }
        if (getCurrentOverlap() || isCurrentMemoryCellFree()) {
            int segment = currentSegment.ordinal();
            int origin = getCurrentOrigin();
            int offset = getCurrentSegmentOffset();
            for (String s : getLabelsOrPointersNames()) {
                putBinding(s, new Binding(Binding.NameType.POINTER, offset + origin));
            }
            constantRanges[segment].set(origin);
            constants[segment].put(origin, constant);
            offsetCurrentOrigin(1);
        }else{
            throw new InvalidMemoryAllocation("Memory overlaps! "+currentSegment.name()+" "+getCurrentOrigin());
        }
    }

    public void putConstant(float constant) throws InvalidOrigin, InvalidMemoryAllocation, InvalidBinding {
        if (!currentSegment.isAllowingConstants()) {
            throw new InvalidMemoryAllocation("Cannot store constants in volatile memory! " + constant);
        }
        int segment = currentSegment.ordinal();
        if (getCurrentOverlap() || isCurrentMemoryCellFree()) {
            int origin = getCurrentOrigin();
            int offset = getCurrentSegmentOffset();
            for (String s : getLabelsOrPointersNames()) {
                putBinding(s, new Binding(Binding.NameType.POINTER, offset + origin));
            }
            constantRanges[segment].set(origin);
            constants[segment].put(origin, Float.floatToIntBits(constant));
            offsetCurrentOrigin(1);
        }else{
            throw new InvalidMemoryAllocation("Memory overlaps! "+currentSegment.name()+" "+getCurrentOrigin());
        }
    }

    public void putConstant(long constant) throws InvalidOrigin, InvalidMemoryAccess, InvalidMemoryAllocation, InvalidBinding {
        if (!currentSegment.isAllowingConstants()) {
            throw new InvalidMemoryAllocation("Cannot store constants in volatile memory! " + constant);
        }
        if (getCurrentOverlap() || isCurrentMemoryBlockFree(2)) {
            int segment = currentSegment.ordinal();
            int origin = getCurrentOrigin();
            int offset = getCurrentSegmentOffset();
            for (String s : getLabelsOrPointersNames()) {
                putBinding(s, new Binding(Binding.NameType.POINTER, offset + origin));
            }
            constantRanges[segment].set(origin, origin + 2);
            constants[segment].put(origin, (int) constant);
            constants[segment].put(origin + 1, (int) (constant >> 32));
            offsetCurrentOrigin(2);
        }else{
            throw new InvalidMemoryAllocation("Memory overlaps! "+currentSegment.name()+" "+getCurrentOrigin());
        }
    }

    public void putConstant(String constant) throws InvalidOrigin, InvalidMemoryAccess, InvalidMemoryAllocation, InvalidConstant, InvalidBinding {
        if (!currentSegment.isAllowingConstants()) {
            throw new InvalidMemoryAllocation("Cannot store constants in volatile memory! " + constant);
        }
        if (constant == null) {
            throw new InvalidConstant("String constant cannot be null!");
        }
        if (constant.length() == 0) {
            throw new InvalidConstant("String constant must not be empty!");
        }
        int length = constant.length();
        if (getCurrentOverlap() || isCurrentMemoryBlockFree(length)) {
            int segment = currentSegment.ordinal();
            int origin = getCurrentOrigin();
            int offset = getCurrentSegmentOffset();
            for (String s : getLabelsOrPointersNames()) {
                putBinding(s, new Binding(Binding.NameType.POINTER, offset + origin));
            }
            constantRanges[segment].set(origin, origin + length);
            for (int i = 0; i < constant.length(); i++) {
                constants[segment].put(origin + i, (int) constant.charAt(i));
            }
            offsetCurrentOrigin(length);
        }else{
            throw new InvalidMemoryAllocation("Memory overlaps! "+currentSegment.name()+" "+getCurrentOrigin());
        }
    }

    public void reserveMemory(int cellCount) throws InvalidOrigin, InvalidMemoryAccess, InvalidMemoryAllocation, InvalidBinding {
        if (!currentSegment.isAllowingVariables()) {
            throw new InvalidMemoryAllocation("Cannot store variables in program memory! " + cellCount);
        }
        if (cellCount <= 0) {
            throw new InvalidMemoryAllocation("Memory block size must have positive size! " + cellCount);
        }
        if (getCurrentOverlap() || isCurrentMemoryBlockFree(cellCount)) {
            int segment = currentSegment.ordinal();
            int origin = getCurrentOrigin();
            int offset = getCurrentSegmentOffset();
            for (String s : getLabelsOrPointersNames()) {
                putBinding(s, new Binding(Binding.NameType.POINTER, offset + origin));
            }
            constantRanges[segment].set(origin, origin + cellCount);
            for (int i = 0; i < cellCount; i++) {
                constants[segment].put(origin + i, 0);
            }
            offsetCurrentOrigin(cellCount);
        }
        throw new InvalidMemoryAllocation("Cannot store variables in program memory! " + cellCount);
    }

    public void putBinding(String name, Binding binding) throws InvalidBinding {
        if (binding == null) {
            throw new InvalidBinding("Invalid binding specified! " + name);
        }
        if (name == null || name.length() == 0 || !name.matches(NAME_FORMAT)) {
            throw new InvalidBinding("Invalid binding name specified! " + name);
        }
        Binding old = compilerBindings.getBinding(name);
        if (old != null) {
            switch (old.type) {
                case SET:
                case DEF:
                    break;
                default:
                    throw new InvalidBinding("Cannot rewrite that binding! " + old.type.name() + " " + name);
            }
        }
        compilerBindings.putBinding(name, binding);
    }

    public void putCodeLine() throws InvalidMemoryAllocation,InvalidOrigin{
        if (currentSegment != Segment.CSEG) {
            throw new InvalidMemoryAllocation("Cannot store program outside program memory!");
        }
        if (getCurrentOverlap() || isCurrentMemoryCellFree()) {
            int segment = currentSegment.ordinal();
            int origin = getCurrentOrigin();
            constantRanges[segment].set(origin);
            offsetCurrentOrigin(1);
        }else{
            throw new InvalidMemoryAllocation("Memory overlaps! "+currentSegment.name()+" "+getCurrentOrigin());
        }
    }

    public void putProgramLabels() throws InvalidMemoryAllocation,InvalidBinding {
        if (currentSegment != Segment.CSEG) {
            throw new InvalidMemoryAllocation("Cannot define program labels outside program memory!");
        }
        for (String name : getLabelsOrPointersNames()) {
            putBinding(name, new Binding(Binding.NameType.LABEL, getCurrentOrigin() + getCurrentSegmentOffset()));
        }
    }

    public ArrayList<String> getLabelsOrPointersNames() {
        ArrayList<String> names = new ArrayList<>();
        Line line = lines.get(currentLine);
        if (line.getLabelOrPointerName() != null) {
            names.add(line.getLabelOrPointerName());
        }
        for (int i = currentLine - 1; i >= 0; i++) {
            line = lines.get(i);
            if (line.isEnabled()) {
                if (line.getMnemonic() == null && line.getDirectiveName() == null) {
                    if (line.getLabelOrPointerName() != null) {
                        names.add(line.getLabelOrPointerName());
                    }
                } else {
                    break;
                }
            }
        }
        return names;
    }
    //endregion

    //region compute
    public String computeString(String script) throws EvaluationException {
        try {
            Object value = scriptEngine.eval(SANDBOX + script);
            if (value instanceof String) {
                return (String) value;
            } else if (value instanceof Number || value instanceof Boolean) {
                return value.toString();
            }
            if(value==null){
                writeError("Returned type: <NULL>");
                return "";
            }else {
                writeError("Returned type: " + value.getClass().getCanonicalName());
                return value.toString();
            }
        } catch (ScriptException e) {
            throw new EvaluationException("Cannot evaluate! " + script, e);
        }
    }

    public Number computeNumber(String script) throws EvaluationException {
        try {
            Object value = scriptEngine.eval(SANDBOX + script);
            if (value instanceof Number) {
                return (Number) value;
            } else if (value instanceof Boolean) {
                return (Boolean) value ? 1 : 0;
            }
            if(value==null){
                writeError("Returned type: <NULL>");
            }else {
                writeError("Returned type: " + value.getClass().getCanonicalName());
            }
            return 0;
        } catch (ScriptException e) {
            throw new EvaluationException("Cannot evaluate! " + script, e);
        }
    }

    public Boolean computeBoolean(String script) throws EvaluationException {
        try {
            Object value = scriptEngine.eval(SANDBOX + script);
            if (value instanceof Boolean) {
                return (Boolean) value;
            } else if (value instanceof Number) {
                return ((Number) value).intValue() != 0;
            }
            if(value==null){
                writeError("Returned type: <NULL>");
            }else {
                writeError("Returned type: " + value.getClass().getCanonicalName());
            }
            return false;
        } catch (ScriptException e) {
            throw new EvaluationException("Cannot evaluate! " + script, e);
        }
    }

    public Object computeObject(String script) throws EvaluationException {
        try {
            Object value = scriptEngine.eval(SANDBOX + script);
            if (value instanceof Number || value instanceof Boolean || value instanceof String) {
                return value;
            }
            if(value==null){
                writeError("Returned type: <NULL>");
            }else {
                writeError("Returned type: " + value.getClass().getCanonicalName());
            }
            return null;
        } catch (ScriptException e) {
            throw new EvaluationException("Cannot evaluate! " + script, e);
        }
    }

    public void writeError(String s) throws PrintingException {
        try {
            scriptContext.getErrorWriter().write(s + "\n");
        } catch (IOException e) {
            throw new PrintingException("Cannot print error! " + s, e);
        }
    }

    public void write(String s) throws PrintingException {
        try {
            scriptContext.getWriter().write(s + "\n");
        } catch (IOException e) {
            throw new PrintingException("Cannot print! " + s, e);
        }
    }

    public void removeBinding(String name) throws InvalidBinding {
        Binding old = compilerBindings.getBinding(name);
        if (old == null) {
            throw new InvalidBinding("Cannot remove binding name is unused! " + name);
        }
        switch (old.type) {
            case SET:
            case DEF:
                break;
            default:
                throw new InvalidBinding("Cannot remove that binding! " + old.type.name() + " " + name);
        }
        compilerBindings.removeBinding(name);
    }

    public boolean containsNotDefs(String... keys) throws InvalidBinding {
        for (String name : keys) {
            if (name == null || name.length() == 0 || !name.matches(NAME_FORMAT)) {
                throw new InvalidBinding("Invalid binding name specified! " + name);
            }
        }
        return compilerBindings.containsNotDefinitions(keys);
    }

    public boolean lacksNotDefs(String... keys) throws InvalidBinding {
        for (String name : keys) {
            if (name == null || name.length() == 0 || !name.matches(NAME_FORMAT)) {
                throw new InvalidBinding("Invalid binding name specified! " + name);
            }
        }
        return compilerBindings.lacksNotDefinitions(keys);
    }
    //endregion

    //region parse
    public static Number parseNumberAdvanced(String str) {
        str = str.replaceAll("_", "");
        if (str.contains(".")) {
            return Double.parseDouble(str);
        }
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
    //endregion

    //region conditional compiler
    public void openIf(boolean compilationEnable) {
        if (compilationEnabled.size() > 1) {
            if (compilationEnabled.get(compilationEnabled.size() - 1) != ConditionalState.ASSEMBLING) {//if not compile
                compilationEnabled.add(ConditionalState.DISABLED);
                return;
            }
        }
        compilationEnabled.add(compilationEnable ? ConditionalState.ASSEMBLING : ConditionalState.CHECKING);
    }

    public void elseIf(boolean compilationEnable) throws InvalidConditionalAssembly, InvalidConditionalEvaluation {
        if (compilationEnabled.size() <= 1) {
            throw new InvalidConditionalAssembly("Missing if opening statement!");
        }
        ConditionalState b = compilationEnabled.get(compilationEnabled.size() - 1);
        if (b == ConditionalState.CHECKING) {
            compilationEnabled.set(compilationEnabled.size() - 1, compilationEnable ? ConditionalState.ASSEMBLING : ConditionalState.CHECKING);
        } else if (b == ConditionalState.ASSEMBLING) {
            compilationEnabled.set(compilationEnabled.size() - 1, ConditionalState.DISABLED);
        } else {
            throw new InvalidConditionalEvaluation("Not ready to evaluate!");
        }
    }

    public void endIf() throws InvalidConditionalAssembly {
        if (compilationEnabled.size() <= 1) {
            throw new InvalidConditionalAssembly("Missing if opening statement!");
        }
        compilationEnabled.remove(compilationEnabled.size() - 1);
    }

    public boolean isCompilationEnabled() {
        return compilationEnabled.get(compilationEnabled.size() - 1) == ConditionalState.ASSEMBLING;
    }

    public void setConditionalState(ConditionalState state) {
        compilationEnabled.set(compilationEnabled.size() - 1, state);
    }

    public int getDepth() {
        return compilationEnabled.size();
    }

    public void resetConditionalState() {
        compilationEnabled.clear();
        compilationEnabled.add(ConditionalState.ASSEMBLING);
    }
    //endregion

    //region macro compiler
    public void addMacro(String name) throws InvalidMacroStatement {
        if (name == null || name.length() == 0 || !name.matches(NAME_FORMAT)) {
            throw new InvalidMacroStatement("Invalid macro name specified! " + name);
        }
        macros.put(name, new ArrayList<>());
        editingMacros.add(name);
    }

    private void addToMacros(Line line) throws InvalidMacroStatement {
        if (line == null) {
            throw new InvalidMacroStatement("Invalid macro line specified!");
        }
        if (editingMacros.isEmpty()) {
            throw new InvalidMacroStatement("Is not editing any macros!");
        }
        editingMacros.forEach(s -> macros.get(s).add(line));
    }

    public void finishMacro(String name) throws InvalidMacroStatement {
        if (name == null || name.length() == 0) {
            if (editingMacros.isEmpty()) {
                throw new InvalidMacroStatement("Is not editing any macros!");
            }
            String[] list = editingMacros.toArray(new String[0]);
            editingMacros.remove(list[list.length - 1]);
        }
        if (!editingMacros.remove(name)) {
            throw new InvalidMacroStatement("Macro is not being edited! " + name);
        }
    }

    public boolean isEditingMacros() {
        return editingMacros.size() > 0;
    }

    private void injectMacro(String macroName, String[] lastestArguments) throws CompilerException {
        ArrayList<Line> macro = macros.get(macroName);
        if (macro == null) {
            throw new InvalidMacroStatement("Macro was never defined! " + macroName);
        }
        for (int i = 0; i < lastestArguments.length; i++) {
            lastestArguments[i] = '(' + lastestArguments[i] + ')';
        }
        ArrayList<String> labels = new ArrayList<>();
        for (Line s : macro) {
            String label = s.getLabelOrPointerName();
            if (label != null) {
                labels.add(label);
            }
        }
        ArrayList<Line> newLines = new ArrayList<>(macro.size());
        for (Line s : macro) {
            String temp = s.getLine();
            for (int i = lastestArguments.length - 1; i >= 0; i--) {
                temp = temp.replaceAll("@" + i + "(?:[^0-9].*)?", lastestArguments[i]);
            }
            if (temp.contains("@")) {
                throw new InvalidMacroStatement("Cannot resolve all parameters! " + s + " " + currentLine);
            }
            for (String l : labels) {
                temp = temp.replaceAll("(?:.*" + NAME_TERMINATOR + ")?" + l + "(?:" + NAME_TERMINATOR + ".*)?", l + "___MACRO___" + currentLine);
            }
            newLines.add(new Line(s.getIncludePath(), s.getIncludeName() + macroName + LINE_NUMBER_SEPARATOR_CHAR + currentLine + MACRO_SEPARATOR_CHAR, s.getLineNumber(), temp, isMacroListing()));
        }
        lines.addAll(currentLine + 1, newLines);
        offsetCurrentOrigin(newLines.size());
    }

    public boolean isMacroDefined(String name) {
        return macros.containsKey(name) && !editingMacros.contains(name);
    }
    //endregion

    //region listing
    public void setCurrentListing(ListingMode mode) throws InvalidListingMode {
        if (mode == null) {
            throw new InvalidListingMode("Listing mode cannot be null!");
        }
        currentListing = mode;
    }

    public ListingMode getCurrentListingMode() {
        return currentListing;
    }

    public boolean isListing() {
        return currentListing != ListingMode.NO_LIST;
    }

    public boolean isMacroListing() {
        return currentListing == ListingMode.LIST_MACRO;
    }
    //endregion

    //region directive
    public IDirective putDirective(String name, IDirective directive) throws InvalidDirective {
        if (name == null || name.length() == 0 || !name.matches(NAME_FORMAT)) {
            throw new InvalidDirective("Invalid directive name specified! " + name);
        }
        return instanceDirectives.put(name, directive);
    }

    public IDirective getDirective(Line line) throws InvalidDirective {
        String name=line.getDirectiveName();
        if (name == null || name.length() == 0 || !name.matches(NAME_FORMAT)) {
            throw new InvalidDirective("Invalid directive name specified! " + name);
        }
        IDirective directive = instanceDirectives.get(name);
        if (directive != null) {
            if (line.isEnabled() || directive.isUnskippable()) {
                return directive;
            } else {
                return null;
            }
        }
        directive = Directive.GLOBAL_DIRECTIVES.get(name);
        if (directive != null) {
            if (line.isEnabled() || directive.isUnskippable()) {
                return directive;
            } else {
                return null;
            }
        }
        directive = Directive.GLOBAL_DIRECTIVES.get(name.toUpperCase());
        if (directive != null) {
            if (line.isEnabled() || directive.isUnskippable()) {
                return directive;
            } else {
                return null;
            }
        }
        if (line.isEnabled()) {
            throw new InvalidDirective("Directive is not defined! " + name);
        }
        return null;
    }
    //endregion
}
