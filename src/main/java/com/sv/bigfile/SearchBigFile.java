package com.sv.bigfile;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
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
import java.util.List;
import java.util.Queue;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.PatternSyntaxException;

public class SearchBigFile extends AppFrame {

    enum Status {
        NOT_STARTED, READING, DONE, CANCELLED
    }

    enum FONT_OPR {
        INCREASE, DECREASE, RESET
    }

    private JTextField txtFilePath;
    private JTextField txtSearch;
    private JEditorPane tpResults;
    private static final String PREFERRED_FONT = "Calibri";
    private static final int PREFERRED_FONT_SIZE = 12;
    private static final String DEFAULT_FONT = "Dialog.plain";
    private static final int DEFAULT_FONT_SIZE = 12;
    private static final int MIN_FONT_SIZE = 8;
    private static final int MAX_FONT_SIZE = 24;
    private long occrTillNow;
    private HTMLDocument htmlDoc;
    private HTMLEditorKit kit;

    private MyLogger logger;
    private DefaultConfigs configs;

    private String searchStr, searchStrReplace;
    private String recentFilesStr, recentSearchesStr;
    private final String REPLACER_PREFIX = "<font style=\"background-color:yellow\">";
    private final String REPLACER_SUFFIX = "</font>";

    private JButton btnWarning;
    private JButton btnSearch;
    private JButton btnLastN;
    private final String TITLE = "Search File";
    private static final int RECENT_LIMIT = 20;
    private static boolean showWarning = false;
    private static final int TIME_LIMIT_FOR_WARN_IN_SEC = 20;
    private static final int OCCUR_LIMIT_FOR_WARN_IN_SEC = 200;
    private static final int APPEND_MSG_CHUNK = 100;
    private static final boolean CB_LIST_WIDER = true, CB_LIST_ABOVE = false;

    private static long startTime = System.currentTimeMillis();
    private static Status status = Status.NOT_STARTED;

    private JCheckBox jcbMatchCase, jcbWholeWord;
    private JComboBox<String> cbFiles, cbSearches;
    private JComboBox<Integer> cbLastN;
    // LIFO
    private static Queue<String> qMsgsToAppend;
    private static boolean readNFlag = false;
    private static AppendMsgCallable msgCallable;
    private static final ExecutorService threadPool = Executors.newFixedThreadPool(8);

