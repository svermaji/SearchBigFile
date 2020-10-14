package com.sv.bigfile;

import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.util.concurrent.TimeUnit;

public final class Constants {

    private Constants() {
    }

    public static final String PREFERRED_FONT = "Calibri";
    public static final long FONT_CHANGE_MIN = 10;
    public static final long FONT_CHANGE_TIME = TimeUnit.MINUTES.toMillis(FONT_CHANGE_MIN);
    public static final int PREFERRED_FONT_SIZE = 12;
    public static final int DEFAULT_FONT_SIZE = 12;
    public static final int MIN_FONT_SIZE = 8;
    public static final int MAX_FONT_SIZE = 24;
    public static final int RECENT_LIMIT = 20;
    public static final int SEARCH_STR_LEN_LIMIT = 2;
    public static final int WARN_LIMIT_SEC = 10;
    public static final int FORCE_STOP_LIMIT_SEC = 50;
    public static final int WARN_LIMIT_OCCR = 100;
    public static final int FORCE_STOP_LIMIT_OCCR = 500;
    public static final int APPEND_MSG_CHUNK = 100;
    public static final int EB = 5;
    public static final Border EMPTY_BORDER = new EmptyBorder(new Insets(EB, EB, EB, EB));
    public static final Border BLUE_BORDER = new LineBorder(Color.BLUE, 1, true);
}
