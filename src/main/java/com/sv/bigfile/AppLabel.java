package com.sv.bigfile;

import javax.swing.*;

public class AppLabel extends JLabel {

    AppLabel(String text, JTextField txtField, char mnemonic) {
        setText(text);
        setLabelFor(txtField);
        setDisplayedMnemonic(mnemonic);
    }
}
