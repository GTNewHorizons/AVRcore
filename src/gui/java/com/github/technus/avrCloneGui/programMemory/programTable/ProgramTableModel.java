package com.github.technus.avrCloneGui.programMemory.programTable;

import com.github.technus.avrClone.AvrCore;
import com.github.technus.avrClone.instructions.IInstruction;
import com.github.technus.avrCloneGui.programMemory.IRefreshProgramMemoryView;

import javax.swing.event.EventListenerList;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

public class ProgramTableModel implements TableModel,IRefreshProgramMemoryView {
    protected EventListenerList listenerList = new EventListenerList();

    private final AvrCore core;

    public ProgramTableModel(AvrCore core) {
        this.core=core;
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        switch (columnIndex) {
            case 0: return Integer.class;//addr
            case 1: return String.class;//instruction
            case 2: return Integer.class;//op0
            case 3: return Integer.class;//op1
        }
        return null;
    }

    @Override
    public String getColumnName(int columnIndex) {
        switch (columnIndex) {
            case 0: return "Address";//value
            case 1: return "OPCODE";//addr
            case 2: return "Value 0";//value
            case 3: return "Value 1";//value
        }
        return null;
    }

    @Override
    public int getColumnCount() {
        return 4;
    }

    @Override
    public int getRowCount() {
        return core.getProgramMemory()==null?0:core.getProgramMemory().instructions.length;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        switch (columnIndex) {
            case 0:
                return rowIndex;
            case 1:
                return core.getProgramMemory()==null?"":core.getInstruction(rowIndex).name();
            case 2:{
                if (core.getProgramMemory() == null) {
                    return null;
                }
                int val=core.getProgramMemory().param0[rowIndex];
                if(core.getInstruction(rowIndex).getOperandCount()>0) {
                    return val;
                }
                return val==0?null:val;
            }
            case 3:{
                if (core.getProgramMemory() == null) {
                    return null;
                }
                int val=core.getProgramMemory().param1[rowIndex];
                if(core.getInstruction(rowIndex).getOperandCount()>1) {
                    return val;
                }
                return val==0?null:val;
            }
        }
        return null;
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        switch (columnIndex) {
            case 0:return;
            case 1:{
                if(core.getProgramMemory()==null){
                    return;
                }
                if(aValue instanceof Integer){
                    core.getProgramMemory().instructions[rowIndex]=(Integer) aValue;
                    refreshProgramMemoryView();
                    IInstruction instruction=core.getInstruction(rowIndex);
                    if(instruction.getOperandCount()>0){
                        core.getProgramMemory().param0[rowIndex]=instruction.getLimit0().clamp((Integer) aValue,core.programCounter,core.isUsingImmersiveOperands());
                    }else{
                        core.getProgramMemory().param0[rowIndex]=0;
                    }
                    if(instruction.getOperandCount()>1){
                        core.getProgramMemory().param1[rowIndex]=instruction.getLimit1().clamp((Integer) aValue,core.programCounter,core.isUsingImmersiveOperands());
                    }else{
                        core.getProgramMemory().param1[rowIndex]=0;
                    }
                }else{
                    aValue=core.getProgramMemory().instructions[rowIndex]=core.getInstructionRegistry().getID(aValue.toString().toUpperCase());
                    refreshProgramMemoryView();
                    IInstruction instruction=core.getInstruction(rowIndex);
                    if(instruction.getOperandCount()>0){
                        core.getProgramMemory().param0[rowIndex]=instruction.getLimit0().clamp((Integer) aValue,core.programCounter,core.isUsingImmersiveOperands());
                    }else{
                        core.getProgramMemory().param0[rowIndex]=0;
                    }
                    if(instruction.getOperandCount()>1){
                        core.getProgramMemory().param1[rowIndex]=instruction.getLimit1().clamp((Integer) aValue,core.programCounter,core.isUsingImmersiveOperands());
                    }else{
                        core.getProgramMemory().param1[rowIndex]=0;
                    }
                }
                break;
            }
            case 2:{
                if(core.getProgramMemory()==null){
                    return;
                }
                IInstruction instruction=core.getInstruction(rowIndex);
                if(instruction.getOperandCount()>0){
                    core.getProgramMemory().param0[rowIndex]=instruction.getLimit0().clamp((Integer) aValue,core.programCounter,core.isUsingImmersiveOperands());
                }else{
                    core.getProgramMemory().param0[rowIndex]=0;
                }
                refreshProgramMemoryView();
                break;
            }
            case 3:{
                if(core.getProgramMemory()==null){
                    return;
                }
                IInstruction instruction=core.getInstruction(rowIndex);
                if(instruction.getOperandCount()>1){
                    core.getProgramMemory().param1[rowIndex]=instruction.getLimit1().clamp((Integer) aValue,core.programCounter,core.isUsingImmersiveOperands());
                }else{
                    core.getProgramMemory().param1[rowIndex]=0;
                }
                refreshProgramMemoryView();
                break;
            }
        }
    }

    @Override
    public void refreshProgramMemoryView() {
        // Guaranteed to return a non-null array
        Object[] listeners = listenerList.getListenerList();
        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == TableModelListener.class) {
                ((TableModelListener) listeners[i + 1]).tableChanged(new TableModelEvent(this));
            }
        }
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return columnIndex > 0;
    }

    @Override
    public void addTableModelListener(TableModelListener l) {
        listenerList.add(TableModelListener.class, l);
    }

    @Override
    public void removeTableModelListener(TableModelListener l) {
        listenerList.remove(TableModelListener.class, l);
    }
}