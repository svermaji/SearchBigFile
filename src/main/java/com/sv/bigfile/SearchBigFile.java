package com.sv.bigfile;

import com.sv.bigfile.helpers.CopyCommandAction;
import com.sv.bigfile.helpers.FontChangerTask;
import com.sv.bigfile.helpers.HelpColorChangerTask;
import com.sv.bigfile.helpers.StartWarnIndicator;
import com.sv.core.config.DefaultConfigs;
import com.sv.core.logger.MyLogger;
import com.sv.core.logger.MyLogger.MsgType;
import com.sv.core.Utils;
import com.sv.swingui.*;
import com.sv.swingui.component.*;
import com.sv.swingui.component.table.*;
import com.sv.swingui.UIConstants.*;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.StyleConstants;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.text.NumberFormat;
import java.util.List;
import java.util.Queue;
import java.util.Timer;
import java.util.*;
import java.util.concurrent.*;

import static com.sv.bigfile.AppConstants.*;
import static com.sv.core.Constants.*;
import static com.sv.swingui.UIConstants.*;

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
        LastN, FontSize, MatchCase, WholeWord, DebugEnabled, UseBRFileSizeInMB
    }

    enum Status {
        NOT_STARTED, READING, DONE, CANCELLED
    }

    enum FONT_OPR {
        INCREASE, DECREASE, RESET
    }

    private MyLogger logger;
    private DefaultConfigs configs;

    private JTabbedPane tabbedPane;
    private JMenu menuRFiles, menuRSearches;
    private JPanel msgPanel;
    private JLabel lblMsg;
    private JButton btnPlusFont, btnMinusFont, btnResetFont, btnFontInfo;
    private JButton btnGoTop, btnGoBottom, btnNextOccr, btnPreOccr, btnFind, btnHelp;
    private JButton btnSearch, btnLastN, btnCancel;
    private AppTextField txtFilePath, txtSearch;
    private JEditorPane epResults, tpHelp;
    private JScrollPane jspResults, jspHelp;
    private HTMLDocument htmlDoc;
    private HTMLEditorKit kit;
    private JCheckBox jcbMatchCase, jcbWholeWord;
    private JComboBox<Integer> cbLastN;

    private final Color[] HELP_COLORS = {
            Color.WHITE, Color.PINK, Color.GREEN,
            Color.YELLOW, Color.ORANGE, Color.CYAN
    };
    private final String W_BG_FONT_PREFIX = "<font style=\"background-color:white\">";
    private final String Y_BG_FONT_PREFIX = "<font style=\"background-color:yellow\">";
    private final String R_FONT_PREFIX = "<font style=\"color:red\">";
    private final String FONT_SUFFIX = "</font>";

    private static boolean showWarning = false;
    private static boolean readNFlag = false;
    private static long insertCounter = 0;
    private static long readCounter = 0;
    private static long startTime = System.currentTimeMillis();
    private static int fontIdx = 0;

    private final int USE_BR_INMB_DEFAULT = 100;
    private int useBRFileSizeInMB;
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

    // LIFO
    private static Queue<String> qMsgsToAppend;
    private static AppendMsgCallable msgCallable;
    private static final ExecutorService threadPool = Executors.newFixedThreadPool(5);

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new SearchBigFile().initComponents());
    }

    public SearchBigFile() {
        super("Search File");
    }

    /**
     * This method initializes the form.
     */
    private void initComponents() {
        logger = MyLogger.createLogger(getClass());
        configs = new DefaultConfigs(logger, Utils.getConfigsAsArr(Configs.class));
        debugAllowed = getBooleanCfg(Configs.DebugEnabled);
        useBRFileSizeInMB = getIntCfg(Configs.UseBRFileSizeInMB);
        if (useBRFileSizeInMB < 0) {
            useBRFileSizeInMB = USE_BR_INMB_DEFAULT;
        }
        logger.setDebug(debugAllowed);
        printConfigs();
        qMsgsToAppend = new LinkedBlockingQueue<>();
        idxMsgsToAppend = new ConcurrentHashMap<>();
        lineOffsets = new ArrayList<>();
        recentFilesStr = getCfg(Configs.RecentFiles);
        recentSearchesStr = getCfg(Configs.RecentSearches);
        msgCallable = new AppendMsgCallable(this);

        Container parentContainer = getContentPane();
        parentContainer.setLayout(new BorderLayout());

        JPanel filePanel = new JPanel();

        final int TXT_COLS = 18;
        UIName uin = UIName.LBL_FILE;
        txtFilePath = new AppTextField(getCfg(Configs.FilePath), TXT_COLS, getFiles());
        AppLabel lblFilePath = new AppLabel(uin.name, txtFilePath, uin.mnemonic);
        uin = UIName.BTN_FILE;
        JButton btnFileOpen = new AppButton(uin.name, uin.mnemonic, uin.tip, "", true);
        btnFileOpen.addActionListener(e -> openFile());

        uin = UIName.BTN_LISTRF;
        JButton btnListRF = new AppButton(uin.name, uin.mnemonic, uin.tip, "./icons/recent-icon.png");
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

        uin = UIName.LBL_RFILES;
        JMenuBar mb = new JMenuBar();
        menuRFiles = new JMenu(uin.name);
        mb.setBackground(Color.LIGHT_GRAY);
        menuRFiles.setMnemonic(uin.mnemonic);
        menuRFiles.setToolTipText(uin.tip);
        mb.add(menuRFiles);
        updateRecentMenu(menuRFiles, getFiles(), txtFilePath);

        filePanel.add(jtbFile);
        filePanel.add(btnListRF);
        filePanel.add(mb);
        filePanel.add(jcbMatchCase);
        filePanel.add(jcbWholeWord);
        filePanel.setBorder(new TitledBorder("File to search"));

        JPanel searchPanel = new JPanel();

        txtSearch = new AppTextField(getCfg(Configs.SearchString), TXT_COLS - 8, getSearches());
        uin = UIName.LBL_SEARCH;
        AppLabel lblSearch = new AppLabel(uin.name, txtSearch, uin.mnemonic);
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
        uin = UIName.BTN_LISTRS;
        JButton btnListRS = new AppButton(uin.name, uin.mnemonic, uin.tip, "./icons/recent-icon.png");
        btnListRS.addActionListener(e -> showListRS());

        JToolBar jtbSearch = new JToolBar();
        jtbSearch.setFloatable(false);
        jtbSearch.setRollover(false);
        jtbSearch.add(txtSearch);

        uin = UIName.LBL_RSEARCHES;
        JMenuBar mbar = new JMenuBar();
        menuRSearches = new JMenu(uin.name);
        mbar.setBackground(Color.LIGHT_GRAY);
        menuRSearches.setMnemonic(uin.mnemonic);
        menuRSearches.setToolTipText(uin.tip);
        mbar.add(menuRSearches);
        updateRecentMenu(menuRSearches, getSearches(), txtSearch);

        uin = UIName.BTN_CANCEL;
        btnCancel = new AppButton(uin.name, uin.mnemonic, uin.tip, "./icons/cancel-icon.png", true);
        btnCancel.setDisabledIcon(new ImageIcon("./icons/cancel-icon-disabled.png"));
        btnCancel.addActionListener(evt -> cancelSearch());

        searchPanel.setLayout(new FlowLayout());
        searchPanel.add(lblSearch);
        searchPanel.add(jtbSearch);
        searchPanel.add(btnListRS);
        searchPanel.add(mbar);
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
        btnFontInfo.setToolTipText("Present font size for results area.");
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
        uin = UIName.BTN_FIND;
        btnFind = new AppButton(uin.name, uin.mnemonic, uin.tip);
        btnFind.addActionListener(e -> findWordInResult());
        uin = UIName.BTN_HELP;
        btnHelp = new AppButton(uin.name, uin.mnemonic, uin.tip);
        btnHelp.setToolTipText(btnHelp.getToolTipText()
                + ". Color changes to [" + HELP_COLORS.length + "] different colors, every [" + HELP_COLOR_CHANGE_SEC + "sec].");

        btnHelp.addActionListener(e -> showHelp());

        setBkColors(new JButton[]{btnPlusFont, btnMinusFont, btnResetFont,
                btnFontInfo, btnGoTop, btnGoBottom, btnNextOccr, btnPreOccr, btnFind, btnHelp});
        btnHelp.setForeground(Color.RED);

        JPanel controlPanel = new JPanel();
        JButton btnExit = new AppExitButton();
        controlPanel.add(jtbActions);
        jtbActions.add(btnPlusFont);
        jtbActions.add(btnMinusFont);
        jtbActions.add(btnResetFont);
        jtbActions.add(btnFontInfo);
        jtbActions.add(btnGoTop);
        jtbActions.add(btnGoBottom);
        jtbActions.add(btnPreOccr);
        jtbActions.add(btnNextOccr);
        jtbActions.add(btnFind);
        jtbActions.add(btnHelp);
        controlPanel.add(btnExit);
        controlPanel.setBorder(new TitledBorder("Controls"));

        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new GridBagLayout());
        inputPanel.add(filePanel);
        inputPanel.add(searchPanel);
        inputPanel.add(controlPanel);

        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BorderLayout());
        topPanel.add(inputPanel, BorderLayout.NORTH);
        msgPanel = new JPanel();
        msgPanel.setBorder(BLUE_BORDER);
        lblMsg = new JLabel(getInitialMsg());
        lblMsg.setFont(getNewFont(lblMsg.getFont(), Font.PLAIN, 12));
        msgPanel.add(lblMsg);
        topPanel.add(msgPanel, BorderLayout.SOUTH);
        resetShowWarning();

        tpHelp = new JEditorPane();
        tpHelp.setEditable(false);
        tpHelp.setContentType("text/html");

        epResults = new JEditorPane();
        epResults.setEditable(false);
        epResults.setContentType("text/html");
        epResults.setFont(getFontForEditor(getCfg(Configs.FontSize)));
        epResults.setForeground(Color.BLACK);
        epResults.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        htmlDoc = new HTMLDocument();
        epResults.setDocument(htmlDoc);
        kit = new HTMLEditorKit();
        jspResults = new JScrollPane(epResults);
        jspHelp = new JScrollPane(tpHelp);
        jspResults.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        jspResults.setBorder(EMPTY_BORDER);

        parentContainer.add(topPanel, BorderLayout.NORTH);
        tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Result", null, jspResults, "Displays Search/Read results");
        tabbedPane.addTab("Help", null, jspHelp, "Displays application help");
        parentContainer.add(tabbedPane, BorderLayout.CENTER);

        btnExit.addActionListener(evt -> exitForm());
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent evt) {
                exitForm();
            }
        });

        btnFontInfo.setText(getFontSize());
        menuRFiles.setSize(menuRFiles.getWidth(), btnSearch.getHeight());
        menuRSearches.setSize(menuRSearches.getWidth(), btnSearch.getHeight());

        new Timer().schedule(new FontChangerTask(this), 0, MIN_10);
        new Timer().schedule(new HelpColorChangerTask(this), 0, HELP_COLOR_CHANGE_TIME);

        setControlsToEnable();
        setupHelp();
        resetForNewSearch();
        enableControls();
        showHelp();

        setToCenter();
        setExtendedState(JFrame.MAXIMIZED_BOTH);
    }

    private void setControlsToEnable() {
        Component[] components = {
                txtFilePath, txtSearch, btnSearch, btnLastN,
                menuRFiles, menuRSearches, cbLastN, jcbMatchCase,
                jcbWholeWord, btnPlusFont, btnMinusFont, btnResetFont,
                btnGoTop, btnGoBottom, btnNextOccr, btnPreOccr, btnFind
        };
        setComponentToEnable(components);
        setComponentContrastToEnable(new Component[]{btnCancel});
        enableControls();
    }

    private void printConfigs() {
        log("Debug enabled [" + logger.isDebug()
                + "], useBRFileSizeInMB [" + useBRFileSizeInMB
                + "]"
        );
    }

    private void updateRecentMenu(JMenu m, String[] arr, JTextField txtF) {
        m.removeAll();
        for (String a : arr) {
            JMenuItem mi = new JMenuItem(a);
            mi.addActionListener(e -> txtF.setText(e.getActionCommand()));
            m.add(mi);
        }
    }

    private void showHelp() {
        selectTab(true);
    }

    private void hideHelp() {
        selectTab(false);
    }

    private void selectTab(boolean show) {
        tabbedPane.setSelectedComponent(show ? jspHelp : jspResults);
    }

    private void setupHelp() {
        showHelp();
        File file = new File("./help.html");
        try {
            tpHelp.setPage(file.toURI().toURL());
        } catch (IOException e) {
            logger.error("Unable to dispaly help");
            updateTitleAndMsg("Unable to dispaly help", MsgType.ERROR);
        }
    }

    private void findWordInResult() {
        if (isValidate()) {
            if (!searchStr.equalsIgnoreCase(getSearchString())) {
                resetLineOffsetsIdx();
                lineOffsets.clear();
                setSearchStrings();
                updateRecentValues();
                updateOffsets();
                int size = lineOffsets.size();
                if (size > 0) {
                    showMsgAsInfo("Search for new word [" + searchStr + "] set, total occurrences [" + size + "] found. Use next/pre occurrences controls.");
                } else {
                    showMsg("Search for new word [" + searchStr + "] set, no occurrence found.", MsgType.WARN);
                }
            } else {
                showMsg("Search for same word [" + searchStr + "] already build.", MsgType.WARN);
            }
        }
    }

    private void resetLineOffsetsIdx() {
        lineOffsetsIdx = -1;
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
            showMsgAsInfo("Going occurrences of [" + searchStr + "] # " + (idx + 1) + "/" + lineOffsets.size());
        } else {
            showMsg("No occurrences of [" + searchStr + "] to show", MsgType.WARN);
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
            debug("Selected file: " + file.getAbsolutePath());
        } else {
            debug("Open command cancelled by user.");
        }
    }

    private String getInitialMsg() {
        return "This bar turns 'Orange' to show warnings and 'Red' to show error/force-stop. " +
                "Time/occurrences limit for warning [" + WARN_LIMIT_SEC
                + "sec/" + WARN_LIMIT_OCCR
                + "] and for error [" + FORCE_STOP_LIMIT_SEC
                + "sec/" + FORCE_STOP_LIMIT_OCCR + "]";

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
        hideHelp();
        Font font = epResults.getFont();
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
            epResults.setFont(font);
            btnFontInfo.setText(getFontSize());
            showMsgAsInfo(m);
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
        showRecentList(menuRFiles, "Recent files", txtFilePath);
    }

    private void showListRS() {
        showRecentList(menuRSearches, "Recent searches", txtSearch);
    }

    // This will be called by reflection from SwingUI jar
    public void handleDblClickOnRow(AppTable table, Object[] params) {
        ((JTextField) params[0]).setText(table.getValueAt(table.getSelectedRow(), 0).toString());
        ((JFrame) params[1]).setVisible(false);
    }

    private void showRecentList(JMenu src, String colName, JTextField dest) {
        AppFrame frame = new AppFrame("ESC to Hide");

        JTextField txtFilter = new JTextField();
        txtFilter.setColumns(30);

        DefaultTableModel model = SwingUtils.getTableModel(new String[]{colName + " - Dbl-click or select & ENTER"});

        AppTable table = new AppTable(model);
        createRowsForRecentVals(src, model);

        // ToolTip and alignment
        TableColumn firstCol = table.getColumnModel().getColumn(0);
        firstCol.setMinWidth(25);
        firstCol.setCellRenderer(new CellRendererLeftAlign());

        //TODO: Analyze why single method not working
        //table.setUpSorterAndFilter(model, this, dest, new CopyCommandAction(table, frame, dest), new Object[]{dest, frame});
        setUpSorterAndFilter(this, table, model, dest, txtFilter, frame, new Object[]{dest, frame});

        table.setScrollProps();
        table.setBorder(EMPTY_BORDER);

        JPanel filterPanel = new JPanel();
        filterPanel.add(new AppLabel("Filter", txtFilter, 'R'));
        filterPanel.add(txtFilter);

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(filterPanel, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        panel.setBorder(EMPTY_BORDER);

        Container pc = frame.getContentPane();
        pc.setLayout(new BorderLayout());
        pc.add(panel);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setAlwaysOnTop(true);
        frame.setToCenter();

        SwingUtils.addEscKeyAction(frame);
    }

    private void setUpSorterAndFilter(SearchBigFile sbf, AppTable table, DefaultTableModel model,
                                      JTextField dest, JTextField txtFilter,
                                      JFrame frame, Object[] params) {
        table.addSorter(model);
        table.addFilter(txtFilter);
        table.addDblClickOnRow(sbf, params);
        table.addEnterOnRow(new CopyCommandAction(table, frame, dest));
        table.applyChangeListener(txtFilter);
    }

    private void createRowsForRecentVals(JMenu src, DefaultTableModel model) {
        int items = src.getItemCount();
        for (int i = 0; i < items; i++) {
            String s = src.getItem(i).getText();
            model.addRow(new String[]{s});
        }
    }

    private Integer[] getLastNOptions() {
        return new Integer[]{100, 200, 500, 1000, 2000, 3000, 4000, 5000};
    }

    private void resetForNewSearch() {
        debug("reset for new search");
        hideHelp();
        printMemoryDetails();
        insertCounter = 0;
        readCounter = 0;
        disableControls();
        resetShowWarning();
        emptyResults();
        updateRecentValues();
        qMsgsToAppend.clear();
        idxMsgsToAppend.clear();
        globalCharIdx = 0;
        lineOffsets.clear();
        resetLineOffsetsIdx();
        setSearchStrings();
        logger.log(getSearchDetails());
        startTime = System.currentTimeMillis();
        status = Status.READING;
        readNFlag = false;
    }

    private void printMemoryDetails() {
        long total = Runtime.getRuntime().totalMemory();
        long free = Runtime.getRuntime().freeMemory();
        debug(String.format("Memory - Total: %s, Free: %s, Occupied: %s",
                Utils.getFileSizeString(total),
                Utils.getFileSizeString(free),
                Utils.getFileSizeString(total - free)
        ));
    }

    private String getLineNumStr(long line) {
        return "<span style=\"color:blue;\">" + line + "&nbsp;&nbsp;</span>";
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

    private void setSearchPattern(String s) {
        txtSearch.setText(s);
        showMsgAsInfo("Search pattern set as [" + s + "]");
    }

    private void setFileToSearch(String s) {
        txtFilePath.setText(s);
        showMsgAsInfo("File set as [" + s + "]");
    }

    private String[] getFiles() {
        return getCfg(Configs.RecentFiles).split(SEMI_COLON);
    }

    private String[] getSearches() {
        return getCfg(Configs.RecentSearches).split(SEMI_COLON);
    }

    private void resetShowWarning() {
        debug("reset show warning");
        showWarning = false;
        timeTillNow = 0;
        occrTillNow = 0;
        linesTillNow = 0;
        showMsgAsInfo(getInitialMsg());
    }

    private void cancelSearch() {
        // To ensure background is red
        if (!isErrorState()) {
            showMsg("Search cancelled.", MsgType.ERROR);
        }
        if (status == Status.READING) {
            logger.warn("Search cancelled by user.");
            status = Status.CANCELLED;
        }
    }

    private void searchFile() {
        if (isValidate()) {
            operation = "search";
            resetForNewSearch();
            showMsgAsInfo("Starting [" + operation + "] for file " + getFilePath());
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
            showMsgAsInfo("Starting [" + operation + "] for file " + getFilePath());
            status = Status.READING;
            threadPool.submit(new LastNRead(this));
            threadPool.submit(new TimerCallable(this));
        } else {
            enableControls();
        }
    }

    private void updateTitleAndMsg(String s) {
        updateTitleAndMsg(s, MyLogger.MsgType.WARN);
    }

    private void updateTitleAndMsg(String s, MyLogger.MsgType type) {
        updateTitle(s);
        showMsg(s, type);
    }

    private boolean isValidate() {
        //updateTitle("");
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

    private void updateRecentValues() {
        debug("update recent search values");

        recentFilesStr = checkItems(getFilePath(), recentFilesStr);
        recentSearchesStr = checkItems(getSearchString(), recentSearchesStr);

        String[] arrF = recentFilesStr.split(SEMI_COLON);
        updateRecentMenu(menuRFiles, arrF, txtFilePath);
        String[] arrS = recentSearchesStr.split(SEMI_COLON);
        updateRecentMenu(menuRSearches, arrS, txtSearch);

        // Updating auto-complete action
        txtFilePath.setAutoCompleteArr(arrF);
        txtSearch.setAutoCompleteArr(arrS);
    }

    private String checkItems(String searchStr, String csv) {
        String csvLC = csv.toLowerCase();
        String ssLC = searchStr.toLowerCase();
        if (csvLC.contains(ssLC)) {
            int idx = csvLC.indexOf(ssLC);
            // remove item and add it again to bring it on top
            csv = csv.substring(0, idx)
                    + csv.substring(idx + searchStr.length() + SEMI_COLON.length());
        }
        csv = searchStr + SEMI_COLON + csv;

        if (csv.split(SEMI_COLON).length >= RECENT_LIMIT) {
            csv = csv.substring(0, csv.lastIndexOf(SEMI_COLON));
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
            data = lineEndToBR(data);
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

    /**
     * This method only converts spaces to &nbsp; till
     * first char comes
     *
     * @param data String
     * @return escaped string
     */
    private String escLSpaces(String data) {
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

    private String lineEndToBR(String data) {
        String NEW_LINE_REGEX = "\r?\n";
        String HTML_LINE_END = "<br>";
        return data.replaceAll(NEW_LINE_REGEX, HTML_LINE_END);
    }

    public void appendResult(String data) {

        // Html escaping here, so html '<' character processed and searched properly
        //data = htmlEsc(data);

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

    /**
     * This method will check string occurrence as in-case-sensitive
     * but highlights whatever original text is
     *
     * @param data String
     * @return converted string
     */
    private String replaceWithSameCase(String data) {
        // Putting yellow background after escaping
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
        epResults.setText("");
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
        return epResults.getFont().getSize() + "";
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

    public String getUseBRFileSizeInMB() {
        return useBRFileSizeInMB + "";
    }

    public void showMsgAsInfo(String msg) {
        showMsg(msg, MyLogger.MsgType.INFO);
    }

    public void showMsg(String msg, MyLogger.MsgType type) {
        if (Utils.hasValue(msg)) {
            Color b = Color.WHITE;
            Color f = Color.BLUE;
            if (type == MyLogger.MsgType.ERROR) {
                b = Color.RED;
                f = Color.WHITE;
            } else if (type == MyLogger.MsgType.WARN) {
                b = Color.ORANGE;
                f = Color.BLACK;
            }
            msgPanel.setBackground(b);
            lblMsg.setForeground(f);
            lblMsg.setText(msg);
        }
    }

    public String getProblemMsg() {
        debug("getProblem: timeTillNow = " + timeTillNow + ", occrTillNow = " + occrTillNow);

        StringBuilder sb = new StringBuilder();
        if (timeTillNow > WARN_LIMIT_SEC) {
            sb.append("Warning: Time [").append(timeTillNow).append("] > warning limit [").append(WARN_LIMIT_SEC).append("]. ");
        }
        if (occrTillNow > WARN_LIMIT_OCCR) {
            sb.append("Warning: Occurrences [").append(occrTillNow).append("] > warning limit [").append(WARN_LIMIT_OCCR).append("], try to narrow your search.");
        }
        StringBuilder sbErr = new StringBuilder();
        if (timeTillNow > FORCE_STOP_LIMIT_SEC) {
            sbErr.append("Error: Time [").append(timeTillNow).append("] > force stop limit [").append(FORCE_STOP_LIMIT_SEC).append("]. Cancelling search...");
        }
        if (occrTillNow > FORCE_STOP_LIMIT_OCCR) {
            sbErr.append("Error: Occurrences [").append(occrTillNow).append("] > force stop limit [").append(FORCE_STOP_LIMIT_OCCR).append("], try to narrow your search. Cancelling search...");
        }

        if (Utils.hasValue(sbErr.toString())) {
            sb = sbErr;
        }

        debug("Returning problem as " + sb.toString());
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

    public void debug(String s) {
        logger.debug(s);
    }

    public void log(String s) {
        logger.log(s);
    }

    public void goToFirst() {
        goToFirst(true);
    }

    public void goToFirst(boolean show) {
        // If this param removed then after search/read ends this msg displayed
        if (show) {
            showMsgAsInfo("Going to first line");
        }
        // Go to first
        selectAndGoToIndex(0);
        resetLineOffsetsIdx();
    }

    public void goToEnd() {
        goToEnd(true);
    }

    public void goToEnd(boolean show) {
        if (show) {
            showMsgAsInfo("Going to last line");
        }
        // Go to end
        selectAndGoToIndex(htmlDoc.getLength());
        lineOffsetsIdx = lineOffsets.size();
    }

    public void selectAndGoToIndex(int idx) {
        selectAndGoToIndex(idx, idx);
    }

    public void selectAndGoToIndex(int sIdx, int eIdx) {
        hideHelp();
        epResults.grabFocus();
        epResults.select(sIdx, eIdx);
    }

    public void finishAction() {
        log("Performing finish action");
        printCounters();
        if (showWarning) {
            SwingUtilities.invokeLater(new StartWarnIndicator(this));
        }
        goToEnd(false);
        logger.debug("Timer: thread pool status " + threadPool.toString());
        // requesting to free used memory
        System.gc();
        printMemoryDetails();
    }

    private void updateOffsets() {
        debug("Offsets size " + lineOffsets.size());
        if (lineOffsets.size() == 0) {
            try {
                String strToSearch = searchStr.toLowerCase();
                int strToSearchLen = strToSearch.length();
                debug("Starting search for string [" + strToSearch + "]");

                String htmlDocText = htmlDoc.getText(0, htmlDoc.getLength()).toLowerCase();
                log("For offsets document length calculated as " + htmlDocText.length());

                int idx = 0;
                while (idx != -1) {
                    idx = htmlDocText.indexOf(strToSearch, globalCharIdx);
                    globalCharIdx = idx + strToSearchLen;
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
        return !isWarningState() && !isErrorState();
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
        if (fontIdx == ColorsNFonts.values().length) {
            fontIdx = 0;
        }
        return ColorsNFonts.values()[fontIdx++].getFont();
    }

    public void changeHelpColor() {
        for (Color c : HELP_COLORS) {
            btnHelp.setForeground(c);
            Utils.sleep(500);
        }
    }

    public void changeMsgFont() {
        Font f = lblMsg.getFont();
        f = new Font(getNextFont(), f.getStyle(), f.getSize());
        lblMsg.setFont(f);
        String msg = getFontDetail(f);
        String tip = "Font for this bar [" + msg + "], changes every [" + MIN_10 + "min]. " + getInitialMsg();
        lblMsg.setToolTipText(tip);
        msgPanel.setToolTipText(tip);
        showMsgAsInfo(msg);
        debug(msg);
    }

    private String escString(String str) {
        return htmlEsc(escLSpaces(str));
    }

    private String addLineNumAndEsc(long lineNum, String str) {
        //return getLineNumStr(lineNum) + escString(str) + System.lineSeparator();
        return "<tr border=0 style=\"padding:0px;border-spacing:0px\" width=\"100%\"><td width=\"3%\" align=\"right\" valign=\"top\">" + getLineNumStr(lineNum) + "</td><td width=\"*\">" + escString(str) + "</td></tr>" + System.lineSeparator();
    }

    private String addLineEnd(String str) {
        return str + System.lineSeparator();
    }

    /*   Inner classes    */
    class LastNRead implements Callable<Boolean> {

        private final Stack<String> stack = new Stack<>();
        private final SearchBigFile sbf;

        LastNRead(SearchBigFile sbf) {
            this.sbf = sbf;
        }

        @Override
        public Boolean call() {
            final int KB = 1024, LIMIT = Integer.parseInt(cbLastN.getSelectedItem().toString());
            int readLines = 0, occr = 0;
            boolean hasError = false;
            String searchPattern = processPattern(), fn = getFilePath();
            StringBuilder sb = new StringBuilder();
            File file = new File(fn);

            readNFlag = true;
            updateTitle("Reading last " + LIMIT + " lines");
            logger.log("Loading last [" + LIMIT + "] lines from [" + fn + "]");
            // FIFO
            stack.removeAllElements();

            // TODO: not stable - need to check
            boolean useBR = file.length() <= (useBRFileSizeInMB * KB * KB);
//            boolean useBR = false;
            log("File read with buffered reader [" + useBR + "].");
            if (useBR) {
                try {
                    BufferedReader br = new BufferedReader(new FileReader(file));
                    List<String> tempCollection = new ArrayList<>();

                    String line;
                    long time = System.currentTimeMillis();
                    while ((line = br.readLine()) != null) {
                        tempCollection.add(line);
                        if (tempCollection.size() > LIMIT) {
                            tempCollection.remove(0);
                        }
                    }
                    log("File read complete in " + Utils.getTimeDiffSecStr(time));
                    int l = 0;
                    while (!tempCollection.isEmpty()) {
                        readLines++;
                        String s = tempCollection.remove(tempCollection.size() - 1);
                        occr += calculateOccr(s, searchPattern);
                        processForRead(l++, s, occr, tempCollection.isEmpty());
                    }
                } catch (Exception e) {
                    catchForRead(e);
                    hasError = true;
                } finally {
                    enableControls();
                }
            } else {
                try (RandomAccessFile randomAccessFile = new RandomAccessFile(file, "r")) {
                    long fileLength = file.length() - 1;
                    // Set the pointer at the last of the file
                    randomAccessFile.seek(fileLength);

                    long time = System.currentTimeMillis();
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

                            occr += calculateOccr(sb.toString(), searchPattern);
                            processForRead(readLines, sb.toString(), occr);

                            sb = new StringBuilder();
                            readLines++;
                            // Last line will be printed after loop
                            if (readLines == LIMIT - 1) {
                                break;
                            }
                            if (isCancelled()) {
                                logger.warn("---xxx--- Read cancelled ---xxx---");
                                break;
                            }
                        } else {
                            sb.append(c);
                        }
                        fileLength = fileLength - pointer;
                    }
                    log("File read complete in " + Utils.getTimeDiffSecStr(time));
                    if (Utils.hasValue(sb.toString())) {
                        sb.reverse();
                    }
                    processForRead(readLines, sb.toString(), occr, true);
                    readLines++;
                } catch (IOException e) {
                    catchForRead(e);
                    hasError = true;
                } finally {
                    enableControls();
                }
            }

            occr += calculateOccr(sb.toString(), searchStr);
            if (!hasError) {
                String result = getSearchResult(
                        fn,
                        Utils.getTimeDiffSecStr(startTime),
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

        private void catchForRead(Exception e) {
            String msg = "ERROR: " + e.getMessage();
            logger.error(e.getMessage());
            epResults.setText(R_FONT_PREFIX + msg + FONT_SUFFIX);
            sbf.updateTitleAndMsg("Unable to read file: " + getFilePath(), MsgType.ERROR);
        }

        private void processForRead(int line, String str, int occr) {
            processForRead(line, str, occr, false);
        }

        private void processForRead(int line, String str, int occr, boolean bypass) {
            String strToAppend = addLineNumAndEsc(line + 1, str);
            synchronized (SearchBigFile.class) {
                // emptying stack to Q
                stack.push(strToAppend);
                if (bypass || stack.size() > APPEND_MSG_CHUNK) {
                    while (!stack.empty()) {
                        qMsgsToAppend.add(stack.pop());
                    }
                    //debug("Read: Starting msg callable and Q size is " + qMsgsToAppend.size());
                    startThread(msgCallable);
                }
            }

            occrTillNow = occr;
            linesTillNow = line;
            if (!showWarning && occr > WARN_LIMIT_OCCR) {
                showWarning = true;
            }
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
                sb.append(addLineNumAndEsc(lineNum, stats.getLine()));
                qMsgsToAppend.add(sb.toString());
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
                    timeElapse = Utils.getTimeDiffSec(startTime);
                    timeTillNow = timeElapse;
                    String msg = timeElapse + " sec, lines [" + sbf.linesTillNow + "] ";
                    if (showWarning || isWarningState()) {
                        msg += sbf.getProblemMsg();
                        sbf.debug("Invoking warning indicator.");
                        SwingUtilities.invokeLater(new StartWarnIndicator(sbf));
                    }
                    if (isErrorState()) {
                        sbf.logger.warn("Stopping forcefully.");
                        cancelSearch();
                    }
                    sbf.updateTitle(msg);
                    logger.debug("Timer callable sleeping now for a second");
                    Utils.sleep(1000, sbf.logger);
                }
            } while (status == Status.READING);

            logger.log("Timer stopped after " + timeElapse + " sec");
            return true;
        }
    }

    private boolean hasOccr(String line, String searchPattern) {
        return (!isMatchCase() && line.toLowerCase().contains(searchPattern))
                || (isMatchCase() && line.contains(searchPattern))
                || (isWholeWord() && line.matches(searchPattern));
    }

    class AppendMsgCallable implements Callable<Boolean> {

        SearchBigFile sbf;

        public AppendMsgCallable(SearchBigFile sbf) {
            this.sbf = sbf;
        }

        @Override
        public Boolean call() {
            StringBuilder sb = new StringBuilder();

            int qSize = qMsgsToAppend.size();
            synchronized (SearchBigFile.class) {
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
                    /*logger.debug("Initial Q size [" + qSize + "], after message processing Q size ["
                            + qMsgsToAppend.size() + "]");*/
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
            final int BUFFER_SIZE = 200 * 1024;
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
                            //debug("Search: Starting msg callable and Q size is " + qMsgsToAppend.size());
                            startThread(msgCallable);
                        }
                    }
                    if (isCancelled()) {
                        String msg = "---xxx--- Search cancelled ---xxx---";
                        debug(msg);
                        qMsgsToAppend.add(addLineEnd(msg));
                        startThread(msgCallable);
                        break;
                    }
                }

                logger.log("File read in " + Utils.getTimeDiffSecStr(time));

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
                    logger.log("Time in waiting all message to append is " + Utils.getTimeDiffSecStr(time));
                }
                String result = getSearchResult(path, Utils.getTimeDiffSecStr(startTime), stats.getLineNum(), stats.getOccurrences());
                if (stats.getOccurrences() == 0 && !isErrorState() && !isCancelled()) {
                    String s = "No match found";
                    sbf.epResults.setText(R_FONT_PREFIX + s + FONT_SUFFIX);
                    sbf.showMsg(s, MsgType.WARN);
                }

                if (isCancelled()) {
                    sbf.updateTitle("Search cancelled - " + result);
                } else {
                    String msg = "--- Search complete ---";
                    qMsgsToAppend.add(addLineEnd(msg));
                    startThread(msgCallable);
                    sbf.updateTitleAndMsg("Search complete - " + result, MsgType.INFO);
                }
                status = Status.DONE;
            } catch (IOException e) {
                String msg = "ERROR: " + e.getMessage();
                sbf.logger.error(e.getMessage());
                sbf.epResults.setText(R_FONT_PREFIX + msg + FONT_SUFFIX);
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
