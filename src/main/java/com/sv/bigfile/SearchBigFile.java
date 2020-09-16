package com.sv.bigfile;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.text.BadLocationException;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.text.NumberFormat;
import java.util.*;
import java.util.concurrent.*;

public class SearchBigFile extends AppFrame {

    enum Status {
        NOT_STARTED, READING, DONE, CANCELLED
    }

    private JTextField txtFilePath;
    private JTextField txtSearch;
    private JEditorPane tpResults;
    private HTMLDocument htmlDoc;
    private HTMLEditorKit kit;

    private MyLogger logger;
    private DefaultConfigs configs;

    private String searchStr, searchStrReplace;
    private String recentFilesStr, recentSearchesStr;
    private final String REPLACER_PREFIX = "<font style=\"background-color:yellow\">";
    private final String REPLACER_SUFFIX = "</font>";
    private final String HTML_LINE_END = "<br>";

    private JButton btnSearch, btnLastN, btnCancel, btnExit;
    private final String TITLE = "Search File";
    private static final int RECENT_LIMIT = 20;
    private static boolean showWarning = false;
    private static final int TIME_LIMIT_FOR_WARN_IN_SEC = 20;
    private static final int OCCUR_LIMIT_FOR_WARN_IN_SEC = 200;
    private static final boolean CB_LIST_WIDER = true, CB_LIST_ABOVE = false;

    private static long startTime = System.currentTimeMillis();
    private static Status status = Status.NOT_STARTED;

    private JCheckBox jcbMatchCase, jcbWholeWord;
    private JComboBox<String> cbFiles, cbSearches;
    private JComboBox<Integer> cbLastN;
    private static Queue<String> qMsgsToAppend;
    private final int APPEND_MSG_CHUNK = 100;
    private static AppendMsgCallable msgCallable;
    private static final ExecutorService threadPool = Executors.newFixedThreadPool(8);

