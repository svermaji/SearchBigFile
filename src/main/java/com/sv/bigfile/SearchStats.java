package com.sv.bigfile;

public class SearchStats {

    long lineNum = 1, occurrences = 0;
    String line;

    public SearchStats(long lineNum, long occurrences, String line) {
        this.lineNum = lineNum;
        this.occurrences = occurrences;
        this.line = line;
    }

    public void setLine(String line) {
        this.line = line;
    }

    public void setLineNum(long lineNum) {
        this.lineNum = lineNum;
    }

    public void setOccurrences(long occurrences) {
        this.occurrences = occurrences;
    }

    public long getLineNum() {
        return lineNum;
    }

    public long getOccurrences() {
        return occurrences;
    }

    public String getLine() {
        return line;
    }

    @Override
    public String toString() {
        return "SearchStats{" +
                "lineNum=" + lineNum +
                ", occurrences=" + occurrences +
                '}';
    }
}
