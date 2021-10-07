package com.sv.bigfile.helpers;

import com.sv.bigfile.SearchBigFile;

import java.util.TimerTask;

public class MemoryTrackTask extends TimerTask {

    private final SearchBigFile sbf;

    public MemoryTrackTask(SearchBigFile sbf) {
        this.sbf = sbf;
    }

    @Override
    public void run() {
        sbf.debug("Running memory track task");
        sbf.trackMemory();
    }
}
