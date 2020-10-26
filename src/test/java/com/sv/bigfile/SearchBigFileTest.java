package com.sv.bigfile;

public class SearchBigFileTest {

    SearchBigFile sbf;

    public static void main(String[] args) {
        new SearchBigFileTest(new SearchBigFile()).testWholeWord();
    }

    public SearchBigFileTest(SearchBigFile sbf) {
        this.sbf = sbf;
    }

    public void testWholeWord() {
        String[] arr = new String[]{
                "THIS IS TEST",
                "test this is",
                "this is test man",
                "(test)",
                "test)",
                "(test",
                " test ",
                ":test;",
                "this is test; test, test)"
        };

        String s = "test";
        for (String a : arr) {
            int idx = a.indexOf(s);
            while (idx != -1) {
                assert (sbf.checkForWholeWord(s, a, idx));
                idx += s.length();
                idx = a.indexOf(s, idx);
            }
        }
    }

}
