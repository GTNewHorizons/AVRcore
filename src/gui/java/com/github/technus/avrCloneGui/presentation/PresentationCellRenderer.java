package com.github.technus.avrCloneGui.presentation;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

public class PresentationCellRenderer extends DefaultTableCellRenderer {
    public Presentation[] presentations;
    private static Color numFG =new Color(104, 151, 187);

    public PresentationCellRenderer(int columnCount,Presentation defaultPresentation){
        presentations=new Presentation[columnCount];
        for (int i=0;i<columnCount;i++){
            presentations[i]=defaultPresentation;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        if(table.getRowCount()>row+1){
            Object value2=table.getValueAt(row+1,column);
            value= presentations[column].present(value,value2);
        }else{
            value=presentations[column].present(value);
        }
        Component component=super.getTableCellRendererComponent(table,value,isSelected,hasFocus,row,column);
        if(table.getColumnClass(column)==Integer.class){
            component.setForeground(numFG);
        }
        return component;
    }

    @Override
    protected void setValue(Object value) {
        setText((value == null) ? null : value.toString());
    }
}
