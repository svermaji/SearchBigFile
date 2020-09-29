package com.sv.bigfile;

import com.sv.core.DefaultConfigs;
import com.sv.core.MyLogger;
import com.sv.core.Utils;
import com.sv.swingui.*;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.StyleConstants;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.text.NumberFormat;
import java.util.Queue;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.PatternSyntaxException;

/**
 * Java Utility to search big files.
 * Searched for a string files of size 1GB
 */
public class SearchBigFile extends AppFrame {

    /**
     * This is config and program will search getter
     * of each enum to store in config file.
     * <p>
     * e.g. if enum is Xyz then when storing getXyz will be called
     */
    enum Configs {
        RecentFiles, FilePath, SearchString, RecentSearches,
        LastN, FontSize, MatchCase, WholeWord, DebugEnabled
    }

    enum Status {
        NOT_STARTED, READING, DONE, CANCELLED
    }

    enum FONT_OPR {
        INCREASE, DECREASE, RESET
    }

    private MyLogger logger;
    private DefaultConfigs configs;

    private JButton btnPlusFont, btnMinusFont, btnResetFont, btnFontInfo, btnWarning;
    private JButton btnSearch;
    private JButton btnLastN;
    private JTextField txtFilePath;
    private JTextField txtSearch;
    private JEditorPane tpResults;
    private HTMLDocument htmlDoc;
    private HTMLEditorKit kit;
    private JCheckBox jcbMatchCase, jcbWholeWord;
    private JComboBox<String> cbFiles, cbSearches;
    private JComboBox<Integer> cbLastN;

    private static final boolean CB_LIST_WIDER = true, CB_LIST_ABOVE = false;
    private static final String PREFERRED_FONT = "Calibri";
    private static final String DEFAULT_FONT = "Dialog.plain";
    private static final int PREFERRED_FONT_SIZE = 12;
    private static final int DEFAULT_FONT_SIZE = 12;
    private static final int MIN_FONT_SIZE = 8;
    private static final int MAX_FONT_SIZE = 24;
    private static final int RECENT_LIMIT = 20;
    private static final int WARN_LIMIT_SEC = 20;
    private static final int FORCE_STOP_LIMIT_SEC = 50;
    private static final int WARN_LIMIT_OCCR = 200;
    private static final int APPEND_MSG_CHUNK = 100;
    private static final int eb = 5;
    private static final Border emptyBorder = new EmptyBorder(new Insets(eb, eb, eb, eb));

    private final String TITLE = "Search File";
    private final String REPLACER_PREFIX = "<font style=\"background-color:yellow\">";
    private final String REPLACER_SUFFIX = "</font>";

    private static boolean showWarning = false;
    private static boolean readNFlag = false;
    private static long insertCounter = 0;
    private static long readCounter = 0;
    private static long startTime = System.currentTimeMillis();

    private boolean debugAllowed;
    private String searchStr, searchStrReplace;
    private String recentFilesStr, recentSearchesStr;
    private long occrTillNow;
    private long linesTillNow;

    private static Status status = Status.NOT_STARTED;

