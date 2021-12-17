package com.github.technus.avrCloneGui.dataMemory.dataTable;

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

public class DataTableModel extends DataTableModelAbstract {
    protected EventListenerList listenerList = new EventListenerList();

    public DataTableModel(AvrCore core, ArrayList<IRefreshDataMemoryView> dataMemoryViews){
        super(core,dataMemoryViews);
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        switch (columnIndex){
            case 0: return String.class;//name
            case 1: return String.class;//name
            case 2: return Integer.class;//addr
            case 3: return Integer.class;//value
        }
        return null;
    }

    @Override
    public String getColumnName(int columnIndex) {
        switch (columnIndex){
            case 0: return "Package";//addr
            case 1: return "Name";//addr
            case 2: return "Address";//addr
            case 3: return "Value";//value
        }
        return null;
    }

    @Override
    public int getColumnCount() {
        return 4;
    }

    @Override
    public int getRowCount() {
        return core.dataMemory==null?0:core.dataMemory.length;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        if(core.dataMemory==null){
            return columnIndex==2?rowIndex:null;
        }
        switch (columnIndex){
            case 0:return core.getPackageName(rowIndex);
            case 1:{
                List<? extends IRegister> registers = core.getDataDefinitions(rowIndex);
                if(registers==null){
                    return null;
                }
                if(registers.size()==1){
                    return registers.get(0).name();
                }
                return Arrays.toString(registers.stream().map(IRegister::name).toArray());
            }
            case 2:return rowIndex;
            case 3:return core.isDataAddressValid(rowIndex) ? core.dataMemory[rowIndex] : null;
        }
        return null;
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        switch (columnIndex){
            case 0:case 1:case 2:return;
            case 3:{
                if(core.dataMemory!=null && core.isDataAddressValid(rowIndex)) {
                    core.dataMemory[rowIndex] = (Integer) aValue;
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
        return columnIndex==3;
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
