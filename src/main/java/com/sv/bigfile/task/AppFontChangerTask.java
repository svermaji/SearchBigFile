package com.sv.bigfile.task;

import com.sv.bigfile.SearchBigFile;

import java.util.TimerTask;

public class AppFontChangerTask extends TimerTask {

    private final SearchBigFile sbf;

    public AppFontChangerTask(SearchBigFile sbf) {
        this.sbf = sbf;
    }

    @Override
    public void run() {
        sbf.changeAppFont();
    }
}
