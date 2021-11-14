package com.sv.bigfile;

import com.sv.core.Constants;
import com.sv.core.Utils;

import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

public class Test {

    String s = "package com.sv.bigfile.helpers;\n" +
            "\n" +
            "import com.sv.bigfile.SearchBigFile;\n" +
            "\n" +
            "import javax.swing.*;\n" +
            "\n" +
            "public class AppendData extends SwingWorker<Integer, String> {\n" +
            "\n" +
            "    private SearchBigFile sbf;\n" +
            "\n" +
            "    public AppendData(SearchBigFile sbf) {\n" +
            "        this.sbf = sbf;\n" +
            "    }\n" +
            "\n" +
            "    @Override\n" +
            "    public Integer doInBackground() {\n" +
            "        synchronized (AppendData.class) {\n" +
            "            sbf.incRCtrNAppendIdxData();\n" +
            "        }\n" +
            "        return 1;\n" +
            "    }\n" +
            "}\n" +
            "\n";

    public static void main(String[] args) {
        new Test().test();
    }

    private void test() {
        System.out.println(s.split("\n", -1).length);
        String ss[] = s.split(Constants.LN_BRK_REGEX);
        for (String a : ss) {
            System.out.println(a);
        }
    }

}
