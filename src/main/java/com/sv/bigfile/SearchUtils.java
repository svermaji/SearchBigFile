package com.sv.bigfile;

import com.sv.core.Constants;
import com.sv.core.Utils;
import com.sv.core.logger.MyLogger;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;

public class SearchUtils {

    private final MyLogger logger;
    private final String BODY_S = "<body>";
    private final String BODY_E = "</body>";

    public SearchUtils(MyLogger logger) {
        this.logger = logger;
    }

    public String getExportName() {
        String dt = Utils.getFormattedDate();
        dt = dt.replaceAll(Constants.COLON, Constants.DOT);
        return AppConstants.EXPORT_FILE_PREFIX + dt + AppConstants.EXPORT_FILE_EXTN;
    }

    public boolean cleanOldExportResults() {
        File folder = new File(".");
        String[] exportFiles = folder.list(
                (dir, fn) -> fn.startsWith(AppConstants.EXPORT_FILE_PREFIX)
                        && fn.endsWith(AppConstants.EXPORT_FILE_EXTN)
        );

        boolean result = true;
        if (exportFiles != null) {
            for (String exportFile : exportFiles) {
                try {
                    Files.deleteIfExists(Utils.createPath(exportFile));
                } catch (IOException e) {
                    if (result) {
                        result = false;
                    }
                    logger.error("Unable to delete file " + Utils.addBraces(exportFile), e);
                }
            }
        }

        return result;
    }

    public boolean exportResults(String text) {
        String fn = getExportName();
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
        // need to check this
        String ls = Constants.LN_BRK;

        text = text.replaceAll(" {4}", "");
        text = Utils.unescape(text);
        text = text.replaceAll("([\\r\\n])", "");
        text = text.replaceAll("<font color=\"#000000\">", "");
        text = text.replaceAll("</font>", "");
        text = text.replaceAll(" {2}</body></html>", "");
        text = text.replaceAll("&#160;", Utils.HtmlEsc.SP.getCh());
        text = text.replaceAll("<br>", ls);

        // handling line numbers
        String toRemove = "</span>";
        String[] allLines = text.split(Constants.LN_BRK_REGEX, -1);
        StringBuilder sb = new StringBuilder();
        for (String l : allLines) {
            if (l.contains(toRemove)) {
                sb.append(l.substring(l.indexOf(toRemove) + toRemove.length())).append(ls);
            }
        }

        text = sb.toString();
        text = text.replaceAll("<font color=\"#a52a2a\">", "");
        text = text.replaceAll(" " + ls, ls);
        if (text.contains(BODY_S) && text.contains(BODY_E)) {
            text = text.substring(text.indexOf(BODY_S) + BODY_S.length(), text.indexOf(BODY_E));
        }

        return text;
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
