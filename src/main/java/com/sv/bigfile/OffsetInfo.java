package com.sv.bigfile;

public class OffsetInfo {
    private Object obj;
    private int sIdx, eIdx;

    public OffsetInfo(Object obj, int sIdx, int eIdx) {
        this.obj = obj;
        this.sIdx = sIdx;
        this.eIdx = eIdx;
    }

    public void setObj(Object obj) {
        this.obj = obj;
    }

    public Object getObj() {
        return obj;
    }

    public int getSIdx() {
        return sIdx;
    }

    public int getEIdx() {
        return eIdx;
    }
}
