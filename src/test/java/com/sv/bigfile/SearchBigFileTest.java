package com.sv.bigfile;

public class SearchBigFileTest {

    SearchBigFile sbf;

    public static void main(String[] args) {
        new SearchBigFileTest(new SearchBigFile()).test();
    }

    public SearchBigFileTest(SearchBigFile sbf) {
        this.sbf = sbf;
    }

    public void test() {
        testWholeWord();
        testOccrExcerpt();
        testRegEx();
        testSpaceSearch();
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

    public void testOccrExcerpt() {
        String[] arr = new String[]{
                "THIS IS TEST",
                "test this is",
                "this is test man",
                "(test)",
                "test)",
                "(test",
                " test() ",
                " test ",
                ":test;",
                "this is test; test, test)"
        };

        int EXCERPT_LIMIT=80;
        String s = "test(";
        for (String a : arr) {
            int idx = a.indexOf(s);
            while (idx != -1) {
                String st = sbf.getOccrExcerpt(s, a, idx, EXCERPT_LIMIT);
                //System.out.println("st = " + st);
                assert (st.length() > 0);
                idx += s.length();
                idx = a.indexOf(s, idx);
            }
        }
    }

    public void testRegEx() {
        String[] arr = new String[]{
                "<font style=\\\"background-color:yellow\\\">this is test( man</font>",
                "this is test( man",
                "this is test( test( test)"
        };

        String f = "test\\(";
        String r = "sv\\(";
        for (String a : arr) {
            System.out.println(a.replaceAll(f, r));
        }
    }

    public void testSpaceSearch() {
        String[] arr = new String[]{
                "a  b   c    d",
                "x   y    z"
        };

        String f = "    ";
        String r = "<font style=\"background-color:yellow\">SP</font>";
        for (String a : arr) {
            System.out.println(a.indexOf(f));
            System.out.println(a.replaceAll(f, r));
        }
    }

}
