package com.sv.bigfile;

public enum UIName {
    LBL_FILE("File", 'F'),
    BTN_FILE("...", 'O', "Select file from browser."),
    LBL_RFILES("Recent", 'R', "Recent used files list."),
    BTN_LISTRF("", 'L', "Search recently used file list."),
    BTN_PLUSFONT("+", '=', "Increase font size for results, max [" + Constants.MAX_FONT_SIZE + "]."),
    BTN_MINUSFONT("—", '-', "Decrease font size for results, min [" + Constants.MIN_FONT_SIZE + "]."),
    BTN_RESETFONT("✔", '0', "Reset font size to [" + Constants.DEFAULT_FONT_SIZE + "] for results."),
    BTN_GOTOP("↑", '1', "Go to first line."),
    BTN_GOBOTTOM("↓", '2', "Go to last line."),
    BTN_PREOCCR("<", ',', "Previous occurrence."),
    BTN_NEXTOCCR(">", '.', "Next occurrence."),
    BTN_FIND("¤", 'z', "Set new word from Search box to see occurrences."),
    BTN_HELP("?", '/', "Application help."),
    JCB_MATCHCASE("case", 'e', "Match case."),
    JCB_WHOLEWORD("word", 'w', "Whole word."),
    LBL_SEARCH("Search", 'H'),
    BTN_SEARCH("Search", 'S'),
    LBL_LASTN("Last N", 'N'),
    BTN_LASTN("Read", 'a', "Read last N lines and highlight."),
    LBL_RSEARCHES("Recent", 't', "Recently used search-patterns list."),
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
