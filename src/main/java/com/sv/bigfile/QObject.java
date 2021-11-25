package com.sv.bigfile;

import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

public class QObject {
    private SearchBigFile.Status threadStatus = SearchBigFile.Status.NOT_STARTED;
    private final int threadNum;
    private final long filePositionStart, filePositionEnd;
    private final Queue<String> qMsgsToAppend;

    public QObject(int threadNum, long filePositionStart, long filePositionEnd) {
        this.threadNum = threadNum;
        this.filePositionEnd = filePositionEnd;
        this.filePositionStart = filePositionStart;
        this.qMsgsToAppend = new LinkedBlockingQueue<>();
    }

    public void add(String data) {
        qMsgsToAppend.add(data);
    }

    public SearchBigFile.Status getThreadStatus() {
        return threadStatus;
    }

    public void setThreadStatus(SearchBigFile.Status threadStatus) {
        this.threadStatus = threadStatus;
    }

    public boolean isThreadCompleted() {
        return SearchBigFile.Status.DONE == threadStatus;
    }

    public boolean isThreadProcessed() {
        return SearchBigFile.Status.PROCESSED == threadStatus;
    }

    public int getThreadNum() {
        return threadNum;
    }

    public long getFilePositionStart() {
        return filePositionStart;
    }

    public long getFilePositionEnd() {
        return filePositionEnd;
    }

    public Queue<String> getQMsgsToAppend() {
        return qMsgsToAppend;
    }

    @Override
    public String toString() {
        return "QObject{" +
                "threadStatus=" + threadStatus +
                ", filePositionStart=" + filePositionStart +
                ", filePositionEnd=" + filePositionEnd +
                ", threadNum=" + threadNum +
                '}';
    }
}
