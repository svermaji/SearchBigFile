package com.sv.bigfile;

import com.sv.core.Utils;

import java.util.ArrayList;
import java.util.List;

public enum UIName {
    LBL_FILE("File", 'F'),
    BTN_FILE("...", 'O', "Select file from browser."),
    BTN_LOCK("<html>&#x1F512;</html>", 'K', "Lock the screen"),
    BTN_CHNG_PWD("<html>&#x1F511;</html>", 'G', "Change password"),
    LBL_RFILES("Recent", 'R', "Recent used files list."),
    BTN_UC("U", 'u', "Make search string to upper case."),
    BTN_SHOWALL("Show/Hide All", 'q', "Show/Hide all search occurrences."),
    BTN_LC("L", 'y', "Make search string to lower case."),
    BTN_TC("T", 'p', "Make search string to title case."),
    BTN_IC("V", 'k', "Invert case of search string."),
    BTN_LISTRF("Ý", 'L', "Search recently used file list."),
    BTN_PLUSFONT("+", '=', "Increase font size for results, max [" + AppConstants.MAX_FONT_SIZE + "]."),
    BTN_MINUSFONT("—", '-', "Decrease font size for results, min [" + AppConstants.MIN_FONT_SIZE + "]."),
    BTN_RESETFONT("", '0', "Text shows present font size of Results.  Click to reset to [" + AppConstants.DEFAULT_FONT_SIZE + "]."),
    BTN_GOTOP("↑", '1', "Ctrl+Home", "Go to first line."),
    BTN_GOBOTTOM("↓", '2', "Ctrl+End", "Go to last line."),
    BTN_PREOCCR("<", ',', "Shift+F3", "Previous occurrence."),
    BTN_NEXTOCCR(">", '.', "F3", "Next occurrence."),
    BTN_FIND("¤", 'z', "Set new word from Search box to see occurrences without highlighting."),
    BTN_HELP("?", '/', "Application help."),
    BTN_HBROWSER("O", ';', "Open help in browser."),
    BTN_EXPORT("×", '9', "Export result."),
    BTN_CLEANEXPORT("ø", '8', "Clean old export results."),
    JCB_MATCHCASE("Aa", 'a', "Match case."),
    JCB_WHOLEWORD("W", 'w', "Whole word."),
    LBL_SEARCH("Search", 'H'),
    BTN_SEARCH("Search", 'S'),
    LBL_LASTN("Last N", 'N'),
    BTN_LASTN("Read", 'e', "Read last N lines and highlight."),
    LBL_RSEARCHES("Recent", 't', "Recently used search-patterns list."),
    MNU_SETTINGS("*", 'b', "Settings, present highlight color is shown as background."),
    BTN_LISTRS("Ý", 'I', "Search recently used search-patterns list."),
    BTN_CANCEL("", 'C', "Cancel/Stop Search/Read");

    String name, tip;
    char mnemonic;
    List<String> keys;

    UIName(String name, char mnemonic) {
        this(name, mnemonic, null);
    }

    UIName(String name, char mnemonic, String tip) {
        this.name = name;
        this.tip = tip;
        this.mnemonic = mnemonic;
    }

    UIName(String name, char mnemonic, String addlKey, String tip) {
        this(name, mnemonic, tip);
        keys = new ArrayList<>();
        keys.add(mnemonic + "");
        if (Utils.hasValue(addlKey)) {
            keys.add(addlKey);
        }
    }
}
