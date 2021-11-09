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
        String line1 = ln + "This is file is created by Shailendra Verma. Line number # ";
        String line2 = ln + "This is test line used for creating big file # ";
        String line3 = ln + "I am enjoying this # ";

        String fn = "test-big-file.txt";
        StringBuilder sb = new StringBuilder("File created on " + Utils.getFormattedDate());
        Utils.writeFile(fn, sb.toString(), null
                , StandardOpenOption.TRUNCATE_EXISTING);
        sb = new StringBuilder();
        int MB_14_LINES = 100000;
        int APPEND_AFTER = 100000;
        long time = System.currentTimeMillis();
        for (int i = 0; i < MB_14_LINES * 100; i++) {
            sb.append(line1).append(i);
            sb.append(line2).append(i);
            sb.append(line3).append(i);
            if (i % APPEND_AFTER == 0) {
                Utils.writeFile(fn, sb.toString(), null, StandardOpenOption.APPEND);
                sb = new StringBuilder();
            }
        }
        Utils.writeFile(fn, sb.toString(), null, StandardOpenOption.APPEND);
        System.out.println("File created in " + Utils.getTimeDiffSecStr(time) + " of size "
                + Utils.getFileSizeString(fn));
    }

}
