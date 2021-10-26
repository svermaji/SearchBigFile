package com.sv.bigfile.helpers;

import com.sv.bigfile.SearchBigFile;
import com.sv.swingui.component.AppTabbedPane;
import com.sv.swingui.component.TabCloseComponent;

public class TabRemoveHandler extends TabCloseComponent {

    private SearchBigFile sbf;

    public TabRemoveHandler(int tabNum, String title, AppTabbedPane pane, SearchBigFile sbf) {
        this(tabNum, title, true, pane, sbf);
    }

    public TabRemoveHandler(int tabNum, String title, boolean closable, AppTabbedPane pane, SearchBigFile sbf) {
        super(pane, tabNum, title, closable);
        this.sbf = sbf;
    }

    @Override
    public void tabRemoved() {
        sbf.debug("tabRemoved event received in tab remove handler");
        sbf.tabRemoved(getTitle(), getTabNum());
    }

    @Override
    public void tabSelected(AppTabbedPane pane, int tabNum) {
        sbf.debug("tabSelected event received in tab remove handler");
        sbf.tabSelected(pane, getTitle(), getTabNum());
    }
}

