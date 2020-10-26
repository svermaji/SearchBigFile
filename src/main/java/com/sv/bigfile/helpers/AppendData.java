package com.sv.bigfile.helpers;

import com.sv.bigfile.SearchBigFile;

import javax.swing.*;

public class AppendData extends SwingWorker<Integer, String> {

    private SearchBigFile sbf;

    public AppendData(SearchBigFile sbf) {
        this.sbf = sbf;
    }

    @Override
    public Integer doInBackground() {
        synchronized (AppendData.class) {
            sbf.incRCtrNAppendIdxData();
        }
        return 1;
    }
}

