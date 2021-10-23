package com.sv.bigfile.task;

import com.sv.bigfile.SearchBigFile;

import java.util.TimerTask;

public class StartClipboardTask extends TimerTask {

    private final SearchBigFile sbf;

    public StartClipboardTask(SearchBigFile sbf) {
        this.sbf = sbf;
    }

    @Override
    public void run() {
        sbf.debug("Starting clipboard action");
        sbf.copyClipboard(sbf.getLogger());
    }
}
