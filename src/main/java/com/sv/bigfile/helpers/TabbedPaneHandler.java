package com.sv.bigfile.helpers;

import com.sv.bigfile.SearchBigFile;
import com.sv.swingui.component.AppTabbedPane;
import com.sv.swingui.component.TabCloseComponent;

/**
 * Handler for events when tab removed from popup menu
 */
public class TabbedPaneHandler extends AppTabbedPane {

    private SearchBigFile sbf;

    public TabbedPaneHandler(SearchBigFile sbf) {
        this.sbf = sbf;
    }

    @Override
    public void tabClosed(AppTabbedPane pane, int removedTabIdx, String removedTabTitle) {
        sbf.tabRemoved(removedTabTitle, removedTabIdx);
    }

}