    // indexed structure to maintain line indexing
    private static Map<Long, String> idxMsgsToAppend;
    // LIFO
    private static Queue<String> qMsgsToAppend;
    private static AppendMsgCallable msgCallable;
    private static final ExecutorService threadPool = Executors.newFixedThreadPool(8);

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new SearchBigFile().initComponents());
    }

    /**
     * This method initializes the form.
     */
    private void initComponents() {
        logger = MyLogger.createLogger(getClass());
        configs = new DefaultConfigs(logger, Utils.getConfigsAsArr(Configs.class));
        debugAllowed = getBooleanCfg(Configs.DebugEnabled);
        logger.setDebug(debugAllowed);
        qMsgsToAppend = new LinkedBlockingQueue<>();
        idxMsgsToAppend = new ConcurrentHashMap<>();
        recentFilesStr = getCfg(Configs.RecentFiles);
        recentSearchesStr = getCfg(Configs.RecentSearches);
        msgCallable = new AppendMsgCallable(this);

        Container parentContainer = getContentPane();
        parentContainer.setLayout(new BorderLayout());

        setTitle(TITLE);

        JPanel filePanel = new JPanel();

        final int TXT_COLS = 15;
        UIName uin = UIName.LBL_FILE;
        txtFilePath = new JTextField(getCfg(Configs.FilePath));
        AppLabel lblFilePath = new AppLabel(uin.name, txtFilePath, uin.mnemonic);
        txtFilePath.setColumns(TXT_COLS);
        cbFiles = new JComboBox<>(getFiles());
        cbFiles.addPopupMenuListener(new BoundsPopupMenuListener(CB_LIST_WIDER, CB_LIST_ABOVE));
        cbFiles.setRenderer(new JComboToolTipRenderer());
        cbFiles.setPrototypeDisplayValue("Recent Files");
        addCBFilesAction();
        uin = UIName.LBL_RFILES;
        AppLabel lblRFiles = new AppLabel(uin.name, cbFiles, uin.mnemonic, uin.tip);
        uin = UIName.BTN_LISTRF;
        JButton btnListRF = new AppButton(uin.name, uin.mnemonic, uin.tip, "./icons/search-icon.png");
        btnListRF.addActionListener(e -> showListRF());
        uin = UIName.JCB_MATCHCASE;
        jcbMatchCase = new JCheckBox(uin.name, getBooleanCfg(Configs.MatchCase));
        jcbMatchCase.setMnemonic(uin.mnemonic);
        jcbMatchCase.setToolTipText(uin.tip);
        uin = UIName.JCB_WHOLEWORD;
        jcbWholeWord = new JCheckBox(uin.name, getBooleanCfg(Configs.WholeWord));
        jcbWholeWord.setMnemonic(uin.mnemonic);
        jcbWholeWord.setToolTipText(uin.tip);

        filePanel.setLayout(new FlowLayout());
        filePanel.add(lblFilePath);
        filePanel.add(txtFilePath);
        filePanel.add(lblRFiles);
        filePanel.add(btnListRF);
        filePanel.add(cbFiles);
        filePanel.add(jcbMatchCase);
        filePanel.add(jcbWholeWord);
        filePanel.setBorder(new TitledBorder("File to search"));

        JPanel searchPanel = new JPanel();

        txtSearch = new JTextField(getCfg(Configs.SearchString));
        uin = UIName.LBL_SEARCH;
        AppLabel lblSearch = new AppLabel(uin.name, txtSearch, uin.mnemonic);
        txtSearch.setColumns(TXT_COLS - 5);
        uin = UIName.BTN_SEARCH;
        btnSearch = new AppButton(uin.name, uin.mnemonic);
        btnSearch.addActionListener(evt -> searchFile());
        cbLastN = new JComboBox<>(getLastNOptions());
        cbLastN.setSelectedItem(getIntCfg(Configs.LastN));
        uin = UIName.LBL_LASTN;
        AppLabel lblLastN = new AppLabel(uin.name, cbLastN, uin.mnemonic);
        uin = UIName.BTN_LASTN;
        btnLastN = new AppButton(uin.name, uin.mnemonic, uin.tip);
        btnLastN.addActionListener(evt -> threadPool.submit(new LastNRead(this)));
        cbSearches = new JComboBox<>(getSearches());
        cbSearches.addPopupMenuListener(new BoundsPopupMenuListener(CB_LIST_WIDER, CB_LIST_ABOVE));
        JComboToolTipRenderer cbSearchRenderer = new JComboToolTipRenderer();
        cbSearches.setRenderer(cbSearchRenderer);
        cbSearches.setPrototypeDisplayValue("Pattern");
        addCBSearchAction();
        uin = UIName.LBL_RSEARCHES;
        AppLabel lblRSearches = new AppLabel(uin.name, cbSearches, uin.mnemonic, uin.tip);
        uin = UIName.BTN_LISTRS;
        JButton btnListRS = new AppButton(uin.name, uin.mnemonic, uin.tip, "./icons/search-icon.png");
        btnListRS.addActionListener(e -> showListRS());
        uin = UIName.BTN_CANCEL;
        JButton btnCancel = new AppButton(uin.name, uin.mnemonic, uin.tip, "./icons/cancel-icon.png", false);
        btnCancel.addActionListener(evt -> cancelSearch());

        JToolBar jtbActions = new JToolBar();
        jtbActions.setFloatable(false);
        jtbActions.setRollover(false);
        uin = UIName.BTN_PLUSFONT;
        btnPlusFont = new AppButton(uin.name, uin.mnemonic, uin.tip);
        btnPlusFont.addActionListener(e -> increaseFontSize());
        uin = UIName.BTN_MINUSFONT;
        btnMinusFont = new AppButton(uin.name, uin.mnemonic, uin.tip);
        btnMinusFont.addActionListener(e -> decreaseFontSize());
        uin = UIName.BTN_RESETFONT;
        btnResetFont = new AppButton(uin.name, uin.mnemonic, uin.tip);
        btnResetFont.addActionListener(e -> resetFontSize());
        btnFontInfo = new JButton();
        btnFontInfo.setToolTipText("Present font size.");
        uin = UIName.BTN_WARNING;
        btnWarning = new AppButton(uin.name, uin.mnemonic, uin.tip);
        btnWarning.setToolTipText("Warning indicator. If blinks then Either search taking more than [" + WARN_LIMIT_SEC
                + "sec] or search occurrences are more than [" + WARN_LIMIT_OCCR
                + "]. Will CANCEL forcefully after [" + FORCE_STOP_LIMIT_SEC
                + "sec]");

        setBkColors(new JButton[]{btnPlusFont, btnMinusFont, btnResetFont, btnFontInfo, btnWarning});
        btnWarning.setBackground(Color.PINK);
        btnWarning.setBorder(BorderFactory.createEmptyBorder());

        searchPanel.setLayout(new FlowLayout());
        searchPanel.add(btnWarning);
        searchPanel.add(lblSearch);
        searchPanel.add(txtSearch);
        searchPanel.add(lblRSearches);
        searchPanel.add(btnListRS);
        searchPanel.add(cbSearches);
        searchPanel.add(btnSearch);
        searchPanel.add(lblLastN);
        searchPanel.add(cbLastN);
        searchPanel.add(btnLastN);
        searchPanel.add(btnCancel);
        searchPanel.add(jtbActions);
        jtbActions.add(btnPlusFont);
        jtbActions.add(btnMinusFont);
        jtbActions.add(btnResetFont);
        jtbActions.add(btnFontInfo);
        searchPanel.setBorder(new TitledBorder("Pattern to search"));

        JPanel exitPanel = new JPanel();
        JButton btnExit = new AppExitButton();
        TitledBorder titledEP = new TitledBorder("Exit");
        exitPanel.setBorder(titledEP);
        exitPanel.add(btnExit);

        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new GridBagLayout());
        inputPanel.add(filePanel);
        inputPanel.add(searchPanel);
        inputPanel.add(exitPanel);

        tpResults = new JEditorPane();
        tpResults.setEditable(false);
        tpResults.setContentType("text/html");
        //tpResults.setFont(getFontForEditor(getCfg(Configs.FontSize)));
        tpResults.setFont(getNewFont(tpResults.getFont(), getIntCfg(Configs.FontSize)));
        tpResults.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        htmlDoc = new HTMLDocument();
        tpResults.setDocument(htmlDoc);
        kit = new HTMLEditorKit();
        JScrollPane jspResults = new JScrollPane(tpResults);
        jspResults.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        jspResults.setBorder(emptyBorder);

        parentContainer.add(inputPanel, BorderLayout.NORTH);
        parentContainer.add(jspResults, BorderLayout.CENTER);

        btnExit.addActionListener(evt -> exitForm());
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent evt) {
                exitForm();
            }
        });

        btnFontInfo.setText(getFontSize());
        setToCenter();
    }

    private String getResourcePath(String path) {
        ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        System.out.println("pt = " + classloader.getResource(path).toString());
        return classloader.getResource(path).toString();
    }

    private void setBkColors(JButton[] btns) {
        for (JButton b : btns) {
            b.setBackground(Color.GRAY);
            b.setForeground(Color.WHITE);
        }
    }

    private Font getFontForEditor(String sizeStr) {
        Font retVal = new Font(DEFAULT_FONT, Font.PLAIN, DEFAULT_FONT_SIZE);
        GraphicsEnvironment g = GraphicsEnvironment.getLocalGraphicsEnvironment();
        for (Font font : g.getAllFonts()) {
            if (font.getName().equals(PREFERRED_FONT)) {
                retVal = font;
                break;
            }
        }
        int fs = Utils.hasValue(sizeStr) ? Integer.parseInt(sizeStr) : PREFERRED_FONT_SIZE;
        retVal = new Font(retVal.getName(), retVal.getStyle(), fs);
        logger.log("Returning " + printFontDetail(retVal));
        return retVal;
    }

    private String printFontDetail(Font font) {
        return String.format("Font %s of size %s", font.getName(), font.getSize());
    }

    private void increaseFontSize() {
        setFontSize(FONT_OPR.INCREASE);
    }

    private void decreaseFontSize() {
        setFontSize(FONT_OPR.DECREASE);
    }

    private void resetFontSize() {
        setFontSize(FONT_OPR.RESET);
    }

    private void setFontSize(FONT_OPR opr) {
        Font font = tpResults.getFont();
        boolean changed = false;
        int fs = font.getName().equals(PREFERRED_FONT) ? PREFERRED_FONT_SIZE : DEFAULT_FONT_SIZE;

        switch (opr) {
            case RESET:
                if (font.getSize() != fs) {
                    font = getNewFont(font, fs);
                    changed = true;
                }
                break;
            case DECREASE:
                if (font.getSize() > MIN_FONT_SIZE) {
                    font = getNewFont(font, font.getSize() - 1);
                    changed = true;
                }
                break;
            case INCREASE:
                if (font.getSize() < MAX_FONT_SIZE) {
                    font = getNewFont(font, font.getSize() + 1);
                    changed = true;
                }
                break;
        }

        if (changed) {
            logger.log("Applying new font as " + printFontDetail(font));
            tpResults.setFont(font);
            btnFontInfo.setText(getFontSize());
        } else {
            logger.log("Ignoring request for " + opr + "font. Present " + printFontDetail(font));
        }
    }

    private Font getNewFont(Font font, int size) {
        log("Returning font as " + font.getName() + ", of size " + size);
        return new Font(font.getName(), font.getStyle(), size);
    }

    private void showListRF() {
        showRecentList(cbFiles, "Recent files");
    }

    private void showListRS() {
        showRecentList(cbSearches, "Recent searches");
    }

    private void showRecentList(JComboBox<String> src, String colName) {
        DefaultTableModel model = new DefaultTableModel() {

            @Override
            public int getColumnCount() {
                return 1;
            }

            @Override
            public String getColumnName(int index) {
                return colName + " - Dbl-click or select & ENTER";
            }

        };

        JFrame frame = new JFrame();

        JTextField txtFilter = new JTextField();
        txtFilter.setColumns(30);
        JTable table = new JTable(model);
        deleteAndCreateRows(src, table, model);

        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);

        // ToolTip and alignment
        TableColumn firstCol = table.getColumnModel().getColumn(0);
        firstCol.setMinWidth(25);
        firstCol.setCellRenderer(new CellRendererLeftAlign());

        addFilter(sorter, txtFilter);

        // For making contents non editable
        table.setDefaultEditor(Object.class, null);

        table.setAutoscrolls(true);
        table.setPreferredScrollableViewportSize(table.getPreferredSize());

        table.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent mouseEvent) {
                JTable table = (JTable) mouseEvent.getSource();
                if (mouseEvent.getClickCount() == 2 && table.getSelectedRow() != -1) {
                    src.setSelectedItem(table.getValueAt(table.getSelectedRow(), 0).toString());
                    frame.setVisible(false);
                }
            }
        });

        InputMap im = table.getInputMap();
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "Action.RunCmdCell");
        ActionMap am = table.getActionMap();
        am.put("Action.RunCmdCell", new CopyCommandAction(table, frame, src));

        txtFilter.getDocument().addDocumentListener(
                new DocumentListener() {
                    public void changedUpdate(DocumentEvent e) {
                        addFilter(sorter, txtFilter);
                    }

                    public void insertUpdate(DocumentEvent e) {
                        addFilter(sorter, txtFilter);
                    }

                    public void removeUpdate(DocumentEvent e) {
                        addFilter(sorter, txtFilter);
                    }
                });

        table.setBorder(emptyBorder);

        JPanel filterPanel = new JPanel();
        filterPanel.add(new AppLabel("Filter", txtFilter, 'R'));
        filterPanel.add(txtFilter);

        JPanel topPanel = new JPanel(new GridLayout(2, 1));
        topPanel.add(filterPanel);

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        panel.setBorder(emptyBorder);

        Container pc = frame.getContentPane();
        pc.setLayout(new BorderLayout());
        pc.add(panel);
        frame.setTitle("ESC to Hide");
        frame.setAlwaysOnTop(true);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        frame.setBackground(Color.CYAN);
        frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);

        addEscKeyAction(frame);
    }

    private void addEscKeyAction(JFrame frame) {
        frame.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "Cancel");
        frame.getRootPane().getActionMap().put("Cancel", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                frame.setVisible(false);
            }
        });

    }

    private void addFilter(TableRowSorter<DefaultTableModel> sorter, JTextField txtFilter) {
        RowFilter<DefaultTableModel, Object> rf;
        try {
            rf = RowFilter.regexFilter(txtFilter.getText(), 0);
        } catch (PatternSyntaxException e) {
            return;
        }
        sorter.setRowFilter(rf);
    }

    private void deleteAndCreateRows(JComboBox<String> src, JTable table, DefaultTableModel model) {
        int rows = table.getRowCount();
        for (int i = 0; i < rows; i++) {
            model.removeRow(i);
        }

        int items = src.getItemCount();
        for (int i = 0; i < items; i++) {
            String s = src.getItemAt(i);
            model.addRow(new String[]{s});
        }
    }

    private Integer[] getLastNOptions() {
        return new Integer[]{200, 500, 1000, 2000, 3000, 4000, 5000};
    }

    private void resetForNewSearch() {
        insertCounter = 0;
        readCounter = 0;
        disableControls();
        resetShowWarning();
        emptyResults();
        updateRecentSearchVals();
        qMsgsToAppend.clear();
        idxMsgsToAppend.clear();
        setSearchStrings();
        logger.log(getSearchDetails());
        startTime = System.currentTimeMillis();
        status = Status.READING;
        readNFlag = false;
    }

    private String getLineNumStr(long line) {
        return "<b>" + line + "</b> ";
    }

    private int lowerCaseSplit(String line, String pattern) {
        return line.toLowerCase().split(pattern.toLowerCase()).length;
    }

    private void removeCBSearchAL() {
        Arrays.stream(cbSearches.getActionListeners()).forEach(a -> cbSearches.removeActionListener(a));
    }

    private void addCBSearchAction() {
        cbSearches.addActionListener(e -> setSearchPattern(cbSearches.getSelectedItem().toString()));
    }

    private void setSearchPattern(String s) {
        txtSearch.setText(s);
    }

    private void setFileToSearch(String s) {
        txtFilePath.setText(s);
    }

    private void removeCBFilesAL() {
        Arrays.stream(cbFiles.getActionListeners()).forEach(a -> cbFiles.removeActionListener(a));
    }

    private void addCBFilesAction() {
        cbFiles.addActionListener(e -> setFileToSearch(cbFiles.getSelectedItem().toString()));
    }

    private String[] getFiles() {
        return getCfg(Configs.RecentFiles).split(";");
    }

    private String[] getSearches() {
        return getCfg(Configs.RecentSearches).split(";");
    }

    private void resetShowWarning() {
        showWarning = false;
        occrTillNow = 0;
        linesTillNow = 0;
        btnWarning.setBackground(Color.PINK);
    }

    private void cancelSearch() {
        resetShowWarning();
        if (status == Status.READING) {
            logger.warn("Search cancelled by user.");
            status = Status.CANCELLED;
        }
    }

    private void searchFile() {
        resetForNewSearch();
        if (isValidate()) {
            status = Status.READING;
            threadPool.submit(new SearchFileCallable(this));
            threadPool.submit(new TimerCallable(this));
        } else {
            enableControls();
        }
    }

    //TODO: remove star tagging and fav concept
    private boolean isValidate() {
        updateTitle("");
        boolean result = true;
        if (!Utils.hasValue(getFilePath())) {
            updateTitle("REQUIRED: file to search");
            result = false;
        }
        if (result && !Utils.hasValue(getSearchString())) {
            updateTitle("REQUIRED: text to search");
            result = false;
        }

        if (!result) {
            logger.log("Validation failed !!");
        }

        return result;
    }

    private void updateRecentSearchVals() {
        recentFilesStr = checkItems(getFilePath(), recentFilesStr, cbFiles.getSelectedItem().toString());
        recentSearchesStr = checkItems(getSearchString(), recentSearchesStr, cbSearches.getSelectedItem().toString());
        removeCBFilesAL();
        cbFiles.removeAllItems();
        Arrays.stream(recentFilesStr.split(Utils.SEMI_COLON)).
                forEach(s -> {
                    if (Utils.hasValue(s)) {
                        cbFiles.addItem(s);
                    }
                });
        addCBFilesAction();

        removeCBSearchAL();
        cbSearches.removeAllItems();
        Arrays.stream(recentSearchesStr.split(Utils.SEMI_COLON)).
                forEach(s -> {
                    if (Utils.hasValue(s)) {
                        cbSearches.addItem(s);
                    }
                });
        addCBSearchAction();
    }

    private String checkItems(String searchStr, String csv, String selectedItem) {
        if (selectedItem.toLowerCase().equals(searchStr.toLowerCase())) {
            // remove item and add it again to bring it on top
            csv = csv.replace(selectedItem + Utils.SEMI_COLON, "");
        }
        csv = searchStr + Utils.SEMI_COLON + csv;

        if (csv.split(Utils.SEMI_COLON).length >= RECENT_LIMIT) {
            csv = csv.substring(0, csv.lastIndexOf(Utils.SEMI_COLON));
        }

        return csv;
    }

    private String getSearchDetails() {
        return String.format("Starting search in file [%s] for pattern [%s] " +
                        "with criteria MatchCase [%s], WholeWord[%s], read-lines[%s] " +
                        "at time [%s]",
                getFilePath(),
                getSearchString(),
                getMatchCase(),
                getWholeWord(),
                getLastN(),
                new Date(startTime));
    }

    private void updateControls(boolean enable) {
        Component[] components = {
                txtFilePath, txtSearch, btnSearch, btnLastN,
                cbFiles, cbSearches, cbLastN, jcbMatchCase,
                jcbWholeWord, btnPlusFont, btnMinusFont, btnResetFont
        };

        Arrays.stream(components).forEach(c -> c.setEnabled(enable));
    }

    private void disableControls() {
        updateControls(false);
    }

    private void enableControls() {
        updateControls(true);
    }

    private boolean isMatchCase() {
        return jcbMatchCase.isSelected();
    }

    private boolean isWholeWord() {
        return jcbWholeWord.isSelected();
    }

    private void setSearchStrings() {
        searchStr = getSearchString();
        searchStrReplace = REPLACER_PREFIX + searchStr + REPLACER_SUFFIX;
    }

    private void startThread(Callable<Boolean> callable) {
        threadPool.submit(callable);
    }

    public void appendResultNoFormat(String data) {
        synchronized (SearchBigFile.class) {
            data = convertForHtml(data);
            // Needs to be sync else line numbers and data will be jumbled
            try {
                if (readNFlag) {
                    Element body = getBodyElement();
                    int offs = Math.max(body.getStartOffset(), 0);
                    kit.insertHTML(htmlDoc, offs, data, 0, 0, null);
                } else {
                    kit.insertHTML(htmlDoc, htmlDoc.getLength(), data, 0, 0, null);
                }
            } catch (BadLocationException | IOException e) {
                logger.error("Unable to append data: " + data);
            }
        }
    }

    private Element getBodyElement() {
        Element[] roots = htmlDoc.getRootElements(); // #0 is the HTML element, #1 the bidi-root
        Element body = null;
        for (int i = 0; i < roots[0].getElementCount(); i++) {
            Element element = roots[0].getElement(i);
            if (element.getAttributes().getAttribute(StyleConstants.NameAttribute) == HTML.Tag.BODY) {
                body = element;
                break;
            }
        }
        return body;
    }

    private String convertStartingSpacesForHtml(String data) {
        StringBuilder sb = new StringBuilder();
        int idx = 0;
        char[] arr = data.toCharArray();
        for (char c : arr) {
            if (Character.isWhitespace(c)) {
                sb.append("&nbsp;");
                idx++;
            } else {
                break;
            }
        }
        return sb.toString() + data.substring(idx);
    }

    private String convertForHtml(String data) {
        String NEW_LINE_REGEX = "\r?\n";
        String HTML_LINE_END = "<br>";
        return data.replaceAll(NEW_LINE_REGEX, HTML_LINE_END);
    }

    public void appendResult(String data) {

        if (Utils.hasValue(searchStr)) {
            if (!isMatchCase()) {
                data = replaceWithSameCase(data);
            } else {
                data = data.replaceAll(searchStr, searchStrReplace);
            }
        }

        // TODO
        /*if (isWholeWord()) {
            searchStr = ".*\\b" + searchStr + "\\b.*";
        }*/

        appendResultNoFormat(data);
    }

    private String replaceWithSameCase(String data) {
        StringBuilder sb = new StringBuilder();
        while (data.toLowerCase().contains(searchStr.toLowerCase())) {
            int idx = data.toLowerCase().indexOf(searchStr.toLowerCase());
            sb.append(data, 0, idx)
                    .append(REPLACER_PREFIX)
                    .append(data, idx, idx + searchStr.length())
                    .append(REPLACER_SUFFIX);
            data = data.substring(idx + searchStr.length());
        }
        sb.append(data);
        return sb.toString();
    }

    private void emptyResults() {
        tpResults.setText("");
    }

    private void setToCenter() {
        setExtendedState(java.awt.Frame.MAXIMIZED_BOTH);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    /**
     * Exit the Application
     */
    private void exitForm() {
        configs.saveConfig(this);
        setVisible(false);
        dispose();
        logger.dispose();
        System.exit(0);
    }

    public String getFilePath() {
        return txtFilePath.getText();
    }

    public String getFontSize() {
        return tpResults.getFont().getSize() + "";
    }

    public String getSearchString() {
        return txtSearch.getText();
    }

    public String getMatchCase() {
        return jcbMatchCase.isSelected() + "";
    }

    public String getWholeWord() {
        return jcbWholeWord.isSelected() + "";
    }

    public String getLastN() {
        return cbLastN.getSelectedItem().toString();
    }

    public String getRecentSearches() {
        return recentSearchesStr;
    }

    public String getRecentFiles() {
        return recentFilesStr;
    }

    public String getDebugEnabled() {
        return debugAllowed + "";
    }

    public void updateTitle(String info) {
        setTitle((Utils.hasValue(info) ? TITLE + Utils.SP_DASH_SP + info : TITLE));
    }

    private String getWarning() {
        return " - Either search taking long or too many results [" + occrTillNow + "] !!  Cancel and try to narrow";
    }

    private String processPattern() {
        String searchPattern = searchStr;

        if (!isMatchCase()) {
            searchPattern = searchPattern.toLowerCase();
        }
        if (isWholeWord()) {
            searchPattern = ".*\\b" + searchPattern + "\\b.*";
        }
        return searchPattern;
    }

    private void printCounters() {
        logger.log("insertCounter [" + insertCounter
                + "], readCounter [" + readCounter
                + "], qMsgsToAppend size [" + qMsgsToAppend.size()
                + "], idxMsgsToAppend size [" + idxMsgsToAppend.size()
                + "]");
    }

    private String getSearchResult(String path, String seconds, long lineNum, long occurrences) {
        String result =
                String.format("File size: %s, " +
                                "time taken: %s, lines read: [%s], occurrences: [%s]",
                        Utils.getFileSizeString(new File(path).length()),
                        seconds,
                        lineNum,
                        occurrences);

        logger.log(result);
        return result;
    }

    public String getSecondsElapsedStr(long time) {
        return "[" + getSecondsElapsed(time) + " sec]";
    }

    public long getSecondsElapsed(long time) {
        return TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - time);
    }

    public void debug(String s) {
        logger.debug(s);
    }

    public void log(String s) {
        logger.log(s);
    }

    public void finishAction() {
        printCounters();
        // Go to end
        tpResults.select(htmlDoc.getLength(), htmlDoc.getLength());
    }

    public boolean getBooleanCfg(Configs c) {
        return configs.getBooleanConfig(c.name());
    }

    public int getIntCfg(Configs c) {
        return configs.getIntConfig(c.name());
    }

    public String getCfg(Configs c) {
        return configs.getConfig(c.name());
    }

    public boolean isCancelled() {
        return status == Status.CANCELLED;
    }

    static class CopyCommandAction extends AbstractAction {

        private final JTable table;
        private final JFrame frame;
        private final JComboBox<String> src;

        public CopyCommandAction(JTable table, JFrame frame, JComboBox<String> src) {
            this.table = table;
            this.frame = frame;
            this.src = src;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            src.setSelectedItem(table.getValueAt(table.getSelectedRow(), 0).toString());
            frame.setVisible(false);
        }
    }

    class LastNRead implements Callable<Boolean> {

        SearchBigFile sbf;

        LastNRead(SearchBigFile sbf) {
            this.sbf = sbf;
        }

        @Override
        public Boolean call() {
            logger.log("Reading last few lines...");
            resetForNewSearch();

            if (isValidate()) {
                final int LIMIT = Integer.parseInt(cbLastN.getSelectedItem().toString());
                int readLines = 0;
                int occr = 0;
                boolean hasError = false;
                String searchPattern = processPattern();
                String fn = getFilePath();
                StringBuilder sb = new StringBuilder();
                File file = new File(fn);

                readNFlag = true;
                startThread(new TimerCallable(sbf));
                updateTitle("Reading last " + LIMIT + " lines");
                logger.log("Loading last " + LIMIT + " lines from: " + fn);

                try (RandomAccessFile randomAccessFile = new RandomAccessFile(file, "r")) {
                    long fileLength = file.length() - 1;
                    // Set the pointer at the last of the file
                    randomAccessFile.seek(fileLength);
                    // FIFO
                    Stack<String> stack = new Stack<>();

                    for (long pointer = fileLength; pointer >= 0; pointer--) {
                        randomAccessFile.seek(pointer);
                        char c;
                        // read from the last, one char at the time
                        c = (char) randomAccessFile.read();
                        // break when end of the line
                        if (c == '\n') {

                            if (Utils.hasValue(sb.toString())) {
                                sb.reverse();
                            }

                            String strToAppend = getLineNumStr(readLines + 1) + convertStartingSpacesForHtml(sb.toString()) + System.lineSeparator();
                            synchronized (SearchBigFile.class) {
                                // emptying stack to Q
                                stack.push(strToAppend);
                                if (stack.size() > APPEND_MSG_CHUNK) {
                                    while (!stack.empty()) {
                                        qMsgsToAppend.add(stack.pop());
                                    }
                                    startThread(msgCallable);
                                }
                            }

                            int len = sb.toString().toLowerCase().split(searchPattern).length;
                            occr += len > 0 ? len - 1 : 0;
                            occrTillNow = occr;
                            linesTillNow = readLines;
                            if (!showWarning && occr > WARN_LIMIT_OCCR) {
                                showWarning = true;
                            }
                            sb = new StringBuilder();
                            readLines++;
                            // Last line will be printed after loop
                            if (readLines == LIMIT - 1) {
                                break;
                            }
                            if (isCancelled()) {
                                appendResultNoFormat("---------------------Search cancelled----------------------------" + System.lineSeparator());
                                break;
                            }

                        } else {
                            sb.append(c);
                        }
                        fileLength = fileLength - pointer;
                    }
                    if (Utils.hasValue(sb.toString())) {
                        sb.reverse();
                    }
                    String strToAppend = getLineNumStr(readLines + 1) + convertStartingSpacesForHtml(sb.toString()) + System.lineSeparator();
                    synchronized (SearchBigFile.class) {
                        stack.push(strToAppend);
                        while (!stack.empty()) {
                            qMsgsToAppend.add(stack.pop());
                        }
                        startThread(msgCallable);
                    }
                    readLines++;
                } catch (IOException e) {
                    updateTitle("Error in reading file");
                    hasError = true;
                    logger.error(e);
                } finally {
                    enableControls();
                }

                int len = lowerCaseSplit(sb.toString(), searchStr);
                occr += len > 0 ? len - 1 : 0;
                if (!hasError) {
                    String result = getSearchResult(
                            fn,
                            getSecondsElapsedStr(startTime),
                            readLines,
                            occr);
                    String statusStr = isCancelled() ? "Read cancelled - " : "Read complete - ";
                    updateTitle(statusStr + result);
                }
                status = Status.DONE;
            }

            sbf.finishAction();
            return true;
        }
    }

    class StartWarnIndicator extends SwingWorker<Integer, String> {

        @Override
        public Integer doInBackground() {
            btnWarning.setBackground(btnWarning.getBackground() == Color.RED ? Color.PINK : Color.RED);
            return 1;
        }
    }

    class AppendData extends SwingWorker<Integer, String> {

        @Override
        public Integer doInBackground() {
            synchronized (SearchBigFile.class) {
                readCounter++;
                appendResult(idxMsgsToAppend.get(readCounter));
            }
            return 1;
        }
    }

    // To avoid async order of lines this cannot be worker
    class SearchData {

        final int LINES_TO_INFORM = 500000;
        private final SearchStats stats;

        SearchData(SearchStats stats) {
            this.stats = stats;
        }

        public void process() {
            long lineNum = stats.getLineNum();
            StringBuilder sb = new StringBuilder();

            if (stats.isMatch()) {
                int occr = lowerCaseSplit(stats.getLine(), stats.getSearchPattern());
                stats.setOccurrences(stats.getOccurrences() + occr - 1);
                sb.append(getLineNumStr(lineNum)).append(stats.getLine()).append(System.lineSeparator());
                qMsgsToAppend.add(convertStartingSpacesForHtml(sb.toString()));
            }
            stats.setLineNum(lineNum + 1);

            if (lineNum % LINES_TO_INFORM == 0) {
                logger.log("Lines searched so far: " + NumberFormat.getNumberInstance().format(lineNum));
            }

            occrTillNow = stats.getOccurrences();
            linesTillNow = stats.getLineNum();
            if (!showWarning && stats.getOccurrences() > WARN_LIMIT_OCCR) {
                showWarning = true;
            }
        }
    }

    class TimerCallable implements Callable<Boolean> {

        private final SearchBigFile sbf;

        public TimerCallable(SearchBigFile sbf) {
            this.sbf = sbf;
        }

        @Override
        public Boolean call() {
            long timeElapse = 0;
            do {
                // Due to multi threading, separate if is imposed
                if (status == Status.READING) {
                    timeElapse = sbf.getSecondsElapsed(startTime);
                    String msg = timeElapse + " sec, lines [" + sbf.linesTillNow + "]";
                    if (showWarning || timeElapse > WARN_LIMIT_SEC) {
                        msg += sbf.getWarning();
                        sbf.log("Invoking warning indicator.");
                        SwingUtilities.invokeLater(new StartWarnIndicator());
                    }
                    if (timeElapse > FORCE_STOP_LIMIT_SEC) {
                        sbf.log("Stopping forcefully.");
                        cancelSearch();
                    }
                    sbf.updateTitle(msg);
                    logger.debug("Timer callable sleeping now for a second");
                    Utils.sleep(1000, sbf.logger);
                }
            } while (status == Status.READING);

            sbf.logger.log("Timer stopped after " + timeElapse + " sec");
            return true;
        }
    }

    class AppendMsgCallable implements Callable<Boolean> {

        SearchBigFile sbf;

        public AppendMsgCallable(SearchBigFile sbf) {
            this.sbf = sbf;
        }

        @Override
        public Boolean call() {
            StringBuilder sb = new StringBuilder();

            logger.debug("Size of qMsgsToAppend is" + qMsgsToAppend.size());
            while (!qMsgsToAppend.isEmpty()) {
                String m;
                synchronized (SearchBigFile.class) {
                    m = qMsgsToAppend.poll();
                }
                if (readNFlag || Utils.hasValue(m)) {
                    sb.append(m);
                }
            }
            // TODO: check if it reads something from qmsg before insert
            if (readNFlag || sb.length() > 0) {
                synchronized (SearchBigFile.class) {
                    insertCounter++;
                    idxMsgsToAppend.put(insertCounter, sb.toString());
                    SwingUtilities.invokeLater(new AppendData());
                }
            }

            return true;
        }
    }

    class SearchFileCallable implements Callable<Boolean> {

        private final SearchBigFile sbf;

        public SearchFileCallable(SearchBigFile sbf) {
            this.sbf = sbf;
        }

        @Override
        public Boolean call() {
            final int BUFFER_SIZE = 5 * 1024;
            String searchPattern = sbf.processPattern();

            String path = sbf.getFilePath();

            /*try (InputStream stream = new FileInputStream(path);
                 Scanner sc = new Scanner(stream, "UTF-8")
            ) {*/
            try (InputStream stream = new FileInputStream(path);
                 BufferedReader br = new BufferedReader(new InputStreamReader(stream), BUFFER_SIZE)
            ) {
                long lineNum = 1, occurrences = 0, time = System.currentTimeMillis();
                SearchStats stats = new SearchStats(lineNum, occurrences, null, searchPattern);
                SearchData searchData = new SearchData(stats);

                String line;
                while ((line = br.readLine()) != null) {
                /*while (sc.hasNextLine()) {
                    String line = sc.nextLine();*/
                    stats.setLine(line);
                    stats.setMatch((!isMatchCase() && line.toLowerCase().contains(searchPattern))
                            || (isMatchCase() && line.contains(searchPattern))
                            || (isWholeWord() && line.matches(searchPattern))
                    );

                    searchData.process();
                    if (qMsgsToAppend.size() > APPEND_MSG_CHUNK) {
                        startThread(msgCallable);
                    }
                    if (isCancelled()) {
                        sbf.appendResultNoFormat("---------------------Search cancelled----------------------------" + System.lineSeparator());
                        break;
                    }
                }

                logger.log("File read in " + getSecondsElapsedStr(time));

                time = System.currentTimeMillis();
                startThread(msgCallable);
                while (readCounter != insertCounter) {
                    logger.debug("Waiting for readCounter to be equal insertCounter");
                    Utils.sleep(200, sbf.logger);
                }
                logger.log("Time in waiting all message to append is " + getSecondsElapsedStr(time));

                String result = getSearchResult(path, getSecondsElapsedStr(startTime), stats.getLineNum(), stats.occurrences);
                if (stats.getOccurrences() == 0) {
                    sbf.appendResultNoFormat("No match found. ");
                }

                if (isCancelled()) {
                    sbf.updateTitle("Search cancelled - " + result);
                } else {
                    sbf.appendResultNoFormat("---------------------Search complete----------------------------" + System.lineSeparator());
                    sbf.updateTitle("Search complete - " + result);
                }
                status = Status.DONE;
            } catch (IOException e) {
                sbf.logger.error(e.getMessage());
                sbf.tpResults.setText("Unable to search file");
            } finally {
                sbf.enableControls();
            }

            finishAction();
            return true;
        }

    }

}

