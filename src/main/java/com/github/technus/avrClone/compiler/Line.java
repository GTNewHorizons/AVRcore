package com.github.technus.avrClone.compiler;

import com.github.technus.avrClone.compiler.exceptions.CompilerException;

public class Line{
    public static final String NAME_FORMAT ="([a-zA-Z][0-9a-zA-Z_]*)";
    public static final String NAME_TERMINATOR ="[^0-9a-zA-Z_]";
    private static final String[] NO_EXPRESSIONS =new String[0];
    private static final String NO_EXPRESSION="";

    private final String labelOrPointer,includeName, includePath,line,sanitizedLine,mnemonic,directive;
    private final int lineNumber;

    private boolean listing=false;

    private boolean enabled=true,processed;
    private String evaluatedArguments,arguments;

    public Line(String includePath,String includeName,int lineNumberInFile,String lineContent,boolean shouldList) throws CompilerException{
        line=lineContent;
        sanitizedLine=sanitizeLine(lineContent);

        mnemonic=getMnemonic(sanitizedLine);
        directive=getDirectiveName(sanitizedLine);
        if(directive!=null && mnemonic!=null){
            throw new CompilerException("Invalid line: " +includePath+" "+lineContent);
        }
        labelOrPointer =getLabelOrPointerName(sanitizedLine);
        arguments=getExpressionsString(sanitizedLine);
        lineNumber=lineNumberInFile;
        this.includePath =includePath;
        this.includeName =includeName;
        listing = shouldList;
    }

    public Line(Line toClone,boolean keepStatus){
        includeName =toClone.includeName;
        includePath =toClone.includePath;
        line=toClone.line;
        labelOrPointer=toClone.labelOrPointer;
        sanitizedLine=toClone.sanitizedLine;
        lineNumber=toClone.lineNumber;
        mnemonic=toClone.mnemonic;
        directive=toClone.directive;
        arguments=toClone.arguments;
        if(keepStatus){
            enabled=toClone.enabled;
            processed=toClone.processed;
            evaluatedArguments=toClone.evaluatedArguments;
            listing=toClone.listing;
        }
    }

    public void setListing(boolean listing) {
        this.listing = listing;
    }

    public boolean isListing() {
        return listing;
    }

    public String getLine() {
        return line;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public String getIncludePath() {
        return includePath;
    }

    public String getIncludeName() {
        return includeName;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setProcessed(boolean processed) {
        this.processed = processed;
    }

    public boolean isProcessed() {
        return processed;
    }

    public String getLabelOrPointerName() {
        return labelOrPointer;
    }

    public String getMnemonic() {
        return mnemonic;
    }

    public String getDirectiveName() {
        return directive;
    }

    public String getTextArguments() {
        return arguments;
    }

    public void setArguments(String arguments) {
        this.arguments = arguments;
        this.evaluatedArguments=null;
    }

    public String[] getTextArgumentArray() {
        return splitExpressionsString(arguments);
    }

    public void setEvaluatedArguments(String evaluatedArguments) {
        this.evaluatedArguments = evaluatedArguments;
    }

    public String getEvaluatedArguments() {
        return evaluatedArguments;
    }

    public String[] getEvaluatedArgumentArray() {
        return splitExpressionsString(evaluatedArguments);
    }

    public String getLatestArguments() {
        return evaluatedArguments==null? arguments:evaluatedArguments;
    }

    public String[] getLatestArgumentArray() {
        return splitExpressionsString(evaluatedArguments == null ? arguments : evaluatedArguments);
    }

    private static boolean containsLabelOrPointerName(String line){
        return line.matches('^'+ NAME_FORMAT +":.*$");
    }
    private static String getLabelOrPointerName(String line){
        return containsLabelOrPointerName(line)?line.replaceFirst(":.*$",""):null;
    }
    private static boolean containsDirectiveName(String line){
        return line.matches("^(?:"+NAME_FORMAT+":)?\\."+NAME_FORMAT+"(?: .*)?$");
    }
    private static String getDirectiveName(String line){
        return containsDirectiveName(line)?line.replaceFirst("^(?:"+NAME_FORMAT+":)?\\.","").replaceFirst(" .*$",""):null;
    }
    private static boolean containsMnemonic(String line){//either macro or operand
        return line.matches("^(?:"+NAME_FORMAT+":)?"+NAME_FORMAT+"(?: .*)?$");
    }
    private static String getMnemonic(String line){//either macro or operand
        return containsMnemonic(line)?line.replaceFirst("^(?:"+NAME_FORMAT+":)?","").replaceFirst(" .*$",""):null;
    }
    private static String getExpressionsString(String line){
        if(containsMnemonic(line) || containsDirectiveName(line)){
            return line.replaceFirst("^(?:"+NAME_FORMAT+":)?\\.?"+NAME_FORMAT+" ?","");
        }else{
            return NO_EXPRESSION;
        }
    }
    private static String[] splitExpressionsString(String expressions){
        if(expressions==null || expressions.length()==0){
            return NO_EXPRESSIONS;
        }
        if(expressions.contains("`")) {
            return expressions.split("`");
        }
        if(expressions.contains("(") || expressions.contains("[")){
            return expressions.split("`");
        }
        return expressions.split(",");
    }

    public static String sanitizeLine(String line){
        return line
                .replaceAll("[\\r\\n]","")
                .replaceAll("[\\s]+"," ")
                .replaceFirst(" *;.*$","")
                .replaceFirst(" *\\\\\\\\.*$","")
                .replaceFirst("^ ","")
                .replaceFirst(" $","")
                .replaceAll(" *: *",":")
                .replaceAll(" *\\. *",".")
                .replaceAll(" *, *",",")
                .replaceAll(" *` *","`");
    }

    @Override
    public String toString() {
        return lineNumber+":"+getLine();
    }
}
