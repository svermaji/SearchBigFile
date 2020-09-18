package com.sv.bigfile;

import javax.swing.*;

public class AppLabel extends JLabel {

    AppLabel(String text, JComponent component, char mnemonic) {
        this (text, component, mnemonic, null);
    }

    AppLabel(String text, JComponent component, char mnemonic, String tip) {
        setText(text);
        setLabelFor(component);
        setDisplayedMnemonic(mnemonic);
        if (Utils.hasValue(tip)) {
            setToolTipText(tip);
        }
    }
}
