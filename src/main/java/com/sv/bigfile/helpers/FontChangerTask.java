package com.sv.bigfile.helpers;

import com.sv.bigfile.SearchBigFile;

import java.util.TimerTask;

public class FontChangerTask extends TimerTask {

    private final SearchBigFile sbf;

    public FontChangerTask(SearchBigFile sbf) {
        this.sbf = sbf;
    }

    @Override
    public void run() {
        sbf.debug("Running font changer task");
        sbf.changeMsgFont();
    }
}