    private static final Border emptyBorder = new EmptyBorder(new Insets(5, 5, 5, 5));

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new SearchBigFile().initComponents());
    }

    /**
     * This method initializes the form.
     */
    private void initComponents() {
        logger = MyLogger.createLogger("search-big-file.log");

        configs = new DefaultConfigs(logger);
        qMsgsToAppend = new LinkedBlockingQueue<>();
        msgCallable = new AppendMsgCallable(this);
        recentFilesStr = configs.getConfig(DefaultConfigs.Config.RECENT_FILES);
        recentSearchesStr = configs.getConfig(DefaultConfigs.Config.RECENT_SEARCHES);

        Container parentContainer = getContentPane();
        parentContainer.setLayout(new BorderLayout());

        JPanel searchPanel = new JPanel();
        JPanel filePanel = new JPanel();
        JPanel exitPanel = new JPanel();
        JPanel inputPanel = new JPanel();

        setTitle(TITLE);

        final int TXT_COLS = 15;
        tpResults = new JEditorPane();
        tpResults.setEditable(false);
        tpResults.setContentType("text/html");
        tpResults.setFont(getFontForEditor(tpResults.getFont(), configs.getConfig(DefaultConfigs.Config.FONT_SIZE)));
        tpResults.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        htmlDoc = new HTMLDocument();
        tpResults.setDocument(htmlDoc);
        kit = new HTMLEditorKit();

        JToolBar jtbActions = new JToolBar();
        jtbActions.setFloatable(false);
        jtbActions.setRollover(false);
        txtFilePath = new JTextField(configs.getConfig(DefaultConfigs.Config.FILEPATH));
        AppLabel lblFilePath = new AppLabel("File", txtFilePath, 'F');
        txtFilePath.setColumns(TXT_COLS);
        cbFiles = new JComboBox<>(getFiles());
        cbFiles.addPopupMenuListener(new BoundsPopupMenuListener(CB_LIST_WIDER, CB_LIST_ABOVE));
        JComboToolTipRenderer cbFilesRenderer = new JComboToolTipRenderer();
        cbFiles.setRenderer(cbFilesRenderer);
        cbFiles.setPrototypeDisplayValue("Recent Files");
        addCBFilesAL();
        AppLabel lblRFiles = new AppLabel("Recent", cbFiles, 'R', "Recent used files list");
        JButton btnListRF = new AppButton("", 'T', "Search recently used file list.", "./search-icon.png");
        btnListRF.addActionListener(e -> showListRF());

        JButton btnPlusFont = new AppButton("+", '=', "Increase font size for file contents.");
        btnPlusFont.addActionListener(e -> increaseFontSize());
        JButton btnMinusFont = new AppButton("—", '-', "Decrease font size for file contents.");//, "./font-minus-icon.png", true);
        btnMinusFont.addActionListener(e -> decreaseFontSize());
        JButton btnResetFont = new AppButton("✔", '0', "Reset font size for file contents.");//, "./font-reset-icon.png", true);
        btnResetFont.addActionListener(e -> resetFontSize());
        btnWarning = new JButton("!");
        btnWarning.setToolTipText("Warning indicator.");
        setColors(new JButton[]{btnPlusFont, btnMinusFont, btnResetFont, btnWarning});

        jcbMatchCase = new JCheckBox("case",
                Boolean.parseBoolean(configs.getConfig(DefaultConfigs.Config.MATCH_CASE)));
        jcbMatchCase.setMnemonic('m');
        jcbMatchCase.setToolTipText("Match case");
        jcbWholeWord = new JCheckBox("word",
                Boolean.parseBoolean(configs.getConfig(DefaultConfigs.Config.WHOLE_WORD)));
        jcbWholeWord.setMnemonic('w');
        jcbWholeWord.setToolTipText("Whole word");

        txtSearch = new JTextField(configs.getConfig(DefaultConfigs.Config.SEARCH));
        AppLabel lblSearch = new AppLabel("Search", txtSearch, 'H');
        txtSearch.setColumns(TXT_COLS - 5);
        btnSearch = new AppButton("Search", 'S');
        btnSearch.addActionListener(evt -> searchFile());
        cbLastN = new JComboBox<>(getLastNOptions());
        cbLastN.setSelectedItem(Integer.parseInt(configs.getConfig(DefaultConfigs.Config.LAST_N)));
        AppLabel lblLastN = new AppLabel("Last N", cbLastN, 'N');
        btnLastN = new AppButton("Read", 'R', "Read last N lines and highlight.");
        btnLastN.addActionListener(evt -> threadPool.submit(new LastNRead(this)));
        cbSearches = new JComboBox<>(getSearches());
        cbSearches.addPopupMenuListener(new BoundsPopupMenuListener(CB_LIST_WIDER, CB_LIST_ABOVE));
        JComboToolTipRenderer cbSearchRenderer = new JComboToolTipRenderer();
        cbSearches.setRenderer(cbSearchRenderer);
        cbSearches.setPrototypeDisplayValue("Pattern");
        addCBSearchAL();
        AppLabel lblRSearches = new AppLabel("Recent", cbSearches, 'e', "Recently used searche-patterns list");
        JButton btnListRS = new AppButton("", 'I', "Search recently used search-patterns list.", "./search-icon.png");
        btnListRS.addActionListener(e -> showListRS());
        JButton btnCancel = new AppButton("", 'C', "Cancel/Stop Search/Read.", "./cancel-icon.png", false);
        btnCancel.addActionListener(evt -> cancelSearch());

        JButton btnExit = new AppExitButton();

        filePanel.setLayout(new FlowLayout());
        filePanel.add(lblFilePath);
        filePanel.add(txtFilePath);
        filePanel.add(lblRFiles);
        filePanel.add(btnListRF);
        filePanel.add(cbFiles);
        filePanel.add(jcbMatchCase);
        filePanel.add(jcbWholeWord);
        TitledBorder titledFP = new TitledBorder("File to search");
        filePanel.setBorder(titledFP);

        searchPanel.setLayout(new FlowLayout());
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
//        jtbActions.add(btnCancel);
        jtbActions.add(btnPlusFont);
        jtbActions.add(btnMinusFont);
        jtbActions.add(btnResetFont);
        jtbActions.add(btnWarning);
        TitledBorder titledSP = new TitledBorder("Pattern to search");
        searchPanel.setBorder(titledSP);

        TitledBorder titledEP = new TitledBorder("Exit");
        exitPanel.setBorder(titledEP);
        exitPanel.add(btnExit);

        inputPanel.setLayout(new GridBagLayout());
        inputPanel.add(filePanel);
        inputPanel.add(searchPanel);
        inputPanel.add(exitPanel);

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

        setToCenter();
    }

    private void setColors(JButton[] btns) {
        for (JButton b : btns) {
            b.setBackground(Color.GRAY);
            b.setForeground(Color.WHITE);
        }
    }

    private Font getFontForEditor(Font defaultFont, String sizeStr) {
        Font retVal = defaultFont;
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
        } else {
            logger.log("Ignoring request for " + opr + "font. Present " + printFontDetail(font));
        }
    }

    private Font getNewFont(Font font, int size) {
        return new Font(font.getName(), font.getStyle(), size);
    }

    private void showListRF() {
        showRecentList(cbFiles, txtFilePath, "Recently used files");
    }

    private void showListRS() {
        showRecentList(cbSearches, txtSearch, "Recently used search-pattern");
    }

    private void showRecentList(JComboBox<String> src, JTextField destination, String colName) {
        DefaultTableModel model = new DefaultTableModel() {

            @Override
            public int getColumnCount() {
                return 1;
            }

            @Override
            public String getColumnName(int index) {
                return colName;
            }

        };

        JFrame frame = new JFrame();

        List<String> favs = new ArrayList<>();

        JTextField txtFilter = new JTextField();
        txtFilter.setColumns(30);
        JTable table = new JTable(model);
        deleteAndCreateRows(src, table, model, favs);

        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);

        table.getColumnModel().getColumn(0).setMinWidth(25);

        addFilter(sorter, txtFilter);

        JButton[] btnFavs;
        final int FAV_BTN_LIMIT = 5;

        btnFavs = new JButton[FAV_BTN_LIMIT];
        for (int i = 0; i < FAV_BTN_LIMIT; i++) {
            btnFavs[i] = new JButton();
        }
        redrawFavBtns(btnFavs, favs, FAV_BTN_LIMIT, destination, frame);

        JPanel favBtnPanel = new JPanel(new GridBagLayout());
        TitledBorder titledFP = new TitledBorder("Favourites (starts with *)");
        favBtnPanel.setBorder(titledFP);
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 0.5;
        c.gridx = 0;
        c.gridy = 0;
        for (JButton b : btnFavs) {
            c.gridx++;
            favBtnPanel.add(b, c);
        }


        // For making contents non editable
        table.setDefaultEditor(Object.class, null);

        table.setAutoscrolls(true);
        table.setPreferredScrollableViewportSize(table.getPreferredSize());

        table.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent mouseEvent) {
                JTable table = (JTable) mouseEvent.getSource();
                Point point = mouseEvent.getPoint();
                int row = table.rowAtPoint(point);
                if (mouseEvent.getClickCount() == 2 && table.getSelectedRow() != -1) {
                    destination.setText(table.getValueAt(row, 0).toString());
                    frame.setVisible(false);
                }
            }
        });

        InputMap im = table.getInputMap();
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "Action.RunCmdCell");
        ActionMap am = table.getActionMap();
        am.put("Action.RunCmdCell", new CopyCommandAction(table, destination, frame));

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
        topPanel.add(favBtnPanel);
        topPanel.add(filterPanel);

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        panel.setBorder(emptyBorder);

        Container pc = frame.getContentPane();
        pc.setLayout(new BorderLayout());
        pc.add(panel);
        frame.setTitle("Double-click OR select & Enter");
        frame.setAlwaysOnTop(true);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        frame.setBackground(Color.CYAN);
        frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
    }

    private void cleanFavBtns(JButton[] btnFavs) {
        for (JButton b : btnFavs) {
            b.setText("X");
            b.setToolTipText("");
            b.setEnabled(false);
        }
    }

    private String checkLength(String s) {
        final int BTN_TEXT_LIMIT = 8;
        if (s.length() > BTN_TEXT_LIMIT) {
            return s.substring(0, BTN_TEXT_LIMIT - Utils.ELLIPSIS.length()) + Utils.ELLIPSIS;
        }
        return s;
    }

    private void redrawFavBtns(JButton[] btnFavs, List<String> favs, int FAV_BTN_LIMIT, JTextField destination, JFrame frame) {
        cleanFavBtns(btnFavs);
        AtomicInteger idx = new AtomicInteger();
        for (String fn : favs) {
            if (idx.get() >= FAV_BTN_LIMIT) {
                break;
            }
            JButton b = btnFavs[idx.get()];
            b.setEnabled(true);
            b.setText(checkLength(getDisplayName(fn)));
            b.setToolTipText(fn);
            if (b.getActionListeners() != null && b.getActionListeners().length == 0) {
                b.addActionListener(evt -> {
                    destination.setText(b.getToolTipText());
                    frame.setVisible(false);
                });
            }
            idx.getAndIncrement();
        }
    }

    static class CopyCommandAction extends AbstractAction {

        private final JTable table;
        private final JTextField destination;
        private final JFrame frame;

        public CopyCommandAction(JTable table, JTextField dest, JFrame frame) {
            this.table = table;
            this.destination = dest;
            this.frame = frame;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            destination.setText((table.getValueAt(table.getSelectedRow(), 0).toString()));
            frame.setVisible(false);
        }
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

    private void deleteAndCreateRows(JComboBox<String> src, JTable table, DefaultTableModel model, List<String> favs) {
        int rows = table.getRowCount();
        for (int i = 0; i < rows; i++) {
            model.removeRow(i);
        }

        int items = src.getItemCount();
        for (int i = 0; i < items; i++) {
            String s = src.getItemAt(i);
            model.addRow(new String[]{s});
            if (s.startsWith("*")) {
                favs.add(s);
            }
        }
    }

    private Integer[] getLastNOptions() {
        return new Integer[]{200, 500, 1000, 2000, 3000, 4000, 5000};
    }

    private void resetForNewSearch() {
        disableControls();
        resetShowWarning();
        emptyResults();
        updateRecentSearchVals();
        qMsgsToAppend.clear();
        setSearchStrings();
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

    private void addCBSearchAL() {
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

    private void addCBFilesAL() {
        cbFiles.addActionListener(e -> setFileToSearch(cbFiles.getSelectedItem().toString()));
    }

    private String[] getFiles() {
        return configs.getConfig(DefaultConfigs.Config.RECENT_FILES).split(";");
    }

    private String[] getSearches() {
        return configs.getConfig(DefaultConfigs.Config.RECENT_SEARCHES).split(";");
    }

    private void resetShowWarning() {
        showWarning = false;
        occrTillNow = 0;
        btnWarning.setBackground(Color.GRAY);
    }

    private void cancelSearch() {
        //SwingUtilities.invokeLater(() -> {
        resetShowWarning();
        if (status == Status.READING) {
            logger.warn("Search cancelled by user.");
            status = Status.CANCELLED;
        }
        //});
    }

    private void searchFile() {
        resetForNewSearch();
        if (isValidate()) {
            status = Status.READING;
            logger.log(getSearchDetails());
            threadPool.submit(new SearchFileCallable(this));
            threadPool.submit(new TimerCallable(this));
        } else {
            enableControls();
        }
    }

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
        return result;
    }

    private void updateRecentSearchVals() {
        recentFilesStr = checkItems(getFilePath(), recentFilesStr);
        recentSearchesStr = checkItems(getSearchString(), recentSearchesStr);
        removeCBFilesAL();
        cbFiles.removeAllItems();
        Arrays.stream(recentFilesStr.split(Utils.SEMI_COLON)).
                forEach(s -> {
                    if (Utils.hasValue(s)) {
                        cbFiles.addItem(s);
                    }
                });
        addCBFilesAL();

        removeCBSearchAL();
        cbSearches.removeAllItems();
        Arrays.stream(recentSearchesStr.split(Utils.SEMI_COLON)).
                forEach(s -> {
                    if (Utils.hasValue(s)) {
                        cbSearches.addItem(s);
                    }
                });
        addCBSearchAL();
    }

    private String checkItems(String searchStr, String csv) {
        if (Utils.isInArray(csv.split(Utils.SEMI_COLON), searchStr)) {
            // remove so after add it will come on top
            csv = csv.replace(searchStr + Utils.SEMI_COLON, "");
        }
        csv = searchStr + Utils.SEMI_COLON + csv;

        if (csv.split(Utils.SEMI_COLON).length >= RECENT_LIMIT) {
            csv = csv.substring(0, csv.lastIndexOf(Utils.SEMI_COLON));
        }

        return csv;
    }

    private String getSearchDetails() {
        return String.format("Starting search in file [%s] for pattern [%s] " +
                        "with criteria MatchCase [%s], WholeWord[%s] at time [%s]",
                getFilePath(),
                getSearchString(),
                getMatchCase(),
                getWholeWord(),
                new Date(startTime));
    }

    private void updateControls(boolean enable) {
        txtFilePath.setEnabled(enable);
        txtSearch.setEnabled(enable);
        btnSearch.setEnabled(enable);
        btnLastN.setEnabled(enable);
        cbFiles.setEnabled(enable);
        cbSearches.setEnabled(enable);
        cbLastN.setEnabled(enable);
        jcbMatchCase.setEnabled(enable);
        jcbWholeWord.setEnabled(enable);
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

    class LastNRead implements Callable<Boolean> {

        SearchBigFile sbf;

        LastNRead(SearchBigFile sbf) {
            this.sbf = sbf;
        }

        @Override
        public Boolean call() {
            resetForNewSearch();
            boolean hasError = false;
            readNFlag = true;
            startThread(new TimerCallable(sbf));
            final int LIMIT = Integer.parseInt(cbLastN.getSelectedItem().toString());
            updateTitle("Reading last " + LIMIT + " lines");
            String fn = getFileToSearch(getFilePath());
            logger.log("Loading last " + LIMIT + " lines from: " + fn);
            int readLines = 0;
            StringBuilder sb = new StringBuilder();
            int occr = 0;
            File file = new File(fn);
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

                        int len = sb.toString().split(searchStr).length;
                        occr += len > 0 ? len - 1 : 0;
                        sb = new StringBuilder();
                        readLines++;
                        // Last line will be printed after loop
                        if (readLines == LIMIT - 1) {
                            break;
                        }
                        if (status == Status.CANCELLED) {
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
                        TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - startTime),
                        readLines,
                        occr);
                String statusStr = status == Status.CANCELLED ? "Read cancelled - " : "Read complete - ";
                updateTitle(statusStr + result);
            }
            status = Status.DONE;

            return true;
        }
    }

    // For now its obsolete due to async line numbers
    @Deprecated
    class AppendData extends SwingWorker<Integer, String> {
        String data;

        AppendData(String data) {
            this.data = data;
        }

        @Override
        public Integer doInBackground() {
            appendResultNoFormat(data);
            return 1;
        }
    }

    // To avoid async order of lines this cannot be worker
    class SearchData {//} extends SwingWorker<Integer, String> {

        final int LINES_TO_INFORM = 500000;
        private final SearchStats stats;

        SearchData(SearchStats stats) {
            this.stats = stats;
        }

        public Integer process() {
//        @Override
//        public Integer doInBackground() {
            long lineNum = stats.getLineNum();
            StringBuilder sb = new StringBuilder();

            if (stats.isMatch()) {
                int occr = lowerCaseSplit(stats.getLine(), stats.getSearchPattern());
                stats.setOccurrences(stats.getOccurrences() + occr - 1);
                sb.append(getLineNumStr(lineNum)).append(stats.getLine()).append(System.lineSeparator());
                //synchronized (SearchBigFile.class) {
                qMsgsToAppend.add(convertStartingSpacesForHtml(sb.toString()));
                //}
            }
            stats.setLineNum(lineNum + 1);

            if (lineNum % LINES_TO_INFORM == 0) {
                logger.log("Lines searched so far: " + NumberFormat.getNumberInstance().format(lineNum));
            }

            occrTillNow = stats.getOccurrences();
            if (!showWarning && stats.getOccurrences() > OCCUR_LIMIT_FOR_WARN_IN_SEC) {
                showWarning = true;
            }

            return 1;
        }
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
                // Go to end
                tpResults.select(htmlDoc.getLength(), htmlDoc.getLength());
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

    public void updateTitle(String info) {
        setTitle((Utils.hasValue(info) ? TITLE + Utils.SP_DASH_SP + info : TITLE));
        /*SwingUtilities.invokeLater(() ->
                setTitle((Utils.hasValue(info) ? TITLE + Utils.SP_DASH_SP + info : TITLE))
        );*/
    }

    static class TimerCallable implements Callable<Boolean> {

        private final SearchBigFile sbf;

        public TimerCallable(SearchBigFile sbf) {
            this.sbf = sbf;
        }

        @Override
        public Boolean call() {
            do {
                // Due to multi threading, separate if is imposed
                if (status == Status.READING) {
                    long timeElapse = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - startTime);
                    String msg = timeElapse + " sec";
                    if (showWarning || timeElapse > TIME_LIMIT_FOR_WARN_IN_SEC) {
                        msg += sbf.getWarning();
                        sbf.btnWarning.setBackground(
                                sbf.btnWarning.getBackground() == Color.RED ?
                                        Color.GRAY : Color.RED);
                    }
                    sbf.updateTitle(msg);
                    Utils.sleep(1000, sbf.logger);
                }
            } while (status == Status.READING);
            return true;
        }
    }

    private String getWarning() {
        return " - Either search taking long or too many results [" + occrTillNow + "] !!  Cancel and try to narrow";
    }

    static class AppendMsgCallable implements Callable<Boolean> {

        SearchBigFile sbf;

        public AppendMsgCallable(SearchBigFile sbf) {
            this.sbf = sbf;
        }

        @Override
        public Boolean call() {
            StringBuilder sb = new StringBuilder();

            while (!qMsgsToAppend.isEmpty()) {
                String m;
                //synchronized (SearchBigFile.class) {
                m = qMsgsToAppend.poll();
                //}
                if (readNFlag || Utils.hasValue(m)) {
                    sb.append(m);
                }
            }
            if (readNFlag || sb.length() > 0) {
                sbf.appendResult(sb.toString());
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
            String searchPattern = sbf.getSearchString();

            if (!sbf.isMatchCase()) {
                searchPattern = searchPattern.toLowerCase();
            }
            if (sbf.isWholeWord()) {
                searchPattern = ".*\\b" + searchPattern + "\\b.*";
            }

            String path = getFileToSearch(sbf.getFilePath());

            try (InputStream stream = new FileInputStream(path);
                 Scanner sc = new Scanner(stream, "UTF-8")
            ) {
            /*try (InputStream stream = new FileInputStream(path);
                 BufferedReader br = new BufferedReader(new InputStreamReader(stream), BUFFER_SIZE)
            ) {*/
                long lineNum = 1, occurrences = 0;
                SearchStats stats = new SearchStats(lineNum, occurrences, null, searchPattern);
                SearchData searchData = new SearchData(stats);

                /*String line;
                while ((line = br.readLine()) != null) {*/
                while (sc.hasNextLine()) {
                    String line = sc.nextLine();
                    stats.setLine(line);
                    stats.setMatch((!isMatchCase() && line.toLowerCase().contains(searchPattern))
                            || (isMatchCase() && line.contains(searchPattern))
                            || (isWholeWord() && line.matches(searchPattern))
                    );

                    /*try {
                        SwingUtilities.invokeAndWait(searchData);
                    } catch (InterruptedException | InvocationTargetException e) {
                        logger.error(e);
                    }*/
                    //searchData.doInBackground();
                    searchData.process();
                    if (qMsgsToAppend.size() > APPEND_MSG_CHUNK) {
                        startThread(msgCallable);
                    }
                    if (status == Status.CANCELLED) {
                        sbf.appendResultNoFormat("---------------------Search cancelled----------------------------" + System.lineSeparator());
                        break;
                    }
                }

                while (qMsgsToAppend.size() > 0) {
                    Utils.sleep(200, sbf.logger);
                    startThread(msgCallable);
                    Utils.sleep(200, sbf.logger);
                }

                long seconds = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - startTime);
                String result = getSearchResult(path, seconds, stats.getLineNum(), stats.occurrences);
                if (stats.getOccurrences() == 0) {
                    sbf.appendResultNoFormat("No match found. ");
                }

                if (status == Status.CANCELLED) {
                    sbf.updateTitle("Search cancelled - " + result);
                } else {
                    sbf.appendResultNoFormat("---------------------Search complete----------------------------" + System.lineSeparator());
                    sbf.logger.log(result);
                    sbf.updateTitle("Search complete - " + result);
                }
                status = Status.DONE;
            } catch (IOException e) {
                sbf.logger.error(e.getMessage());
                sbf.tpResults.setText("Unable to search file");
            } finally {
                sbf.enableControls();
            }

            return true;
        }
    }

    private String getSearchResult(String path, long seconds, long lineNum, long occurrences) {
        return String.format("File size: %s, " +
                        "time taken: [%s sec], lines read: [%s], occurrences: [%s]",
                Utils.getFileSizeString(new File(path).length()),
                seconds,
                lineNum,
                occurrences);
    }

    private String chopStar(String cmd) {
        if (cmd.startsWith("*")) {
            cmd = cmd.substring(1);
        }
        return cmd;
    }

    private String getFileToSearch(String name) {
        String chk = " (";
        name = name.contains(chk) ?
                name.substring(0, name.indexOf(chk)) : name;
        return chopStar(name);
    }

    private String getDisplayName(String cmd) {
        String chk = " (";
        return cmd.contains(chk) ?
                cmd.substring(cmd.indexOf(chk) + chk.length(), cmd.lastIndexOf(")")) :
                cmd.substring(cmd.lastIndexOf(Utils.SLASH) + Utils.SLASH.length());
    }

}

