package com.sv.bigfile;

import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
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
}
