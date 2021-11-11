package com.sv.bigfile;

import com.sv.core.Utils;

import java.nio.file.StandardOpenOption;

public class CreateBigFileToTest {

    public static void main(String[] args) {
        new CreateBigFileToTest().test();
    }

    private void test() {
        createBigFile();
    }

    private void createBigFile() {
        String ln = System.lineSeparator();
        String line1 = "This is file is created by Shailendra Verma. Line number # ";
        String line2 = "This is test line used for creating big file # ";
        String line3 = "I am enjoying this # ";

        String fn = "test-big-file.txt";
        String fn2 = "test-single-line-big-file.txt";
        StringBuilder sb = new StringBuilder("File created on " + Utils.getFormattedDate());
        Utils.writeFile(fn, sb.toString(), null, StandardOpenOption.TRUNCATE_EXISTING);
        Utils.writeFile(fn2, sb.toString(), null, StandardOpenOption.TRUNCATE_EXISTING);
        sb = new StringBuilder();
        int MB_14_LINES = 100000;
        int GB_1_5_LINES = MB_14_LINES * 100;
        int APPEND_AFTER = 100000;
        long time = System.currentTimeMillis();
        for (int i = 0; i < GB_1_5_LINES; i++) {
            sb.append(ln).append(line1).append(i);
            sb.append(ln).append(line2).append(i);
            sb.append(ln).append(line3).append(i);
            if (i % APPEND_AFTER == 0) {
                Utils.writeFile(fn, sb.toString(), null, StandardOpenOption.APPEND);
                sb = new StringBuilder();
            }
        }
        Utils.writeFile(fn, sb.toString(), null, StandardOpenOption.APPEND);
        System.out.println("File [" + fn + "] created in " + Utils.getTimeDiffSecStr(time) + " of size "
                + Utils.getFileSizeString(fn));
        time = System.currentTimeMillis();
        for (int i = 0; i < MB_14_LINES * 3; i++) {
            sb.append(line1).append(i);
            sb.append(line2).append(i);
            sb.append(line3).append(i);
            if (i % APPEND_AFTER == 0) {
                Utils.writeFile(fn2, sb.toString(), null, StandardOpenOption.APPEND);
                sb = new StringBuilder();
            }
        }
        Utils.writeFile(fn2, sb.toString(), null, StandardOpenOption.APPEND);
        System.out.println("File [" + fn2 + "] created in " + Utils.getTimeDiffSecStr(time) + " of size "
                + Utils.getFileSizeString(fn2));
    }

}
