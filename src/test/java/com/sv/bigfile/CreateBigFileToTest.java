package com.sv.bigfile;

import com.sv.core.Utils;

import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;

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

        String fn = "test-file-1GB.txt";
        String fn2 = "test-file-single-line.txt";
        String fn3 = "test-file-3GB.txt";
        String fn4 = "test-file-500MB.txt";
        StringBuilder sb = new StringBuilder("File created on " + Utils.getFormattedDate());
        String[] arr = {fn, fn2, fn3, fn4};
        StringBuilder finalSb = sb;
        Arrays.stream(arr).forEach(f -> {
            StandardOpenOption soo = Files.exists(Utils.createPath(f)) ?
                    StandardOpenOption.TRUNCATE_EXISTING : StandardOpenOption.CREATE;
            Utils.writeFile(f, finalSb.toString(), null, soo);
        });

        sb = new StringBuilder();
        int MB_14_LINES = 100000;
        int GB_1_LINES = MB_14_LINES * 65;
        int MB_500_LINES = MB_14_LINES * 30;
        int GB_3_LINES = MB_14_LINES * 200;
        int APPEND_AFTER = 100000;
        long time = System.currentTimeMillis();
        for (int i = 0; i < GB_1_LINES; i++) {
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
        sb = new StringBuilder();
        for (int i = 0; i < MB_14_LINES / 2; i++) {
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
        time = System.currentTimeMillis();
        sb = new StringBuilder();
        for (int i = 0; i < GB_3_LINES; i++) {
            sb.append(ln).append(line1).append(i);
            sb.append(ln).append(line2).append(i);
            sb.append(ln).append(line3).append(i);
            if (i % APPEND_AFTER == 0) {
                Utils.writeFile(fn3, sb.toString(), null, StandardOpenOption.APPEND);
                sb = new StringBuilder();
            }
        }
        Utils.writeFile(fn3, sb.toString(), null, StandardOpenOption.APPEND);
        System.out.println("File [" + fn3 + "] created in " + Utils.getTimeDiffSecStr(time) + " of size "
                + Utils.getFileSizeString(fn3));
        time = System.currentTimeMillis();
        sb = new StringBuilder();
        for (int i = 0; i < MB_500_LINES; i++) {
            sb.append(ln).append(line1).append(i);
            sb.append(ln).append(line2).append(i);
            sb.append(ln).append(line3).append(i);
            if (i % APPEND_AFTER == 0) {
                Utils.writeFile(fn4, sb.toString(), null, StandardOpenOption.APPEND);
                sb = new StringBuilder();
            }
        }
        Utils.writeFile(fn4, sb.toString(), null, StandardOpenOption.APPEND);
        System.out.println("File [" + fn4 + "] created in " + Utils.getTimeDiffSecStr(time) + " of size "
                + Utils.getFileSizeString(fn4));
    }

}
