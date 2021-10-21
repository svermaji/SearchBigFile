package com.sv.bigfile;

import com.sv.core.Utils;
import com.sv.core.logger.MyLogger;
import com.sv.swingui.component.TabCloseComponent;

import javax.swing.*;
import javax.swing.text.Highlighter;
import javax.swing.text.html.HTMLDocument;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

import static com.sv.swingui.UIConstants.EMPTY_BORDER;

/**
 * Java Utility to search big files.
 * Searched for a string files of size 1GB
 */
public class ResultTabData {

    private JTextPane resultPane;
    private JScrollPane jspPane;
    private HTMLDocument htmlDoc;
    private Highlighter highlighter;

    private TabCloseComponent tabCloseComponent;

    private String title;
    private int tabIdx;

    // indexed structure to maintain line indexing
    private Map<Long, String> idxMsgsToAppend;
    private Map<Integer, OffsetInfo> lineOffsets;
    private int lastSelectedRow = -1, lineOffsetsIdx, lastLineOffsetsIdx = -1;
    private int globalCharIdx;
    private MyLogger logger;
    private final SearchBigFile sbf;

    // LIFO
    private Queue<String> qMsgsToAppend;

    public ResultTabData(String title, int tabIdx, SearchBigFile sbf) {
        this.title = title;
        this.tabIdx = tabIdx;
        this.sbf = sbf;
        logger = sbf.getLogger();
        init();
    }

    private void init() {
        qMsgsToAppend = new LinkedBlockingQueue<>();
        idxMsgsToAppend = new ConcurrentHashMap<>();
        lineOffsets = new HashMap<>();
        setupResultPane();
    }

    private void setupResultPane() {
        resultPane = new JTextPane() {
            @Override
            public Color getSelectionColor() {
                return sbf.getSelectionColor();
            }

            @Override
            public Color getSelectedTextColor() {
                return sbf.getSelectionTextColor();
            }
        };
        highlighter = resultPane.getHighlighter();
        resultPane.setEditable(false);
        resultPane.setContentType("text/html");
        resultPane.setFont(sbf.getFontForEditor(sbf.getCfg(SearchBigFile.Configs.FontSize)));
        resultPane.setForeground(Color.black);
        resultPane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        htmlDoc = new HTMLDocument();
        resultPane.setDocument(htmlDoc);
        final JTextPane finalTp = resultPane;
        resultPane.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                if (!Utils.hasValue(finalTp.getSelectedText())) {
                    sbf.highlightLastSelectedItem();
                }
            }
        });
        resultPane.addFocusListener(new FocusAdapter() {

            @Override
            public void focusLost(FocusEvent e) {
                sbf.highlightLastSelectedItem();
            }
        });
        jspPane = new JScrollPane(resultPane);
        jspPane.setBorder(EMPTY_BORDER);
        jspPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    }

    public TabCloseComponent getTabCloseComponent() {
        return tabCloseComponent;
    }

    public void setTabCloseComponent(TabCloseComponent tabCloseComponent) {
        this.tabCloseComponent = tabCloseComponent;
    }

    public JTextPane getResultPane() {
        return resultPane;
    }

    public JScrollPane getJspPane() {
        return jspPane;
    }

    public HTMLDocument getHtmlDoc() {
        return htmlDoc;
    }

    public Highlighter getHighlighter() {
        return highlighter;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getTabIdx() {
        return tabIdx;
    }

    public void setTabIdx(int tabIdx) {
        this.tabIdx = tabIdx;
    }

    public Map<Long, String> getIdxMsgsToAppend() {
        return idxMsgsToAppend;
    }

    public Map<Integer, OffsetInfo> getLineOffsets() {
        return lineOffsets;
    }

    public int getLastSelectedRow() {
        return lastSelectedRow;
    }

    public int getLineOffsetsIdx() {
        return lineOffsetsIdx;
    }

    public int getLastLineOffsetsIdx() {
        return lastLineOffsetsIdx;
    }

    public int getGlobalCharIdx() {
        return globalCharIdx;
    }

    public Queue<String> getqMsgsToAppend() {
        return qMsgsToAppend;
    }

    @Override
    public String toString() {
        return "ResultTabData{" +
                "title='" + title + '\'' +
                ", tabIdx=" + tabIdx +
                ", tabCloseComponent=" + tabCloseComponent +
                '}';
    }
}
