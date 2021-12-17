package com.github.technus.avrCloneGui.dataMemory.dataTable;

import com.github.technus.avrCloneGui.AvrTest;
import com.github.technus.avrCloneGui.presentation.PresentationCellRenderer;
import com.github.technus.avrCloneGui.presentation.Presentations;

import javax.swing.*;

public class DataTablePopup extends JPopupMenu {
    public DataTablePopup(AvrTest test, JTable table, DataTableModel model, PresentationCellRenderer renderer){
        for(Presentations p:Presentations.values()){
            JMenuItem item=new JMenuItem(p.name());
            item.addActionListener(e -> {
                int[] c= table.getSelectedColumns();
                for (int i:c){
                    renderer.presentations[i]=p;
                }
                if(c.length>0){
                    model.refreshDataMemoryView();
                }
            });
            add(item);
        }
    }
}
