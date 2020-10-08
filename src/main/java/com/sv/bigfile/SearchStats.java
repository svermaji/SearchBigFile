package com.sv.bigfile;

public class SearchStats {

    private long lineNum, occurrences;
    private String line;
    private boolean match;
    private final String searchPattern;

    public SearchStats(long lineNum, long occurrences, String line, String searchPattern) {
        this.lineNum = lineNum;
        this.occurrences = occurrences;
        this.line = line;
        this.searchPattern = searchPattern;
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

    public boolean isMatch() {
        return match;
    }

    public void setMatch(boolean match) {
        this.match = match;
    }

    public String getSearchPattern() {
        return searchPattern;
    }

    @Override
    public String toString() {
        return "SearchStats{" +
                "lineNum=" + lineNum +
                ", occurrences=" + occurrences +
                ", match=" + match +
                ", searchPattern='" + searchPattern + '\'' +
                '}';
    }
}
