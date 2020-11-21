package com.sv.bigfile;

public enum UIName {
    LBL_FILE("File", 'F'),
    BTN_FILE("...", 'O', "Select file from browser."),
    LBL_RFILES("Recent", 'R', "Recent used files list."),
    BTN_UC("U", 'u', "Make search string to upper case."),
    BTN_SHOWALL("Show/Hide All", 'q', "Show/Hide all search occurrences."),
    BTN_LC("L", 'y', "Make search string to lower case."),
    BTN_TC("T", 'p', "Make search string to title case."),
    BTN_IC("V", 'k', "Invert case of search string."),
    BTN_LISTRF("", 'L', "Search recently used file list."),
    BTN_PLUSFONT("+", '=', "Increase font size for results, max [" + AppConstants.MAX_FONT_SIZE + "]."),
    BTN_MINUSFONT("—", '-', "Decrease font size for results, min [" + AppConstants.MIN_FONT_SIZE + "]."),
    BTN_RESETFONT("✔", '0', "Reset font size to [" + AppConstants.DEFAULT_FONT_SIZE + "] for results."),
    BTN_GOTOP("↑", '1', "Go to first line."),
    BTN_GOBOTTOM("↓", '2', "Go to last line."),
    BTN_PREOCCR("<", ',', "Previous occurrence."),
    BTN_NEXTOCCR(">", '.', "Next occurrence."),
    BTN_FIND("¤", 'z', "Set new word from Search box to see occurrences without highlighting."),
    BTN_HELP("?", '/', "Application help."),
    JCB_MATCHCASE("Aa", 'a', "Match case."),
    JCB_WHOLEWORD("W", 'w', "Whole word."),
    LBL_SEARCH("Search", 'H'),
    BTN_SEARCH("Search", 'S'),
    LBL_LASTN("Last N", 'N'),
    BTN_LASTN("Read", 'e', "Read last N lines and highlight."),
    LBL_RSEARCHES("Recent", 't', "Recently used search-patterns list."),
    MNU_SETTINGS("", 'g', "Settings"),
    MNU_COLOR("*", 'b', "Change highlight color and selected text & background, present highlight color is shown as background."),
    BTN_LISTRS("", 'I', "Search recently used search-patterns list."),
    BTN_CANCEL("", 'C', "Cancel/Stop Search/Read");

    String name, tip;
    char mnemonic;

    UIName(String name, char mnemonic) {
        this(name, mnemonic, null);
    }

    UIName(String name, char mnemonic, String tip) {
        this.name = name;
        this.tip = tip;
        this.mnemonic = mnemonic;
    }


}
