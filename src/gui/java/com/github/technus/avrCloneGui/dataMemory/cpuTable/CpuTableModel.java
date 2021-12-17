package com.github.technus.avrCloneGui.dataMemory.cpuTable;

import com.github.technus.avrClone.AvrCore;
import com.github.technus.avrClone.registerPackages.IRegister;
import com.github.technus.avrCloneGui.dataMemory.DataTableModelAbstract;
import com.github.technus.avrCloneGui.dataMemory.IRefreshDataMemoryView;

import javax.swing.event.EventListenerList;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CpuTableModel extends DataTableModelAbstract {
    protected EventListenerList listenerList = new EventListenerList();

    public CpuTableModel(AvrCore core, ArrayList<IRefreshDataMemoryView> data){
        super(core,data);
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        switch (columnIndex){
            case 0: return String.class;//name
            case 1: return Integer.class;//addr
            case 2: return Integer.class;//value
        }
        return null;
    }

    @Override
    public String getColumnName(int columnIndex) {
        switch (columnIndex){
            case 0: return "Name";//addr
            case 1: return "Address";//addr
            case 2: return "Value";//value
        }
        return null;
    }

    @Override
    public int getColumnCount() {
        return 3;
    }

    @Override
    public int getRowCount() {
        return core.getCpuRegisters()==null?0:core.getCpuRegisters().getSize();
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        switch (columnIndex){
            case 0:{
                List<? extends IRegister> registers = core.getCpuRegisters().addressesMap().get(core.getCpuRegisters().getOffset()+rowIndex);
                return registers==null?"":registers.size()==1?registers.get(0).name():Arrays.toString(registers.stream().map(IRegister::name).toArray());
            }
            case 1:return core.getCpuRegisters()==null?"+"+rowIndex:core.getCpuRegisters().getOffset()+rowIndex;
            case 2:return core.getCpuRegisters()==null?0:core.dataMemory[core.getCpuRegisters().getOffset()+rowIndex];
        }
        return null;
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        switch (columnIndex){
            case 0: case 1:return;
            case 2:{
                if(core.getCpuRegisters()!=null) {
                    core.dataMemory[core.getCpuRegisters().getOffset()+rowIndex] = (Integer) aValue;
                    refreshDataMemoryViewOfAllTables();
                }
            }
        }
    }

    @Override
    public void refreshDataMemoryView() {
        // Guaranteed to return a non-null array
        Object[] listeners = listenerList.getListenerList();
        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length-2; i>=0; i-=2) {
            if (listeners[i]==TableModelListener.class) {
                ((TableModelListener)listeners[i+1]).tableChanged(new TableModelEvent(this));
            }
        }
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return columnIndex==2;
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
