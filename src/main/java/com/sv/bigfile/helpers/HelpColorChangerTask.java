package com.sv.bigfile.helpers;

import com.sv.bigfile.SearchBigFile;

import java.util.TimerTask;

public class HelpColorChangerTask extends TimerTask {

    private final SearchBigFile sbf;

    public HelpColorChangerTask(SearchBigFile sbf) {
        this.sbf = sbf;
    }

    @Override
    public void run() {
        sbf.changeHelpColor();
    }
}
