package com.sv.bigfile;

import javax.swing.*;

public class AppLabel extends JLabel {

    AppLabel(String text, JComponent component, char mnemonic) {
        setText(text);
        setLabelFor(component);
        setDisplayedMnemonic(mnemonic);
    }
}
