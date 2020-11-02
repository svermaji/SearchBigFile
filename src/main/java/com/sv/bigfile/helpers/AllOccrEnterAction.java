package com.sv.bigfile.helpers;

import com.sv.bigfile.SearchBigFile;
import com.sv.swingui.component.table.AppTable;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class AllOccrEnterAction extends AbstractAction {

    private final AppTable table;
    private final SearchBigFile sbf;

    public AllOccrEnterAction(AppTable table, SearchBigFile sbf) {
        this.table = table;
        this.sbf = sbf;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        sbf.dblClickOffset(table, null);
    }
}
