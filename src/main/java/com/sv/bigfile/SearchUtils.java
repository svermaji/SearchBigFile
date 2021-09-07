package com.sv.bigfile;

import com.sv.core.Constants;
import com.sv.core.Utils;
import com.sv.core.logger.MyLogger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class SearchUtils {

    private final MyLogger logger;

    public SearchUtils(MyLogger logger) {
        this.logger = logger;
    }

    public String getExportName () {
        String dt = Utils.getFormattedDate();
        dt = dt.replaceAll(Constants.COLON, Constants.DOT);
        return "./export_" + dt + ".txt";
    }

    public boolean exportResults (String text) {
        String fn = getExportName();
        logger.log(text);
        text = removeHtml(text);
        boolean result = true;
        try {
            Files.write(Utils.createPath(fn), text.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            result = false;
            logger.error("Unable to create file " + Utils.addBraces(fn), e);
        }
        return result;
    }

    private String removeHtml(String text) {
        String BODY_S = "<body>";
        String BODY_E = "</body>";
        text = unescape(text);
        text = text.replace("<span><font color=\"blue\">", "");
        text = text.replace("</font></span>", "");
        text = text.replace("&#160;", Utils.HtmlEsc.SP.getCh());
        text = text.replace("<br>", System.lineSeparator());

        if (text.contains(BODY_S) && text.contains(BODY_E)) {
            text = text.substring(text.indexOf(BODY_S) + BODY_S.length(), text.indexOf(BODY_E));
        }
        
        return text;
    }

    /**
     * This method is reverse of escape
     *
     * @param data String
     * @return escaped string
     */
    private String unescape(String data) {
        return data.replaceAll(Utils.HtmlEsc.SP.getEscStr(), Utils.HtmlEsc.SP.getCh());
    }

    /**
     * This method only converts spaces to &nbsp; till
     * first char comes
     *
     * @param data String
     * @return escaped string
     */
    private String escLSpaces(String data) {
        StringBuilder sb = new StringBuilder();
        int idx = 0;
        char[] arr = data.toCharArray();
        for (char c : arr) {
            if (Character.isWhitespace(c)) {
                sb.append(Utils.HtmlEsc.SP.getEscStr());
                idx++;
            } else {
                break;
            }
        }
        return sb.toString() + data.substring(idx);
    }

    public String escString(String str) {
        return htmlEsc(escLSpaces(str));
    }

    public String htmlEsc(String str) {
        return str.replaceAll(Utils.HtmlEsc.LT.getCh(), Utils.HtmlEsc.LT.getEscStr());
    }

}
