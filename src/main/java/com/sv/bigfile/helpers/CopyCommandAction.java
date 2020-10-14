package com.sv.bigfile.helpers;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class CopyCommandAction extends AbstractAction {

    private final JTable table;
    private final JFrame frame;
    private final JTextField src;

    public CopyCommandAction(JTable table, JFrame frame, JTextField src) {
        this.table = table;
        this.frame = frame;
        this.src = src;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        src.setText(table.getValueAt(table.getSelectedRow(), 0).toString());
        frame.setVisible(false);
    }
}
