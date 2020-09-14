package com.sv.bigfile;

import javax.swing.text.Element;
import javax.swing.text.LabelView;
import javax.swing.text.StyleConstants;
import javax.swing.text.View;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;

class WrapColumnFactory extends HTMLEditorKit.HTMLFactory {

    @Override
    public View create(Element elem) {
        View v = super.create(elem);

        if (v instanceof LabelView) {

            // the javax.swing.text.html.BRView (representing <br> tag) is a LabelView but must not be handled
            // by a WrapLabelView. As BRView is private, check the html tag from elem attribute
            Object o = elem.getAttributes().getAttribute(StyleConstants.NameAttribute);
            if ((o instanceof HTML.Tag) && o == HTML.Tag.BR) {
                return v;
            }

            return new WrapLabelView(elem);
        }

        return v;
    }
}
