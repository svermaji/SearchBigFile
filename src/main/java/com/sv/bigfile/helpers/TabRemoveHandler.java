package com.sv.bigfile.helpers;

import com.sv.bigfile.SearchBigFile;
import com.sv.core.Utils;
import com.sv.swingui.component.TabCloseComponent;

import javax.swing.*;

public class TabRemoveHandler extends TabCloseComponent {

    private SearchBigFile sbf;

    public TabRemoveHandler(int tabNum, String title, JTabbedPane pane, SearchBigFile sbf) {
        super(pane, tabNum, title);
        this.sbf = sbf;
    }

    @Override
    public void tabRemoved() {
        sbf.tabRemoved(getTitle(), getTabNum());
    }
}

