package com.sv.bigfile;

public class SearchStats {

    private long lineNum, occurrences;
    private String line;
    private boolean match;
    private boolean addLineEnding;
    private final String searchPattern;
    private final Integer threadNum;

    public SearchStats(long lineNum, long occurrences, String line, String searchPattern) {
        this(lineNum, occurrences, line, searchPattern, null);
    }

    public SearchStats(long lineNum, long occurrences, String line, String searchPattern, Integer threadNum) {
        this.lineNum = lineNum;
        this.occurrences = occurrences;
        this.line = line;
        this.searchPattern = searchPattern;
        this.threadNum = threadNum;
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

    public boolean isAddLineEnding() {
        return addLineEnding;
    }

    public void setAddLineEnding(boolean addLineEnding) {
        this.addLineEnding = addLineEnding;
    }

    public int getThreadNum() {
        return threadNum;
    }

    @Override
    public String toString() {
        return "SearchStats{" +
                "lineNum=" + lineNum +
                ", occurrences=" + occurrences +
                ", line='" + line + '\'' +
                ", match=" + match +
                ", eofLine=" + addLineEnding +
                ", searchPattern='" + searchPattern + '\'' +
                '}';
    }
}
