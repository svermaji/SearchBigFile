package com.sv.bigfile.task;

import com.sv.bigfile.SearchBigFile;

import java.util.TimerTask;

public class ReloadLastTabsTask extends TimerTask {

    private final SearchBigFile sbf;

    public ReloadLastTabsTask(SearchBigFile sbf) {
        this.sbf = sbf;
    }

    @Override
    public void run() {
        sbf.debug("Running open file task");
        sbf.reloadLastTabs();
    }
}
