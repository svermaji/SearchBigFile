package com.sv.bigfile;

public enum ComponentName {
    TXT_SEARCH ("", 'x', "");

    String name, tip;
    char mnemonic;

    ComponentName(String name, char mnemonic, String tip) {
        this.name = name;
        this.tip = tip;
        this.mnemonic = mnemonic;
    }


}
