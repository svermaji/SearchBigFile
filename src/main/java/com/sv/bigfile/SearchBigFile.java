package com.sv.bigfile;

import com.sv.bigfile.action.AllOccrEnterAction;
import com.sv.bigfile.action.CopyCommandAction;
import com.sv.bigfile.action.RecentMenuAction;
import com.sv.bigfile.helpers.AppendData;
import com.sv.bigfile.helpers.StartWarnIndicator;
import com.sv.bigfile.helpers.TabRemoveHandler;
import com.sv.bigfile.helpers.TabbedPaneHandler;
import com.sv.bigfile.html.WrapHtmlKit;
import com.sv.bigfile.task.*;
import com.sv.core.Constants;
import com.sv.core.Utils;
import com.sv.core.config.DefaultConfigs;
import com.sv.core.logger.MyLogger;
import com.sv.core.logger.MyLogger.MsgType;
import com.sv.runcmd.RunCommand;
import com.sv.swingui.KeyActionDetails;
import com.sv.swingui.SwingUtils;
import com.sv.swingui.UIConstants;
import com.sv.swingui.component.*;
import com.sv.swingui.component.table.AppTable;
import com.sv.swingui.component.table.CellRendererCenterAlign;
import com.sv.swingui.component.table.CellRendererLeftAlign;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.text.*;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Queue;
import java.util.Timer;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

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
    public enum Configs {
        RecentFiles, FilePath, SearchString, RecentSearches, LastN, FontSize, FontIndex,
        ColorIndex, ChangeFontAuto, ChangeHighlightAuto, ApplyColorToApp, AutoLock,
        ClipboardSupport, MatchCase, WholeWord, FixedWidth, MultiTab, ReopenLastTabs,
        ErrorTimeLimit, ErrorOccrLimit, DebugEnabled, LogSimpleClassName, ErrorMemoryLimitInMB,
        AppFontSize
    }

    public enum Status {
        NOT_STARTED, READING, DONE, CANCELLED
    }

    public enum FONT_OPR {
        INCREASE, DECREASE, RESET, NONE
    }

    public enum FILE_OPR {
        SEARCH, READ, FIND
    }

    private SearchUtils searchUtils;
    private MyLogger logger;
    private DefaultConfigs configs;

    private JSplitPane splitAllOccr;
    private DefaultTableModel modelAllOccr;
    private JLabel lblNoRow;
    private AppTable tblAllOccr;
    private JPanel bottomPanel, inputPanel, filePanel, searchPanel, controlPanel;
    private TitledBorder filePanelBorder, searchPanelBorder, controlPanelBorder;
    private String filePanelHeading, searchPanelHeading, controlPanelHeading;
    private JScrollPane jspAllOccr;
    private AppTabbedPane tabbedPane;
    private Map<String, ResultTabData> resultTabsData;
    private ResultTabData activeResultTabData = null;
    private JMenu menuRFiles, menuRSearches, menuSettings, menuFonts, menuAppFonts;
    private AppToolBar jtbFile, jtbSearch, jtbControls, msgButtons;
    private JPanel msgPanel;
    private JLabel lblMsg, lblFilePath, lblSearch;
    private JButton btnShowAll, btnMemory, btnListRS, btnListRF;
    private JButton btnPlusFont, btnMinusFont, btnResetFont, btnLock;
    private JButton btnFileOpen, btnGoTop, btnGoBottom, btnNextOccr, btnPreOccr, btnFind, btnHelp;
    private JButton btnSearch, btnLastN, btnCancel;
    private AppTextField txtFilePath, txtSearch;
    private JTextPane tpResults, tpHelp, tpContactMe;
    // todo: for multi thread - incomplete
    private JTextPane[] tpResult;
    private final SimpleAttributeSet highlightSAS = new SimpleAttributeSet();
    private final SimpleAttributeSet nonhighlightSAS = new SimpleAttributeSet();
    //private Highlighter.HighlightPainter painter;
    private Highlighter highlighter;
    private JScrollPane jspResults, jspHelp, jspContactMe;
    private JScrollPane[] jspResult;
    private HTMLDocument htmlDoc;
    private HTMLDocument[] htmlDocs;
    private HTMLEditorKit kit;
    private AppCheckBox jcbMatchCase, jcbWholeWord;
    private AppCheckBoxMenuItem jcbmiFonts, jcbmiHighlights, jcbmiApplyToApp, jcbmiAutoLock,
            jcbmiClipboardSupport, jcbmiFixedWidth, jcbmiDebugEnabled, jcbmiMultiTab,
            jcbmiLogSimpleClassName, jcbmiReopenLastTabs;
    private AppRadioButtonMenuItem jrbmiErrorTimeSec10, jrbmiErrorTimeSec30, jrbmiErrorTimeSec50, jrbmiErrorTimeSec100,
            jrbmiErrorOccr100, jrbmiErrorOccr300, jrbmiErrorOccr500, jrbmiErrorOccr1000;
    private JComboBox<Integer> cbLastN;

    private static FILE_OPR operation;

    private static final Color ORIG_COLOR = UIConstants.ORIG_COLOR;
    private static final int TXT_HEIGHT = 28;
    private static ColorsNFonts[] appColors;
    private static boolean ignoreBlackAndWhite = true;
    private static boolean showWarning = false;
    private static boolean fixedWidth = false;
    private static boolean multiTab = false;
    private static boolean reopenLastTabs = false;
    private static long insertCounter = 0;
    private static long readCounter = 0;
    private static long startTime = System.currentTimeMillis();
    private static String timeTaken;
    private static long lineNums;
    private static final int MIN_APPFONTSIZE = 8;
    private static final int MAX_APPFONTSIZE = 28;
    private static final int DEFAULT_APPFONTSIZE = 12;
    private static int appFontSize = 0;
    private static int resultFontSize = 0;
    private static int fontIdx = 0;
    private static int colorIdx = 0;
    private static int errorMemoryLimitInMB = 0;
    private static int warnMemoryLimitInMB = 0;

    private final String SEPARATOR = "~";
    private final String TXT_F_MAP_KEY = "Action.FileMenuItem";
    private final String TXT_S_MAP_KEY = "Action.SearchMenuItem";
    private final int EXCERPT_LIMIT = 80;
    private final int MAX_READ_CHAR_LIMIT = 5000;
    private final int MAX_RESULTS_TAB = 7;
    private final int TAB_TITLE_LIMIT = 20;
    private static int maxReadCharTimes = 0;
    private boolean debugEnabled;
    private String searchStr, recentFilesStr, recentSearchesStr;
    private int errorTimeLimit, errorOccrLimit;
    private long timeTillNow;
    private long occrTillNow;
    private long linesTillNow;

    private JComponent[] bkColorComponents;
    private JMenuBar mbSettings, mbRSearches, mbRFiles;
    private static Color highlightColor, highlightTextColor, selectionColor, selectionTextColor;
    private static String highlightColorStr;
    private static Status status = Status.NOT_STARTED;

    // indexed structure to maintain line indexing
    private static Map<Long, String> idxMsgsToAppend;
    private static Map<Integer, OffsetInfo> lineOffsets;
    private static int lastSelectedRow = -1, lineOffsetsIdx, lastLineOffsetsIdx = -1;
    private static int globalCharIdx;

    // LIFO
    private static Queue<String> qMsgsToAppend;
    private static AppendMsgCallable msgCallable;
    private static final ExecutorService threadPool = Executors.newFixedThreadPool(5);
    private static List<Timer> timers = new ArrayList<>();

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
        searchUtils = new SearchUtils(logger);
        configs = new DefaultConfigs(logger, Utils.getConfigsAsArr(Configs.class));
        debugEnabled = getBooleanCfg(Configs.DebugEnabled);
        logger.setDebug(debugEnabled);
        logger.setSimpleClassName(true);
        printConfigs();

        super.setLogger(logger);

        appFontSize = Utils.validateInt(getIntCfg(Configs.AppFontSize), DEFAULT_APPFONTSIZE, MIN_APPFONTSIZE, MAX_APPFONTSIZE);
        resultFontSize = Utils.validateInt(getIntCfg(Configs.FontSize), DEFAULT_FONT_SIZE, MIN_FONT_SIZE, MAX_FONT_SIZE);
        errorTimeLimit = getIntCfg(Configs.ErrorTimeLimit);
        errorOccrLimit = getIntCfg(Configs.ErrorOccrLimit);
        errorMemoryLimitInMB = getIntCfg(Configs.ErrorMemoryLimitInMB);
        warnMemoryLimitInMB = errorMemoryLimitInMB / 2;
        info("appFontSize [" + appFontSize + "], " +
                "resultFontSize [" + resultFontSize + "], " +
                "errorTimeLimit [" + errorTimeLimit + "], " +
                "errorOccrLimit [" + errorOccrLimit + "], " +
                "warnMemoryLimitInMB [" + warnMemoryLimitInMB + "], " +
                "errorMemoryLimitInMB [" + errorMemoryLimitInMB + "]");

        resultTabsData = new HashMap<>();
        appColors = SwingUtils.getFilteredCnF(ignoreBlackAndWhite);
        qMsgsToAppend = new LinkedBlockingQueue<>();
        idxMsgsToAppend = new ConcurrentHashMap<>();
        lineOffsets = new HashMap<>();
        colorIdx = getIntCfg(Configs.ColorIndex);
        fontIdx = getIntCfg(Configs.FontIndex);
        setColorFromIdx();
        recentFilesStr = checkSep(getCfg(Configs.RecentFiles));
        recentSearchesStr = checkSep(getCfg(Configs.RecentSearches));
        // to avoid length mismatch
        fixedWidth = getBooleanCfg(Configs.FixedWidth);
        multiTab = getBooleanCfg(Configs.MultiTab);
        reopenLastTabs = getBooleanCfg(Configs.ReopenLastTabs);
        msgCallable = new AppendMsgCallable(this);
        jspResult = new JScrollPane[MAX_RESULTS_TAB];

        Container parentContainer = getContentPane();
        parentContainer.setLayout(new BorderLayout());

        filePanelHeading = "File to search";
        searchPanelHeading = "Pattern to search";
        controlPanelHeading = "Controls";

        filePanel = new JPanel();

        final int TXT_COLS = 12;
        UIName uin = UIName.LBL_FILE;
        txtFilePath = new AppTextField(getCfg(Configs.FilePath), TXT_COLS, getFiles());
        if (fixedWidth) {
            txtFilePath.setMaximumSize(new Dimension(150, TXT_HEIGHT));
        }
        lblFilePath = new AppLabel(uin.name, txtFilePath, uin.mnemonic);
        uin = UIName.BTN_FILE;
        btnFileOpen = new AppButton(uin.name, uin.mnemonic, uin.tip);// no need as image //, "", true);
        btnFileOpen.addActionListener(e -> openFile());

        uin = UIName.BTN_LISTRF;
        btnListRF = new AppButton(uin.name, uin.mnemonic, uin.tip);
        btnListRF.addActionListener(e -> showListRF());
        uin = UIName.JCB_MATCHCASE;
        jcbMatchCase = new AppCheckBox(uin.name, getBooleanCfg(Configs.MatchCase));
        jcbMatchCase.setMnemonic(uin.mnemonic);
        jcbMatchCase.setToolTipText(uin.tip);
        uin = UIName.JCB_WHOLEWORD;
        jcbWholeWord = new AppCheckBox(uin.name, getBooleanCfg(Configs.WholeWord));
        jcbWholeWord.setMnemonic(uin.mnemonic);
        jcbWholeWord.setToolTipText(uin.tip);

        filePanel.setLayout(new FlowLayout());
        filePanel.add(lblFilePath);

        uin = UIName.LBL_RFILES;
        mbRFiles = new JMenuBar();
        menuRFiles = new AppMenu(uin.name, uin.mnemonic, uin.tip);
        mbRFiles.add(menuRFiles);
        updateRecentMenu(menuRFiles, getFiles(), txtFilePath, TXT_F_MAP_KEY);

        jtbFile = new AppToolBar(fixedWidth);
        jtbFile.add(txtFilePath);
        jtbFile.add(btnFileOpen);
        jtbFile.add(btnListRF);
        jtbFile.add(btnListRF);
        jtbFile.add(mbRFiles);
        jtbFile.add(jcbMatchCase);
        jtbFile.add(jcbWholeWord);

        filePanelBorder = (TitledBorder) SwingUtils.createTitledBorder(filePanelHeading, highlightColor);
        searchPanelBorder = (TitledBorder) SwingUtils.createTitledBorder(searchPanelHeading, highlightColor);
        controlPanelBorder = (TitledBorder) SwingUtils.createTitledBorder(controlPanelHeading, highlightColor);

        filePanel.add(jtbFile);
        filePanel.setBorder(filePanelBorder);

        searchPanel = new JPanel();

        txtSearch = new AppTextField(getCfg(Configs.SearchString), TXT_COLS - 6, getSearches());
        if (fixedWidth) {
            txtSearch.setMaximumSize(new Dimension(100, TXT_HEIGHT));
        }
        txtSearch.setToolTipText("Ctrl+F to come here and enter to perform Search. Also support search as you type");
        txtSearch.addKeyListener(new KeyAdapter() {

            @Override
            public void keyReleased(KeyEvent e) {
                findAsType();
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    searchFile();
                }
            }
        });
        txtSearch.addFocusListener(new FocusAdapter() {

            @Override
            public void focusLost(FocusEvent e) {
                updateRecentValues();
            }
        });
        uin = UIName.LBL_SEARCH;
        lblSearch = new AppLabel(uin.name, txtSearch, uin.mnemonic);
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
        btnListRS = new AppButton(uin.name, uin.mnemonic, uin.tip);
        btnListRS.addActionListener(e -> showListRS());

        uin = UIName.BTN_UC;
        AppButton btnUC = new AppButton(uin.name, uin.mnemonic, uin.tip);
        btnUC.addActionListener(e -> changeCase(CaseType.UPPER));
        uin = UIName.BTN_LC;
        AppButton btnLC = new AppButton(uin.name, uin.mnemonic, uin.tip);
        btnLC.addActionListener(e -> changeCase(CaseType.LOWER));
        uin = UIName.BTN_TC;
        AppButton btnTC = new AppButton(uin.name, uin.mnemonic, uin.tip);
        btnTC.addActionListener(e -> changeCase(CaseType.TITLE));
        uin = UIName.BTN_IC;
        AppButton btnIC = new AppButton(uin.name, uin.mnemonic, uin.tip);
        btnIC.addActionListener(e -> changeCase(CaseType.INVERT));

        //Supports if single line is around 30mb in one file as json response
        uin = UIName.LBL_RSEARCHES;
        mbRSearches = new JMenuBar();
        mbRSearches.setBorder(ZERO_BORDER);
        menuRSearches = new AppMenu(uin.name, uin.mnemonic, uin.tip);
        mbRSearches.add(menuRSearches);
        updateRecentMenu(menuRSearches, getSearches(), txtSearch, TXT_S_MAP_KEY);

        jtbSearch = new AppToolBar(fixedWidth);
        jtbSearch.add(txtSearch);
        jtbSearch.add(btnUC);
        jtbSearch.add(btnLC);
        jtbSearch.add(btnTC);
        jtbSearch.add(btnIC);
        jtbSearch.add(btnListRS);
        jtbSearch.add(mbRSearches);
        jtbSearch.add(btnSearch);

        uin = UIName.MNU_SETTINGS;
        mbSettings = new JMenuBar();
        menuSettings = new AppMenu(uin.name, uin.mnemonic, uin.tip + SHORTCUT + uin.mnemonic);
        mbSettings.add(menuSettings);
        // populating settings menu at end so font can be applied to lblMsg

        uin = UIName.BTN_CANCEL;
        btnCancel = new AppButton(uin.name, uin.mnemonic, uin.tip, "./icons/cancel-icon.png", true);
        btnCancel.setDisabledIcon(new ImageIcon("./icons/cancel-icon-disabled.png"));
        btnCancel.addActionListener(evt -> cancelSearch());

        /*jtbSearch.add(lblLastN);
        jtbSearch.add(cbLastN);
        jtbSearch.add(btnLastN);
        jtbSearch.add(btnCancel);*/

        searchPanel.setLayout(new FlowLayout());
        searchPanel.add(lblSearch);
        searchPanel.add(jtbSearch);
        searchPanel.add(btnSearch);
        searchPanel.add(lblLastN);
        searchPanel.add(cbLastN);
        searchPanel.add(btnLastN);
        searchPanel.add(btnCancel);
        searchPanel.setBorder(searchPanelBorder);

        jtbControls = new AppToolBar(fixedWidth);
        uin = UIName.BTN_PLUSFONT;
        btnPlusFont = new AppButton(uin.name, uin.mnemonic, uin.tip);
        btnPlusFont.addActionListener(e -> increaseFontSize());
        uin = UIName.BTN_MINUSFONT;
        btnMinusFont = new AppButton(uin.name, uin.mnemonic, uin.tip);
        btnMinusFont.addActionListener(e -> decreaseFontSize());
        uin = UIName.BTN_RESETFONT;
        btnResetFont = new AppButton(uin.name, uin.mnemonic, uin.tip);
        btnResetFont.addActionListener(e -> resetFontSize());
        uin = UIName.BTN_LOCK;
        btnLock = new AppButton(uin.name, uin.mnemonic, uin.tip);
        btnLock.addActionListener(evt -> showLockScreen(highlightColor));
        uin = UIName.BTN_GOTOP;
        btnGoTop = new AppButton(uin.name, uin.keys, uin.tip);
        btnGoTop.addActionListener(e -> goToFirst());
        uin = UIName.BTN_GOBOTTOM;
        btnGoBottom = new AppButton(uin.name, uin.keys, uin.tip);
        btnGoBottom.addActionListener(e -> goToEnd());
        uin = UIName.BTN_NEXTOCCR;
        btnNextOccr = new AppButton(uin.name, uin.keys, uin.tip);
        btnNextOccr.addActionListener(e -> nextOccr());
        uin = UIName.BTN_PREOCCR;
        btnPreOccr = new AppButton(uin.name, uin.keys, uin.tip);
        btnPreOccr.addActionListener(e -> preOccr());
        uin = UIName.BTN_FIND;
        btnFind = new AppButton(uin.name, uin.mnemonic, uin.tip);
        btnFind.addActionListener(e -> findWordInResult());
        uin = UIName.BTN_HELP;
        btnHelp = new AppButton(uin.name, uin.mnemonic, uin.tip);
        btnHelp.setToolTipText(btnHelp.getToolTipText()
                + ". Color changes to [" + ColorsNFonts.values().length + "] different colors, every [" + HELP_COLOR_CHANGE_SEC + "sec].");
        btnHelp.addActionListener(e -> showHelp());

        uin = UIName.BTN_HBROWSER;
        JButton btnHelpBrowser = new AppButton(uin.name, uin.mnemonic, uin.tip);
        btnHelpBrowser.addActionListener(e -> showHelpInBrowser());

        uin = UIName.BTN_EXPORT;
        JButton btnExport = new AppButton(uin.name, uin.mnemonic, uin.tip);
        btnExport.addActionListener(e -> exportResults());

        uin = UIName.BTN_CLEANEXPORT;
        JButton btnCleanExport = new AppButton(uin.name, uin.mnemonic, uin.tip);
        btnCleanExport.addActionListener(e -> cleanOldExportResults());

        controlPanel = new JPanel();
        JButton btnExit = new AppExitButton();
        jtbControls.add(mbSettings);
        jtbControls.add(btnPlusFont);
        menuSettings.setSize(menuSettings.getWidth(), 20);
        mbSettings.setSize(mbSettings.getWidth(), 20);
        jtbControls.add(btnMinusFont);
        jtbControls.add(btnResetFont);
        jtbControls.add(btnGoTop);
        jtbControls.add(btnGoBottom);
        jtbControls.add(btnPreOccr);
        jtbControls.add(btnNextOccr);
        jtbControls.add(btnFind);
        jtbControls.add(btnLock);
        jtbControls.add(btnExport);
        jtbControls.add(btnCleanExport);
        jtbControls.add(btnHelpBrowser);
        jtbControls.add(btnHelp);
        controlPanel.add(jtbControls);
        controlPanel.add(btnExit);
        controlPanel.setBorder(controlPanelBorder);

        inputPanel = new JPanel();
        inputPanel.setOpaque(true);
        inputPanel.setLayout(new GridBagLayout());
        inputPanel.add(filePanel);
        inputPanel.add(searchPanel);
        inputPanel.add(controlPanel);

        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BorderLayout());
        topPanel.add(inputPanel, BorderLayout.NORTH);
        msgPanel = new JPanel(new BorderLayout());
        lblMsg = new AppLabel();
        lblMsg.setHorizontalAlignment(SwingConstants.CENTER);
        lblMsg.setFont(getNewFont(lblMsg.getFont(), Font.PLAIN, appFontSize));
        uin = UIName.BTN_SHOWALL;
        btnShowAll = new AppButton(uin.name, uin.mnemonic, uin.tip);
        btnShowAll.addActionListener(e -> showAllOccr());
        uin = UIName.BTN_MEMORY;
        btnMemory = new AppButton(uin.name, uin.mnemonic, uin.tip);
        btnMemory.addActionListener(e -> freeMemory());
        msgPanel.add(lblMsg, BorderLayout.CENTER);
        msgButtons = new AppToolBar(fixedWidth);
        msgPanel.setBorder(ZERO_BORDER);
        msgButtons.add(btnShowAll);
        msgButtons.add(btnMemory);
        msgPanel.add(msgButtons, BorderLayout.LINE_END);
        topPanel.add(msgPanel, BorderLayout.SOUTH);

        tpHelp = new JTextPane();
        tpHelp.setEditable(false);
        tpHelp.setContentType("text/html");

        tpContactMe = new JTextPane();
        tpContactMe.setEditable(false);
        tpContactMe.setContentType("text/html");

        kit = new WrapHtmlKit();
        //kit = new HTMLEditorKit();
        /*kit = new WrapHtmlKit();

        ResultTabData rtb = new ResultTabData(title, tabIdx, this);
        tpResults = rtb.getResultPane();
        jspResults = rtb.getJspPane();
        htmlDoc = rtb.getHtmlDoc();
        highlighter = rtb.getHighlighter();*/

        jspHelp = new JScrollPane(tpHelp);
        jspContactMe = new JScrollPane(tpContactMe);

        parentContainer.add(topPanel, BorderLayout.NORTH);

        prepareSettingsMenu();

        tabbedPane = new TabbedPaneHandler(true, this);
        updateForActiveTab();

        int tabIdx = 1;
        String tabTitle = "Help";
        tabbedPane.addTab("", null, jspHelp);
        ResultTabData rtb = new ResultTabData(tabIdx, tabTitle, this);
        TabRemoveHandler trh = new TabRemoveHandler(tabIdx, tabTitle, false, tabbedPane, this);
        trh.getTabLabel().setToolTipText("Displays application help");
        rtb.setTabCloseComponent(SwingUtils.makeTabClosable(tabIdx, trh, tabbedPane));
        applyTabCloseCompColor(rtb.getTabCloseComponent());
        resultTabsData.put(tabTitle, rtb);

        tabIdx = 2;
        tabTitle = "Contact Me";
        tabbedPane.addTab("", null, jspContactMe);
        rtb = new ResultTabData(tabIdx, tabTitle, this);
        trh = new TabRemoveHandler(tabIdx, tabTitle, false, tabbedPane, this);
        trh.getTabLabel().setToolTipText("Displays application help");
        rtb.setTabCloseComponent(SwingUtils.makeTabClosable(tabIdx, trh, tabbedPane));
        applyTabCloseCompColor(rtb.getTabCloseComponent());
        resultTabsData.put(tabTitle, rtb);

        tabbedPane.addChangeListener(e -> setActiveTabVars());

        bottomPanel = new JPanel(new BorderLayout());
        jspAllOccr = new JScrollPane(createAllOccrTable());
        splitAllOccr = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tabbedPane, jspAllOccr);
        splitAllOccr.setOneTouchExpandable(true);
        bottomPanel.add(splitAllOccr, BorderLayout.CENTER);

        parentContainer.add(bottomPanel, BorderLayout.CENTER);

        btnExit.addActionListener(evt -> exitForm());
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent evt) {
                exitForm();
            }
        });

        menuRFiles.setSize(menuRFiles.getWidth(), btnSearch.getHeight());
        menuRSearches.setSize(menuRSearches.getWidth(), btnSearch.getHeight());

        bkColorComponents = new JComponent[]{
                menuSettings, menuRFiles, menuRSearches,
                btnListRS, btnListRF, btnUC, btnLC, btnTC, btnIC,
                btnFileOpen, btnPlusFont, btnMinusFont, btnResetFont, btnGoTop,
                btnGoBottom, btnNextOccr, btnLock, btnPreOccr, btnFind,
                btnHelp, btnHelpBrowser, btnExport, btnCleanExport, btnSearch,
                btnLastN, btnShowAll, btnMemory, btnHelp, jcbMatchCase, jcbWholeWord
        };

        setControlsToEnable();
        setupHelp();
        setupContactMe();
        addBindings();

        setToCenter();
        setExtendedState(JFrame.MAXIMIZED_BOTH);

        resetForNewSearch();
        enableControls();
        showHelp();
        showAllOccr();
        setHighlightColor();
        SwingUtils.getInFocus(menuRFiles);

        // Delay so window can be activated
        Timer t = new Timer();
        t.schedule(new FontChangerTask(this), SEC_1, MIN_10);
        timers.add(t);
        t = new Timer();
        t.schedule(new HelpColorChangerTask(this), SEC_1, HELP_COLOR_CHANGE_TIME);
        timers.add(t);
        t = new Timer();
        t.schedule(new MemoryTrackTask(this), SEC_1, SEC_1 * 4);
        timers.add(t);

        List<WindowChecks> windowChecks = new ArrayList<>();
        windowChecks.add(WindowChecks.WINDOW_ACTIVE);
        if (configs.getBooleanConfig(Configs.AutoLock.name())) {
            windowChecks.add(WindowChecks.AUTO_LOCK);
        }
        if (configs.getBooleanConfig(Configs.ClipboardSupport.name())) {
            windowChecks.add(WindowChecks.CLIPBOARD);
        }
        applyWindowActiveCheck(windowChecks.toArray(new WindowChecks[0]));

        // setting colors here if window is not active then it will set only when color changes
        StyleConstants.setForeground(nonhighlightSAS, Color.black);
        StyleConstants.setBackground(nonhighlightSAS, Color.white);
        StyleConstants.setForeground(highlightSAS, Color.white);
        StyleConstants.setBackground(highlightSAS, Color.blue);

        new Timer().schedule(new ReloadLastTabsTask(this), SEC_1 * 2);
    }

    public void reloadLastTabs() {
        disableControls();
        if (jcbmiMultiTab.isSelected() && jcbmiReopenLastTabs.isSelected()) {
            String[] arrF = recentFilesStr.split(SEPARATOR);
            boolean skipFirst = false; // as first tab is already opened
            for (String s : arrF) {
                if (Utils.hasValue(s)) {
                    if (skipFirst) {
                        txtFilePath.setText(s);
                        updateForActiveTab();
                        Utils.sleep(100);
                    } else {
                        skipFirst = true;
                    }
                }
                if (resultTabsData.size() >= MAX_RESULTS_TAB) {
                    break;
                }
            }
            updateMsgAndTip();
        }
        enableControls();
        setTabCloseButtonColor();
        info("Last tabs reloaded. Results tab data size " + Utils.addBraces(resultTabsData.size()));
        lblMsg.setText(getInitialMsg());
        new Timer().schedule(new AppFontChangerTask(this), SEC_1);
    }

    public void changeAppFont() {
        SwingUtils.applyAppFont(this, appFontSize, this, logger);
    }

    private void updateForActiveTab() {
        addOrSetActiveTab();
        setEditorVars();
    }

    public void tabRemoved(String title, int tabNum) {
        info("Tab removed with title/number " + Utils.addBraces(title + "/" + tabNum));
        resultTabsData.remove(title);
        debug("Tab length is " + Utils.addBraces(tabbedPane.getTabCount())
                + " and resultTabsData in active tab is " + resultTabsData.toString());
        reIndexTabs();
    }

    private void setActiveTabVars() {
        if (resultTabsData.size() > 0 && tabbedPane.getSelectedIndex() > -1) {
            String title = tabbedPane.getTitleAt(tabbedPane.getSelectedIndex());
            if (activeResultTabData == null || !activeResultTabData.getTitle().equalsIgnoreCase(title)) {
                activeResultTabData = resultTabsData.get(title);
                if (activeResultTabData != null) {
                    selectTab(activeResultTabData.getJspPane());
                    setEditorVars();
                }
            }
        }
        info("active tab set to " +
                (activeResultTabData == null ? null : activeResultTabData.toString()));
    }

    private synchronized void reIndexTabs() {
        // update tab indexes
        int tc = tabbedPane.getTabCount();
        logger.info("Before re-indexing tab count is " + Utils.addBraces(tc)
                + " and resultTabs are " + Utils.addBraces(resultTabsData.size()));
        for (int i = 0; i < tc; i++) {
            TabCloseComponent tcc = (TabCloseComponent) tabbedPane.getTabComponentAt(i);
            if (tcc != null) {
                tcc.setTabNum(i);
                resultTabsData.get(tcc.getTitle()).setTabIdx(i);
                resultTabsData.get(tcc.getTitle()).getTabCloseComponent().setTabNum(i);
            }
        }
        debug("Tab length is " + Utils.addBraces(tabbedPane.getTabCount())
                + " and resultTabsData on re-indexing is " + resultTabsData.toString());
    }

    private void setEditorVars() {
        if (activeResultTabData != null) {
            jspResults = activeResultTabData.getJspPane();
            tpResults = activeResultTabData.getResultPane();
            highlighter = activeResultTabData.getHighlighter();
            htmlDoc = activeResultTabData.getHtmlDoc();
            globalCharIdx = activeResultTabData.getGlobalCharIdx();
            qMsgsToAppend = activeResultTabData.getqMsgsToAppend();
            idxMsgsToAppend = activeResultTabData.getIdxMsgsToAppend();
            lastLineOffsetsIdx = activeResultTabData.getLastLineOffsetsIdx();
            lastSelectedRow = activeResultTabData.getLastSelectedRow();
            lineOffsets = activeResultTabData.getLineOffsets();
        }
        resetResultsFont();
    }

    private void addOrSetActiveTab() {
        boolean tabExists = false;
        String title = getTitleForFilePath();
        ResultTabData rtb;
        String activeTitle = "";
        int activeIdx = -1;
        if (activeResultTabData != null) {
            activeTitle = activeResultTabData.getTitle();
            activeIdx = activeResultTabData.getTabIdx();
        }
        info("map size " + Utils.addBraces(resultTabsData.size())
                + " and present activeResultTabData " + activeResultTabData
                + ", activeTitle " + Utils.addBraces(activeTitle)
                + " and activeIdx " + Utils.addBraces(activeIdx));
        activeResultTabData = null;
        if (jcbmiMultiTab.isSelected()) {
            if (isValidate()) {
                for (ResultTabData r : resultTabsData.values()) {
                    if (r.getTitle().equalsIgnoreCase(title)) {
                        tabExists = true;
                        activeResultTabData = r;
                        selectTab(activeResultTabData.getJspPane());
                        break;
                    }
                }
                if (!tabExists) {
                    if (resultTabsData.size() >= MAX_RESULTS_TAB) {
                        if (Utils.hasValue(activeTitle)) {
                            tabbedPane.remove(activeIdx);
                            tabRemoved(activeTitle, activeIdx);
                        }
                    }
                    int tabsCnt = tabbedPane.getTabCount();
                    rtb = new ResultTabData(tabsCnt, title, this);
                    resultTabsData.put(title, rtb);
                    // not adding tooltip on tab as color not applied over it
                    tabbedPane.addTab(title, null, rtb.getJspPane());
                    TabRemoveHandler trh = new TabRemoveHandler(tabsCnt, title, tabbedPane, this);
                    trh.getTabLabel().setToolTipText(getFilePath());
                    rtb.setTabCloseComponent(SwingUtils.makeTabClosable(tabsCnt, trh, tabbedPane));
                    applyTabCloseCompColor(rtb.getTabCloseComponent());
                    activeResultTabData = rtb;
                    addBindingsToNewEditors();
                    selectTab(activeResultTabData.getJspPane());
                }
            }
        } else {
            if (resultTabsData.size() == 0) {
                int tabIdx = 0;
                rtb = new ResultTabData(tabIdx, title, this);
                tabbedPane.addTab(title, null, rtb.getJspPane());
                TabRemoveHandler trh = new TabRemoveHandler(tabIdx, title, false, tabbedPane, this);
                trh.getTabLabel().setToolTipText(getFilePath());
                rtb.setTabCloseComponent(SwingUtils.makeTabClosable(tabIdx, trh, tabbedPane));
                applyTabCloseCompColor(rtb.getTabCloseComponent());
                resultTabsData.put(title, rtb);
                activeResultTabData = rtb;
                selectTab(activeResultTabData.getJspPane());
            }
        }
        if (jspResults != null) {
            SwingUtils.applyAppFont(tabbedPane, appFontSize, this, logger);
        }
        info("activeResultTabData set as " + activeResultTabData);
    }

    public void trackMemory() {
        //debug(getMemoryDetails());
        if (isWindowActive()) {
            long total = Runtime.getRuntime().totalMemory();
            long free = Runtime.getRuntime().freeMemory();
            String mem = Utils.getSizeString(total - free, false, false, 0)
                    + F_SLASH + Utils.getSizeString(total, false, false, 0);
            btnMemory.setText(mem);
            btnMemory.setToolTipText(getMemoryDetails() + ". Click to free memory. Shortcut: Alt+" + UIName.BTN_MEMORY.mnemonic);
        }
    }

    private String checkSep(String s) {
        if (!s.startsWith(SEPARATOR)) {
            s = SEPARATOR + s;
        }
        if (!s.endsWith(SEPARATOR)) {
            s = s + SEPARATOR;
        }
        return s;
    }

    private void setDragNDrop(JComponent[] addBindingsTo) {
        Arrays.stream(addBindingsTo).forEach(j -> {
            // to make drag n drop specific set below method for txtFilePath
            j.setDropTarget(new DropTarget() {
                public synchronized void drop(DropTargetDropEvent e) {
                    try {
                        e.acceptDrop(DnDConstants.ACTION_COPY);
                        List<File> droppedFiles = (List<File>) e.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
                        setFileToSearch(droppedFiles.get(0).getAbsolutePath());
                    } catch (Exception ex) {
                        logger.error("Unable to set dragged file name. " + ex.getMessage());
                    }
                }
            });
        });
    }

    public MyLogger getLogger() {
        return logger;
    }

    @Override
    public void startClipboardAction() {
        Timer t = new Timer();
        t.schedule(new StartClipboardTask(this), SEC_1);
        timers.add(t);
    }

    @Override
    public void copyClipboardYes(String data) {
        setFileToSearch(data);
    }

    @Override
    public void pwdChangedStatus(boolean pwdChanged) {
        if (pwdChanged) {
            updateTitleAndMsg("Password changed", MsgType.INFO);
        }
    }

    private void updateForTabPopupMenu() {
        JPopupMenu pm = tabbedPane.getPopupMenu();
        int tc = tabbedPane.getTabCount();
        for (int i = 0; i < tc; i++) {
            if (tabbedPane.getTabComponentAt(i) instanceof TabCloseComponent) {
                TabCloseComponent lbl = (TabCloseComponent) tabbedPane.getTabComponentAt(i);
                lbl.setComponentPopupMenu(pm);
            }
        }
    }

    private void addBindingsToNewEditors() {
        addKeyBindings(resultTabsData.values().stream().map(ResultTabData::getJspPane)
                .filter(Objects::nonNull).toArray(JComponent[]::new));
        addKeyBindings(resultTabsData.values().stream().map(ResultTabData::getResultPane)
                .filter(Objects::nonNull).toArray(JComponent[]::new));
        updateForTabPopupMenu();
    }

    private void addBindings() {
        final JComponent[] addBindingsTo = {txtFilePath, tpContactMe, tpHelp, lblMsg, btnShowAll, btnMemory, msgPanel};
        addKeyBindings(addBindingsTo);
    }

    public void tabSelected(AppTabbedPane pane, String title, int tabNum) {
        setActiveTabVars();
    }

    private void addKeyBindings(JComponent[] addBindingsTo) {
        Action actionTxtSearch = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                SwingUtils.getInFocus(txtSearch);
            }
        };

        Action actionF3 = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                nextOccr();
            }
        };

        Action actionShiftF3 = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                preOccr();
            }
        };

        Action actionCtrlHome = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                goToFirst();
            }
        };

        Action actionCtrlEnd = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                goToEnd();
            }
        };

        List<KeyActionDetails> keyActionDetails = new ArrayList<>();
        keyActionDetails.add(new KeyActionDetails(KS_CTRL_F, actionTxtSearch));
        keyActionDetails.add(new KeyActionDetails(KS_SHIFT_F3, actionShiftF3));
        keyActionDetails.add(new KeyActionDetails(KS_F3, actionF3));
        keyActionDetails.add(new KeyActionDetails(KeyEvent.VK_HOME, InputEvent.CTRL_DOWN_MASK, actionCtrlHome));
        keyActionDetails.add(new KeyActionDetails(KeyEvent.VK_END, InputEvent.CTRL_DOWN_MASK, actionCtrlEnd));

        SwingUtils.addKeyBindings(addBindingsTo, keyActionDetails);
        setDragNDrop(addBindingsTo);
    }

    // This will be called by reflection from SwingUI jar
    public void appFontChanged(Integer fs) {
        appFontSize = fs;
        logger.info("Application font changed to " + Utils.addBraces(fs));

        TitledBorder[] borders = {filePanelBorder, searchPanelBorder, controlPanelBorder};
        Arrays.stream(borders).forEach(t -> t.setTitleFont(SwingUtils.getNewFontSize(t.getTitleFont(), fs)));
        resetResultsFont();
        // calling to change tooltip font
        changeAppColor();
    }

    private void resetResultsFont() {
        if (tpResults != null) {
            debug("After changing app font, resetting results font to " + Utils.addBraces(resultFontSize));
            tpResults.setFont(getNewFont(tpResults.getFont(), resultFontSize));
            btnResetFont.setText(getFontSize());
        }
    }

    // This will be called by reflection from SwingUI jar
    public void colorChange(Integer x) {
        if (isWindowActive()) {
            colorIdx = x;
            setHighlightColor();
        }
    }

    // This will be called by reflection from SwingUI jar
    public void fontChange(Font f, Integer x) {
        if (isWindowActive()) {
            fontIdx = x;
            setMsgFont(f);
            // calling to change tooltip font
            changeAppColor();
        }
    }

    private void setColorFromIdx() {
        ColorsNFonts c = appColors[colorIdx];
        highlightColor = c.getBk();
        highlightTextColor = c.getFg(); // foreground not working with highlighter //c.getFg();
        selectionColor = c.getSelbk();
        selectionTextColor = c.getSelfg();
    }

    private void prepareSettingsMenu() {
        jcbmiFonts = new AppCheckBoxMenuItem(
                "Change fonts auto",
                getBooleanCfg(Configs.ChangeFontAuto),
                'F',
                "Changes font for information bar every 10 minutes");
        jcbmiHighlights = new AppCheckBoxMenuItem(
                "Change highlight auto",
                getBooleanCfg(Configs.ChangeHighlightAuto),
                'H',
                "Changes colors of highlighted text, selected-text and selected background every 10 minutes");
        jcbmiApplyToApp = new AppCheckBoxMenuItem(
                "Apply color to App",
                getBooleanCfg(Configs.ApplyColorToApp),
                'y',
                "Changes colors of complete application whenever highlight color changes");
        jcbmiAutoLock = new AppCheckBoxMenuItem(
                "Auto Lock*",
                configs.getBooleanConfig(Configs.AutoLock.name()),
                'L',
                "Auto Lock App if idle for 10 min - *change need restart");
        jcbmiClipboardSupport = new AppCheckBoxMenuItem(
                "Clipboard Support*",
                configs.getBooleanConfig(Configs.ClipboardSupport.name()),
                'b',
                "Clipboard support (to use copied text as search file) - *change need restart");
        jcbmiMultiTab = new AppCheckBoxMenuItem(
                "Multi tabs",
                configs.getBooleanConfig(Configs.MultiTab.name()),
                'u',
                "Results will be opened in new tabs, max " + Utils.addBraces(MAX_RESULTS_TAB));
        jcbmiFixedWidth = new AppCheckBoxMenuItem(
                "Fixed width*",
                configs.getBooleanConfig(Configs.FixedWidth.name()),
                'x',
                "Applies fixed width and look to toolbar, use only when UI goes off for menu buttons - *change need restart");
        jcbmiDebugEnabled = new AppCheckBoxMenuItem(
                "Enable debug*",
                configs.getBooleanConfig(Configs.DebugEnabled.name()),
                'g',
                "Enable debug logging - *change need restart");
        jcbmiLogSimpleClassName = new AppCheckBoxMenuItem(
                "Log Simple Class Name",
                configs.getBooleanConfig(Configs.LogSimpleClassName.name()),
                'c',
                "Log Simple Class Name i.e. with package name in logs - *change need restart");
        jcbmiReopenLastTabs = new AppCheckBoxMenuItem(
                "Reopen Last Tabs*",
                configs.getBooleanConfig(Configs.ReopenLastTabs.name()),
                'p',
                "Reopen last opened tabs at restart - *change need restart");


        String t = "Time 10 seconds";
        char mn = 'a';
        jrbmiErrorTimeSec10 = new AppRadioButtonMenuItem(t, isErrorTimeLimit(t), mn++, t);
        jrbmiErrorTimeSec10.addActionListener(e -> setErrorTimeLimit(jrbmiErrorTimeSec10.getText()));
        t = "Time 30 seconds";
        jrbmiErrorTimeSec30 = new AppRadioButtonMenuItem(t, isErrorTimeLimit(t), mn++, t);
        jrbmiErrorTimeSec30.addActionListener(e -> setErrorTimeLimit(jrbmiErrorTimeSec30.getText()));
        t = "Time 50 seconds";
        jrbmiErrorTimeSec50 = new AppRadioButtonMenuItem(t, isErrorTimeLimit(t), mn++, t);
        jrbmiErrorTimeSec50.addActionListener(e -> setErrorTimeLimit(jrbmiErrorTimeSec50.getText()));
        t = "Time 100 seconds";
        jrbmiErrorTimeSec100 = new AppRadioButtonMenuItem(t, isErrorTimeLimit(t), mn++, t);
        jrbmiErrorTimeSec100.addActionListener(e -> setErrorTimeLimit(jrbmiErrorTimeSec100.getText()));
        ButtonGroup errorTimeBG = new ButtonGroup();
        errorTimeBG.add(jrbmiErrorTimeSec10);
        errorTimeBG.add(jrbmiErrorTimeSec30);
        errorTimeBG.add(jrbmiErrorTimeSec50);
        errorTimeBG.add(jrbmiErrorTimeSec100);
        t = "Occurrences 100";
        jrbmiErrorOccr100 = new AppRadioButtonMenuItem(t, isErrorOccrLimit(t), mn++, t);
        jrbmiErrorOccr100.addActionListener(e -> setErrorOccrLimit(jrbmiErrorOccr100.getText()));
        t = "Occurrences 300";
        jrbmiErrorOccr300 = new AppRadioButtonMenuItem(t, isErrorOccrLimit(t), mn++, t);
        jrbmiErrorOccr300.addActionListener(e -> setErrorOccrLimit(jrbmiErrorOccr300.getText()));
        t = "Occurrences 500";
        jrbmiErrorOccr500 = new AppRadioButtonMenuItem(t, isErrorOccrLimit(t), mn++, t);
        jrbmiErrorOccr500.addActionListener(e -> setErrorOccrLimit(jrbmiErrorOccr500.getText()));
        t = "Occurrences 1000";
        jrbmiErrorOccr1000 = new AppRadioButtonMenuItem(t, isErrorOccrLimit(t), mn++, t);
        jrbmiErrorOccr1000.addActionListener(e -> setErrorOccrLimit(jrbmiErrorOccr1000.getText()));
        ButtonGroup errorOccrBG = new ButtonGroup();
        errorOccrBG.add(jrbmiErrorOccr100);
        errorOccrBG.add(jrbmiErrorOccr300);
        errorOccrBG.add(jrbmiErrorOccr500);
        errorOccrBG.add(jrbmiErrorOccr1000);

        AppMenu errorLimitsMenu = new AppMenu("Error Limits", 'r', "Define hard limits");
        errorLimitsMenu.add(jrbmiErrorTimeSec10);
        errorLimitsMenu.add(jrbmiErrorTimeSec30);
        errorLimitsMenu.add(jrbmiErrorTimeSec50);
        errorLimitsMenu.add(jrbmiErrorTimeSec100);
        errorLimitsMenu.addSeparator();
        errorLimitsMenu.add(jrbmiErrorOccr100);
        errorLimitsMenu.add(jrbmiErrorOccr300);
        errorLimitsMenu.add(jrbmiErrorOccr500);
        errorLimitsMenu.add(jrbmiErrorOccr1000);

        menuSettings.add(jcbmiFonts);
        menuFonts = SwingUtils.getFontsMenu("Fonts", 'o', "Fonts",
                Utils.addBraces(getFontFromEnum()), this, logger);
        menuAppFonts = SwingUtils.getAppFontMenu(this, this, appFontSize, logger);
        menuSettings.add(menuFonts);
        menuSettings.addSeparator();
        menuSettings.add(menuAppFonts);
        menuSettings.addSeparator();
        menuSettings.add(jcbmiHighlights);
        menuSettings.add(SwingUtils.getColorsMenu("Highlights", 'g', "Highlight colors",
                true, true, true, false, ignoreBlackAndWhite, this, logger));
        menuSettings.addSeparator();
        menuSettings.add(jcbmiApplyToApp);
        menuSettings.addSeparator();
        AppMenuItem jmiChangePwd = new AppMenuItem("Change Password", 'c');
        jmiChangePwd.setToolTipText("Change password for lock screen");
        jmiChangePwd.addActionListener(e -> showChangePwdScreen(highlightColor));
        AppMenuItem jmiLock = new AppMenuItem("Lock screen", 'o');
        jmiLock.setToolTipText("Lock screen now. Password required to unlock");
        jmiLock.addActionListener(e -> showLockScreen(highlightColor));
        menuSettings.add(jcbmiMultiTab);
        menuSettings.add(jcbmiReopenLastTabs);
        menuSettings.addSeparator();
        menuSettings.add(errorLimitsMenu);
        menuSettings.addSeparator();
        menuSettings.add(jmiChangePwd);
        menuSettings.add(jmiLock);
        menuSettings.add(jcbmiAutoLock);
        menuSettings.add(jcbmiClipboardSupport);
        menuSettings.addSeparator();
        menuSettings.add(jcbmiFixedWidth);
        menuSettings.add(jcbmiDebugEnabled);
        menuSettings.add(jcbmiLogSimpleClassName);

        // setting font from config
        setMsgFont(getNewFont(lblMsg.getFont(), getFontFromEnum()));
    }

    private boolean isErrorTimeLimit(String text) {
        return errorTimeLimit == Utils.convertToInt(Utils.filterNumbers(text), ERROR_LIMIT_SEC);
    }

    private boolean isErrorOccrLimit(String text) {
        return errorOccrLimit == Utils.convertToInt(Utils.filterNumbers(text), ERROR_LIMIT_OCCR);
    }

    private void setErrorTimeLimit(String text) {
        errorTimeLimit = Utils.convertToInt(Utils.filterNumbers(text), ERROR_LIMIT_SEC);
        setMsgBarTip("New hard stop Time/occurrences limit [" + errorTimeLimit + "sec/" + errorOccrLimit + "]");
    }

    private void setErrorOccrLimit(String text) {
        errorOccrLimit = Utils.convertToInt(Utils.filterNumbers(text), ERROR_LIMIT_OCCR);
        setMsgBarTip("New hard stop Time/occurrences limit [" + errorTimeLimit + "sec/" + errorOccrLimit + "]");
    }

    private void setSplitPaneLoc() {
        splitAllOccr.setBottomComponent(jspAllOccr);
        splitAllOccr.setDividerLocation(0.70);
        splitAllOccr.revalidate();
    }

    public void dblClickOffset(AppTable table, Object[] params) {
        // -1 as row number starts from 1
        int selRow = table.getSelectedRow();
        lineOffsetsIdx = Integer.parseInt(table.getValueAt(selRow, 0).toString()) - 1;
        gotoOccr(lineOffsetsIdx);
        lastSelectedRow = selRow;
    }

    private AppTable createAllOccrTable() {
        modelAllOccr = SwingUtils.getTableModel(
                new String[]{"#", "All occurrences - Double click or Enter (Show/Hide this panel. "
                        + SHORTCUT + (Character.toLowerCase((char) btnShowAll.getMnemonic())) + ")"});
        tblAllOccr = new AppTable(modelAllOccr);
        tblAllOccr.setOpaque(true);
        tblAllOccr.addEnterOnRow(new AllOccrEnterAction(tblAllOccr, this));
        tblAllOccr.addDblClickOnRow(this, new Object[]{}, "dblClickOffset");
        tblAllOccr.getColumnModel().getColumn(0).setMaxWidth(100);
        tblAllOccr.getColumnModel().getColumn(0).setMinWidth(100);
        tblAllOccr.getColumnModel().getColumn(0)
                .setCellRenderer(new CellRendererCenterAlign());

        String msg = "Search or read";
        lblNoRow = new JLabel(msg);
        lblNoRow.setToolTipText(msg);
        lblNoRow.setSize(lblNoRow.getPreferredSize());
        tblAllOccr.add(lblNoRow);
        tblAllOccr.setFillsViewportHeight(true);

        return tblAllOccr;
    }

    private void removeOldHighlights() {
        for (int idx : lineOffsets.keySet()) {
            OffsetInfo info = lineOffsets.get(idx);
            removeHighlight(info.getSIdx(), info.getEIdx());
        }
    }

    private void highlightSearch() {
        //highlighter.removeAllHighlights();
        for (int idx : lineOffsets.keySet()) {
            OffsetInfo info = lineOffsets.get(idx);
            highLightInResult(info.getSIdx(), info.getEIdx());
        }
    }

    private void highLightInResult(int s, int e) {
        tpResults.getStyledDocument().setCharacterAttributes(s, e - s, highlightSAS, false);
    }

    /*private Object highLightInResult(int s, int e) {
        try {
            return highlighter.addHighlight(s, e, painter);
        } catch (BadLocationException ex) {
            logger.error("Unable to highlight for start index " + Utils.addBraces(s)
                    + ", end index " + Utils.addBraces(e));
        }
        return null;
    }*/

    private void removeHighlight(int s, int e) {
        tpResults.getStyledDocument().setCharacterAttributes(s, e - s, nonhighlightSAS, false);
    }

    private void createAllOccrRows() {
        int sz = lineOffsets.size();
        TableColumn col0 = tblAllOccr.getColumnModel().getColumn(0);
        col0.setHeaderValue("# " + Utils.addBraces(sz + " row(s)"));

        // removing previous rows
        modelAllOccr.setRowCount(0);

        debug("Total offsets " + Utils.addBraces(sz));
        lblNoRow.setVisible(sz == 0);

        if (sz > 0) {
            String htmlDocText = getResultsTextAsHtml();
            for (int i = 0; i < sz; i++) {
                modelAllOccr.addRow(new String[]{(i + 1) + "",
                        formatValueAsHtml(getOccrExcerpt(getSearchString(),
                                htmlDocText, lineOffsets.get(i).getSIdx(), EXCERPT_LIMIT))});
            }
        }

        if (sz > 0) {
            // select first row or last selected
            int r = 0;
            if (lastSelectedRow != -1 && lastSelectedRow < sz) {
                r = lastSelectedRow;
            }
            logger.debug("Selecting row number " + Utils.addBraces(r));
            tblAllOccr.changeSelection(r, 0, false, false);
        }

        // refresh column name change with result count
        refreshBottomPanel();
    }

    public String formatValueAsHtml(String val) {
        return HTML_STR + ELLIPSIS + val + ELLIPSIS + HTML_END;
    }

    // made public for test, will check later
    public String getOccrExcerpt(String searchStr, String htmlDocText, int sIdx, int limit) {
        int halfLimit = limit / 2;
        int htmlDocLen = htmlDocText.length();

        // below lines will select offset occurrence only in an excerpt
        int searchLen = searchStr.length();
        int searchIdx = sIdx + searchLen;
        String str;
        if (htmlDocLen > limit) {
            if (sIdx == 0) {
                str = htmlDocText.substring(0, htmlDocLen);
                return highlightColorStr
                        + searchUtils.htmlEsc(str.substring(0, searchLen))
                        + FONT_SUFFIX
                        + searchUtils.htmlEsc(str.substring(searchLen));
            } else if (sIdx > halfLimit && htmlDocLen > searchIdx + halfLimit) {
                str = htmlDocText.substring(sIdx - halfLimit, searchIdx + halfLimit);
                return searchUtils.htmlEsc(str.substring(0, halfLimit))
                        + highlightColorStr
                        + searchUtils.htmlEsc(str.substring(halfLimit, halfLimit + searchLen))
                        + FONT_SUFFIX
                        + searchUtils.htmlEsc(str.substring(halfLimit + searchLen));
            }

            if (limit <= searchLen * 2) {
                return highlightColorStr
                        + htmlDocText.substring(sIdx, searchIdx)
                        + FONT_SUFFIX;
            }
        }

        // give it a try with reduce limit using recursion
        return getOccrExcerpt(searchStr, htmlDocText, sIdx, halfLimit);
    }

    private void showAllOccr() {
        jspAllOccr.setVisible(!jspAllOccr.isVisible());
        // as per order
        if (jspAllOccr.isVisible()) {
            setSplitPaneLoc();
        }
        refreshBottomPanel();
    }

    private void refreshBottomPanel() {
        bottomPanel.revalidate();
        bottomPanel.repaint();
    }

    private void changeCase(CaseType type) {
        if (isValidate()) {
            txtSearch.setText(Utils.changeCase(type, txtSearch.getText()));
        }
    }

    private void setControlsToEnable() {
        Component[] components = {
                txtFilePath, txtSearch, btnSearch, btnLastN,
                menuRFiles, menuRSearches, cbLastN, jcbMatchCase,
                jcbWholeWord, btnPlusFont, btnMinusFont, btnResetFont,
                btnGoTop, btnGoBottom, btnNextOccr, btnPreOccr, btnFind,
                menuSettings, btnShowAll, btnLock
        };
        setComponentToEnable(components);
        setComponentContrastToEnable(new Component[]{btnCancel});
        enableControls();
    }

    private void printConfigs() {
        info("Debug enabled " + Utils.addBraces(logger.isDebug()));
    }

    private String prepareToolTip(Color[] c) {
        return HTML_STR +
                "Sample: " + SwingUtils.htmlBGColor(c[0], "Highlight text") + BR +
                "and " + SwingUtils.htmlBGColor(c[1], c[2], "Selected text") +
                HTML_END;
    }

    private void changeHighlightColor() {
        colorIdx++;
        if (colorIdx == appColors.length) {
            colorIdx = 0;
        }
        if (appColors[colorIdx].getBk() == Color.white) {
            colorIdx++;
        }
        setHighlightColor();
        logger.debug("Setting highlight color with index " + Utils.addBraces(colorIdx));
    }

    private void setHighlightColor() {
        if (isWindowActive()) {
            setColorFromIdx();
            StyleConstants.setForeground(highlightSAS, highlightTextColor);
            StyleConstants.setBackground(highlightSAS, highlightColor);
            highlightColorStr = SwingUtils.htmlBGColor(highlightColor);
            //painter = new DefaultHighlighter.DefaultHighlightPainter(highlightColor);
            if (timeTaken != null) {
                findWordInResult();
            }
            if (tblAllOccr != null && tblAllOccr.getRowCount() != 0) {
                dblClickOffset(tblAllOccr, null);
            }

            changeAppColor();
        }
    }

    private void changeAppColor() {
        Color cl = jcbmiApplyToApp.getState() ? highlightColor : ORIG_COLOR;

        filePanelBorder = (TitledBorder) SwingUtils.createTitledBorder(filePanelHeading, highlightTextColor);
        searchPanelBorder = (TitledBorder) SwingUtils.createTitledBorder(searchPanelHeading, highlightTextColor);
        controlPanelBorder = (TitledBorder) SwingUtils.createTitledBorder(controlPanelHeading, highlightTextColor);

        // for tooltip
        JComponent[] tt = {filePanel, searchPanel, controlPanel, msgPanel, msgButtons,
                jtbFile, jtbSearch, jtbControls, tabbedPane};
        Arrays.stream(tt).forEach(t -> Arrays.stream(t.getComponents()).forEach(this::applyTooltipColor));
        resultTabsData.values().stream().map(ResultTabData::getTabCloseComponent)
                .filter(Objects::nonNull).forEach(this::applyTooltipColor);
        setTabCloseButtonColor();

        TitledBorder[] toTitleColor = {filePanelBorder, searchPanelBorder, controlPanelBorder};
        Arrays.stream(toTitleColor).forEach(t -> t.setTitleColor(highlightTextColor));

        filePanel.setBorder(filePanelBorder);
        searchPanel.setBorder(searchPanelBorder);
        controlPanel.setBorder(controlPanelBorder);

        lblMsg.setForeground(selectionColor);

        JComponent[] toSetBorder = {msgPanel, txtFilePath, txtSearch, cbLastN, mbRFiles, mbRSearches, mbSettings};
        Arrays.stream(toSetBorder).forEach(c -> c.setBorder(SwingUtils.createLineBorder(highlightTextColor)));

        // only set border color - not working
        /*toSetBorder = new JComponent[]{btnSearch, btnLastN};
        Arrays.stream(toSetBorder).forEach(c -> c.setBorder(new LineBorder(highlightTextColor)));*/

        // This sets foreground of scroll bar but removes background color
        /*UIManager.put("ScrollBar.thumb", new ColorUIResource(selectionColor));
        jspResults.getVerticalScrollBar().setUI(new BasicScrollBarUI() );
        jspHelp.getVerticalScrollBar().setUI(new BasicScrollBarUI() );*/

        JScrollPane[] panes = {jspResults, jspHelp}; // jspContactMe has no scroll bar
        Arrays.stream(panes).forEach(p -> {
            p.getVerticalScrollBar().setBackground(cl);
            p.getHorizontalScrollBar().setBackground(cl);
        });

        // calling it separately as making opaque is making it weird, so just changing tab color

        Arrays.stream(inputPanel.getComponents()).forEach(c -> SwingUtils.setComponentColor((JComponent) c, cl, null));

        // memory info bar
        JComponent[] ca = {tblAllOccr.getTableHeader(), inputPanel, jtbFile, jtbSearch, jtbControls};
        SwingUtils.setComponentColor(ca, cl, null);

        setBkColors(bkColorComponents);

        //tbc.setColors(selectionTextColor, selectionColor, highlightTextColor, highlightColor);
        resultTabsData.values().forEach(v -> applyTabCloseCompColor(v.getTabCloseComponent()));
        tabbedPane.setBackground(highlightColor);
        tabbedPane.setForeground(highlightTextColor);
        //tabbedPane.setForegroundAt(tabbedPane.getSelectedIndex(), selectionTextColor);
        // This changes selected tab color
        // UIManager.put("TabbedPane.selected", selectionColor);
        // tabbedPane.updateUI();
        //SwingUtilities.updateComponentTreeUI(tabbedPane);
        //tabbedPane.repaint();
    }

    private void setTabCloseButtonColor() {
        resultTabsData.values().forEach(v -> {
            if (v.getTabCloseComponent().isClosable()) {
                applyTooltipColor(v.getTabCloseComponent().getTabButton());
            }
        });
        resultTabsData.values().forEach(v -> applyTooltipColor(v.getTabCloseComponent().getTabLabel()));
    }

    private void applyTooltipColor(Component c) {
        SwingUtils.applyTooltipColorNFont(c, selectionTextColor, selectionColor, lblMsg.getFont());
    }

    private void applyTabCloseCompColor(TabCloseComponent tcc) {
        if (tcc != null) {
            tcc.setColors(selectionTextColor, selectionColor, highlightTextColor, highlightColor);
            setTabCloseButtonColor();
        }
    }

    private void updateRecentMenu(JMenu m, String[] arr, JTextField txtF, String mapKey) {
        m.removeAll();

        int i = 'a';
        for (String a : arr) {
            if (Utils.hasValue(a)) {
                char ch = (char) i;
                AppMenuItem mi = new AppMenuItem(ch + SP_DASH_SP + a);
                mi.addActionListener(e -> {
                    txtF.setText(a);
                    updateMsgAndTip();
                });
                if (i <= 'z') {
                    mi.setMnemonic(i++);
                    addKeyMapToMenuItem(new RecentMenuAction(txtF, a), mi, ch, mapKey + ch);
                }
                m.add(mi);
            }
        }
        SwingUtils.changeFont(m, appFontSize);
    }

    private void addKeyMapToMenuItem(AbstractAction action, AppMenuItem mi, char keycode, String mapKey) {
        InputMap im = mi.getInputMap();
        im.put(KeyStroke.getKeyStroke(keycode, 0), mapKey);
        ActionMap am = mi.getActionMap();
        am.put(mapKey, action);
    }

    private void showHelp() {
        selectTab(jspContactMe);
    }

    private void cleanOldExportResults() {
        int retry = 1;
        while (retry <= MAX_RETRY_EXPORT_DEL) {
            if (searchUtils.cleanOldExportResults()) {
                showMsgAsInfo("Old exported files deleted successfully.");
                break;
            } else {
                showMsg("Failed to delete all export results. Retrying " + Utils.addBraces(retry), MsgType.WARN);
                retry++;
            }
        }
    }

    private boolean resultsAreaHasValue() {
        // Will get text and NOT html document which will be easy to process
        String resultsText = tpResults.getText();
        String resultsTextNoNewLine = resultsText.replaceAll("([\\r\\n])", "");

        return (Utils.hasValue(resultsText) &&
                !resultsTextNoNewLine.equalsIgnoreCase(AppConstants.EMPTY_RESULT_TEXT));
    }

    private void exportResults() {
        if (resultsAreaHasValue()) {
            // Will get text and NOT html document which will be easy to process
            if (searchUtils.exportResults(tpResults.getText())) {
                showMsgAsInfo("Result exported to file successfully.");
            } else {
                showMsg("Failed to export result to file.", MsgType.ERROR);
            }
        } else {
            showMsg("No text to export.", MsgType.WARN);
        }
    }

    private void showHelpInBrowser() {
        new RunCommand(new String[]{"./show-help.bat " + Utils.getCurrentDir()}, logger);
    }

    private void selectTab(JComponent c) {
        tabbedPane.setSelectedComponent(c);
    }

    private void setupHelp() {
        showHelp();
        File file = new File("./help.html");
        try {
            tpHelp.setPage(file.toURI().toURL());
        } catch (IOException e) {
            logger.error("Unable to display help");
            updateTitleAndMsg("Unable to display help", MsgType.ERROR);
        }
    }

    private void setupContactMe() {
        File file = new File("./contact-me.html");
        try {
            tpContactMe.setPage(file.toURI().toURL());
        } catch (IOException e) {
            logger.error("Unable to display contact information");
            updateTitleAndMsg("Unable to display contact information", MsgType.ERROR);
        }
    }

    private void findAsType() {
        if (resultsAreaHasValue()) {
            operation = FILE_OPR.FIND;
            //if (isValidate())
            {
                removeOldHighlights();
                resetOffsets();
                setSearchStrings();
                updateOffsets();
                // Setting here to avoid break and count mismatch during offset processing
                occrTillNow = lineOffsets.size();
                if (occrTillNow > 0) {
                    if (occrTillNow > ERROR_LIMIT_OCCR) {
                        showMsg(getProblemMsg(), MsgType.ERROR);
                    } else if (occrTillNow > WARN_LIMIT_OCCR) {
                        showMsg(getProblemMsg(), MsgType.WARN);
                    } else {
                        showMsgAsInfo("Search for new word [" + searchStr + "] set, total occurrences [" + occrTillNow + "] found. Use next/pre occurrences controls.");
                    }
                } else {
                    showMsg("Search for new word [" + searchStr + "] set, no occurrence found.", MsgType.WARN);
                }
                updateTitle("Find complete - " + getSearchResult(getFilePath(), timeTaken, lineNums, occrTillNow));
            }
        }
    }

    private void findWordInResult() {
        operation = FILE_OPR.FIND;
        if (isValidate()) {
            removeOldHighlights();
            resetOffsets();
            setSearchStrings();
            updateRecentValues();
            updateOffsets();
            // Setting here to avoid break and count mismatch during offset processing
            occrTillNow = lineOffsets.size();
            if (occrTillNow > 0) {
                if (occrTillNow > ERROR_LIMIT_OCCR) {
                    showMsg(getProblemMsg(), MsgType.ERROR);
                } else if (occrTillNow > WARN_LIMIT_OCCR) {
                    showMsg(getProblemMsg(), MsgType.WARN);
                } else {
                    showMsgAsInfo("Search for new word [" + searchStr + "] set, total occurrences [" + occrTillNow + "] found. Use next/pre occurrences controls.");
                }
            } else {
                showMsg("Search for new word [" + searchStr + "] set, no occurrence found.", MsgType.WARN);
            }
            updateTitle("Find complete - " + getSearchResult(getFilePath(), timeTaken, lineNums, occrTillNow));
        }
    }

    private void resetOffsets() {
        lineOffsets.clear();
        lineOffsetsIdx = -1;
        createAllOccrRows();
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

        debug("Going to index " + Utils.addBraces(idx));
        if (offsetsNeedUpdate()) {
            updateOffsets();
        }

        if (lineOffsets.size() != 0 && lineOffsets.size() > idx) {
            selectAndGoToIndex(lineOffsets.get(idx).getSIdx(), lineOffsets.get(idx).getEIdx());
            showMsgAsInfo("Going occurrences for " + Utils.addBraces(searchStr) + " # " + (idx + 1) + "/" + lineOffsets.size());
            lastLineOffsetsIdx = idx;
        } else {
            showMsg("No occurrences for " + Utils.addBraces(searchStr) + " to show", MsgType.WARN);
        }

    }

    private boolean offsetsNeedUpdate() {
        return lineOffsets.size() == 0 || lineOffsets.size() != occrTillNow;
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
        return "Bar turns 'Orange' for warnings and 'Red' for error/force-stop. " +
                "Time/occurrences limit for warning [" + WARN_LIMIT_SEC
                + "sec/" + WARN_LIMIT_OCCR
                + "] and for error [" + errorTimeLimit
                + "sec/" + errorOccrLimit + "] or memory goes above [" + errorMemoryLimitInMB + "MB]";

    }

    //TODO: read from resource path and override icons
    private String getResourcePath(String path) {
        ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        return classloader.getResource(path).toString();
    }

    private void setBkColors(JComponent[] c) {
        Color cl = jcbmiApplyToApp.getState() ? highlightColor : ORIG_COLOR;
        SwingUtils.setComponentColor(c, cl, highlightTextColor, selectionColor, selectionTextColor);
    }

    public Color getSelectionTextColor() {
        return selectionTextColor;
    }

    public Color getSelectionColor() {
        return selectionColor;
    }

    public Font getFontForEditor(String sizeStr) {
        Font retVal = SwingUtils.getPlainCalibriFont(Utils.hasValue(sizeStr) ? Integer.parseInt(sizeStr) : PREFERRED_FONT_SIZE);
        logger.debug("Returning " + getFontDetail(retVal));
        return retVal;
    }

    private String getFontDetail(Font f) {
        return Utils.addBraces(String.format("Font: %s/%s/%s", f.getName(), (f.isBold() ? "bold" : "plain"), f.getSize()));
    }

    private void increaseFontSize() {
        setEditorFontSize(FONT_OPR.INCREASE);
    }

    private void decreaseFontSize() {
        setEditorFontSize(FONT_OPR.DECREASE);
    }

    private void resetFontSize() {
        setEditorFontSize(FONT_OPR.RESET);
    }

    private void setEditorFontSize(FONT_OPR opr) {
        //hideHelp();
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
            default:
                changed = true;
        }

        if (changed) {
            String m = "Applying new font as " + getFontDetail(font);
            logger.info(m);
            tpResults.setFont(font);
            btnResetFont.setText(getFontSize());
            showMsgAsInfo(m);
        } else {
            logger.info("Ignoring request for " + opr + " font. Present " + getFontDetail(font));
        }
    }

    private Font getNewFont(Font font, String name) {
        return getNewFont(name, font.getStyle(), font.getSize());
    }

    private Font getNewFont(Font font, int size) {
        return getNewFont(font, font.getStyle(), size);
    }

    private Font getNewFont(Font font, int style, int size) {
        return getNewFont(font.getName(), style, size);
    }

    private Font getNewFont(String name, int style, int size) {
        info("Returning font as " + name + ", style " + (style == Font.BOLD ? "bold" : "plain") + ", of size " + size);
        return new Font(name, style, size);
    }

    private void showListRF() {
        showRecentList(getFiles(), "Recent files", txtFilePath);
    }

    private void showListRS() {
        showRecentList(getSearches(), "Recent searches", txtSearch);
    }

    // This will be called by reflection from SwingUI jar
    public void handleDblClickOnRow(AppTable table, Object[] params) {
        ((JTextField) params[0]).setText(table.getValueAt(table.getSelectedRow(), 0).toString());
        ((JFrame) params[1]).setVisible(false);
    }

    private void showRecentList(String[] src, String colName, JTextField dest) {
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

        // as this is dynamic not taking to setAppColor method
        Color cl = jcbmiApplyToApp.getState() ? highlightColor : ORIG_COLOR;
        table.getTableHeader().setBackground(cl);
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

    private void createRowsForRecentVals(String[] src, DefaultTableModel model) {
        for (String s : src) {
            model.addRow(new String[]{s});
        }
    }

    private Integer[] getLastNOptions() {
        return new Integer[]{100, 200, 500, 1000, 2000, 3000, 4000, 5000};
    }

    public void updateOnChangeToolTips() {
        lblFilePath.setToolTipText(getFilePath());
        txtFilePath.setToolTipText(getFilePath());
        lblSearch.setToolTipText("Search/Read: " + getSearchString());
    }

    private void resetForNewSearch() {
        debug("reset for new search");

        updateOnChangeToolTips();
        //hideHelp();
        printMemoryDetails();
        insertCounter = 0;
        readCounter = 0;
        maxReadCharTimes = 0;
        disableControls();
        resetShowWarning();
        // sequence is important for removeOldHighlights and resetOffsets
        removeOldHighlights();
        emptyResults();
        updateRecentValues();
        qMsgsToAppend.clear();
        idxMsgsToAppend.clear();
        globalCharIdx = 0;
        resetOffsets();
        setSearchStrings();
        logger.info(getSearchDetails());
        startTime = System.currentTimeMillis();
        status = Status.READING;
    }

    private void printMemoryDetails() {
        info(getMemoryDetails());
    }

    private boolean isMemoryOutOfLimit() {
        return getTotalMemory() > getErrorLimitMemory();
    }

    private String getErrorMemoryLimitStr() {
        return Utils.getSizeString(getErrorLimitMemory());
    }

    private String getTotalMemoryStrNoDecimal() {
        return Utils.getSizeString(getTotalMemory(), true, false, 0);
    }

    private String getTotalMemoryStr() {
        return Utils.getSizeString(getTotalMemory());
    }

    private long getTotalMemory() {
        return Runtime.getRuntime().totalMemory();
    }

    private long getErrorLimitMemory() {
        return errorMemoryLimitInMB * KB * KB;
    }

    private String getMemoryDetails() {
        long total = getTotalMemory();
        long free = Runtime.getRuntime().freeMemory();
        return String.format("Memory: occupied %s of %s free %s, ",
                Utils.getSizeString(total - free),
                Utils.getSizeString(total),
                Utils.getSizeString(free)
        );
    }

    private String getLineNumStr(long line) {
        return "<span style=\"color:blue;\">" + line + "&nbsp;&nbsp;</span>";
    }

    private int calculateOccr(String line, String pattern) {
        // Pattern already processed when calling this method
        String lineLC = isMatchCase() ? line : line.toLowerCase();
        int occr = 0;
        if (Utils.hasValue(lineLC) && Utils.hasValue(pattern)) {
            int idx = 0;
            while (idx != -1) {
                idx = lineLC.indexOf(pattern, idx);
                if (idx != -1) {
                    if (checkForWholeWord(pattern, lineLC, idx)) {
                        occr++;
                    }
                    idx = idx + pattern.length();
                }
            }
        }
        //debug("calculateOccr: pattern [" + pattern + "], occr [" + occr + "]");*/
        return occr;
    }

    private void setSearchPattern(String s) {
        txtSearch.setText(s);
        showMsgAsInfo("Search pattern set as [" + s + "]");
    }

    public void setFileToSearch(String s) {
        txtFilePath.setText(s);
        updateMsgAndTip();
    }

    private void updateMsgAndTip() {
        showMsgAsInfo("File set [" + getFilePath() + "] and search set [" + getSearchString() + "]");
        updateOnChangeToolTips();
    }

    private String[] getFiles() {
        return getCfg(Configs.RecentFiles).split(SEPARATOR);
    }

    private String[] getSearches() {
        return getCfg(Configs.RecentSearches).split(SEPARATOR);
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
            logger.warn("Search cancelled by user.");
            showMsg("Search cancelled.", MsgType.ERROR);
        }

        if (isReading() || isErrorState()) {
            status = Status.CANCELLED;
        }
    }

    private void searchFile() {
        updateForActiveTab();
        operation = FILE_OPR.SEARCH;
        if (isValidate()) {
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
        updateForActiveTab();
        operation = FILE_OPR.READ;
        if (isValidate()) {
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

        if (result && operation != FILE_OPR.READ && !Utils.hasValue(getSearchString())) {
            updateTitleAndMsg("Validation error - REQUIRED: text to search");
            result = false;
        }
        int len = getSearchString().length();
        if (result && len != 0 && len < SEARCH_STR_LEN_LIMIT) {
            updateTitleAndMsg("Validation error - LENGTH: text to search should be " + SEARCH_STR_LEN_LIMIT + " or more characters");
            result = false;
        }

        if (!result) {
            logger.info("Validation failed !!");
        }

        return result;
    }

    private void updateRecentValues() {
        debug("update recent search values");

        String s = getFilePath();
        if (Utils.hasValue(s)) {
            recentFilesStr = checkItems(s, recentFilesStr);
            String[] arrF = recentFilesStr.split(SEPARATOR);
            updateRecentMenu(menuRFiles, arrF, txtFilePath, TXT_F_MAP_KEY);
            txtFilePath.setAutoCompleteArr(arrF);
        }

        s = getSearchString();
        if (Utils.hasValue(s)) {
            recentSearchesStr = checkItems(s, recentSearchesStr);
            String[] arrS = recentSearchesStr.split(SEPARATOR);
            updateRecentMenu(menuRSearches, arrS, txtSearch, TXT_S_MAP_KEY);
            txtSearch.setAutoCompleteArr(arrS);
        }
    }

    private void removeItemAndUpdate() {
        String strToRemove = SEPARATOR + getFilePath() + SEPARATOR;
        boolean exists = recentFilesStr.contains(strToRemove);
        debug("String to remove " + Utils.addBraces(strToRemove) + " exists " + Utils.addBraces(exists));
        if (exists) {
            recentFilesStr = recentFilesStr.replace(strToRemove, SEPARATOR);
        }
        txtFilePath.setText("");
        String[] arrF = recentFilesStr.split(SEPARATOR);
        updateRecentMenu(menuRFiles, arrF, txtFilePath, TXT_F_MAP_KEY);
        txtFilePath.setAutoCompleteArr(arrF);
    }

    private String checkItems(String searchStr, String csv) {
        if (!Utils.hasValue(searchStr)) {
            return csv;
        }

        String csvLC = csv.toLowerCase();
        String ssp = SEPARATOR + searchStr;
        String ss = ssp + SEPARATOR;
        String ssLC = ss.toLowerCase();
        if (csvLC.contains(ssLC)) {
            int idx = csvLC.indexOf(ssLC);
            // remove item and add it again to bring it on top
            csv = csv.substring(0, idx)
                    + SEPARATOR + csv.substring(idx + ssLC.length());
        }
        csv = ssp + csv;

        String[] arr = csv.split(SEPARATOR);
        if (arr.length > RECENT_LIMIT) {
            arr = Arrays.stream(arr).limit(RECENT_LIMIT).collect(Collectors.toList()).toArray(new String[RECENT_LIMIT]);
            csv = SEPARATOR + String.join(SEPARATOR, arr) + SEPARATOR;
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

    private String regexEsc(String str) {
        str = str.replaceAll("\\(", "\\\\(");
        //str = str.replaceAll("\\.", "\\\\.");
        //str = str.replaceAll("\\[", "\\\\[");
        return str;
    }

    private void setSearchStrings() {
        searchStr = getSearchString();
    }

    private void startThread(Callable<Boolean> callable) {
        threadPool.submit(callable);
    }

    public void appendResult(String data) {
        synchronized (SearchBigFile.class) {
            // Needs to be sync else line numbers and data will be jumbled
            try {
                if (isReadOpr()) {
                    Element body = getBodyElement();
                    int offs = Math.max(body.getStartOffset(), 0);
                    kit.insertHTML(htmlDoc, offs, data, 0, 0, null);
//                    kit.insertHTML(htmlDoc, 0, data, 0, 0, null);
                } else {
                    kit.insertHTML(htmlDoc, htmlDoc.getLength(), data, 0, 0, null);
                }
            } catch (BadLocationException | IOException e) {
                logger.error("Unable to append data: " + data);
            }

            if (readCounter == insertCounter) {
                // clearing so if text without occurrence appended offset will change
                lineOffsets.clear();
                updateOffsets();
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

    private void emptyResults() {
        tpResults.setText("");
    }

    /**
     * Exit the Application
     */
    private void exitForm() {
        cancelTimers();
        configs.saveConfig(this);
        setVisible(false);
        dispose();
        logger.dispose();
        System.exit(0);
    }

    private void cancelTimers() {
        timers.forEach(Timer::cancel);
    }

    public String getFilePath() {
        return txtFilePath.getText();
    }

    public String getTitleForFilePath() {
        String nm = Utils.getFileName(txtFilePath.getText());
        if (nm.length() > TAB_TITLE_LIMIT) {
            nm = nm.substring(0, TAB_TITLE_LIMIT - ELLIPSIS.length()) + ELLIPSIS;
        }
        return nm;
    }

    public String getFontSize() {
        return tpResults.getFont().getSize() + "";
    }

    public String getAppFontSize() {
        return appFontSize + "";
    }

    public String getFontIndex() {
        return fontIdx + "";
    }

    public String getFixedWidth() {
        return jcbmiFixedWidth.isSelected() + "";
    }

    public String getMultiTab() {
        return jcbmiMultiTab.isSelected() + "";
    }

    public String getReopenLastTabs() {
        return jcbmiReopenLastTabs.isSelected() + "";
    }

    public String getColorIndex() {
        return colorIdx + "";
    }

    public String getChangeHighlightAuto() {
        return jcbmiHighlights.isSelected() + "";
    }

    public String getApplyColorToApp() {
        return jcbmiApplyToApp.isSelected() + "";
    }

    public String getAutoLock() {
        return jcbmiAutoLock.isSelected() + "";
    }

    public String getClipboardSupport() {
        return jcbmiClipboardSupport.isSelected() + "";
    }

    public String getChangeFontAuto() {
        return jcbmiFonts.isSelected() + "";
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
        return jcbmiDebugEnabled.isSelected() + "";
    }

    public String getErrorTimeLimit() {
        return errorTimeLimit + "";
    }

    public String getErrorOccrLimit() {
        return errorOccrLimit + "";
    }

    public String getLogSimpleClassName() {
        return jcbmiLogSimpleClassName.isSelected() + "";
    }

    public String getErrorMemoryLimitInMB() {
        return errorMemoryLimitInMB + "";
    }

    public void showMsgAsInfo(String msg) {
        showMsg(msg, MyLogger.MsgType.INFO);
    }

    public void showMsg(String msg, MyLogger.MsgType type) {
        if (Utils.hasValue(msg)) {
            Color b = Color.white;
            Color f = selectionColor;
            if (type == MyLogger.MsgType.ERROR) {
                b = Color.red;
                f = Color.white;
            } else if (type == MyLogger.MsgType.WARN) {
                b = Color.orange;
                f = Color.black;
            }
            msgPanel.setBackground(b);
            lblMsg.setForeground(f);
            lblMsg.setText(Utils.getTimeGlobal() + Constants.SP_DASH_SP + msg);
        }
    }

    public String getProblemMsg() {
        StringBuilder sb = new StringBuilder();
        if (timeTillNow > WARN_LIMIT_SEC) {
            sb.append("Warning: Time [").append(timeTillNow)
                    .append("] > warning limit [").append(WARN_LIMIT_SEC).append("]. ");
        }
        if (occrTillNow > WARN_LIMIT_OCCR) {
            sb.append("Warning: Occurrences [").append(occrTillNow)
                    .append("] > warning limit [").append(WARN_LIMIT_OCCR).append("], try to narrow your search.");
        }
        sb.append("Memory ").append(getTotalMemoryStr());
        StringBuilder sbErr = new StringBuilder();
        if (timeTillNow > errorTimeLimit) {
            sbErr.append("Error: Time [").append(timeTillNow)
                    .append("] > force stop limit [").append(errorTimeLimit).append("]. Cancelling search...");
        }
        if (occrTillNow > errorOccrLimit) {
            sbErr.append("Error: Occurrences [").append(occrTillNow)
                    .append("] > force stop limit [").append(errorOccrLimit)
                    .append("], try to narrow your search. Cancelling search...");
        }
        if (isMemoryOutOfLimit()) {
            sbErr.append("Error: Out of memory limits. Total memory ").append(getTotalMemoryStr())
                    .append(" > force stop limit ").append(getErrorMemoryLimitStr())
                    .append(". Cancelling search...");
        }

        if (Utils.hasValue(sbErr.toString())) {
            sb = sbErr;
            debug("Error message: " + sb);
        }

        return sb.toString();
    }

    private String processPattern() {
        String searchPattern = searchStr;

        if (!isMatchCase()) {
            searchPattern = searchPattern.toLowerCase();
        }

        return searchPattern;
    }

    private void printCounters() {
        logger.debug("insertCounter [" + insertCounter
                + "], readCounter [" + readCounter
                + "], qMsgsToAppend size [" + qMsgsToAppend.size()
                + "], idxMsgsToAppend size [" + idxMsgsToAppend.size()
                + "], lineOffsets size [" + lineOffsets.size()
                + "]");
    }

    private String getSearchResult(String path, String seconds, long lineNum, long occurrences) {
        String ln = Utils.formatNumber(lineNum);
        String result =
                String.format("File size %s, " +
                                "time taken %s, lines read [%s]" +
                                (isSearchStrEmpty() ? "" : ", occurrences [%s], memory %s"),
                        Utils.getSizeString(new File(path).length()),
                        seconds,
                        ln,
                        occurrences,
                        getTotalMemoryStrNoDecimal());

        logger.info(result);
        return result;
    }

    public void debug(String s) {
        logger.debug(s);
    }

    public void info(String s) {
        logger.info(s);
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
        // so next occr can work
        lineOffsetsIdx = -1;
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
        lineOffsetsIdx = lineOffsets.size() > 0 ? lineOffsets.size() : -1;
        lastLineOffsetsIdx = lineOffsets.size() > 0 ? lineOffsets.size() - 1 : -1;
    }

    public void selectAndGoToIndex(int idx) {
        selectAndGoToIndex(idx, idx);
    }

    public void highlightLastSelectedItem() {
        // as called on lost focus
        synchronized (SearchBigFile.class) {
            if (lastLineOffsetsIdx != -1) {
                OffsetInfo offsetInfo = lineOffsets.get(lastLineOffsetsIdx);
                if (offsetInfo != null) {
                    //highlighter.removeHighlight(offsetInfo.getObj());
                    highLightInResult(offsetInfo.getSIdx(), offsetInfo.getEIdx());
                }
            }
        }
    }

    private void repaintLastItem() {
        if (lineOffsetsIdx != -1) {
            if (lineOffsetsIdx == lineOffsets.size()) {
                lineOffsetsIdx--;
            }
        }
        if (lineOffsetsIdx > -1) {
            highlightLastSelectedItem();
        }
    }

    public void selectAndGoToIndex(int sIdx, int eIdx) {
        //hideHelp();
        tpResults.grabFocus();
        repaintLastItem();
        tpResults.select(sIdx, eIdx);
        highlightLastSelectedItem();
    }

    public void finishAction() {
        info("Performing finish action");
        printCounters();
        /*if (showWarning && !isErrorState()) {
            SwingUtilities.invokeLater(new StartWarnIndicator(this));
        }*/
        goToEnd(false);
        logger.debug("Timer: thread pool status " + threadPool.toString());
        // requesting to free used memory
        freeMemory();
        printMemoryDetails();
    }

    private void freeMemory() {
        System.gc();
    }

    private void updateOffsets() {
        debug("Offsets size " + Utils.addBraces(lineOffsets.size()));
        timeTillNow = 0;
        if (offsetsNeedUpdate()) {
            lineOffsets.clear();
            if (!isSearchStrEmpty()) {
                String strToSearch = processPattern();
                int strToSearchLen = strToSearch.length();
                debug("Starting search for string [" + strToSearch + "]");

                String htmlDocText = getResultsTextAsHtml();
                if (!isMatchCase()) {
                    htmlDocText = htmlDocText.toLowerCase();
                }
                info("Updating offsets.  Doc length " + Utils.getSizeString(htmlDocText.length()));
                //debug(htmlDocText);

                int idx = 0, x = 0;
                while (idx != -1) {
                    // Let create offsets for all occurrences even if
                    // those are slightly higher then error due to in-line processing
                    // This is because count will be mismatched if break is applied
                    idx = htmlDocText.indexOf(strToSearch, globalCharIdx);
                    globalCharIdx = idx + strToSearchLen;
                    if (idx != -1 && checkForWholeWord(strToSearch, htmlDocText, idx)) {
                        lineOffsets.put(x++, new OffsetInfo(null, idx, globalCharIdx));
                    }
                }
                createAllOccrRows();
                highlightSearch();
                //debug("All offsets are " + lineOffsets);
            }
        } else {
            debug("No need to update offsets, selecting row now");
        }
    }

    public String getResultsTextAsHtml() {
        try {
            return htmlDoc.getText(0, htmlDoc.getLength());
        } catch (BadLocationException e) {
            logger.error("Unable to get results.  Details: ", e);
        }

        return "";
    }

    // made public for test, will check later
    public boolean checkForWholeWord(String strToSearch, String line, int idx) {
        if (!isWholeWord()) {
            return true;
        }

        int searchLen = strToSearch.length();
        int lineLen = line.length();
        // starts with case
        /*debug("strToSearch " + Utils.addBraces(strToSearch)
                //+ ", line " + Utils.addBraces(line)
                + ", idx " + Utils.addBraces(idx)
                + ", lineLen " + Utils.addBraces(lineLen)
                + ", searchLen " + Utils.addBraces(searchLen));*/
        if (idx == 0) {
            if (lineLen == idx + searchLen) {
                return true;
            } else if (lineLen >= idx + searchLen + 1) {
                return Utils.isWholeWordChar(line.charAt(idx + searchLen));
            }
        } else if (idx == lineLen - searchLen) { // ends with case
            if (idx > 0) {
                return Utils.isWholeWordChar(line.charAt(idx - 1));
            }
        } else if (idx != -1 && lineLen > idx + searchLen) { // + 2 is for char before and after
            // in between
            return Utils.isWholeWordChar(line.charAt(idx + searchLen)) && Utils.isWholeWordChar(line.charAt(idx - 1));
        }
        return false;
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

    public boolean isReading() {
        return status == Status.READING;
    }

    public boolean isReadOpr() {
        return operation == FILE_OPR.READ;
    }

    public boolean isSearchStrEmpty() {
        return !Utils.hasValue(getSearchString());
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

    public boolean isWarnMemoryState() {
        return getTotalMemory() > warnMemoryLimitInMB;
    }

    public boolean isErrorState() {
        return timeTillNow > errorTimeLimit || occrTillNow > errorOccrLimit
                || isMemoryOutOfLimit();
    }

    public void incRCtrNAppendIdxData() {
        synchronized (SearchBigFile.class) {
            readCounter++;
            appendResult(idxMsgsToAppend.get(readCounter));
        }
    }

    private String getNextFont() {
        fontIdx++;
        if (fontIdx == ColorsNFonts.values().length) {
            fontIdx = 0;
        }
        return getFontFromEnum();
    }

    private String getFontFromEnum() {
        return ColorsNFonts.values()[fontIdx].getFont();
    }

    public void changeHelpColor() {
        if (isWindowActive()) {
            for (ColorsNFonts c : ColorsNFonts.values()) {
                btnHelp.setForeground(c.getFg());
                Utils.sleep(500);
            }
        }
    }

    public void changeMsgFont() {
        if (Boolean.parseBoolean(getChangeFontAuto())) {
            Font f = lblMsg.getFont();
            f = new Font(getNextFont(), f.getStyle(), appFontSize);
            setMsgFont(f);
        }

        if (Boolean.parseBoolean(getChangeHighlightAuto())) {
            changeHighlightColor();
        }
    }

    // called from AppFrame
    public void removeFileFromRecentYes(String data) {
        logger.info("removing file from recents " + Utils.addBraces(getFilePath()));
        removeItemAndUpdate();
        updateTitleAndMsg("Removed from recent file list", MsgType.INFO);
    }

    // called from AppFrame
    public void removeFileFromRecentNo(String data) {
        logger.info("removeFileFromRecentYes: No action taken for data " + Utils.addBraces(data));
    }

    public void setMsgFont(Font f) {
        menuFonts.setText("Fonts " + Utils.addBraces(getFontFromEnum()));
        lblMsg.setFont(f);
        setMsgBarTip();
    }

    private void setMsgBarTip(String msg) {
        String tip = HTML_STR
                + "Font for this bar " + msg + ", changes every [" + TEN + "min] if chosen - see 'Settings' menu. "
                + BR
                + "Highlight/Selected color changes every [" + TEN + "min] if chosen - see 'Settings' menu. "
                + BR +
                getInitialMsg() + HTML_END;
        lblMsg.setToolTipText(tip);
        //msgPanel.setToolTipText(tip);
        showMsgAsInfo(msg);
        debug(msg);
    }

    private void setMsgBarTip() {
        setMsgBarTip(getFontDetail(lblMsg.getFont()));
    }

    private void fileNotFoundAction() {
        updateTitleAndMsg("File not exists: " + getFilePath(), MsgType.ERROR);
        createYesNoDialog("Remove entry ?",
                "File not exists " + Utils.addBraces(getFilePath())
                        + ", do you want to remove it from Recent list ?",
                "removeFileFromRecent");
    }

    private String addLineNumAndEscAtStart(long lineNum, String str) {
        return BR + addOnlyLineNumAndEsc(lineNum, str);
    }

    private String addOnlyLineNumAndEsc(long lineNum, String str) {
        return getLineNumStr(lineNum) + escString(str);
    }

    private String addLineNumAndEsc(long lineNum, String str) {
        return getLineNumStr(lineNum) + escString(str) + BR;
    }

    private String escString(String str) {
        return searchUtils.escString(str);
    }

    private String addLineEnd(String str) {
        return str + BR;
    }

    private MsgType getMsgTypeForOpr() {
        if (isErrorState()) {
            return MsgType.ERROR;
        }
        return isWarningState() ? MsgType.WARN : MsgType.INFO;
    }

    /* Inner classes */
    class LastNRead implements Callable<Boolean> {

        private final Stack<String> stack = new Stack<>();
        private final SearchBigFile sbf;

        LastNRead(SearchBigFile sbf) {
            this.sbf = sbf;
        }

        @Override
        public Boolean call() {
            final int LIMIT = Integer.parseInt(cbLastN.getSelectedItem().toString());
            int readLines = 0, occr = 0;
            boolean hasError = false;
            String searchPattern = processPattern(), fn = getFilePath();
            StringBuilder sb = new StringBuilder();
            File file = new File(fn);

            updateTitle("Reading last " + LIMIT + " lines");
            logger.info("Loading last [" + LIMIT + "] lines from [" + Utils.addBraces(fn));
            // FIFO
            stack.removeAllElements();

            try (RandomAccessFile randomAccessFile = new RandomAccessFile(file, "r")) {
                long fileLength = file.length();
                long time = System.currentTimeMillis();
                for (long pointer = fileLength; pointer >= 0; ) {
                    int len = pointer > MAX_READ_CHAR_LIMIT ? MAX_READ_CHAR_LIMIT : (int) pointer;
                    pointer -= len;
                    randomAccessFile.seek(pointer);
                    byte[] bytes = new byte[len];
                    randomAccessFile.read(bytes);
                    if (pointer <= 0) {
                        // to break loop
                        pointer = -1;
                    }
                    int bl = bytes.length;
                    boolean maxReadCharLimitReached;
                    for (int i = bl - 1; i > -1; i--) {
                        char c = (char) bytes[i];
                        // break when end of the line
                        maxReadCharLimitReached = sb.length() >= MAX_READ_CHAR_LIMIT;
                        if (maxReadCharLimitReached) {
                            maxReadCharTimes++;
                        }
                        boolean newLineChar = c == '\n';
                        if (newLineChar || maxReadCharLimitReached) {

                            if (!newLineChar) {
                                sb.append(c);
                            }

                            if (Utils.hasValue(sb.toString())) {
                                sb.reverse();
                            }

                            //TODO: check if mixed approach can be used based on size
                            occr += calculateOccr(sb.toString(), searchPattern);
                            processForRead(readLines, sb.toString(), occr, newLineChar);

                            sb = new StringBuilder();
                            if (newLineChar) {
                                readLines++;
                            }

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
                    }
                    if (readLines == LIMIT - 1 || isCancelled()) {
                        break;
                    }
                }
                if (maxReadCharTimes > 0) {
                    logger.info("read: max read char limit " + Utils.addBraces(MAX_READ_CHAR_LIMIT) + " reached "
                            + Utils.addBraces(maxReadCharTimes) + " times, processing...");
                }

                info("File read complete in " + Utils.getTimeDiffSecMilliStr(time));
                if (Utils.hasValue(sb.toString())) {
                    sb.reverse();
                }
                // last remaining data
                processForRead(false, readLines, sb.toString(), occr, true, true);
                readLines++;
            } catch (FileNotFoundException e) {
                catchForRead(e);
                hasError = true;
                sbf.fileNotFoundAction();
            } catch (Exception e) {
                catchForRead(e);
                hasError = true;
            } finally {
                enableControls();
            }

            occr += calculateOccr(sb.toString(), searchPattern);
            occrTillNow = occr;
            if (!hasError) {
                timeTaken = Utils.getTimeDiffSecMilliStr(startTime);
                lineNums = readLines;
                String result = getSearchResult(
                        fn,
                        timeTaken,
                        lineNums,
                        occr);
                String statusStr = isCancelled() ? "Read cancelled - " : "Read complete - ";
                updateTitleAndMsg(statusStr + result, getMsgTypeForOpr());
            }
            status = Status.DONE;

            // No need to wait as data can be async added at top
            sbf.finishAction();
            return true;
        }

        private void catchForRead(Exception e) {
            String msg = "ERROR: " + e.getMessage();
            logger.error(e.getMessage());
            tpResults.setText(R_FONT_PREFIX + msg + FONT_SUFFIX);
            sbf.updateTitleAndMsg("Unable to read file: " + getFilePath(), MsgType.ERROR);
        }

        private void processForRead(int line, String str, int occr, boolean appendLineNum) {
            processForRead(true, line, str, occr, appendLineNum, false);
        }

        private void processForRead(boolean needBRAtStart, int lineNum, String str, int occr, boolean appendLineNum, boolean bypass) {
            String strToAppend = "";
            if (appendLineNum) {
                strToAppend = needBRAtStart ? addLineNumAndEscAtStart(lineNum + 1, str) :
                        addOnlyLineNumAndEsc(lineNum + 1, str);
            } else {
                strToAppend = escString(str);
            }

            synchronized (SearchBigFile.class) {
                // emptying stack to Q
                stack.push(strToAppend);
                if (bypass || stack.size() > APPEND_MSG_CHUNK) {
                    while (!stack.empty()) {
                        qMsgsToAppend.add(stack.pop());
                    }
                    startThread(msgCallable);
                }
            }

            occrTillNow = occr;
            linesTillNow = lineNum;
            if (!showWarning && occr > WARN_LIMIT_OCCR) {
                showWarning = true;
            }
        }
    }

    // To avoid async order of lines this cannot be worker
    class SearchData {

        private final int LINES_TO_INFORM = 1000_000;
        private final SearchStats stats;

        SearchData(SearchStats stats) {
            this.stats = stats;
        }

        public void process() {
            long lineNum = stats.getLineNum();
            StringBuilder sb = new StringBuilder();

            boolean addLineEnding = stats.isAddLineEnding();
            if (stats.isMatch()) {
                int occr = calculateOccr(stats.getLine(), stats.getSearchPattern());
                if (occr > 0) {
                    stats.setOccurrences(stats.getOccurrences() + occr);
                    sb.append(escString(stats.getLine()));
                    String s = addLineEnding ? addLineNumAndEscAtStart(stats.getLineNum(), "") : "";
                    qMsgsToAppend.add(s + sb.toString());
                }
            }
            if (addLineEnding) {
                stats.setLineNum(lineNum + 1);
            }

            if (lineNum % LINES_TO_INFORM == 0) {
                logger.debug("Lines searched so far: " + Utils.formatNumber(lineNum));
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
            boolean warnMemoryState = false;
            do {
                // Due to multi threading, separate if is imposed
                if (isReading()) {
                    timeElapse = Utils.getTimeDiffSec(startTime);
                    timeTillNow = timeElapse;
                    String msg = timeElapse + " sec, lines [" + sbf.linesTillNow + "], memory " + getTotalMemoryStr();
                    if (showWarning || isWarningState()) {
                        msg = sbf.getProblemMsg();
                        sbf.debug("Invoking warning indicator.");
                        SwingUtilities.invokeLater(new StartWarnIndicator(sbf));
                    }
                    if (!warnMemoryState && isWarnMemoryState()) {
                        warnMemoryState = true;
                        sbf.debug("Memory " + getTotalMemoryStr() + " raised from warning state, trying to free memory.");
                        freeMemory();
                    }
                    if (isErrorState()) {
                        sbf.logger.warn("Stopping forcefully.");
                        cancelSearch();
                    }
                    if (isReading()) {
                        sbf.updateTitle(msg);
                    }
                    logger.debug("Timer callable sleeping now for a second, status: " + status);
                    Utils.sleep(1000, sbf.logger);
                }
            } while (isReading());

            logger.info("Timer stopped after " + Utils.addBraces(timeElapse) + " sec");
            return true;
        }
    }

    private boolean hasOccr(String line, String searchPattern) {
        return (!isMatchCase() && line.toLowerCase().contains(searchPattern))
                || (isMatchCase() && line.contains(searchPattern))
                || (isWholeWord() && line.matches(searchPattern));
    }

    static class AppendMsgCallable implements Callable<Boolean> {

        private final SearchBigFile sbf;

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
                        if (sbf.isReadOpr() || Utils.hasValue(m)) {
                            sb.append(m);
                        }
                    }
                    if (sbf.isReadOpr() || sb.length() > 0) {
                        insertCounter++;
                        idxMsgsToAppend.put(insertCounter, sb.toString());
                        SwingUtilities.invokeLater(new AppendData(sbf));
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
            final int BUFFER_SIZE = 200 * KB;
            String searchPattern = sbf.processPattern();
            String path = sbf.getFilePath();
/*
            try (InputStream stream = new FileInputStream(path);
                 BufferedReader br = new BufferedReader(new InputStreamReader(stream), BUFFER_SIZE)
            ) {
*/
            try (SeekableByteChannel ch = Files.newByteChannel(Utils.createPath(path), EnumSet.of(StandardOpenOption.READ))) {
                ByteBuffer bb = ByteBuffer.allocateDirect(BUFFER_SIZE);

                long lineNum = 1, occurrences = 0, time = System.currentTimeMillis();
                SearchStats stats = new SearchStats(lineNum, occurrences, null, searchPattern);
                SearchData searchData = new SearchData(stats);

                while (ch.read(bb) >= 0) {
                    bb.flip();
                    String s = StandardCharsets.UTF_8.decode(bb).toString();
                    bb.clear();
                    String lnBrk = s.contains(LN_BRK) ? LN_BRK : LN_BRK_REGEX;
                    String[] lines = s.split(lnBrk);

                    for (int i = 0, linesLength = lines.length; i < linesLength; i++) {
                        String ln = lines[i];
                        stats.setAddLineEnding(i < linesLength - 1);
                        stats.setLine(ln);
                        stats.setMatch(sbf.hasOccr(ln, searchPattern));

                        if (!isCancelled() && occrTillNow <= ERROR_LIMIT_OCCR) {
                            searchData.process();
                            if (qMsgsToAppend.size() > APPEND_MSG_CHUNK) {
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
                    if (isCancelled()) {
                        break;
                    }
                }
                if (maxReadCharTimes > 0) {
                    logger.info("search: max read char limit " + Utils.addBraces(MAX_READ_CHAR_LIMIT) + " reached "
                            + Utils.addBraces(maxReadCharTimes) + " times, processing...");
                }
                logger.info("File read in " + Utils.getTimeDiffSecMilliStr(time));

                if (!isCancelled()) {
                    time = System.currentTimeMillis();
                    startThread(msgCallable);
                    while (readCounter != insertCounter) {
                        if (isCancelled()) {
                            debug("Status is cancelled.  Exiting wait condition.");
                            break;
                        }
                        logger.debug("Waiting for readCounter to be equal insertCounter");
                        Utils.sleep(200, sbf.logger);
                    }
                    idxMsgsToAppend.clear();
                    logger.info("Time in waiting all message to append is " + Utils.getTimeDiffSecMilliStr(time));
                }
                timeTaken = Utils.getTimeDiffSecMilliStr(startTime);
                lineNums = stats.getLineNum();
                String result = getSearchResult(path, timeTaken, lineNums, stats.getOccurrences());
                if (stats.getOccurrences() == 0 && !isErrorState() && !isCancelled()) {
                    String s = "No match found";
                    sbf.tpResults.setText(R_FONT_PREFIX + s + FONT_SUFFIX);
                    sbf.showMsg(s, MsgType.WARN);
                }

                if (isCancelled()) {
                    sbf.updateTitle("Search cancelled - " + result);
                } else {
                    String msg = "--- Search complete ---";
                    qMsgsToAppend.add(addLineEnd(msg));
                    startThread(msgCallable);
                    sbf.updateTitleAndMsg("Search complete - " + result, getMsgTypeForOpr());
                }
                status = Status.DONE;
            } catch (FileNotFoundException e) {
                searchFailed(e);
                sbf.fileNotFoundAction();
                logger.error(e);
            } catch (Exception e) {
                searchFailed(e);
                logger.error(e);
            } finally {
                sbf.enableControls();
            }

            finishAction();
            return true;
        }

        private void searchFailed(Exception e) {
            String msg = "ERROR: " + e.getMessage();
            sbf.logger.error(e.getMessage());
            sbf.tpResults.setText(R_FONT_PREFIX + msg + FONT_SUFFIX);
            sbf.updateTitleAndMsg("Unable to search file: " + getFilePath(), MsgType.ERROR);
            status = Status.DONE;
        }
    }
}
