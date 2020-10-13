package com.sv.bigfile.helpers;

import com.sv.bigfile.SearchBigFile;

import javax.swing.*;

public class StartWarnIndicator extends SwingWorker<Integer, String> {

    private SearchBigFile sbf;

    public StartWarnIndicator(SearchBigFile sbf) {
        this.sbf = sbf;
    }

    @Override
    public Integer doInBackground() {
        sbf.updateMsg(sbf.getProblemMsg(), sbf.getMsgType());
        return 1;
    }
}
