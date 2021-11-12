package com.sv.bigfile.task;

import com.sv.bigfile.SearchBigFile;

import java.util.TimerTask;

public class MemoryTrackTask extends TimerTask {

    private final SearchBigFile sbf;

    public MemoryTrackTask(SearchBigFile sbf) {
        this.sbf = sbf;
    }

    @Override
    public void run() {
        sbf.trackMemory();
    }
}
