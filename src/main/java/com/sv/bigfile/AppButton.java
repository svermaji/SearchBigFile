package com.sv.bigfile;

import javax.swing.*;

public class AppButton extends JButton {

    AppButton(String text, char mnemonic) {
        this(text, mnemonic, null, null);
    }

    AppButton(String text, char mnemonic, String tip) {
        this(text, mnemonic, tip, null);
    }

    AppButton(String text, char mnemonic, String tip, String iconPath) {
        this(text, mnemonic, tip, iconPath, false);
    }

    AppButton(String text, char mnemonic, String tip, String iconPath, boolean drawAsButton) {
        if (Utils.hasValue(text)) {
            setText(text);
        }
        setMnemonic(mnemonic);
        if (Utils.hasValue(tip)) {
            setToolTipText(tip + " Shortcut: Alt+" + mnemonic);
        }
        if (Utils.hasValue(iconPath)) {
            setIcon(new ImageIcon(iconPath));
            if (!drawAsButton) {
                setContentAreaFilled(false);
                setBorder(BorderFactory.createEmptyBorder());
            }
        }
    }
}
