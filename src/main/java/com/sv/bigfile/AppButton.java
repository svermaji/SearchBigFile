package com.sv.bigfile;

import javax.swing.*;

public class AppButton extends JButton {

    AppButton(String text, char mnemonic) {
        this (text, mnemonic, null, null);
    }

    AppButton(String text, char mnemonic, String tip) {
        this (text, mnemonic, tip, null);
    }

    AppButton(String text, char mnemonic, String tip, String iconPath) {
        if (Utils.hasValue(text)) {
            setText(text);
        }
        setMnemonic(mnemonic);
        if (Utils.hasValue(tip)) {
            setToolTipText(tip);
        }
        if (Utils.hasValue(iconPath)) {
            setIcon(new ImageIcon(iconPath));
            if (!Utils.hasValue(text)) {
                setContentAreaFilled(false);
                setBorder(BorderFactory.createEmptyBorder());
            }
        }
    }
}
