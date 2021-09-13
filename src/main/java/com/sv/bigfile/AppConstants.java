package com.sv.bigfile;

import com.sv.core.Constants;

import java.util.concurrent.TimeUnit;

public final class AppConstants {

    private AppConstants() {
    }

    public static final long HELP_COLOR_CHANGE_SEC = 10;
    public static final long HELP_COLOR_CHANGE_TIME = TimeUnit.SECONDS.toMillis(HELP_COLOR_CHANGE_SEC);
    public static final int PREFERRED_FONT_SIZE = 12;
    public static final int DEFAULT_FONT_SIZE = 12;
    public static final int MIN_FONT_SIZE = 8;
    public static final int MAX_FONT_SIZE = 28;
    public static final int RECENT_LIMIT = 20;
    public static final int SEARCH_STR_LEN_LIMIT = 2;
    public static final int WARN_LIMIT_SEC = 10;
    public static final int ERROR_LIMIT_SEC = 50;
    public static final int WARN_LIMIT_OCCR = 100;
    public static final int ERROR_LIMIT_OCCR = 500;
    public static final int APPEND_MSG_CHUNK = 100;

    public static final int MAX_RETRY_EXPORT_DEL = 3;
    public static final String EXPORT_FILE_PREFIX = "export_";
    public static final String EXPORT_FILE_EXTN = ".txt";
    public static final String EMPTY_RESULT_TEXT = "<html>  <head>  </head>  <body>    <p style=\"margin-top: 0\">          </p>  </body></html>";
}
