package com.sv.bigfile;

import com.sv.core.Utils;

import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;

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

        StringBuilder sb = new StringBuilder("File created on " + Utils.getFormattedDate());
        int MB_14_LINES = 100000;
        int GB_1_LINES = MB_14_LINES * 65;
        int GB_3_LINES = MB_14_LINES * 200;
        int APPEND_AFTER = 100000;
        long time;

        String fnsf = "test-file-single-line.txt";
        if (!Files.exists(Utils.createPath(fnsf))) {
            Utils.writeFile(fnsf, sb.toString(), null, StandardOpenOption.APPEND);
            time = System.currentTimeMillis();
            sb = new StringBuilder();
            for (int i = 0; i < MB_14_LINES / 2; i++) {
                sb.append(line1).append(i);
                sb.append(line2).append(i);
                sb.append(line3).append(i);
                if (i % APPEND_AFTER == 0) {
                    Utils.writeFile(fnsf, sb.toString(), null, StandardOpenOption.APPEND);
                    sb = new StringBuilder();
                }
            }
            Utils.writeFile(fnsf, sb.toString(), null, StandardOpenOption.APPEND);
            System.out.println("File [" + fnsf + "] created in " + Utils.getTimeDiffSecStr(time) + " of size "
                    + Utils.getFileSizeString(fnsf));
        } else {
            System.out.println("File [" + fnsf + "] already exists of size "
                    + Utils.getFileSizeString(fnsf));
        }

        String fn = "test-file-1GB.txt";
        String fn2 = "test-file-500MB.txt";
        String fn3 = "test-file-3GB.txt";
        sb = new StringBuilder("File created on " + Utils.getFormattedDate());
        Map<String, Integer> files = new HashMap<>();
        files.put(fn, GB_1_LINES);
        files.put(fn2, MB_14_LINES * 30);
        files.put(fn3, GB_3_LINES);

        for (Map.Entry<String, Integer> entry : files.entrySet()) {
            String k = entry.getKey();
            Integer v = entry.getValue();
            time = System.currentTimeMillis();
            if (!Files.exists(Utils.createPath(k))) {
                for (int i = 0; i < v; i++) {
                    sb.append(ln).append(line1).append(i);
                    sb.append(ln).append(line2).append(i);
                    sb.append(ln).append(line3).append(i);
                    if (i % APPEND_AFTER == 0) {
                        Utils.writeFile(fn, sb.toString(), null, StandardOpenOption.APPEND);
                        sb = new StringBuilder();
                    }
                }
                Utils.writeFile(k, sb.toString(), null, StandardOpenOption.APPEND);
                System.out.println("File [" + k + "] created in " + Utils.getTimeDiffSecStr(time) + " of size "
                        + Utils.getFileSizeString(k));
            } else {
                System.out.println("File [" + k + "] already exists of size "
                        + Utils.getFileSizeString(k));
            }
        }
    }

}
