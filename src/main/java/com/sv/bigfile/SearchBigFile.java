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
import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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
    private final String REPLACER_PREFIX = "<font style=\"background-color:yellow\">";
    private final String REPLACER_SUFFIX = "</font>";
    private final String HTML_LINE_END = "<br>";

    private JButton btnSearch, btnCancel, btnExit;
    private JButton btnFav1, btnFav2;
    private final String TITLE = "Search File";

    private static long startTime = System.currentTimeMillis();
    private static Status status = Status.NOT_STARTED;

    private JCheckBox jcbMatchCase, jcbWholeWord;
    private static final ExecutorService threadPool = Executors.newFixedThreadPool(3);

    public static void main(String[] args) {
        new SearchBigFile().initComponents();
    }

    /**
     * This method initializes the form.
     */
    private void initComponents() {
        logger = MyLogger.createLogger("search-big-file.log");

        configs = new DefaultConfigs(logger);

        Container parentContainer = getContentPane();
        parentContainer.setLayout(new BorderLayout());

        setIconImage(new ImageIcon("./search.png").getImage());

        JPanel searchPanel = new JPanel();
        JPanel filePanel = new JPanel();
        JPanel exitPanel = new JPanel();
        JPanel inputPanel = new JPanel();

        setTitle(TITLE);

        Border emptyBorder = new EmptyBorder(new Insets(5, 5, 5, 5));

        final int TXT_COLS = 25;
        tpResults = new JEditorPane();
        tpResults.setEditable(false);
        tpResults.setContentType("text/html");
        htmlDoc = new HTMLDocument();
        tpResults.setDocument(htmlDoc);
        kit = new HTMLEditorKit();
        txtFilePath = new JTextField(configs.getConfig(DefaultConfigs.Config.FILEPATH));
        txtFilePath.setColumns(TXT_COLS + 10);
        jcbMatchCase = new JCheckBox("match case",
                Boolean.parseBoolean(configs.getConfig(DefaultConfigs.Config.MATCH_CASE)));
        jcbWholeWord = new JCheckBox("whole word",
                Boolean.parseBoolean(configs.getConfig(DefaultConfigs.Config.WHOLE_WORD)));
        txtSearch = new JTextField(configs.getConfig(DefaultConfigs.Config.SEARCH));
        txtSearch.setColumns(TXT_COLS);
        btnSearch = new AppButton("Search", 'S');
        btnSearch.addActionListener(evt -> searchFile());
        btnCancel = new AppButton("Cancel",
                'C');
        btnCancel.addActionListener(evt -> cancelSearch());

        btnFav1 = new AppButton("Fav1", 'M');
        btnFav1.addActionListener(evt -> setFav1Path());
        btnFav2 = new AppButton("Fav2", 'E');
        btnFav2.addActionListener(evt -> setFav2Path());

        btnExit = new AppExitButton();

        filePanel.setLayout(new FlowLayout());
        filePanel.add(txtFilePath);
        filePanel.add(jcbMatchCase);
        filePanel.add(jcbWholeWord);
        filePanel.add(btnFav1);
        filePanel.add(btnFav2);
        TitledBorder titledFP = new TitledBorder("File to search");
        filePanel.setBorder(titledFP);

        searchPanel.setLayout(new FlowLayout());
        searchPanel.add(txtSearch);
        searchPanel.add(btnSearch);
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
        jspResults.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
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

    private void cancelSearch() {
        if (status == Status.READING) {
            logger.warn("Search cancelled by user.");
            status = Status.CANCELLED;
        }
    }

    private void setFav1Path() {
        txtFilePath.setText("c:\\sv\\fav1.log");
    }

    private void setFav2Path() {
        txtFilePath.setText("c:\\sv\\fav2.log");
    }

    private void searchFile() {
        disableControls();
        emptyResults();
        status = Status.READING;
        startTime = System.currentTimeMillis();
        logger.log(getSearchDetails());
        threadPool.submit(new SearchFileCallable(this));
        threadPool.submit(new TimerCallable(this));
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
        btnFav1.setEnabled(enable);
        btnFav2.setEnabled(enable);
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

    public void appendResultNoFormat(String data) {
        new AppendData(data).doInBackground();
    }

    public void appendResult(String data) {
        data = data.replaceAll(System.lineSeparator(), HTML_LINE_END);

        if (!isMatchCase()) {
            data = replaceWithSameCase(data);
        } else {
            data = data.replaceAll(searchStr, searchStrReplace);
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

    public void updateTitle(String info) {
        setTitle(Utils.hasValue(info) ? TITLE + " - " + info : TITLE);
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
                    sbf.updateTitle(TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - startTime) + " sec");
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        sbf.logger.error(e.getMessage());
                    }
                }
            } while (status == Status.READING);
            return true;
        }
    }

    static class SearchFileCallable implements Callable<Boolean> {

        private final SearchBigFile sbf;

        public SearchFileCallable(SearchBigFile sbf) {
            this.sbf = sbf;
        }

        @Override
        public Boolean call() {
            final int LINES_TO_FLUSH = 10000;
            final int LINES_TO_INFORM = 500000;
            final int BUFFER_SIZE = 5 * 1024;
            String searchStr = sbf.txtSearch.getText();

            if (!sbf.isMatchCase()) {
                searchStr = searchStr.toLowerCase();
            }
            if (sbf.isWholeWord()) {
                searchStr = ".*\\b" + searchStr + "\\b.*";
            }

            String path = sbf.txtFilePath.getText();

            /*try (InputStream stream = new FileInputStream(path);
                 Scanner sc = new Scanner(stream, "UTF-8")
            ) {*/
            try (InputStream stream = new FileInputStream(path);
                 BufferedReader br = new BufferedReader(new InputStreamReader(stream), BUFFER_SIZE)
            ) {
                long lineNum = 1, occurrences = 0;
                StringBuilder sb = new StringBuilder();
                sbf.setSearchStrings();

                /*
                    while (sc.hasNextLine()) {
                        String line = sc.nextLine();
                */

                String line;
                while ((line = br.readLine()) != null) {

                    if ((sbf.isWholeWord() && line.matches(searchStr))
                            || (!sbf.isMatchCase() && line.toLowerCase().contains(searchStr.toLowerCase()))
                            || (sbf.isMatchCase() && line.contains(searchStr))
                    ) {
                        occurrences++;
                        sb.append("<b>").append(lineNum).append("  </b>").append(line).append(System.lineSeparator());
                    }
                    lineNum++;
                    if (lineNum % LINES_TO_FLUSH == 0) {
                        sbf.appendResult(sb.toString());
                        sb = new StringBuilder();
                    }

                    if (lineNum % LINES_TO_INFORM == 0) {
                        sbf.logger.log("Lines searched so far: " + NumberFormat.getNumberInstance().format(lineNum));
                    }

                    if (status == Status.CANCELLED) {
                        sbf.appendResultNoFormat("---------------------Search cancelled----------------------------" + System.lineSeparator());
                        break;
                    }

                }

                long seconds = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - startTime);
                String result = String.format("Search completed. File size: %s, " +
                                "time taken: [%sseconds], lines read: [%s], occurrences: [%s]",
                        Utils.getFileSizeString(new File(path).length()),
                        seconds,
                        lineNum,
                        occurrences);
                if (occurrences == 0) {
                    sbf.appendResultNoFormat("No match found. ");
                } else {
                    sbf.appendResult(sb.toString());
                }
                if (status == Status.CANCELLED) {
                    sbf.updateTitle("Search cancelled -- " + result);
                } else {
                    sbf.appendResultNoFormat("---------------------Search complete----------------------------" + System.lineSeparator());
                    sbf.logger.log(result);
                    sbf.updateTitle(result);
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
}