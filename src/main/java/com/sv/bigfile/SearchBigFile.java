package com.sv.bigfile;

import com.sv.bigfile.helpers.CopyCommandAction;
import com.sv.bigfile.helpers.FontChangerTask;
import com.sv.bigfile.helpers.StartWarnIndicator;
import com.sv.core.DefaultConfigs;
import com.sv.core.MyLogger;
import com.sv.core.Utils;
import com.sv.swingui.*;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
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
import java.util.List;
import java.util.Queue;
import java.util.*;
import java.util.Timer;
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

    public enum AppFonts {
        CALIBRI("Calibri"),
        ALGERIAN("Algerian"),
        ELEPHANT("Elephant"),
        LUCIDA("Lucida Bright"),
        LUCIDA_ITALIC("Lucida Calligraphy Italic"),
        SEGOE_UI("Segoe UI"),
        TAHOMA("Tahoma"),
        TNR("Times New Roman"),
        VARDANA("Vardana"),
        ARIAL("Arial Black"),
        COMIC("Comic Sans MS"),
        CONSOLAS("Consolas");

        String font;

        AppFonts(String font) {
            this.font = font;
        }

        public String getFont() {
            return font;
        }
    }


    enum MsgType {
        INFO, WARN, ERROR
    }

    private MyLogger logger;
    private DefaultConfigs configs;

    private JPanel msgPanel;
    private JLabel lblMsg;
    private JButton btnPlusFont, btnMinusFont, btnResetFont, btnFontInfo;
    private JButton btnGoTop, btnGoBottom, btnNextOccr, btnPreOccr;
    private JButton btnSearch, btnLastN, btnCancel;
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
    private static final long FONT_CHANGE_TIME = TimeUnit.MINUTES.toMillis(10);
    private static final int PREFERRED_FONT_SIZE = 12;
    private static final int DEFAULT_FONT_SIZE = 12;
    private static final int MIN_FONT_SIZE = 8;
    private static final int MAX_FONT_SIZE = 24;
    private static final int RECENT_LIMIT = 20;
    private static final int SEARCH_STR_LEN_LIMIT = 2;
    private static final int WARN_LIMIT_SEC = 20;
    private static final int FORCE_STOP_LIMIT_SEC = 50;
    private static final int WARN_LIMIT_OCCR = 200;
    private static final int FORCE_STOP_LIMIT_OCCR = 500;
    private static final int APPEND_MSG_CHUNK = 100;
    private static final int eb = 5;
    private static final Border emptyBorder = new EmptyBorder(new Insets(eb, eb, eb, eb));

    private final String TITLE = "Search File";
    private final String Y_BG_FONT_PREFIX = "<font style=\"background-color:yellow\">";
    private final String R_FONT_PREFIX = "<font style=\"color:red\">";
    private final String FONT_SUFFIX = "</font>";

    private static boolean showWarning = false;
    private static boolean readNFlag = false;
    private static long insertCounter = 0;
    private static long readCounter = 0;
    private static long startTime = System.currentTimeMillis();
    private static int fontIdx = 0;

    private boolean debugAllowed;
    private String searchStr, searchStrEsc, searchStrReplace, operation;
    private String recentFilesStr, recentSearchesStr;
    private long timeTillNow;
    private long occrTillNow;
    private long linesTillNow;

    private static Status status = Status.NOT_STARTED;

    // indexed structure to maintain line indexing
    private static Map<Long, String> idxMsgsToAppend;
    private static List<Integer> lineOffsets;
    private static int lineOffsetsIdx;
    private static int globalCharIdx;
    private static String htmlDocText;

    // LIFO
    private static Queue<String> qMsgsToAppend;
    private static AppendMsgCallable msgCallable;
    private static final ExecutorService threadPool = Executors.newFixedThreadPool(5);

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
        log("Debug enabled " + logger.isDebug());
        qMsgsToAppend = new LinkedBlockingQueue<>();
        idxMsgsToAppend = new ConcurrentHashMap<>();
        lineOffsets = new ArrayList<>();
        lineOffsetsIdx = -1;
        recentFilesStr = getCfg(Configs.RecentFiles);
        recentSearchesStr = getCfg(Configs.RecentSearches);
        msgCallable = new AppendMsgCallable(this);

        Container parentContainer = getContentPane();
        parentContainer.setLayout(new BorderLayout());

        setTitle(TITLE);

        JPanel filePanel = new JPanel();

        final int TXT_COLS = 12;
        UIName uin = UIName.LBL_FILE;
        txtFilePath = new JTextField(getCfg(Configs.FilePath));
        AppLabel lblFilePath = new AppLabel(uin.name, txtFilePath, uin.mnemonic);
        txtFilePath.setColumns(TXT_COLS);
        uin = UIName.BTN_FILE;
        JButton btnFileOpen = new AppButton(uin.name, uin.mnemonic, uin.tip, "", true);
        btnFileOpen.addActionListener(e -> openFile());
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
        JToolBar jtbFile = new JToolBar();
        jtbFile.setFloatable(false);
        jtbFile.setRollover(false);
        jtbFile.add(txtFilePath);
        jtbFile.add(btnFileOpen);
        filePanel.add(jtbFile);
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
        btnLastN.addActionListener(evt -> readFile());
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
        btnCancel = new AppButton(uin.name, uin.mnemonic, uin.tip, "./icons/cancel-icon.png", true);
        btnCancel.setDisabledIcon(new ImageIcon("./icons/cancel-icon-disabled.png"));
        btnCancel.addActionListener(evt -> cancelSearch());

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
        searchPanel.setBorder(new TitledBorder("Pattern to search"));

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
        uin = UIName.BTN_GOTOP;
        btnGoTop = new AppButton(uin.name, uin.mnemonic, uin.tip);
        btnGoTop.addActionListener(e -> goToFirst());
        uin = UIName.BTN_GOBOTTOM;
        btnGoBottom = new AppButton(uin.name, uin.mnemonic, uin.tip);
        btnGoBottom.addActionListener(e -> goToEnd());
        uin = UIName.BTN_NEXTOCCR;
        btnNextOccr = new AppButton(uin.name, uin.mnemonic, uin.tip);
        btnNextOccr.addActionListener(e -> nextOccr());
        uin = UIName.BTN_PREOCCR;
        btnPreOccr = new AppButton(uin.name, uin.mnemonic, uin.tip);
        btnPreOccr.addActionListener(e -> preOccr());

        setBkColors(new JButton[]{btnPlusFont, btnMinusFont, btnResetFont,
                btnFontInfo, btnGoTop, btnGoBottom, btnNextOccr, btnPreOccr});

        JPanel controlPanel = new JPanel();
        JButton btnExit = new AppExitButton();
        TitledBorder titledEP = new TitledBorder("Controls");
        controlPanel.setBorder(titledEP);
        controlPanel.add(jtbActions);
        jtbActions.add(btnPlusFont);
        jtbActions.add(btnMinusFont);
        jtbActions.add(btnResetFont);
        jtbActions.add(btnFontInfo);
        jtbActions.add(btnGoTop);
        jtbActions.add(btnGoBottom);
        jtbActions.add(btnPreOccr);
        jtbActions.add(btnNextOccr);
        controlPanel.add(btnExit);

        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new GridBagLayout());
        inputPanel.add(filePanel);
        inputPanel.add(searchPanel);
        inputPanel.add(controlPanel);

        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BorderLayout());
        topPanel.add(inputPanel, BorderLayout.NORTH);
        msgPanel = new JPanel();
        msgPanel.setBorder(new LineBorder(Color.BLUE, 1, true));
        lblMsg = new JLabel(getInitialMsg());
        lblMsg.setFont(getNewFont(lblMsg.getFont(), Font.PLAIN, 12));
        msgPanel.add(lblMsg);
        topPanel.add(msgPanel, BorderLayout.SOUTH);
        resetShowWarning();

        tpResults = new JEditorPane();
        tpResults.setEditable(false);
        tpResults.setContentType("text/html");
        tpResults.setFont(getFontForEditor(getCfg(Configs.FontSize)));
        tpResults.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        htmlDoc = new HTMLDocument();
        tpResults.setDocument(htmlDoc);
        kit = new HTMLEditorKit();
        JScrollPane jspResults = new JScrollPane(tpResults);
        jspResults.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        jspResults.setBorder(emptyBorder);

        parentContainer.add(topPanel, BorderLayout.NORTH);
        parentContainer.add(jspResults, BorderLayout.CENTER);

        btnExit.addActionListener(evt -> exitForm());
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent evt) {
                exitForm();
            }
        });

        btnFontInfo.setText(getFontSize());
        resetForNewSearch();
        enableControls();
        new Timer().schedule(new FontChangerTask(this), 0, FONT_CHANGE_TIME);
        setToCenter();
    }

    private void nextOccr() {
        increaseOffsetIdx();
        gotoOccr(lineOffsetsIdx);
    }

    private void increaseOffsetIdx() {
        lineOffsetsIdx++;
        if (lineOffsetsIdx > lineOffsets.size() - 1) {
            lineOffsetsIdx = 0;
        }
    }

    private void decreaseOffsetIdx() {
        lineOffsetsIdx--;
        if (lineOffsetsIdx < 0) {
            lineOffsetsIdx = lineOffsets.size() - 1;
        }
        if (lineOffsetsIdx < 0) {
            lineOffsetsIdx = 0;
        }
    }

    private void preOccr() {
        decreaseOffsetIdx();
        gotoOccr(lineOffsetsIdx);
    }

    private void gotoOccr(int idx) {

        debug("Going to occurrence with index " + idx);
        if (lineOffsets.size() == 0) {
            updateOffsets();
        }

        if (lineOffsets.size() != 0 && lineOffsets.size() > idx) {
            int ix = lineOffsets.get(idx);
            selectAndGoToIndex(ix, ix + searchStr.length());
            updateMsgAsInfo("Going occurrences # " + (idx + 1) + "/" + lineOffsets.size());
        } else {
            updateMsg("No occurrences to show", MsgType.WARN);
        }
    }

    private void openFile() {
        File file = new File(".");
        if (Utils.hasValue(getFilePath())) {
            File tmpFile = new File(getFilePath());
            tmpFile = tmpFile.getParentFile();
            if (tmpFile.isDirectory()) {
                file = tmpFile;
            }
        }
        JFileChooser jfc = new JFileChooser(file);
        int returnVal = jfc.showOpenDialog(this);

        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File selectedFile = jfc.getSelectedFile();
            setFileToSearch(selectedFile.getAbsolutePath());
            //This is where a real application would open the file.
            debug("Selected file: " + file.getAbsolutePath());
        } else {
            debug("Open command cancelled by user.");
        }
    }

    private String getInitialMsg() {
        return "This bar turns 'Orange' for showing warning and 'Red' for error/force-stop. " +
                "Warning limit for time [" + WARN_LIMIT_SEC
                + " sec] and occurrences [" + WARN_LIMIT_OCCR
                + "]. Error" +
                " limit for time [" + FORCE_STOP_LIMIT_SEC
                + " sec] and occurrences [" + FORCE_STOP_LIMIT_OCCR + "]";

    }

    //TODO: read from resource path and override icons
    private String getResourcePath(String path) {
        ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        return classloader.getResource(path).toString();
    }

    private void setBkColors(JButton[] btns) {
        for (JButton b : btns) {
            b.setBackground(Color.GRAY);
            b.setForeground(Color.WHITE);
        }
    }

    private Font getFontForEditor(String sizeStr) {
        Font retVal = SwingUtils.getPlainCalibriFont(Utils.hasValue(sizeStr) ? Integer.parseInt(sizeStr) : PREFERRED_FONT_SIZE);
        logger.log("Returning " + getFontDetail(retVal));
        return retVal;
    }

    private void printFontDetail(Font font) {
        log(getFontDetail(font));
    }

    private String getFontDetail(Font f) {
        return String.format("Font: %s/%s/%s", f.getName(), (f.isBold() ? "bold" : "plain"), f.getSize());
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
            String m = "Applying new font as " + getFontDetail(font);
            logger.log(m);
            tpResults.setFont(font);
            btnFontInfo.setText(getFontSize());
            updateMsgAsInfo(m);
        } else {
            logger.log("Ignoring request for " + opr + "font. Present " + getFontDetail(font));
        }
    }

    private Font getNewFont(Font font, int size) {
        return getNewFont(font, font.getStyle(), size);
    }

    private Font getNewFont(Font font, int style, int size) {
        log("Returning font as " + font.getName() + ", style " + (font.isBold() ? "bold" : "plain") + ", of size " + size);
        return new Font(font.getName(), style, size);
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
        debug("reset for new search");
        printMemoryDetails();
        insertCounter = 0;
        readCounter = 0;
        disableControls();
        resetShowWarning();
        emptyResults();
        updateRecentSearchVals();
        qMsgsToAppend.clear();
        idxMsgsToAppend.clear();
        globalCharIdx = 0;
        htmlDocText = "";
        lineOffsets.clear();
        lineOffsetsIdx = -1;
        setSearchStrings();
        logger.log(getSearchDetails());
        startTime = System.currentTimeMillis();
        status = Status.READING;
        readNFlag = false;
    }

    private void printMemoryDetails() {
        long total = Runtime.getRuntime().totalMemory();
        long free = Runtime.getRuntime().freeMemory();
        debug(String.format("Total: %s, Free: %s, Occupied: %s",
                Utils.getFileSizeString(total),
                Utils.getFileSizeString(free),
                Utils.getFileSizeString(total - free)
        ));
    }

    private String getLineNumStr(long line) {
        // Due to html escaping removing bold tags
        //return "<b>" + line + "</b> ";
        return line + "&nbsp;&nbsp;";
    }

    private int calculateOccr(String line, String pattern) {
        String lineLC = line.toLowerCase();
        String patternLC = pattern.toLowerCase();
        int occr = 0;
        if (Utils.hasValue(lineLC) && Utils.hasValue(patternLC)) {
            occr = lineLC.split(patternLC).length;
            if (!lineLC.endsWith(patternLC)) {
                occr--;
            }
        }
        /*String lineStr = "";
        if (occr > 0) {
            lineStr = "line [" + line + "],";
        }
        debug("calculateOccr: " + lineStr + " pattern [" + pattern + "], occr [" + occr + "]");*/
        return occr;
    }

    private void removeCBSearchAL() {
        Arrays.stream(cbSearches.getActionListeners()).forEach(a -> cbSearches.removeActionListener(a));
    }

    private void addCBSearchAction() {
        cbSearches.addActionListener(e -> setSearchPattern(cbSearches.getSelectedItem().toString()));
    }

    private void setSearchPattern(String s) {
        txtSearch.setText(s);
        updateMsgAsInfo("Search pattern set as [" + s + "]");
    }

    private void setFileToSearch(String s) {
        txtFilePath.setText(s);
        updateMsgAsInfo("File set as [" + s + "]");
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
        debug("reset show warning");
        showWarning = false;
        timeTillNow = 0;
        occrTillNow = 0;
        linesTillNow = 0;
        updateMsgAsInfo(getInitialMsg());
    }

    private void cancelSearch() {
        // To ensure background is red
        updateMsg("Search cancelled.", getMsgType());
        if (status == Status.READING) {
            logger.warn("Search cancelled by user.");
            status = Status.CANCELLED;
        }
    }

    private void searchFile() {
        if (isValidate()) {
            operation = "search";
            resetForNewSearch();
            updateMsgAsInfo("Starting [" + operation + "] for file " + getFilePath());
            status = Status.READING;
            threadPool.submit(new SearchFileCallable(this));
            threadPool.submit(new TimerCallable(this));
        } else {
            enableControls();
        }
    }

    private void readFile() {
        if (isValidate()) {
            operation = "read";
            resetForNewSearch();
            updateMsgAsInfo("Starting [" + operation + "] for file " + getFilePath());
            status = Status.READING;
            threadPool.submit(new LastNRead(this));
            threadPool.submit(new TimerCallable(this));
        } else {
            enableControls();
        }
    }

    private void updateTitleAndMsg(String s) {
        updateTitleAndMsg(s, MsgType.WARN);
    }

    private void updateTitleAndMsg(String s, MsgType type) {
        updateTitle(s);
        updateMsg(s, type);
    }

    private boolean isValidate() {
        updateTitle("");
        boolean result = true;
        if (!Utils.hasValue(getFilePath())) {
            updateTitleAndMsg("Validation error - REQUIRED: file to search");
            result = false;
        }
        if (result && !Utils.hasValue(getSearchString())) {
            updateTitleAndMsg("Validation error - REQUIRED: text to search");
            result = false;
        }
        if (result && getSearchString().length() < SEARCH_STR_LEN_LIMIT) {
            updateTitleAndMsg("Validation error - LENGTH: text to search should be " + SEARCH_STR_LEN_LIMIT + " or more characters");
            result = false;
        }

        if (!result) {
            logger.log("Validation failed !!");
        }

        return result;
    }

    private void updateRecentSearchVals() {
        debug("update recent search values");
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
        return String.format("Starting [%s] in file [%s] for pattern [%s] " +
                        "with criteria MatchCase [%s], WholeWord[%s], read-lines[%s] " +
                        "at time [%s]",
                operation,
                getFilePath(),
                getSearchString(),
                getMatchCase(),
                getWholeWord(),
                getLastN(),
                new Date(startTime));
    }

    private void updateControls(boolean enable) {
        debug("Updating controls: " + enable);
        Component[] components = {
                txtFilePath, txtSearch, btnSearch, btnLastN,
                cbFiles, cbSearches, cbLastN, jcbMatchCase,
                jcbWholeWord, btnPlusFont, btnMinusFont, btnResetFont,
                btnGoTop, btnGoBottom, btnNextOccr, btnPreOccr
        };

        Arrays.stream(components).forEach(c -> c.setEnabled(enable));
        updateContrastControls(!enable);
    }

    private void updateContrastControls(boolean enable) {
        debug("Updating contrast controls: " + enable);
        Component[] components = {
                btnCancel
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

    private String htmlEsc(String str) {
        return str.replaceAll(Utils.HtmlEsc.LT.getCh(), Utils.HtmlEsc.LT.getEscStr());
    }

    private void setSearchStrings() {
        searchStr = getSearchString();
        searchStrEsc = htmlEsc(searchStr);
        searchStrReplace = Y_BG_FONT_PREFIX + searchStr + FONT_SUFFIX;
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

        // Html escaping here, so html '<' character processed and searched properly
        data = htmlEsc(data);

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
        String s = searchStrEsc.toLowerCase();
        StringBuilder sb = new StringBuilder();
        while (data.toLowerCase().contains(s)) {
            int idx = data.toLowerCase().indexOf(s);
            sb.append(data, 0, idx)
                    .append(Y_BG_FONT_PREFIX)
                    .append(data, idx, idx + s.length())
                    .append(FONT_SUFFIX);
            data = data.substring(idx + s.length());
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

    public void updateMsgAsInfo(String msg) {
        updateMsg(msg, MsgType.INFO);
    }

    public void updateMsg(String msg, MsgType type) {
        Color b = Color.WHITE;
        Color f = Color.BLUE;
        if (type == MsgType.ERROR) {
            b = Color.RED;
            f = Color.WHITE;
        } else if (type == MsgType.WARN) {
            b = Color.ORANGE;
            f = Color.BLACK;
        }
        msgPanel.setBackground(b);
        lblMsg.setForeground(f);

        if (Utils.hasValue(msg)) {
            lblMsg.setText(msg);
        }
    }

    public String getWarning() {
        debug("getWarning: timeTillNow = " + timeTillNow + ", occrTillNow = " + occrTillNow);

        StringBuilder sb = new StringBuilder();
        if (timeTillNow > WARN_LIMIT_SEC) {
            sb.append("Warning: Time [").append(timeTillNow).append("] > warning limit [").append(WARN_LIMIT_SEC).append("]. ");
        }
        if (occrTillNow > WARN_LIMIT_OCCR) {
            sb.append("Warning: Occurrences [").append(occrTillNow).append("] > warning limit [").append(WARN_LIMIT_OCCR).append("], try to narrow your search.");
        }
        StringBuilder sbErr = new StringBuilder();
        if (timeTillNow > FORCE_STOP_LIMIT_SEC) {
            sbErr.append("Error: Time [").append(timeTillNow).append("] > force stop limit [").append(FORCE_STOP_LIMIT_SEC).append("]. ");
        }
        if (occrTillNow > FORCE_STOP_LIMIT_OCCR) {
            sbErr.append("Error: Occurrences [").append(occrTillNow).append("] > force stop limit [").append(FORCE_STOP_LIMIT_OCCR).append("], try to narrow your search.");
        }

        if (Utils.hasValue(sbErr.toString())) {
            sb = sbErr;
        }

        debug("Returning warning as " + sb.toString());
        return sb.toString();
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
                + "], lineOffsets size [" + lineOffsets.size()
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

    public void goToFirst() {
        goToFirst(true);
    }

    public void goToFirst(boolean showMsg) {
        // If this param removed then after search/read ends this msg displayed
        if (showMsg) {
            updateMsgAsInfo("Going to first line");
        }
        // Go to first
        selectAndGoToIndex(0);
    }

    public void goToEnd() {
        goToEnd(true);
    }

    public void goToEnd(boolean showMsg) {
        if (showMsg) {
            updateMsgAsInfo("Going to last line");
        }
        // Go to end
        selectAndGoToIndex(htmlDoc.getLength());
    }

    public void selectAndGoToIndex(int idx) {
        selectAndGoToIndex(idx, idx);
    }

    public void selectAndGoToIndex(int sIdx, int eIdx) {
        tpResults.grabFocus();
        tpResults.select(sIdx, eIdx);
    }

    public void finishAction() {
        printCounters();
        if (showWarning) {
            SwingUtilities.invokeLater(new StartWarnIndicator(this));
        }
        goToEnd(false);
    }

    private void updateOffsets() {
        debug("Offsets size " + lineOffsets.size());
        if (lineOffsets.size() == 0) {
            try {
                htmlDocText = htmlDoc.getText(0, htmlDoc.getLength()).toLowerCase();
                log("For offsets document length calculated as " + htmlDocText.length());

                int idx = 0;
                String strToSearch = searchStr.toLowerCase();
                debug("Starting search for string [" + strToSearch + "]");
                while (idx != -1) {
                    idx = htmlDocText.indexOf(strToSearch, globalCharIdx);
                    debug("idx = " + idx + ", globalCharIdx = " + globalCharIdx);
                    globalCharIdx = idx + strToSearch.length();
                    if (idx != -1) {
                        lineOffsets.add(idx);
                    }
                }
                log("Total occurrences offsets are " + lineOffsets.size());
                debug("All offsets are " + lineOffsets);

            } catch (BadLocationException e) {
                logger.error("Unable to get document text");
            }
        }
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

    public MsgType getMsgType() {
        if (isErrorState()) {
            return MsgType.ERROR;
        }
        if (isWarningState()) {
            return MsgType.WARN;
        }

        return MsgType.INFO;
    }

    public boolean isInfoState() {
        return isWarningState() || isErrorState();
    }

    public boolean isWarningState() {
        return timeTillNow > WARN_LIMIT_SEC || occrTillNow > WARN_LIMIT_OCCR;
    }

    public boolean isErrorState() {
        return timeTillNow > FORCE_STOP_LIMIT_SEC || occrTillNow > FORCE_STOP_LIMIT_OCCR;
    }

    public void incRCtrNAppendIdxData() {
        readCounter++;
        appendResult(idxMsgsToAppend.get(readCounter));
    }

    private String getNextFont() {
        if (fontIdx == AppFonts.values().length) {
            fontIdx = 0;
        }
        return AppFonts.values()[fontIdx++].getFont();
    }

    public void changeMsgFont() {
        Font f = lblMsg.getFont();
        f = new Font(getNextFont(), f.getStyle(), f.getSize());
        lblMsg.setFont(f);
        String msg = getFontDetail(f);
        lblMsg.setToolTipText("Font changes every 10 minutes. " + msg);
        updateMsgAsInfo(msg);
        debug(msg);
    }

    /*   Inner classes    */
    class LastNRead implements Callable<Boolean> {

        SearchBigFile sbf;

        LastNRead(SearchBigFile sbf) {
            this.sbf = sbf;
        }

        @Override
        public Boolean call() {
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
                                debug("Read: Starting msg callable and Q size is " + qMsgsToAppend.size());
                                startThread(msgCallable);
                            }
                        }

                        occr += calculateOccr(sb.toString(), searchPattern);
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
                            appendResultNoFormat("---------------------Read cancelled----------------------------" + System.lineSeparator());
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
                    debug("Read: After loop, starting msg callable and Q size is " + qMsgsToAppend.size());
                    startThread(msgCallable);
                }
                readLines++;
            } catch (IOException e) {
                sbf.tpResults.setText(R_FONT_PREFIX + "----------Unable to read file-------------" + FONT_SUFFIX);
                sbf.updateTitleAndMsg("Unable to read file: " + getFilePath(), MsgType.ERROR);
                hasError = true;
                logger.error(e);
            } finally {
                enableControls();
            }

            occr += calculateOccr(sb.toString(), searchStr);
            if (!hasError) {
                String result = getSearchResult(
                        fn,
                        getSecondsElapsedStr(startTime),
                        readLines,
                        occr);
                String statusStr = isCancelled() ? "Read cancelled - " : "Read complete - ";
                updateTitleAndMsg(statusStr + result, MsgType.INFO);
            }
            status = Status.DONE;

            // No need to wait as data can be async added at top
            sbf.finishAction();
            return true;
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
                int occr = calculateOccr(stats.getLine(), stats.getSearchPattern());
                stats.setOccurrences(stats.getOccurrences() + occr);
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
                    timeTillNow = timeElapse;
                    String msg = timeElapse + " sec, lines [" + sbf.linesTillNow + "] ";
                    if (showWarning || timeElapse > WARN_LIMIT_SEC) {
                        msg += sbf.getWarning();
                        sbf.debug("Invoking warning indicator.");
                        SwingUtilities.invokeLater(new StartWarnIndicator(sbf));
                    }
                    if (forceStop()) {
                        sbf.logger.warn("Stopping forcefully.");
                        cancelSearch();
                    }
                    sbf.updateTitle(msg);
                    logger.debug("Timer callable sleeping now for a second");
                    printMemoryDetails();
                    Utils.sleep(1000, sbf.logger);
                }
            } while (status == Status.READING);

            logger.debug("Timer: thread pool status " + threadPool.toString());
            logger.log("Timer stopped after " + timeElapse + " sec");
            return true;
        }
    }

    private boolean forceStop() {
        return timeTillNow > FORCE_STOP_LIMIT_SEC || occrTillNow > FORCE_STOP_LIMIT_OCCR;
    }

    private boolean hasOccr(String line, String searchPattern) {
        boolean result = (!isMatchCase() && line.toLowerCase().contains(searchPattern))
                || (isMatchCase() && line.contains(searchPattern))
                || (isWholeWord() && line.matches(searchPattern));

        String lineStr = "";
        if (result) {
            lineStr = "line [" + line + "],";
        }
        debug("hasOccr: " + lineStr + " pattern [" + searchPattern + "], result [" + result + "]");

        return result;
    }

    class AppendMsgCallable implements Callable<Boolean> {

        SearchBigFile sbf;

        public AppendMsgCallable(SearchBigFile sbf) {
            this.sbf = sbf;
        }

        @Override
        public Boolean call() {
            StringBuilder sb = new StringBuilder();

            synchronized (this) {
                logger.debug("Size of qMsgsToAppend is " + qMsgsToAppend.size());
                if (!qMsgsToAppend.isEmpty()) {
                    while (!qMsgsToAppend.isEmpty()) {
                        String m = qMsgsToAppend.poll();
                        if (readNFlag || Utils.hasValue(m)) {
                            sb.append(m);
                        }
                    }
                    if (readNFlag || sb.length() > 0) {
                        insertCounter++;
                        idxMsgsToAppend.put(insertCounter, sb.toString());
                        SwingUtilities.invokeLater(new AppendData());
                    }
                    logger.debug("All messages processed.  Now size is " + qMsgsToAppend.size());
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
                    stats.setMatch(sbf.hasOccr(line, searchPattern));

                    if (!isCancelled() && occrTillNow <= FORCE_STOP_LIMIT_OCCR) {
                        searchData.process();
                        if (qMsgsToAppend.size() > APPEND_MSG_CHUNK) {
                            debug("Search: Starting msg callable and Q size is " + qMsgsToAppend.size());
                            startThread(msgCallable);
                        }
                    }
                    if (isCancelled()) {
                        debug("---------------------Search cancelled----------------------------");
                        qMsgsToAppend.add("---------------------Search cancelled----------------------------" + System.lineSeparator());
                        startThread(msgCallable);
                        break;
                    }
                }

                logger.log("File read in " + getSecondsElapsedStr(time));

                if (!isCancelled()) {
                    time = System.currentTimeMillis();
                    startThread(msgCallable);
                    while (readCounter != insertCounter) {
                        if (!isCancelled()) {
                            debug("Status is cancelled.  Exiting wait condition.");
                            break;
                        }
                        logger.debug("Waiting for readCounter to be equal insertCounter");
                        Utils.sleep(200, sbf.logger);
                    }
                    logger.log("Time in waiting all message to append is " + getSecondsElapsedStr(time));
                }
                String result = getSearchResult(path, getSecondsElapsedStr(startTime), stats.getLineNum(), stats.getOccurrences());
                if (stats.getOccurrences() == 0) {
                    String s = "No match found";
                    sbf.tpResults.setText(R_FONT_PREFIX + s + FONT_SUFFIX);
                    sbf.updateMsg(s, MsgType.WARN);
                }

                if (isCancelled()) {
                    sbf.updateTitle("Search cancelled - " + result);
                } else {
                    qMsgsToAppend.add("---------------------Search complete----------------------------" + System.lineSeparator());
                    startThread(msgCallable);
                    sbf.updateTitleAndMsg("Search complete - " + result, MsgType.INFO);
                }
                status = Status.DONE;
            } catch (IOException e) {
                sbf.logger.error(e.getMessage());
                sbf.tpResults.setText(R_FONT_PREFIX + "----------Unable to search file-------------" + FONT_SUFFIX);
                sbf.updateTitleAndMsg("Unable to search file: " + getFilePath(), MsgType.ERROR);
                status = Status.DONE;
            } finally {
                logger.debug("Search: Enabling controls.");
                sbf.enableControls();
            }

            finishAction();
            return true;
        }

    }
}

