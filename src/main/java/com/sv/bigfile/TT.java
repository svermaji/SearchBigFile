package com.sv.bigfile;

import com.sv.core.Utils;

public class TT {
    public static void main(String[] args) {
        new TT().x();
    }

    public void x() {
        String[] arr = new String[]{
                "THIS IS TEST",
                /*"test this is",
                "this is test testmantest",
                "(test)",
                "test)",
                "(test",
                " test ",
                ":test;",*/
                "this is test; test, test)"
        };
        String s = "test";
        for (String a : arr) {
            int idx = a.indexOf(s);
            while (idx != -1) {
                System.out.println(a + "---" + checkForWholeWord(s, a, idx));
                idx += s.length();
                idx = a.indexOf(s, idx);
            }
        }
    }

    private boolean checkForWholeWord(String strToSearch, String line, int idx) {
        System.out.println("strToSearch = " + strToSearch + ", line = " + line + ", idx = " + idx);
        System.out.println("line len = " + line.length() + ", strToSearch.length() = " + strToSearch.length());
        int searchLen = strToSearch.length();
        int lineLen = line.length();
        // starts with case
        if (idx == 0) {
            if (lineLen == idx + searchLen) {
                System.out.println(".....1");
                return true;
            } else if (lineLen >= idx + searchLen + 1) {
                System.out.println(".....2--"+line.charAt(idx + searchLen));
                return Utils.isWholeWordChar(line.charAt(idx + searchLen));
            }
        } else if (idx == lineLen - searchLen) { // ends with case
            if (idx > 0) {
                System.out.println(".....3");
                return Utils.isWholeWordChar(line.charAt(idx - 1));
            }
        } else if (idx != -1 && lineLen > idx + searchLen) { // + 2 is for char before and after
            // in between
            System.out.println(".....4");
            return Utils.isWholeWordChar(line.charAt(idx + searchLen)) && Utils.isWholeWordChar(line.charAt(idx - 1));
        }
        System.out.println(".....5");
        return false;
    }

}
