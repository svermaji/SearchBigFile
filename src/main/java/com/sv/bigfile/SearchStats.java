package com.sv.bigfile;

public class SearchStats {

    private long lineNum, occurrences;
    private String line;
    private boolean match, eofLine, sofFile = true;
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

    public boolean isEofLine() {
        return eofLine;
    }

    public void setEofLine(boolean eofLine) {
        this.eofLine = eofLine;
    }

    public boolean isSofFile() {
        return sofFile;
    }

    public void setSofFile(boolean sofFile) {
        this.sofFile = sofFile;
    }

    @Override
    public String toString() {
        return "SearchStats{" +
                "lineNum=" + lineNum +
                ", occurrences=" + occurrences +
                ", line='" + line + '\'' +
                ", match=" + match +
                ", eofLine=" + eofLine +
                ", sofFile=" + sofFile +
                ", searchPattern='" + searchPattern + '\'' +
                '}';
    }
}
