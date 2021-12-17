package com.github.technus.avrCloneGui.registerFile.registerTable;

import com.github.technus.avrClone.AvrCore;
import com.github.technus.avrCloneGui.registerFile.IRefreshRegisterMemoryView;

import javax.swing.event.EventListenerList;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

public class RegisterTableModel implements TableModel,IRefreshRegisterMemoryView {
    protected EventListenerList listenerList = new EventListenerList();
    private final AvrCore core;

    public RegisterTableModel(AvrCore core){
        this.core=core;
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
        return core.registerFile==null?0:core.registerFile.length;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        switch (columnIndex){
            case 0: {
                String name="R" + rowIndex;
                switch (rowIndex) {
                    case 24:name+=" W";break;
                    case 26:name+=" X";break;
                    case 28:name+=" Y";break;
                    case 30:name+=" Z";break;
                }
                return name;
            }
            case 1:return rowIndex;
            case 2:return core.registerFile==null?0:core.registerFile[rowIndex];
        }
        return null;
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        switch (columnIndex){
            case 0: case 1:return;
            case 2:{
                if(core.registerFile!=null) {
                    core.registerFile[rowIndex] = (Integer) aValue;
                    refreshRegisterMemoryView();
                }
            }
        }
    }

    public void refreshRegisterMemoryView() {
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