    public static void main(String[] args) {
        new SearchBigFile().initComponents();
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

        Border emptyBorder = new EmptyBorder(new Insets(5, 5, 5, 5));

        final int TXT_COLS = 15;
        tpResults = new JEditorPane();
        tpResults.setEditable(false);
        tpResults.setContentType("text/html");
        htmlDoc = new HTMLDocument();
        tpResults.setDocument(htmlDoc);
        kit = new HTMLEditorKit();
        txtFilePath = new JTextField(configs.getConfig(DefaultConfigs.Config.FILEPATH));
        AppLabel lblFilePath = new AppLabel("File", txtFilePath, 'F');
        txtFilePath.setColumns(TXT_COLS);
        cbFiles = new JComboBox<>(getFiles());
        cbFiles.addPopupMenuListener(new BoundsPopupMenuListener(CB_LIST_WIDER, CB_LIST_ABOVE));
        JComboToolTipRenderer cbFilesRenderer = new JComboToolTipRenderer();
        cbFiles.setRenderer(cbFilesRenderer);
        cbFiles.setPrototypeDisplayValue("Recent Files");
        addCBFilesAL();
        AppLabel lblRFiles = new AppLabel("Recent", cbFiles, 'R');
        lblRFiles.setToolTipText("Recent files list");
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
        btnLastN = new AppButton("Read", 'R');
        btnLastN.setToolTipText("Read last N lines and highlight.");
        btnLastN.addActionListener(evt -> readLastNLines());
        cbSearches = new JComboBox<>(getSearches());
        cbSearches.addPopupMenuListener(new BoundsPopupMenuListener(CB_LIST_WIDER, CB_LIST_ABOVE));
        JComboToolTipRenderer cbSearchRenderer = new JComboToolTipRenderer();
        cbSearches.setRenderer(cbSearchRenderer);
        cbSearches.setPrototypeDisplayValue("Pattern");
        addCBSearchAL();
        AppLabel lblRSearches = new AppLabel("Recent", cbSearches, 'e');
        lblRSearches.setToolTipText("Recent searches list");
        btnCancel = new AppButton("Cancel", 'C');
        btnCancel.addActionListener(evt -> cancelSearch());

        btnExit = new AppExitButton();

        filePanel.setLayout(new FlowLayout());
        filePanel.add(lblFilePath);
        filePanel.add(txtFilePath);
        filePanel.add(lblRFiles);
        filePanel.add(cbFiles);
        filePanel.add(jcbMatchCase);
        filePanel.add(jcbWholeWord);
        TitledBorder titledFP = new TitledBorder("File to search");
        filePanel.setBorder(titledFP);

        searchPanel.setLayout(new FlowLayout());
        searchPanel.add(lblSearch);
        searchPanel.add(txtSearch);
        searchPanel.add(lblRSearches);
        searchPanel.add(cbSearches);
        searchPanel.add(btnSearch);
        searchPanel.add(lblLastN);
        searchPanel.add(cbLastN);
        searchPanel.add(btnLastN);
        searchPanel.add(btnCancel);
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

    private Integer[] getLastNOptions() {
        return new Integer[]{500, 1000, 2000, 3000, 4000, 5000};
    }

    private void resetForNewSearch() {
        resetShowWarning();
        emptyResults();
        updateRecentSearchVals();
        qMsgsToAppend.clear();
        setSearchStrings();
        disableControls();
        startTime = System.currentTimeMillis();
    }

    private void readLastNLines() {
        logger.log("Loading last 500 lines from: " + txtFilePath.getText());
        resetForNewSearch();
        int readLines = 0;
        StringBuilder sb = new StringBuilder();
        File file = new File(txtFilePath.getText());
        final int LIMIT = Integer.parseInt(cbLastN.getSelectedItem().toString());
        try (RandomAccessFile randomAccessFile = new RandomAccessFile(file, "r")) {
            long fileLength = file.length() - 1;
            // Set the pointer at the last of the file
            randomAccessFile.seek(fileLength);

            for (long pointer = fileLength; pointer >= 0; pointer--) {
                randomAccessFile.seek(pointer);
                char c;
                // read from the last, one char at the time
                c = (char) randomAccessFile.read();
                // break when end of the line
                if (c == '\n') {
                    readLines++;
                    if (readLines == LIMIT) {
                        break;
                    }
                }
                sb.append(c);
                fileLength = fileLength - pointer;
            }
            sb.reverse();
            appendResult(sb.toString());
        } catch (IOException e) {
            logger.error(e);
        }
        int len = lowerCaseSplit(sb.toString(), searchStr);
        updateTitle(
                getSearchResult(
                        txtFilePath.getText(),
                        TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - startTime),
                        readLines,
                        len > 0 ? len - 1 : 0)
        );
        enableControls();
    }

    private int lowerCaseSplit(String line, String pattern) {
        return line.toLowerCase().split(pattern.toLowerCase()).length;
    }

    private void removeCBSearchAL() {
        Arrays.stream(cbSearches.getActionListeners()).forEach(a -> cbSearches.removeActionListener(a));
    }

    private void addCBSearchAL() {
        cbSearches.addActionListener(e -> txtSearch.setText(cbSearches.getSelectedItem().toString()));
    }

    private void removeCBFilesAL() {
        Arrays.stream(cbFiles.getActionListeners()).forEach(a -> cbFiles.removeActionListener(a));
    }

    private void addCBFilesAL() {
        cbFiles.addActionListener(e -> txtFilePath.setText(cbFiles.getSelectedItem().toString()));
    }

    private String[] getFiles() {
        return configs.getConfig(DefaultConfigs.Config.RECENT_FILES).split(";");
    }

    private String[] getSearches() {
        return configs.getConfig(DefaultConfigs.Config.RECENT_SEARCHES).split(";");
    }

    private void resetShowWarning() {
        showWarning = false;
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
        if (!Utils.hasValue(txtFilePath.getText())) {
            updateTitle("REQUIRED: file to search");
            result = false;
        }
        if (result && !Utils.hasValue(txtSearch.getText())) {
            updateTitle("REQUIRED: text to search");
            result = false;
        }
        return result;
    }

