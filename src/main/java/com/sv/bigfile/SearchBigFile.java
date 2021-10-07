package com.sv.bigfile;

import com.sv.bigfile.helpers.*;
import com.sv.bigfile.html.WrapHtmlKit;
import com.sv.core.Constants;
import com.sv.core.Utils;
import com.sv.core.config.DefaultConfigs;
import com.sv.core.logger.MyLogger;
import com.sv.core.logger.MyLogger.MsgType;
import com.sv.runcmd.RunCommand;
import com.sv.runcmd.RunCommandUI;
import com.sv.swingui.SwingUtils;
import com.sv.swingui.UIConstants;
import com.sv.swingui.component.*;
import com.sv.swingui.component.table.AppTable;
import com.sv.swingui.component.table.CellRendererCenterAlign;
import com.sv.swingui.component.table.CellRendererLeftAlign;

import javax.print.attribute.ResolutionSyntax;
import javax.swing.*;
import javax.swing.border.Border;
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
        RecentFiles, FilePath, SearchString, RecentSearches, LastN, FontSize, FontIndex,
        ColorIndex, ChangeFontAuto, ChangeHighlightAuto, ApplyColorToApp, AutoLock,
        MatchCase, WholeWord, FixedWidth, DebugEnabled
    }

    enum Status {
        NOT_STARTED, READING, DONE, CANCELLED
    }

    enum FONT_OPR {
        INCREASE, DECREASE, RESET, NONE
    }

    enum FILE_OPR {
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
    private JTabbedPane tabbedPane;
    private JMenu menuRFiles, menuRSearches, menuSettings, menuFonts;
    private AppToolBar jtbFile, jtbSearch, jtbControls;
    private JPanel msgPanel;
    private JLabel lblMsg;
    private JButton btnShowAll, btnMemory, btnListRS, btnListRF;
    private JButton btnPlusFont, btnMinusFont, btnResetFont, btnLock;
    private JButton btnFileOpen, btnGoTop, btnGoBottom, btnNextOccr, btnPreOccr, btnFind, btnHelp;
    private JButton btnSearch, btnLastN, btnCancel;
    private AppTextField txtFilePath, txtSearch;
    private JTextPane tpResults, tpHelp, tpContactMe;
    private final SimpleAttributeSet highlighted = new SimpleAttributeSet();
    private Highlighter highlighter;
    private JScrollPane jspResults, jspHelp, jspContactMe;
    private HTMLDocument htmlDoc;
    private HTMLEditorKit kit;
    private JCheckBox jcbMatchCase, jcbWholeWord;
    private JCheckBoxMenuItem jcbmiFonts, jcbmiHighlights, jcbmiApplyToApp, jcbmiAutoLock;
    private JComboBox<Integer> cbLastN;

    private static FILE_OPR operation;

    private static final Color ORIG_COLOR = UIConstants.ORIG_COLOR;
    private static final int TXT_HEIGHT = 28;
    private static ColorsNFonts[] appColors;
    private static boolean ignoreBlackAndWhite = true;
    private static boolean showWarning = false;
    private static boolean fixedWidth = false;
    private static long insertCounter = 0;
    private static long readCounter = 0;
    private static long startTime = System.currentTimeMillis();
    private static String timeTaken;
    private static long lineNums;
    private static int fontIdx = 0;
    private static int colorIdx = 0;

    private final String SEPARATOR = "~";
    private final String TXT_F_MAP_KEY = "Action.FileMenuItem";
    private final String TXT_S_MAP_KEY = "Action.SearchMenuItem";
    private final int EXCERPT_LIMIT = 80;
    private final int MAX_READ_CHAR_LIMIT = 5000;
    private static int maxReadCharTimes = 0;
    private boolean debugAllowed;
    private String searchStr, recentFilesStr, recentSearchesStr;
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
        debugAllowed = getBooleanCfg(Configs.DebugEnabled);
        logger.setDebug(debugAllowed);
        printConfigs();

        super.setLogger(logger);

        appColors = SwingUtils.getFilteredCnF(ignoreBlackAndWhite);
        qMsgsToAppend = new LinkedBlockingQueue<>();
        idxMsgsToAppend = new ConcurrentHashMap<>();
        lineOffsets = new HashMap<>();
        colorIdx = getIntCfg(Configs.ColorIndex);
        fontIdx = getIntCfg(Configs.FontIndex);
        setColorFromIdx();
        recentFilesStr = checkSep(getCfg(Configs.RecentFiles));
        recentSearchesStr = checkSep(getCfg(Configs.RecentSearches));
        fixedWidth = getBooleanCfg(Configs.FixedWidth);
        msgCallable = new AppendMsgCallable(this);

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
        AppLabel lblFilePath = new AppLabel(uin.name, txtFilePath, uin.mnemonic);
        uin = UIName.BTN_FILE;
        btnFileOpen = new AppButton(uin.name, uin.mnemonic, uin.tip);// no need as image //, "", true);
        btnFileOpen.addActionListener(e -> openFile());

        uin = UIName.BTN_LISTRF;
        btnListRF = new AppButton(uin.name, uin.mnemonic, uin.tip);
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

        uin = UIName.LBL_RFILES;
        mbRFiles = new JMenuBar();
        menuRFiles = new JMenu(uin.name);
        menuRFiles.setMnemonic(uin.mnemonic);
        menuRFiles.setToolTipText(uin.tip);
        mbRFiles.add(menuRFiles);
        updateRecentMenu(menuRFiles, getFiles(), txtFilePath, TXT_F_MAP_KEY);

        jtbFile = new AppToolBar();
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
        txtSearch.setToolTipText("Ctrl+F to come here and enter to perform Search");
        txtSearch.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                super.keyPressed(e);
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    searchFile();
                }
            }
        });
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
        menuRSearches = new JMenu(uin.name);
        menuRSearches.setMnemonic(uin.mnemonic);
        menuRSearches.setToolTipText(uin.tip);
        mbRSearches.add(menuRSearches);
        updateRecentMenu(menuRSearches, getSearches(), txtSearch, TXT_S_MAP_KEY);

        jtbSearch = new AppToolBar();
        jtbSearch.add(txtSearch);
        jtbSearch.add(btnUC);
        jtbSearch.add(btnLC);
        jtbSearch.add(btnTC);
        jtbSearch.add(btnIC);
        jtbSearch.add(btnListRS);
        jtbSearch.add(mbRSearches);

        uin = UIName.MNU_SETTINGS;
        mbSettings = new JMenuBar();
        menuSettings = new JMenu(uin.name);
        menuSettings.setMnemonic(uin.mnemonic);
        menuSettings.setToolTipText(uin.tip + SHORTCUT + uin.mnemonic);
        mbSettings.add(menuSettings);
        // populating settings menu at end so font can be applied to lblMsg

        uin = UIName.BTN_CANCEL;
        btnCancel = new AppButton(uin.name, uin.mnemonic, uin.tip, "./icons/cancel-icon.png", true);
        btnCancel.setDisabledIcon(new ImageIcon("./icons/cancel-icon-disabled.png"));
        btnCancel.addActionListener(evt -> cancelSearch());

        searchPanel.setLayout(new FlowLayout());
        searchPanel.add(lblSearch);
        searchPanel.add(jtbSearch);
        searchPanel.add(btnSearch);
        searchPanel.add(lblLastN);
        searchPanel.add(cbLastN);
        searchPanel.add(btnLastN);
        searchPanel.add(btnCancel);
        searchPanel.setBorder(searchPanelBorder);

        jtbControls = new AppToolBar();
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
        controlPanel.add(jtbControls);
        jtbControls.add(mbSettings);
        jtbControls.add(btnPlusFont);
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
        lblMsg = new JLabel(getInitialMsg());
        lblMsg.setHorizontalAlignment(SwingConstants.CENTER);
        lblMsg.setFont(getNewFont(lblMsg.getFont(), Font.PLAIN, 12));
        uin = UIName.BTN_SHOWALL;
        btnShowAll = new AppButton(uin.name, uin.mnemonic, uin.tip);
        btnShowAll.addActionListener(e -> showAllOccr());
        uin = UIName.BTN_MEMORY;
        btnMemory = new AppButton(uin.name, uin.mnemonic, uin.tip);
        btnMemory.addActionListener(e -> freeMemory());
        msgPanel.add(lblMsg, BorderLayout.CENTER);
        AppToolBar msgButtons = new AppToolBar();
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

        tpResults = new JTextPane() {
            @Override
            public Color getSelectionColor() {
                return selectionColor;
            }

            @Override
            public Color getSelectedTextColor() {
                return selectionTextColor;
            }
        };
        highlighter = tpResults.getHighlighter();
        tpResults.setEditable(false);
        tpResults.setContentType("text/html");
        tpResults.setFont(getFontForEditor(getCfg(Configs.FontSize)));
        tpResults.setForeground(Color.black);
        tpResults.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        htmlDoc = new HTMLDocument();
        tpResults.setDocument(htmlDoc);
        tpResults.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                if (!Utils.hasValue(tpResults.getSelectedText())) {
                    highlightLastSelectedItem();
                }
            }
        });
        tpResults.addFocusListener(new FocusAdapter() {

            @Override
            public void focusLost(FocusEvent e) {
                highlightLastSelectedItem();
            }
        });
        //kit = new HTMLEditorKit();
        kit = new WrapHtmlKit();
        jspResults = new JScrollPane(tpResults);
        jspHelp = new JScrollPane(tpHelp);
        jspContactMe = new JScrollPane(tpContactMe);
        jspResults.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        jspResults.setBorder(EMPTY_BORDER);

        parentContainer.add(topPanel, BorderLayout.NORTH);
        tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Result", null, jspResults, "Displays Search/Read results");
        tabbedPane.addTab("Help", null, jspHelp, "Displays application help");
        tabbedPane.addTab("Contact Me", null, jspContactMe, "Displays my information");

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

        prepareSettingsMenu();

        setFontSize(FONT_OPR.NONE);
        setControlsToEnable();
        setupHelp();
        setupContactMe();
        setDragNDrop();
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
        new Timer().schedule(new FontChangerTask(this), SEC_1, MIN_10);
        new Timer().schedule(new HelpColorChangerTask(this), SEC_1, HELP_COLOR_CHANGE_TIME);
        new Timer().schedule(new MemoryTrackTask(this), SEC_1, SEC_1 * 10);

        if (configs.getBooleanConfig(Configs.AutoLock.name())) {
            applyWindowActiveCheck(new WindowChecks[]{WindowChecks.WINDOW_ACTIVE, WindowChecks.CLIPBOARD, WindowChecks.AUTO_LOCK});
        } else {
            applyWindowActiveCheck(new WindowChecks[]{WindowChecks.WINDOW_ACTIVE, WindowChecks.CLIPBOARD});
        }
    }

    public void trackMemory() {
        long total = Runtime.getRuntime().totalMemory();
        long free = Runtime.getRuntime().freeMemory();
        String mem = Utils.addBraces(Utils.getSizeString(total - free, false, false)
                + F_SLASH + Utils.getSizeString(total, false, false));
        btnMemory.setText(mem);
        btnMemory.setToolTipText(getMemoryDetails()+". Click to free memory. Shortcut: Alt+" + UIName.BTN_MEMORY.mnemonic);
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

    private void setDragNDrop() {
        JComponent[] addBindingsTo = {tpContactMe, tpHelp, tpResults, lblMsg, btnShowAll, btnMemory, msgPanel};
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
        new Timer().schedule(new StartClipboardTask(this), SEC_1);
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

    private void addBindings() {

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
        keyActionDetails.add(new KeyActionDetails(KeyEvent.VK_F, InputEvent.CTRL_DOWN_MASK, actionTxtSearch));
        keyActionDetails.add(new KeyActionDetails(KeyEvent.VK_F3, InputEvent.SHIFT_DOWN_MASK, actionShiftF3));
        keyActionDetails.add(new KeyActionDetails(KeyEvent.VK_F3, 0, actionF3));
        keyActionDetails.add(new KeyActionDetails(KeyEvent.VK_HOME, InputEvent.CTRL_DOWN_MASK, actionCtrlHome));
        keyActionDetails.add(new KeyActionDetails(KeyEvent.VK_END, InputEvent.CTRL_DOWN_MASK, actionCtrlEnd));

        final JComponent[] addBindingsTo = {tpResults, tpHelp, lblMsg, btnShowAll, btnMemory, msgPanel};
        keyActionDetails.forEach(ka -> {
            KeyStroke keyS = KeyStroke.getKeyStroke(ka.getKeyEvent(), ka.getInputEvent());
            Arrays.stream(addBindingsTo).forEach(j ->
                    j.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(keyS, ka.getAction()));
        });
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
        jcbmiFonts = new JCheckBoxMenuItem("Change fonts auto", null, getBooleanCfg(Configs.ChangeFontAuto));
        jcbmiFonts.setMnemonic('F');
        jcbmiFonts.setToolTipText("Changes font for information bar every 10 minutes");
        jcbmiHighlights = new JCheckBoxMenuItem("Change highlight auto", null, getBooleanCfg(Configs.ChangeHighlightAuto));
        jcbmiHighlights.setMnemonic('H');
        jcbmiHighlights.setToolTipText("Changes colors of highlighted text, selected-text and selected background every 10 minutes");
        jcbmiApplyToApp = new JCheckBoxMenuItem("Apply color to App", null, getBooleanCfg(Configs.ApplyColorToApp));
        jcbmiApplyToApp.setMnemonic('y');
        jcbmiApplyToApp.setToolTipText("Changes colors of complete application whenever highlight color changes");
        jcbmiAutoLock = new JCheckBoxMenuItem("Auto Lock", null, configs.getBooleanConfig(Configs.AutoLock.name()));
        jcbmiAutoLock.setMnemonic('L');
        jcbmiAutoLock.setToolTipText("Auto Lock App if idle for 10 min - change need restart");

        menuSettings.add(jcbmiFonts);
        menuFonts = SwingUtils.getFontsMenu("Fonts", 'o', "Fonts",
                Utils.addBraces(getFontFromEnum()), this, logger);
        menuSettings.add(menuFonts);
        menuSettings.addSeparator();
        menuSettings.add(jcbmiHighlights);
        menuSettings.add(SwingUtils.getColorsMenu("Highlights", 'g', "Highlight colors",
                true, true, true, false, ignoreBlackAndWhite, this, logger));
        menuSettings.addSeparator();
        menuSettings.add(jcbmiApplyToApp);
        menuSettings.addSeparator();
        JMenuItem jmiChangePwd = new JMenuItem("Change Password", 'c');
        jmiChangePwd.addActionListener(e -> showChangePwdScreen(highlightColor));
        JMenuItem jmiLock = new JMenuItem("Lock screen", 'o');
        jmiLock.addActionListener(e -> showLockScreen(highlightColor));
        menuSettings.add(jmiChangePwd);
        menuSettings.add(jmiLock);
        menuSettings.add(jcbmiAutoLock);

        // setting font from config
        setMsgFont(getNewFont(lblMsg.getFont(), getFontFromEnum()));
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

        String msg = "Search/read and use < or >";
        lblNoRow = new JLabel(msg);
        lblNoRow.setToolTipText(msg);
        lblNoRow.setSize(lblNoRow.getPreferredSize());
        tblAllOccr.add(lblNoRow);
        tblAllOccr.setFillsViewportHeight(true);

        return tblAllOccr;
    }

    private void highlightSearch() {
        highlighter.removeAllHighlights();
        for (int idx : lineOffsets.keySet()) {
            OffsetInfo info = lineOffsets.get(idx);
            highLightInResult(info.getSIdx(), info.getEIdx());
        }
    }

    private void highLightInResult(int s, int e) {
        tpResults.getStyledDocument().setCharacterAttributes(s, e - s, highlighted, false);
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
                menuSettings, btnShowAll
        };
        setComponentToEnable(components);
        setComponentContrastToEnable(new Component[]{btnCancel});
        enableControls();
    }

    private void printConfigs() {
        log("Debug enabled " + Utils.addBraces(logger.isDebug()));
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
            StyleConstants.setForeground(highlighted, highlightTextColor);
            StyleConstants.setBackground(highlighted, highlightColor);
            highlightColorStr = SwingUtils.htmlBGColor(highlightColor);
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

        TitledBorder[] toTitleColor = {filePanelBorder, searchPanelBorder, controlPanelBorder};
        Arrays.stream(toTitleColor).forEach(t -> t.setTitleColor(highlightTextColor));

        filePanel.setBorder(filePanelBorder);
        searchPanel.setBorder(searchPanelBorder);
        controlPanel.setBorder(controlPanelBorder);

        lblMsg.setForeground(selectionColor);

        JComponent[] toSetBorder = {msgPanel, txtFilePath, txtSearch, cbLastN, mbRFiles, mbRSearches, mbSettings};
        Arrays.stream(toSetBorder).forEach(c -> c.setBorder(SwingUtils.createLineBorder(selectionColor)));

        //TODO: search as type

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

        // TODO: color active tab
        /*int tabsCount = tabbedPane.getTabCount();
        for (int i = 0; i < tabsCount; i++) {
            tabbedPane.setBackgroundAt(i, cl);
            tabbedPane.setForegroundAt(i, highlightTextColor);
        }*/
        tabbedPane.setBackground(cl);
        tabbedPane.setForeground(highlightTextColor);
//        tabbedPane.setBackgroundAt(tabbedPane.getSelectedIndex(), selectionColor);
//        tabbedPane.setForegroundAt(tabbedPane.getSelectedIndex(), selectionTextColor);
        Arrays.stream(inputPanel.getComponents()).forEach(c -> SwingUtils.setComponentColor((JComponent) c, cl, null));

        // memory info bar
        JComponent[] ca = {tblAllOccr.getTableHeader(), inputPanel, jtbFile, jtbSearch, jtbControls};
        SwingUtils.setComponentColor(ca, cl, null);

        setBkColors(bkColorComponents);
    }

    private void updateRecentMenu(JMenu m, String[] arr, JTextField txtF, String mapKey) {
        m.removeAll();

        int i = 'a';
        for (String a : arr) {
            if (Utils.hasValue(a)) {
                char ch = (char) i;
                JMenuItem mi = new JMenuItem(ch + SP_DASH_SP + a);
                mi.addActionListener(e -> txtF.setText(a));
                if (i <= 'z') {
                    mi.setMnemonic(i++);
                    addActionOnMenu(new RecentMenuAction(txtF, a), mi, ch, mapKey + ch);
                }
                m.add(mi);
            }
        }
    }

    private void addActionOnMenu(AbstractAction action, JMenuItem mi, char keycode, String mapKey) {
        InputMap im = mi.getInputMap();
        im.put(KeyStroke.getKeyStroke(keycode, 0), mapKey);
        ActionMap am = mi.getActionMap();
        am.put(mapKey, action);
    }

    private void showHelp() {
        selectTab(true);
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

    private void exportResults() {
        // Will get get text and NOT html document which will be easy to process
        String resultsText = tpResults.getText();
        String resultsTextNoNewLine = resultsText.replaceAll("([\\r\\n])", "");

        if (Utils.hasValue(resultsText) &&
                !resultsTextNoNewLine.equalsIgnoreCase(AppConstants.EMPTY_RESULT_TEXT)) {
            if (searchUtils.exportResults(resultsText)) {
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

    private void findWordInResult() {
        operation = FILE_OPR.FIND;
        if (isValidate()) {
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
        return "This bar turns 'Orange' to show warnings and 'Red' to show error/force-stop. " +
                "Time/occurrences limit for warning [" + WARN_LIMIT_SEC
                + "sec/" + WARN_LIMIT_OCCR
                + "] and for error [" + ERROR_LIMIT_SEC
                + "sec/" + ERROR_LIMIT_OCCR + "]";

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

    private Font getFontForEditor(String sizeStr) {
        Font retVal = SwingUtils.getPlainCalibriFont(Utils.hasValue(sizeStr) ? Integer.parseInt(sizeStr) : PREFERRED_FONT_SIZE);
        logger.info("Returning " + getFontDetail(retVal));
        return retVal;
    }

    private String getFontDetail(Font f) {
        return Utils.addBraces(String.format("Font: %s/%s/%s", f.getName(), (f.isBold() ? "bold" : "plain"), f.getSize()));
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
        log("Returning font as " + name + ", style " + (style == Font.BOLD ? "bold" : "plain") + ", of size " + size);
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

    private void resetForNewSearch() {
        debug("reset for new search");
        hideHelp();
        printMemoryDetails();
        insertCounter = 0;
        readCounter = 0;
        maxReadCharTimes = 0;
        disableControls();
        resetShowWarning();
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
        debug(getMemoryDetails());
    }

    private String getMemoryDetails() {
        long total = Runtime.getRuntime().totalMemory();
        long free = Runtime.getRuntime().freeMemory();
        return String.format("Memory - Total: %s, Free: %s, Occupied: %s",
                Utils.getSizeString(total),
                Utils.getSizeString(free),
                Utils.getSizeString(total - free)
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

    private void setFileToSearch(String s) {
        txtFilePath.setText(s);
        showMsgAsInfo("File set as [" + s + "]");
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
            showMsg("Search cancelled.", MsgType.ERROR);
        }
        if (isReading()) {
            logger.warn("Search cancelled by user.");
            status = Status.CANCELLED;
        }
    }

    private void searchFile() {
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

        if (csv.split(SEPARATOR).length > RECENT_LIMIT) {
            csv = csv.substring(0, csv.lastIndexOf(SEPARATOR) + SEPARATOR.length());
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

    public String getFontIndex() {
        return fontIdx + "";
    }

    public String getFixedWidth() {
        return fixedWidth + "";
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
        return debugAllowed + "";
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
            sb.append("Warning: Time [").append(timeTillNow).append("] > warning limit [").append(WARN_LIMIT_SEC).append("]. ");
        }
        if (occrTillNow > WARN_LIMIT_OCCR) {
            sb.append("Warning: Occurrences [").append(occrTillNow).append("] > warning limit [").append(WARN_LIMIT_OCCR).append("], try to narrow your search.");
        }
        StringBuilder sbErr = new StringBuilder();
        if (timeTillNow > ERROR_LIMIT_SEC) {
            sbErr.append("Error: Time [").append(timeTillNow).append("] > force stop limit [").append(ERROR_LIMIT_SEC).append("]. Cancelling search...");
        }
        if (occrTillNow > ERROR_LIMIT_OCCR) {
            sbErr.append("Error: Occurrences [").append(occrTillNow).append("] > force stop limit [").append(ERROR_LIMIT_OCCR).append("], try to narrow your search. Cancelling search...");
        }

        if (Utils.hasValue(sbErr.toString())) {
            sb = sbErr;
            debug("Problem found - " + sb.toString());
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
        logger.info("insertCounter [" + insertCounter
                + "], readCounter [" + readCounter
                + "], qMsgsToAppend size [" + qMsgsToAppend.size()
                + "], idxMsgsToAppend size [" + idxMsgsToAppend.size()
                + "], lineOffsets size [" + lineOffsets.size()
                + "]");
    }

    private String getSearchResult(String path, String seconds, long lineNum, long occurrences) {
        String result =
                String.format("File size %s, " +
                                "time taken %s, lines read [%s]" +
                                (isSearchStrEmpty() ? "" : ", occurrences [%s]"),
                        Utils.getSizeString(new File(path).length()),
                        seconds,
                        lineNum,
                        occurrences);

        logger.info(result);
        return result;
    }

    public void debug(String s) {
        logger.debug(s);
    }

    public void log(String s) {
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

    private void highlightLastSelectedItem() {
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
        hideHelp();
        tpResults.grabFocus();
        repaintLastItem();
        tpResults.select(sIdx, eIdx);
        highlightLastSelectedItem();
    }

    public void finishAction() {
        log("Performing finish action");
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
                log("Updating offsets.  Doc length " + Utils.getSizeString(htmlDocText.length()));
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

    public boolean isErrorState() {
        return timeTillNow > ERROR_LIMIT_SEC || occrTillNow > ERROR_LIMIT_OCCR;
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
            f = new Font(getNextFont(), f.getStyle(), f.getSize());
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
        String msg = getFontDetail(f);
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

    private void fileNotFoundAction() {
        updateTitleAndMsg("File not exists: " + getFilePath(), MsgType.ERROR);
        createYesNoDialog("Remove entry ?",
                "File not exists " + Utils.addBraces(getFilePath())
                        + ", do you want to remove it from Recent list ?",
                "removeFileFromRecent");
    }

    private String addLineNumAndEscAtStart(long lineNum, String str) {
        return BR + getLineNumStr(lineNum) + escString(str);
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

    /*   Inner classes    */
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
                    boolean maxReadCharLimitReached = sb.length() >= MAX_READ_CHAR_LIMIT;
                    if (maxReadCharLimitReached) {
                        maxReadCharTimes++;
                    }
                    boolean isNewLineChar = c == '\n';
                    // TODO: check how to avoid \n at start for READ opr only
                    if (isNewLineChar || maxReadCharLimitReached) {

                        if (!isNewLineChar) {
                            sb.append(c);
                        }

                        if (Utils.hasValue(sb.toString())) {
                            sb.reverse();
                        }

                        occr += calculateOccr(sb.toString(), searchPattern);
                        if (isNewLineChar) {
                            // to handle \n
                            processForRead(readLines, "", occr, false);
                        }
                        processForRead(readLines, sb.toString(), occr, isNewLineChar);

                        sb = new StringBuilder();
                        if (isNewLineChar) {
                            readLines++;
                        }
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
                if (maxReadCharTimes > 0) {
                    logger.info("read: max read char limit " + Utils.addBraces(MAX_READ_CHAR_LIMIT) + " reached "
                            + Utils.addBraces(maxReadCharTimes) + " times, processing...");
                }

                log("File read complete in " + Utils.getTimeDiffSecMilliStr(time));
                if (Utils.hasValue(sb.toString())) {
                    sb.reverse();
                }
                processForRead(readLines, sb.toString(), occr, true, true);
                readLines++;
            } catch (FileNotFoundException e) {
                catchForRead(e);
                hasError = true;
                sbf.fileNotFoundAction();
            } catch (IOException e) {
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
            processForRead(line, str, occr, appendLineNum, false);
        }

        private void processForRead(int line, String str, int occr, boolean appendLineNum, boolean bypass) {
            String strToAppend = "";
            if (appendLineNum) {
                strToAppend = addLineNumAndEscAtStart(line + 1, str);
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
            linesTillNow = line;
            if (!showWarning && occr > WARN_LIMIT_OCCR) {
                showWarning = true;
            }
        }
    }

    // To avoid async order of lines this cannot be worker
    class SearchData {

        private final int LINES_TO_INFORM = 5_00_000;
        private final SearchStats stats;

        SearchData(SearchStats stats) {
            this.stats = stats;
        }

        public void process() {
            long lineNum = stats.getLineNum();
            StringBuilder sb = new StringBuilder();

            boolean sof = stats.isSofFile();
            boolean eol = stats.isEofLine();
            if (stats.isMatch()) {
                int occr = calculateOccr(stats.getLine(), stats.getSearchPattern());
                if (occr > 0) {
                    if (sof) {
                        sb.append(addOnlyLineNumAndEsc(stats.getLineNum(), ""));
                        stats.setSofFile(false);
                    }
                    stats.setOccurrences(stats.getOccurrences() + occr);
                    sb.append(escString(stats.getLine()));
                    if (eol && !sof) {
                        qMsgsToAppend.add(addLineNumAndEscAtStart(stats.getLineNum(), ""));
                    }
                    //TODO: empty lines in read opr
                    qMsgsToAppend.add(sb.toString());
                }
            }
            if (sof || eol) {
                stats.setLineNum(lineNum + 1);
            }

            if (lineNum % LINES_TO_INFORM == 0) {
                logger.info("Lines searched so far: " + NumberFormat.getNumberInstance().format(lineNum));
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
                if (isReading()) {
                    timeElapse = Utils.getTimeDiffSec(startTime);
                    timeTillNow = timeElapse;
                    String msg = timeElapse + " sec, lines [" + sbf.linesTillNow + "] ";
                    if ((showWarning || isWarningState())) {
                        msg += sbf.getProblemMsg();
                        sbf.debug("Invoking warning indicator.");
                        SwingUtilities.invokeLater(new StartWarnIndicator(sbf));
                    }
                    if (isErrorState()) {
                        sbf.logger.warn("Stopping forcefully.");
                        cancelSearch();
                    }
                    if (isReading()) {
                        sbf.updateTitle(msg);
                    }
                    logger.debug("Timer callable sleeping now for a second");
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
            final int BUFFER_SIZE = 200 * 1024;
            String searchPattern = sbf.processPattern();

            String path = sbf.getFilePath();

            try (InputStream stream = new FileInputStream(path);
                 BufferedReader br = new BufferedReader(new InputStreamReader(stream), BUFFER_SIZE)
            ) {
                long lineNum = 1, occurrences = 0, time = System.currentTimeMillis();
                SearchStats stats = new SearchStats(lineNum, occurrences, null, searchPattern);
                SearchData searchData = new SearchData(stats);

                String line;
                char c;
                int i;
                StringBuilder sb = new StringBuilder();
                boolean appendLine = false;
                while ((i = br.read()) != -1) {

                    c = (char) i;
                    boolean isNewLineChar = c == '\n';
                    if (!isNewLineChar) {
                        sb.append(c);
                    }
                    boolean maxReadCharLimitReached = sb.length() >= MAX_READ_CHAR_LIMIT;
                    if (maxReadCharLimitReached) {
                        maxReadCharTimes++;
                    }

                    if (isNewLineChar || maxReadCharLimitReached) {
                        stats.setEofLine(appendLine);
                        line = sb.toString();
                        sb = new StringBuilder();
                        stats.setLine(line);
                        stats.setMatch(sbf.hasOccr(line, searchPattern));

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
                    if (!appendLine && !stats.isSofFile()) {
                        appendLine = isNewLineChar;
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
            } catch (IOException e) {
                searchFailed(e);
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
