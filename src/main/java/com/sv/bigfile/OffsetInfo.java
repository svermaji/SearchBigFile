package com.sv.bigfile;

public class OffsetInfo {

    private long fileLineNum;
    private String excerpt;
    private int num, offset;

    public OffsetInfo(long fileLineNum, int offset, String excerpt, int num) {
        this.fileLineNum = fileLineNum;
        this.offset = offset;
        this.excerpt = excerpt;
        this.num = num;
    }

    public String getExcerpt() {
        return excerpt;
    }

    public long getFileLineNum() {
        return fileLineNum;
    }

    public int getNum() {
        return num;
    }

    public int getOffset() {
        return offset;
    }

    @Override
    public String toString() {
        return "OffsetInfo{" +
                "fileLineNum=" + fileLineNum +
                ", num=" + num +
                ", offset=" + offset +
                '}';
    }
}