    private void updateRecentSearchVals() {
        recentFilesStr = checkItems(txtFilePath.getText(), recentFilesStr);
        recentSearchesStr = checkItems(txtSearch.getText(), recentSearchesStr);
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
        searchStr = txtSearch.getText();
        searchStrReplace = REPLACER_PREFIX + searchStr + REPLACER_SUFFIX;
    }

    class AppendData extends SwingWorker<Integer, String> {
        String data;

        AppendData(String data) {
            this.data = data;
        }

        @Override
        public Integer doInBackground() {
            try {
                kit.insertHTML(htmlDoc, htmlDoc.getLength(), data, 0, 0, null);
            } catch (BadLocationException | IOException e) {
                logger.error("Unable to append data: " + data);
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

        public Integer process() {
            long lineNum = stats.getLineNum();
            StringBuilder sb = new StringBuilder();

            if (stats.isMatch()) {
                int occr = lowerCaseSplit(stats.getLine(), stats.getSearchPattern());
                stats.setOccurrences(stats.getOccurrences() + occr - 1);
                sb.append("<b>").append(lineNum).append("  </b>").append(stats.getLine()).append(System.lineSeparator());
                synchronized (SearchBigFile.class) {
                    qMsgsToAppend.add(sb.toString());
                }
            }
            stats.setLineNum(lineNum + 1);

            if (lineNum % LINES_TO_INFORM == 0) {
                logger.log("Lines searched so far: " + NumberFormat.getNumberInstance().format(lineNum));
            }

            if (!showWarning && stats.getOccurrences() > OCCUR_LIMIT_FOR_WARN_IN_SEC) {
                showWarning = true;
            }

            return 1;
        }
    }

    public void appendResultNoFormat(String data) {
        synchronized (SearchBigFile.class) {
            //data = Utils.escape (data);
            data = data.replaceAll("\r?\n", HTML_LINE_END);
            // Needs to be sync else line numbers and data will be jumbled
            try {
                kit.insertHTML(htmlDoc, htmlDoc.getLength(), data.replaceAll(" ", "&nbsp;"), 0, 0, null);
            } catch (BadLocationException | IOException e) {
                logger.error("Unable to append data: " + data);
            }
        }
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
                    }
                    sbf.updateTitle(msg);
                    Utils.sleep(1000, sbf.logger);
                }
            } while (status == Status.READING);
            return true;
        }
    }

    private String getWarning() {
        return " - Either search taking long or too many results !!  Cancel and try to narrow";
    }

    class AppendMsgCallable implements Callable<Boolean> {

        SearchBigFile sbf;

        public AppendMsgCallable(SearchBigFile sbf) {
            this.sbf = sbf;
        }

        @Override
        public Boolean call() {
            StringBuilder sb = new StringBuilder();

            while (!qMsgsToAppend.isEmpty()) {
                String m;
                synchronized (SearchBigFile.class) {
                    m = qMsgsToAppend.poll();
                }
                if (Utils.hasValue(m)) {
                    sb.append(m);
                }
            }
            if (sb.length() > 0) {
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
            String searchPattern = sbf.txtSearch.getText();

            if (!sbf.isMatchCase()) {
                searchPattern = searchPattern.toLowerCase();
            }
            if (sbf.isWholeWord()) {
                searchPattern = ".*\\b" + searchPattern + "\\b.*";
            }

            String path = sbf.txtFilePath.getText();

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

                    //searchData.doInBackground();
                    searchData.process();
                    if (qMsgsToAppend.size() > APPEND_MSG_CHUNK) {
                        threadPool.submit(msgCallable);
                    }
                    if (status == Status.CANCELLED) {
                        sbf.appendResultNoFormat("---------------------Search cancelled----------------------------" + System.lineSeparator());
                        break;
                    }
                }

                while (qMsgsToAppend.size() > 0) {
                    Utils.sleep(200, sbf.logger);
                    threadPool.submit(msgCallable);
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
}

