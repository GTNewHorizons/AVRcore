package com.github.technus.avrCloneGui.dataMemory;

import com.github.technus.avrClone.AvrCore;

import javax.swing.table.TableModel;
import java.util.ArrayList;

public abstract class DataTableModelAbstract implements TableModel,IRefreshDataMemoryView {
    public final ArrayList<IRefreshDataMemoryView> tablesToNotify;
    public final AvrCore core;
    protected DataTableModelAbstract(AvrCore core, ArrayList<IRefreshDataMemoryView> tablesToNotify){
        this.core=core;
        this.tablesToNotify=tablesToNotify;
    }

    public void refreshDataMemoryViewOfAllTables(){
        for (IRefreshDataMemoryView dataTableModel:tablesToNotify) {
            dataTableModel.refreshDataMemoryView();
        }
    }
}
