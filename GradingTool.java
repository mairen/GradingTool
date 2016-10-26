/*
--------------
2016-10-26
Mai Ren

Update support for 2240-A3.

2240-A3 programs will redirect their stdin to /dev/tty. To auto send
input to these programs, GradingTool will modify their source code and
change this to a file, then create that file with the input for the 
test case.

--------------
2016-09-15
Mai Ren

Add feature
* Allows multi intervel select in source code viewer.
* Auto include line numbers in the feedback, group them into intervals.

Improved
* Method/function name finding.
* Method/function doc checking.

--------------
2016-09-05
Mai Ren

Add feature
* Auto calculate total points after applying seconc chance or late submission
factors. The result will be displayed as part of the formatted feedback for copy
to Blackboard.
* Display a title in the beginning of the formatted feedback for second chance
and late submissions.

--------------
2016-08-29
Mai Ren

When posting formatted feedback to Blackboard grading feedback input area, the
line breaks may be removed in certain conditions, cauins incorrect formatting.
The percise cause was not found yet, but a fix is to use two line breaks when 
we need a line break.

A GUI option was added to provide 3 options:
        ALL: Add an extra line after every single line.
      CLASS: Add extra lines before and after class names only.
        OFF: No extra lines.

--------------
2016-08-02
Mai Ren

Add feature:
* Use "Others" as feedback location if the feedback location textarea was 
empty.
* Select "*" as points removed, if nothing was selected.

--------------
2016-07-19
Mai Ren

Bugfix: 
    A student added comment at the same line of the method definition.
    Starting with "//". This has caused the algorithm of finding method
    name to fail. Now this has been fixed.

Add feature:
* Auto select CodeTester.
    Now that we have two CodeTesters, one for speed and the other one 
    for the accuracy of the output. We can auto select which CodeTester
    to use: 
    
    If the selected test case has a result file, then we select the
    accurate but slow CodeTester so that we can compare if the output
    is an exact match with the pre-defined output. Otherwise we select
    the fast but inaccurate CodeTester for its speed.
        

--------------
2016-05-14
Mai Ren

General code/documentation improvement with the help of eclipse. 
Previously this code was only written in simple editors.

--------------
2016-01-29
Mai Ren

Use CodeTester to test programs.
CodeTester must be compiled first.

--------------
2015-09-10
Mai Ren

Add features:
* Handle multiple courses.
* Handle C programs.
* Interact with subprocess.

https://groups.google.com/forum/#!topic/clojure/IzlapA0RcQo

--------------
2015-09-08
Mai Ren

Bug fix: Line number should start from 1, not 0.

--------------
2015-02-23
Mai Ren

Version 1.0

This is a tool that helps grading programming assignments.

--------------
To use it:

First select the folder that contains submissions for an assignment, 
for example: "CSCI-1620-5-S15-A2".

Now all submissions will be listed, select one will show its source 
code files.

Click "Compile" button will copy source code files, alone with test 
case files to a temporary working folder. If this assignment has
files that were provided and were not supposed to be changed by 
students, then the original files will be copied to overwrite any 
such files from student's submission. Then the program will be compiled
according to the command defined in the first test case file.

After a successful compiling. You can select a test case and click 
"Test" button to run this test case, output will be displayed in the 
output area at the right. Double click a test case will do the same 
thing.

To review the source code, select a file, its content will be displayed
in the source code viewing area in the middle. Select one line of source
code, the method name or class name will be displayed in the bottom. You
can select a pre-defined feedback, or edit a new one, and select amount
of points you want to remove or add(set to positive). Then click the 
"Add/Save" button to add this grading item to the overall feedback.

Click "Format/Save" button to format feedback and show the resule in the 
output area.

There is a privacy mode, in which userID and identifying info in the honor
pledge will be replaced. After selecting privacy mode, reopen the
assignment folder to update the student list.

----------------------------------
How to config for each assignment:

Folder "Assignments-1620" contains files required to help grade each 
assignment in course 1620.

For example, for A0, create a folder "A0". In folder A0, there should be 
three types of files:

First is a file "CommonIssues.txt", which contains common issues for this 
assignment. Each issue should be in one line. Contents in this files will 
be loaded into a list on the GUI from where you can select and generate
feedback.

Second is provided files that should not be changed by students. Since
some students change it anyway, the file here will be used to overwrite 
the same file submitted by students for compiling.

Third is at least one test case file. Please name the file as "TestCase0",
"TestCase1", "TestCase2", etc. Do not put any extension in the filename.
In a test case file, the first line should be the name of the class that
contains the main method. The rest of the file should be the input for 
this test case. See included examples.

-----------------------------------
Tested with:

Windows 8.1 Pro
Ubuntu 14.10
OSX 10.10

Oracle JDK 8
*/

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Pattern;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;

import static java.nio.file.StandardCopyOption.*;

public class GradingTool extends JPanel implements ListSelectionListener, ActionListener {
    private static final long serialVersionUID = 1L;
    
    // Currently we support just these two languages.
    private enum Language {Java, C};
    
    private enum SubmissionType {Normal, SecondChance, Late};
    
    private static final String FILE_NAME_FEEDBACK = "feedback.txt";
    private static final String FILE_NAME_COMMON_ISSUES = "CommonIssues.txt";
    private static final String PROGRAM_NAME = "Grading Tool";
    private static final String TEMP_FOLDER_NAME = "Temp";
    private static final String TEST_CASE_RESULT_FILE_NAME_SUFFIX = "Result";
    private static final String CODE_TESTER_CLASS_NAME = "CodeTester";
    private static final String CODE_TESTER_FAST_FOLDER_NAME = "CodeTester-old";
    private static final String CODE_TESTER_ACCURATE_FOLDER_NAME = "CodeTester";
    private static final String DEFAULT_GRADING_LOCATION = "Others";

    private static final String NEW_LINE = "\n";

    private static final int DEFAULT_FONT_SIZE = 16;
    private int fontSizeAdjustment = 0;

    private JFrame frame;
    
    JButton openButton, compileButton, testButton, addButton, saveButton, increaseFontSizeButton, decreaseFontSizeButton;
    JRadioButton buttonPrivacyOn;
    JRadioButton buttonPrivacyOff;
    JRadioButton buttonFeedbackFormat1;
    JRadioButton buttonFeedbackFormat2;
    JRadioButton buttonFeedbackFormat3;
    
    // List students under current folder
    private JList<String> studentList;
    private DefaultListModel<String> studentListModel;
    
    // List the source code files
    private JList<String> fileList;
    private DefaultListModel<String> fileListModel;
    
    // Use a list to display source code
    private JList<String> scViewerList;
    private DefaultListModel<String> scViewerListModel;

    // Use a list to display test cases
    private JList<String> testCaseList;
    private DefaultListModel<String> testCaseListModel;

    // Use a list to display points
    private JList<String> pointsList;
    private DefaultListModel<String> pointsListModel;
    
    // Use a list to display points
    private JList<String> commonIssueList;
    private DefaultListModel<String> commonIssueListModel;

    JTextArea logTextArea;
    JTextArea outputTextArea;
    JTextArea pointsTextArea;
    JTextArea feedbackLocationTextArea;// The method or class the feedback in feedbackContentTextArea is referring to.
    JTextArea feedbackLinesTextArea;// The lines the feedback in feedbackContentTextArea is referring to.
    JTextArea feedbackContentTextArea; // The content of a feedback. 
    JTextArea feedbackTextArea; // For all feedbacks
    JTextArea fullPointsTextArea; 
    JTextArea totalPointsTextArea; 
    
    JFileChooser fc;
    
    File[] folderNames;
    File[] fileNames;
    File[] testCaseNames;
    
    String feedbackFilePath = null;
    
    private int courseID = -1;
    private int assignmentNum = -1;
    
    private Language language;
    private SubmissionType submissionType;
    
    // If we are currently on Windows or not
    private boolean m_bWindows = false;
    
    /**
     * Create the GUI and show it.  For thread safety,
     * this method should be invoked from the
     * event-dispatching thread.
     */
    private static void createAndShowGUI() {
        //Create and set up the window.
        JFrame frame = new JFrame(PROGRAM_NAME);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        //Create and set up the content pane.
        JComponent newContentPane = new GradingTool(frame);
        newContentPane.setOpaque(true); //content panes must be opaque
        frame.setContentPane(newContentPane);

        //Display the window.
        frame.pack();
        frame.setVisible(true);
    }

    public static void main(String[] args) {
        //Schedule a job for the event-dispatching thread:
        //creating and showing this application's GUI.
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                createAndShowGUI();
            }
        });
    }
    
    public GradingTool(JFrame frame) {
        super(new BorderLayout());

        this.frame = frame;
        
        // Build GUI
        fc = new JFileChooser();
        
        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BorderLayout());
        add(bottomPanel, BorderLayout.PAGE_END);
        
        JPanel mainControlPanel = new JPanel();
        mainControlPanel.setLayout(new FlowLayout());
        bottomPanel.add(mainControlPanel, BorderLayout.LINE_START);

        JPanel privacyModePanel = new JPanel();
        privacyModePanel.setLayout(new GridLayout(0, 1));
        mainControlPanel.add(privacyModePanel);
        
        JLabel privacyModeLabel1 = new JLabel("Privacy");
        privacyModeLabel1.setHorizontalAlignment(0);
        privacyModePanel.add(privacyModeLabel1);

        JLabel privacyModeLabel2 = new JLabel("Mode");
        privacyModeLabel2.setHorizontalAlignment(0);
        privacyModePanel.add(privacyModeLabel2);
                     
        buttonPrivacyOn = new JRadioButton("ON", false);
        buttonPrivacyOn.addActionListener(this);
        privacyModePanel.add(buttonPrivacyOn);

        buttonPrivacyOff = new JRadioButton("OFF", true);
        buttonPrivacyOff.addActionListener(this);
        privacyModePanel.add(buttonPrivacyOff);
        
        ButtonGroup privateButtonGroup = new ButtonGroup();
        privateButtonGroup.add(buttonPrivacyOn);
        privateButtonGroup.add(buttonPrivacyOff);
        
        JPanel fontSizePanel = new JPanel();
        fontSizePanel.setLayout(new GridLayout(1, 0));
        privacyModePanel.add(fontSizePanel);
        
        increaseFontSizeButton = new JButton("+");
        increaseFontSizeButton.setMargin(new Insets(1, 1, 1, 1));
        increaseFontSizeButton.addActionListener(this);
        fontSizePanel.add(increaseFontSizeButton);
        
        decreaseFontSizeButton = new JButton("-");
        decreaseFontSizeButton.setMargin(new Insets(1, 1, 1, 1));
        decreaseFontSizeButton.addActionListener(this);
        fontSizePanel.add(decreaseFontSizeButton);

        JPanel testPanel = new JPanel();
        testPanel.setLayout(new BorderLayout());
        mainControlPanel.add(testPanel);

        compileButton = new JButton("Compile");
        compileButton.setEnabled(false);
        compileButton.addActionListener(this);
        testPanel.add(compileButton, BorderLayout.PAGE_START);
        
        testCaseListModel = new DefaultListModel<String>();
        testCaseListModel.addElement("Test cases");
        testCaseList = new JList<String>(testCaseListModel);
        testCaseList.setEnabled(false);
        testCaseList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        testCaseList.addListSelectionListener(this);
        testCaseList.setVisibleRowCount(4);
        testCaseList.addMouseListener(new MouseAdapter(){
            @Override
            public void mouseClicked(MouseEvent e){
                if(e.getClickCount() == 2){
                    runSelectedTestCase();
                }
            }
        });
        testPanel.add(new JScrollPane(testCaseList), BorderLayout.CENTER);
        
        testButton = new JButton("Test");
        testButton.setEnabled(false);
        testButton.addActionListener(this);
        testPanel.add(testButton, BorderLayout.PAGE_END);
        
        JPanel pointsPanel = new JPanel();
        pointsPanel.setLayout(new BorderLayout());
        mainControlPanel.add(pointsPanel);

        pointsTextArea = new JTextArea(1,4);
        pointsTextArea.setBorder(BorderFactory.createLineBorder(Color.BLACK)); 
        pointsPanel.add(pointsTextArea, BorderLayout.PAGE_START);
        
        pointsListModel = new DefaultListModel<String>();
        pointsListModel.addElement("*");
        pointsListModel.addElement("-0.5");
        pointsListModel.addElement("-1");
        pointsListModel.addElement("-2");
        pointsListModel.addElement("-3");
        pointsListModel.addElement("-4");
        pointsList = new JList<String>(pointsListModel);
        pointsList.setEnabled(false);
        pointsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        pointsList.addListSelectionListener(this);
        pointsList.setVisibleRowCount(6);
        pointsPanel.add(new JScrollPane(pointsList), BorderLayout.CENTER);
        
        JPanel feedbackPanel = new JPanel();
        feedbackPanel.setLayout(new GridBagLayout());
        mainControlPanel.add(feedbackPanel);

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        
        feedbackLocationTextArea = new JTextArea(1,20);
        feedbackLocationTextArea.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        c.gridy = 0;
        feedbackPanel.add(feedbackLocationTextArea, c);
        
        feedbackLinesTextArea = new JTextArea(1,20);
        feedbackLinesTextArea.setBorder(BorderFactory.createLineBorder(Color.BLACK)); 
        c.gridy = 1;
        feedbackPanel.add(feedbackLinesTextArea, c);

        feedbackContentTextArea = new JTextArea(4,20);
        feedbackContentTextArea.setLineWrap(true);
        c.gridy = 2;
        feedbackPanel.add(new JScrollPane(feedbackContentTextArea), c);
        
        addButton = new JButton("________ Add / Save ________");
        addButton.setEnabled(false);
        addButton.addActionListener(this);
        c.gridy = 3;
        feedbackPanel.add(addButton, c);
        
        commonIssueListModel = new DefaultListModel<String>();
        commonIssueListModel.addElement("___________ Common Issues ___________");
        commonIssueList = new JList<String>(commonIssueListModel);
        commonIssueList.setEnabled(false);
        commonIssueList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        commonIssueList.addListSelectionListener(this);
        commonIssueList.setVisibleRowCount(7);
        commonIssueList.setFixedCellWidth(250);
        mainControlPanel.add(new JScrollPane(commonIssueList));

        feedbackTextArea = new JTextArea(1,20);
        feedbackTextArea.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 0)); 
        feedbackTextArea.setMargin(new Insets(5,5,5,5));
        bottomPanel.add(new JScrollPane(feedbackTextArea), BorderLayout.CENTER);
        
        JPanel rightControlPanel = new JPanel();
        rightControlPanel.setLayout(new FlowLayout());
        bottomPanel.add(rightControlPanel, BorderLayout.LINE_END);

        JPanel formatFeedbackPanel = new JPanel();
        formatFeedbackPanel.setLayout(new GridLayout(0,1));
        rightControlPanel.add(formatFeedbackPanel);

        formatFeedbackPanel.add(new JLabel("Extra lines"));
        
        buttonFeedbackFormat1 = new JRadioButton("ALL", false);
        buttonFeedbackFormat1.addActionListener(this);
        formatFeedbackPanel.add(buttonFeedbackFormat1);

        buttonFeedbackFormat2 = new JRadioButton("CLASS", true);
        buttonFeedbackFormat2.addActionListener(this);
        formatFeedbackPanel.add(buttonFeedbackFormat2);

        buttonFeedbackFormat3 = new JRadioButton("OFF", true);
        buttonFeedbackFormat3.addActionListener(this);
        formatFeedbackPanel.add(buttonFeedbackFormat3);

        ButtonGroup feedbackFormatButtonGroup = new ButtonGroup();
        feedbackFormatButtonGroup.add(buttonFeedbackFormat1);
        feedbackFormatButtonGroup.add(buttonFeedbackFormat2);
        feedbackFormatButtonGroup.add(buttonFeedbackFormat3);
        
        saveButton = new JButton("Process");
        saveButton.setToolTipText("Format / Save the feedback to my left and calculate points.");
        saveButton.setEnabled(false);
        saveButton.addActionListener(this);
        formatFeedbackPanel.add(saveButton);
        
        JPanel calcPointsPanel = new JPanel();
        calcPointsPanel.setLayout(new GridLayout(0,1));
        rightControlPanel.add(calcPointsPanel);
        
        calcPointsPanel.add(new JLabel("Full"));
        calcPointsPanel.add(new JLabel("points:"));
        
        fullPointsTextArea = new JTextArea(1,4);
        fullPointsTextArea.setBorder(BorderFactory.createLineBorder(Color.BLACK)); 
        calcPointsPanel.add(fullPointsTextArea);

        calcPointsPanel.add(new JLabel("Total"));
        calcPointsPanel.add(new JLabel("points:"));
       
        totalPointsTextArea = new JTextArea(1,4);
        totalPointsTextArea.setEditable(false);
        calcPointsPanel.add(totalPointsTextArea);

        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BorderLayout());
        add(leftPanel, BorderLayout.LINE_START);
        
        openButton = new JButton("  Open Location  ");
        openButton.addActionListener(this);
        leftPanel.add(openButton, BorderLayout.PAGE_START);
        
        studentListModel = new DefaultListModel<String>();
        studentListModel.addElement("Student List");
        studentList = new JList<String>(studentListModel);
        studentList.setEnabled(false);
        studentList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        studentList.addListSelectionListener(this);
        studentList.setVisibleRowCount(5);
        JScrollPane studentListScrollPane = new JScrollPane(studentList);
        leftPanel.add(studentListScrollPane, BorderLayout.CENTER);
        
        fileListModel = new DefaultListModel<String>();
        fileListModel.addElement("File List");
        fileList = new JList<String>(fileListModel);
        fileList.setEnabled(false);
        fileList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        fileList.addListSelectionListener(this);
        fileList.setVisibleRowCount(8);
        JScrollPane fileListScrollPane = new JScrollPane(fileList);
        leftPanel.add(fileListScrollPane, BorderLayout.PAGE_END);

        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BorderLayout());
        
        rightPanel.add(new JLabel("Output"), BorderLayout.PAGE_START);
        outputTextArea = new JTextArea();
        outputTextArea.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 0)); 
        outputTextArea.setLineWrap(true);
        rightPanel.add(new JScrollPane(outputTextArea), BorderLayout.CENTER);

        scViewerListModel = new DefaultListModel<String>();
        scViewerListModel.addElement("Source code viewer");
        scViewerList = new JList<String>(scViewerListModel);
        scViewerList.setEnabled(false);
        // The default selection mode is multi interval.
        //scViewerList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        scViewerList.addListSelectionListener(this);
        scViewerList.setVisibleRowCount(5);
        JScrollPane scViewerListScrollPane = new JScrollPane(scViewerList);
        
        //Create a split pane with the two scroll panes in it.
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, scViewerListScrollPane, rightPanel);
        splitPane.setOneTouchExpandable(true);
        splitPane.setDividerLocation(200);
        //splitPane.setDividerLocation(splitPane.getSize().width - splitPane.getInsets().right - splitPane.getDividerSize() - 200);
        splitPane.setResizeWeight(1.0);

        //Provide minimum sizes for the two components in the split pane
        Dimension minimumSize = new Dimension(100, 50);
        scViewerListScrollPane.setMinimumSize(minimumSize);
        rightPanel.setMinimumSize(minimumSize);
        
        add(splitPane, BorderLayout.CENTER);

        // Show current path
        log("Current path: " + System.getProperty("user.dir"));
        log("Current OS: " + System.getProperty("os.name"));
        if (System.getProperty("os.name").toLowerCase().startsWith("windows"))
            m_bWindows = true;
        
        //listAllFonts();
        setFont();
    }

    private void setFont() {
        // Through experiments, I know at least one of these fonts is available on any of the three 
        // operating systems: Windows, OSX, Ubuntu.
        String[] fonts = {"Liberation Mono", "Ubuntu Mono", "Consolas", "Courier New", "Monospaced", "Courier"};

        for (String font : fonts) {
            if (!hasFont(font))
                continue;
            
            int fontStyle = Font.BOLD;
            if (fontSizeAdjustment % 2 == 0)
                fontStyle = Font.PLAIN;
            
            // The first step towards either end is Bold only, no size change.
            int sizeAdjustment = fontSizeAdjustment / 2;

            scViewerList.setFont(new Font(font, fontStyle, DEFAULT_FONT_SIZE + sizeAdjustment));
            outputTextArea.setFont(new Font(font, fontStyle, DEFAULT_FONT_SIZE + sizeAdjustment));
            log("Font set to: " + font);
            return;
        }
    }

    // Use this to find out what fonts a computer has.
    // Use this when none of the predefined fonts are available.
    /*
    private void listAllFonts() {
        GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
        Font[] allfonts = env.getAllFonts();

        for (Font font : allfonts) {
            log(font.getFontName());
        }
    }
    */

    private boolean hasFont(String fontName) {
        Font f = new Font(fontName, 0, 0);
        if (f.getFontName().equals(fontName))
            return true;
        else
            return false;
    }
    
    /**
     * Is the given file a source code file or not.
     * @param fileName Absolute or relative path to a file.
     * @return true if fileName is a source code file.
     */
    private boolean isSourceCodeFile(String fileName) {
        fileName = fileName.toLowerCase();
        
        if (language == Language.Java) {
            if (fileName.endsWith(".java"))
                return true;
        }
        else if (language == Language.C) {
            if (fileName.endsWith(".c") ||
                fileName.endsWith(".h"))
                return true;
                
            // For 2240 A3, 3 extra files are part of the submission.
            if (courseID == 2240 && assignmentNum == 3 && fileName.startsWith("prog"))
                return true;
        }
        
        return false;
    }
    
    //This method is required by ListSelectionListener.
    public void valueChanged(ListSelectionEvent e) {
        if (e.getValueIsAdjusting())
            return;
        
        if (e.getSource() == studentList) {
            compileButton.setEnabled(true);
            testButton.setEnabled(false);
            addButton.setEnabled(true);
            saveButton.setEnabled(true);
            scViewerList.setEnabled(true);
            fileList.setEnabled(true);
            testCaseList.setEnabled(false);
            pointsList.setEnabled(true);
            commonIssueList.setEnabled(true);
            
            fileListModel.clear();
            clearLog();
            
            int index = studentList.getSelectedIndex();
            if (index < 0) return;
            
            // Load file list
            if (index >= 0) {
                fileNames = folderNames[index].listFiles();
                Arrays.sort(fileNames);
                
                // Get a clean list, only source code files count.
                int count = 0;
                for (File f: fileNames) {
                    if (f != null && isSourceCodeFile(f.getName()))
                        count++;
                }
                File[] tmpList = new File[count];
                count = 0;
                for (File f: fileNames) {
                    if (f != null && isSourceCodeFile(f.getName())) {
                        tmpList[count] = f;
                        fileListModel.addElement(f.getName());
                        count++;
                    }
                }
                fileNames = tmpList;
            }
            
            // Load feedback
            feedbackTextArea.setText(null);
            feedbackFilePath = folderNames[index].getAbsolutePath() + File.separator + FILE_NAME_FEEDBACK;
            
            List<String> inputLines = readFile(feedbackFilePath);
            for (String line : inputLines)
                feedbackTextArea.append(line + NEW_LINE);

            calculatePoints();
        }
        else if (e.getSource() == fileList) {
            scViewerListModel.clear();
            int index = fileList.getSelectedIndex();
            
            if (index >= 0) {
                List<String> inputLines = readFile(fileNames[index].getPath());

                boolean bInHeader = true;
                int lineNum = 1;
                for (String line: inputLines) {
                    // Replace tab characters with 4 spaces to correct the indentation.
                    line = line.replaceAll("\t", "    ");
                    
                    // Replace blank lines with indentation check markings.
                    //if (line.trim().length() == 0)
                    //    line = "|---|---|---|---|";
                    
                    /**
                     * Todo: Replace open and close double quotation marks with ".
                     * 
                     * The source of this problem is students copy text from assignment
                     * sheet into their source code to use as part of the documentation.
                     * 
                     * Sometimes they copy non-standard characters, causing compiling 
                     * errors in their code. The only such characters that's causing 
                     * the problem are open and close double quotation marks, the ones
                     * that looked sideways. Replace them with the standard one ", the 
                     * one that looked straight solves the problem.
                     * 
                     * Whether this will cause a compiling error is dependent on the 
                     * compiling environment, but it's better to get it fixed.
                     */
                    
                    if (bInHeader) {
                        if (line.indexOf("public") >= 0 &&
                            line.indexOf("class") >= 0 )
                            bInHeader = false;
                    }
                    
                    /**
                     * In privacy mode, we replace any line that contains sensitive word
                     * with a privacy text.
                     */
                    if (buttonPrivacyOn.isSelected() && bInHeader) {
                        if (line.toLowerCase().indexOf("name") >= 0 ||
                            line.toLowerCase().indexOf("id") >= 0 ||
                            line.toLowerCase().indexOf("email") >= 0)
                            line = "***** Replaced by privacy text *****";
                    }
                        
                    scViewerListModel.addElement(String.format("%4d | ", lineNum) + line);
                    lineNum++;
                }
            }
        }
        else if (e.getSource() == testCaseList) {
            scViewerListModel.clear();
            int index = testCaseList.getSelectedIndex();

            if (index >= 0) {
                List<String> inputLines = readFile(testCaseNames[index].getPath());

                int lineNum = 0;
                for (String line: inputLines) {
                    scViewerListModel.addElement(String.format("%4d | ", lineNum) + line);
                    lineNum++;
                }
            }
        }
        else if (e.getSource() == pointsList) {
            int index = pointsList.getSelectedIndex();
            pointsTextArea.setText(pointsListModel.getElementAt(index));
        }
        else if (e.getSource() == scViewerList) {
            if (fileList.getSelectedIndex() < 0) return;
            
            feedbackLocationTextArea.setText(null);
            int index = scViewerList.getSelectedIndex();
            if (index < 0) return;
            
            // Update line numbers
            feedbackLinesTextArea.setText(indicesToString(scViewerList.getSelectedIndices()));
            
            /**
             * Do not repeat the rest of the work if the smallest selected
             * index does not change.
            static int lastSelectedIndex = -1;
            if (index == lastSelectedIndex)
                return;
            lastSelectedIndex = index;
             */
            
            // Use file name as class name
            String className = fileNames[fileList.getSelectedIndex()].getName();
            if (className.indexOf(".") >= 0)
                className = className.substring(0, className.indexOf("."));
            
            // Get method/function name and line index
            int[] methodNameLineIndex = new int[1];
            String methodName = getMethodName(index, methodNameLineIndex);
            
            String location = className;
            if (methodName != null && methodName.length() > 0)
                location += "." + methodName + "()";
            
            feedbackLocationTextArea.setText(location);

            // Check method doc of the current method.
            if (methodName != null) {
                int maxSearchLengh = 12;
                String searchArea = "";
                
                for (int lineNum = methodNameLineIndex[0] - 1; lineNum >= 0 && maxSearchLengh-- > 0; lineNum--) {
                    String line = scViewerListModel.getElementAt(lineNum);
                    
                    if (line.indexOf("}") >= 0 ||
                        line.indexOf("{") >= 0) {
                        break;
                    }
                    searchArea += line.toLowerCase();
                }
                
                int methodDocItemCount = 0;
                String[] methodDocItems = {"method name", "parameters", "return value", "partners", "description"};
                if (language == Language.C)
                    methodDocItems[0] = "function name";
                
                for (String s : methodDocItems)
                    if (searchArea.indexOf(s) >= 0)
                        methodDocItemCount++;
                    
                if (methodDocItemCount == 0)
                    //feedbackContentTextArea.setText("Missing method doc.");
                    log(location + "Missing method doc.");
                else if (methodDocItemCount < methodDocItems.length)
                    //feedbackContentTextArea.setText("Incorrect method doc format.");
                    log(location + "Incorrect method doc format.");
            }
        }
        else if (e.getSource() == commonIssueList) {
            int index = commonIssueList.getSelectedIndex();
            feedbackContentTextArea.setText(commonIssueListModel.getElementAt(index));
        }
    }
    
     /** 
     * Look backwards for method name or class name
     * 
     * Input: index of the line to start the search.
     * Return value: Name of the method that contains the given line.
     *
     * When the user click a line of code in the code viewer, we try to find
     * the name of the method this line of code belongs to.
     */
    private String getMethodName(int index, int[] methodNameLineIndex) {
        String methodName = null;
        
        for (; index >= 0; index--) {
            // Remove line number
            String line = scViewerListModel.getElementAt(index).substring(7).replaceAll(";", " ").trim();
            
            line = cutString(line, "//");
            line = cutString(line, "throws");
            
            if (!(line.endsWith(")") || line.endsWith("{") || line.endsWith(",")))
                continue;
            
            if (line.indexOf("=") >= 0)
                continue;
            
            if ((language == Language.Java && line.startsWith("p")) ||
                (language == Language.C && !line.startsWith("if ") && !line.startsWith("if(") && !line.startsWith("for ") && !line.startsWith("for(") && !line.startsWith("while ") && !line.startsWith("while(") && !line.startsWith("else ") &&
                    Pattern.matches("^\\s*[a-z]+\\s+\\S+\\s*[(].*[)].*", line))) {
                int index1 = line.indexOf("(");
                if (index1 < 0)
                    break;
                
                if (methodNameLineIndex != null && methodNameLineIndex.length > 0)
                    methodNameLineIndex[0] = index;
                
                String str1 = line.substring(0, index1).trim();
                methodName = str1.substring(str1.lastIndexOf(" ") + 1);
                break;
            }
        }
        
        return methodName;
    }
    
    /**
     * Format indices into a string.
     * Group continous indices into a single range.
     *
     * The returned string has a clean end(no comma, semicolon or spaces in the end).
     */
    private String indicesToString(int[] indices) {
        if (indices == null || indices.length == 0)
            return "";
        
        StringBuilder sb = new StringBuilder();
        if (indices.length == 1)
            sb.append("Line: ");
        else
            sb.append("Lines: ");
        
        int start = 0;
        for (int i = 1; i <= indices.length; i++) {
            if (i == indices.length || indices[i] != indices[i - 1] + 1) {
                if (i == start + 1) {
                    sb.append((indices[start] + 1) + ", ");
                }
                else if (i == start + 2) {
                    sb.append((indices[start] + 1) + ", ");
                    sb.append((indices[start + 1] + 1) + ", ");
                }
                else {
                    sb.append((indices[start] + 1) + "~" + (indices[i - 1] + 1) + ", ");
                }
                start = i;
            }
        }
        
        sb.setLength(sb.length() - 2);
        return sb.toString();
    }
    
    /**
     * If str contains from, then return the portion of str before the first
     * occurance of from. Otherwise return str.
     */
    private String cutString(String str, String from) {
        int index = str.indexOf(from);
        if (index > 0)
            str = str.substring(0, index).trim();
        return str;
    }
    
    /**
     * This is purely based on texts in feedbackTextArea.
     * 
     * The benefit of this is that the user can manually edit
     * the content in feedbackTextArea should there be any error.
     */
    private double calculatePoints() {
        String all = feedbackTextArea.getText();
        String lines[] = all.split("\\r?\\n");
        
        double total = 0;
        for (String line : lines) {
            line = line.trim();
            //log(line);
            double points = 0;
            try {
                points = Double.parseDouble(line.substring(0, line.indexOf(" ")));
            } catch (Exception e) {}
            total += points;
        }
        
        double fullPoints = 0;
        try {
            fullPoints = Double.parseDouble(fullPointsTextArea.getText());
        } catch (Exception e) {}
        
        totalPointsTextArea.setText(Double.toString(fullPoints + total));
        
        return fullPoints + total;
    }
    
    /**
     * Convert the content in the feedbackTextArea into something
     * that can be directly copied to Blackboard as grading feedback.
     * 
     * All the deductions will be grouped by class.
     */
    private void formatFeedback() {
        outputTextArea.setText("");
        
        switch (submissionType) {
            case SecondChance:
                log("================");
                log("Second chance:");
                break;
            case Late:
                log("Late submission:");
                break;
            default:
                break;
        }

        String all = feedbackTextArea.getText();
        String lines[] = all.split("\\r?\\n");

        ArrayList<String> list = new ArrayList<String>();
        for (String line : lines) {
            line = line.trim();
            if (line.length() > 0)
                list.add(line.trim());
        }
        
        String classNameBase = null;
        while(list.size() > 0) {
            for (int i = 0; i < list.size(); i++) {
                String line = list.get(i);
                String sections[] = line.split(" ");
                
                String content = "";
                for (int j = 2; j < sections.length; j++)
                    content += " " + sections[j];
                content = content.substring(1);
                
                String className = sections[1];
                if (className.indexOf(".") >= 0) {
                    className = className.substring(0, className.indexOf("."));
                    content = sections[1].substring(sections[1].indexOf(".") + 1) + ". " + content;
                }
                
                if (classNameBase == null && i == 0) {
                    classNameBase = className;
                    if ((buttonFeedbackFormat1.isSelected() || buttonFeedbackFormat2.isSelected()) && !outputTextArea.getText().endsWith("\n\n") )
                        log("");
                    log(classNameBase);
                    if (buttonFeedbackFormat1.isSelected() || buttonFeedbackFormat2.isSelected())
                        log("");
                    log(sections[0] + " " + content);
                    if (buttonFeedbackFormat1.isSelected())
                        log("");
                    list.remove(0);
                    i--;
                }
                else if (classNameBase.equals(className)) {
                    log(sections[0] + " " + content);
                    if (buttonFeedbackFormat1.isSelected())
                        log("");
                    list.remove(i);
                    i--;
                }
            }
            
            classNameBase = null;
        }
    }
    
    private void log(String log) {
        outputTextArea.append(log + NEW_LINE);
        //String timeStamp = new SimpleDateFormat("HH:mm:ss").format(Calendar.getInstance().getTime());
        //outputTextArea.append("[" + timeStamp + "]" + log + newline);
    }
    
    private void clearLog() {
        outputTextArea.setText(null);
    }

    // A folder name will be like "CSCI-2240-2-F15-A1", we are trying to find the "2240" as course ID.
    private void getCourseID(String folderName) {
        int index1 = folderName.indexOf("-");
        if (index1 < 0) {
            log("Cannot find course name in folder name.");
            return;
        }
        
        String str = folderName.substring(index1 + 1);
        int index2 = str.indexOf("-");
        if (index2 < 0) {
            log("Cannot find course name in folder name.");
            return;
        }
        courseID = Integer.parseInt(str.substring(0, index2));
        log("Course ID is: " + courseID);
        
        // Set the language of the course at here:
        if (courseID == 1620 || courseID == 1420)
            language = Language.Java;
        else if (courseID == 2240)
            language = Language.C;
        else
            log("ERROR: Unrecognized course: " + courseID);
    }

    // A folder name will be like "CSCI-2240-2-F15-A1", we are trying to find the "1" as assignment number.
    private void getAssignmentNumber(String folderName) {
        String name = folderName;
        // For late and second chance submissions, "-L" or "-S" will be added to the end of the name. We need to remove those first.
        if (name.endsWith("-S")) {
            submissionType = SubmissionType.SecondChance;
            name = name.substring(0, name.length() - 2);
        }
        else if (name.endsWith("-L")) {
            submissionType = SubmissionType.Late;
            name = name.substring(0, name.length() - 2);
        }
        else{
            submissionType = SubmissionType.Normal;
        }
        
        int index1 = name.lastIndexOf("-A");
        if (index1 < 0) {
            log("Cannot find assignment number in folder name.");
            return;
        }
        
        String str = name.substring(index1 + 2);
        assignmentNum = Integer.parseInt(str);
        log("Assignment number is: " + assignmentNum);
    }
    
    /**
     * Find the path where the configuration for current assignment is located.
     * 
     * @return A relative path to the folder that contains the configuration
     *         for current assignment
     */
    private String getAssignmentFolderName() {
        return "Assignments-" + courseID + File.separator + "A" + assignmentNum;
    }
    
    // Automatically find and load test cases for this assignment, based on the name of the folder.
    private void loadTestCases() {
        log("Loading test cases.");
        File file = new File(getAssignmentFolderName());
        testCaseNames = file.listFiles();
        Arrays.sort(testCaseNames);
        testCaseListModel.clear();
        
        // Get a clean list
        int count = 0;
        for (File f: testCaseNames)
            //if (f != null && f.getName().startsWith("TestCase") && !f.getName().startsWith(TEST_CASE_RESULT_FILE_NAME_SUFFIX))
            if (f != null && Pattern.matches("TestCase\\d*", f.getName()))
                count++;
            
        log("Found " + count + " test cases.");
        
        File[] tmpList = new File[count];
        count = 0;
        for (File f: testCaseNames) {
            //if (f != null && f.getName().startsWith("TestCase") && !f.getName().startsWith(TEST_CASE_RESULT_FILE_NAME_SUFFIX)) {
            if (f != null && Pattern.matches("TestCase\\d*", f.getName())) {
                tmpList[count] = f;
                testCaseListModel.addElement(f.getName());
                count++;
            }
        }
        testCaseNames = tmpList;
    }
    
    private void loadCommonIssues() {
        commonIssueListModel.clear();

        File file = new File(getAssignmentFolderName());
        File[] files = file.listFiles();
        
        for (File f: files) {
            if (f != null && f.getName().toLowerCase().indexOf("common") >= 0) {
                for (String line : readFile(f.getAbsolutePath())) {
                    commonIssueListModel.addElement(line);
                }
                break;
            }
        }
    }    

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == openButton) {
            fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            int returnVal = fc.showOpenDialog(GradingTool.this);

            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File file = fc.getSelectedFile();
                log("Selected folder: " + file.getAbsolutePath());
                log("Opening: " + file.getName());
                frame.setTitle(PROGRAM_NAME + " - " + file.getName());
                getCourseID(file.getName());
                getAssignmentNumber(file.getName());
                
                studentListModel.clear();
                if (file.isDirectory()) {
                    folderNames = file.listFiles();
                    Arrays.sort(folderNames);
                    int count = 0;
                    for (File f: folderNames) {
                        //if (!f.isDirectory())
                        //    continue;
                    
                        if (buttonPrivacyOff.isSelected())
                            studentListModel.addElement(f.getName());
                        else
                            studentListModel.addElement(String.format("ID = %d", count));
                    
                        count++;
                    }
                }
                
                loadCommonIssues();
                loadTestCases();
                
                studentList.setEnabled(true);
                compileButton.setEnabled(false);
            } else {
                log("Open command cancelled by user.");
            }
        }
        else if (e.getSource() == compileButton) {
            compileButton.setEnabled(false);
            copyToTempFolder();
            
            log("Compiling...");
            
            boolean bSuccess = false;
            String[] lines = null;
            
            try {
                lines = runTest(testCaseNames[0].getName(), true);
                bSuccess = true;
            }
            catch(Exception ee) {
                log("Exception during compiling:");
                log(ee.toString());
            }
            
            if (language == Language.Java) {
                // Ignore certain warnings
                if (lines != null && lines.length > 0 && lines[0].length() > 0) {
                    for (String line : lines) {
                        log(line);
                        if (!line.startsWith("Note:")) {
                            bSuccess = false;
                            break;
                        }
                    }
                }
            }
            else if (language == Language.C) {
                Path path = Paths.get(TEMP_FOLDER_NAME + File.separator + getCExecutableName());
                if (!Files.exists(path))
                    bSuccess = false;
            }
            
            if (!bSuccess) {
                log("\r\n*** Failed to compile ***");
            }
            else {
                log("Success!");
                testButton.setEnabled(true);
                testCaseList.setEnabled(true);
            }
        }
        else if (e.getSource() == testButton) {
            runSelectedTestCase();
        }
        else if (e.getSource() == addButton) {
            // Set some default values if none were given.
            if (feedbackLocationTextArea.getText().trim().length() == 0)
                feedbackLocationTextArea.setText(DEFAULT_GRADING_LOCATION);
            if (pointsTextArea.getText().trim().length() == 0) {
                pointsTextArea.setText(pointsListModel.getElementAt(0));
                pointsList.setSelectedIndex(0);
            }
                
            feedbackTextArea.append(pointsTextArea.getText() + " " 
                + feedbackLocationTextArea.getText() + " "
                + (feedbackLinesTextArea.getText().length() > 0 ? feedbackLinesTextArea.getText() + ". " : "")
                + feedbackContentTextArea.getText() + NEW_LINE);
                
            // Save to file, too.
            calculatePoints();
            saveFeedback();
        }
        else if (e.getSource() == saveButton) {
            // Format feedback
            formatFeedback();
                
            // Save to file.
            double points = calculatePoints();
            double factor = 1;
            switch (submissionType) {
                case SecondChance:
                    if (language == Language.Java)
                        factor = 0.85;
                    else if (language == Language.C)
                        factor = 0.7;
                    break;
                case Late:
                    if (language == Language.Java)
                        factor = 0.65;
                    else if (language == Language.C)
                        factor = 0.5;
                    break;
                default:
                    break;
            }
            log("");
            if (factor == 1)
                log("Total = " + points);
            else
                log("Total = " + points + " x " + factor + " = " + points * factor);
            
            saveFeedback();
        }
        else if (e.getSource() == increaseFontSizeButton) {
            fontSizeAdjustment++;
            setFont();
        }
        else if (e.getSource() == decreaseFontSizeButton) {
            fontSizeAdjustment--;
            setFont();
        }
    }
    
    private void saveFeedback() {
        writeFile(feedbackFilePath, feedbackTextArea.getText());
    }
    
    private void writeFile(String pathName, String content) {
        FileWriter fw = null;
        BufferedWriter bw = null;
        try {
            fw = new FileWriter(pathName);
            bw = new BufferedWriter(fw);
            bw.write(content);
        }
        catch (IOException e) {}
        finally {
            try {
                if (bw != null) bw.close();
                if (fw != null) fw.close();
            } catch (IOException e) {}
        }
    }
    
    private List<String> readFile(String pathName) {
        System.out.println("Reading file: " + pathName);
        
        Path path = Paths.get(pathName);
        if (!Files.exists(path))
            return new ArrayList<String>();
            
        List<String> inputLines = null;
        try {
            inputLines = Files.readAllLines(path, StandardCharsets.UTF_8);
            
            /**
             * Try other encodings, this part is not working very well
             * 
             * Some international students have their computers setup in other
             * languages, and they would save their source code in non-standard
             * encodings, and resulting in compiling errors on other computers. 
             */
            if (inputLines == null)
                inputLines = Files.readAllLines(path, StandardCharsets.ISO_8859_1);
            if (inputLines == null)
                inputLines = Files.readAllLines(path, StandardCharsets.US_ASCII);
        } catch (IOException e) {
            System.out.println("Failed to read file: " + pathName);
            System.out.println(e);  
        }
        
        if (inputLines == null)
            return new ArrayList<String>(); // Do not return null
        else
            return inputLines;
    }
    
    private void recursivelyDeleteFile(File target) {
        for(File f : target.listFiles()) {
            if (f.isDirectory())
                recursivelyDeleteFile(f);
            f.delete();
        }
    }
    
    private void copyToTempFolder() {
        // Clear temp folder first
        File tempFolder = new File(TEMP_FOLDER_NAME);
        recursivelyDeleteFile(tempFolder);

        try {
            // Copy source code files from user folder to temp folder
            for (File f : fileNames)
                Files.copy(Paths.get(f.getAbsolutePath()), Paths.get(tempFolder.getAbsolutePath() + File.separator + f.getName()), REPLACE_EXISTING);

            // Copy everything in the assignment folder (except CommonIssues.txt) to temp folder.
            {
                File file = new File(getAssignmentFolderName());
                for (File f: file.listFiles())
                    if (f != null && !f.getName().equals(FILE_NAME_COMMON_ISSUES))
                        Files.copy(Paths.get(f.getAbsolutePath()), Paths.get(tempFolder.getAbsolutePath() + File.separator + f.getName()), REPLACE_EXISTING);
            }
        } catch (IOException e) {
            log("Error: " + e.getMessage());
        }
    }
      
    private void copyCodeTester(String codeTestFolderName)
    {
        File tempFolder = new File(TEMP_FOLDER_NAME);
        File file = new File(codeTestFolderName);
        File[] files = file.listFiles();
        
        try {
            for (File f: file.listFiles())
                if (f != null && f.getName().endsWith(".class"))
                    Files.copy(Paths.get(f.getAbsolutePath()), Paths.get(tempFolder.getAbsolutePath() + File.separator + f.getName()), REPLACE_EXISTING);
        } catch (IOException e) {
            log("Error: " + e.getMessage());
        }
    }

    private void runSelectedTestCase() {
        int index = testCaseList.getSelectedIndex();
        if (index < 0)
            log("Select a test case first.");
            
        clearLog();
        String[] lines = runTest(testCaseNames[index].getName(), false);
        if (lines == null) {
            log("Test run unsuccessful.");
            return;
        }
        
        for (String line : lines)
            log(line);
    }
    
    private String getCExecutableName() {
        if (m_bWindows)
            return "a.exe";
        else
            return "a.out";
    }
    
    private String[] runTest(String testCaseName, boolean bCompile) {
        // Read test case
        String testCaseFile = TEMP_FOLDER_NAME + File.separator + testCaseName;
        String testResultFile = testCaseFile + TEST_CASE_RESULT_FILE_NAME_SUFFIX;
        
        // Copy the correct CodeTester to the Temp folder.
        if (language == Language.C || Files.exists(Paths.get(testResultFile))) {
            System.out.println("Selecting accurate CodeTester.");
            copyCodeTester(CODE_TESTER_ACCURATE_FOLDER_NAME);
        }
        else {
            System.out.println("Selecting fast CodeTester.");
            copyCodeTester(CODE_TESTER_FAST_FOLDER_NAME);
        }

        // Read input lines from the test case file.
        List<String> inputLines = readFile(testCaseFile);
        
        // Get executable name and launching arguments
        String[] sections = inputLines.get(0).split(" ");
        String executableName = sections[0];
        
        log("----------");
        log("There are " + sections.length + " sections.");
        for (int i = 0; i < sections.length; i++)
            log("Section " + i + ": " + sections[i]);
        log("----------");
        
        // Setting the command to compile and run the program depending on the language.
        String tempFolder = System.getProperty("user.dir") + File.separator + TEMP_FOLDER_NAME + File.separator;
        Process process = null;
        if (language == Language.Java) {
            try {
                ProcessBuilder pb = null;
                if (bCompile) {
                    pb = new ProcessBuilder("javac", "-d", ".", "@sources");
                    //pb = new ProcessBuilder("javac", executableName + ".java");
                }
                else {
                    pb = new ProcessBuilder("java", CODE_TESTER_CLASS_NAME, testCaseName);
                }
                pb.directory(new File(tempFolder));
                process = pb.start();
            } catch (IOException e) {
                log(e.toString());
                return null;
            }
        }
        else if (language == Language.C) {     
            if (bCompile) {
                /////////////////////
                // Code for specific assignments
                if (courseID == 2240 && assignmentNum == 3)
                    CSCI2240_A3_UseAlternateInputFile(executableName + ".c");
                if (courseID == 2240 && assignmentNum == 5)
                    CSCI2240_A5_FixServerAddress();
            }
            else {
                if (courseID == 2240 && assignmentNum == 3)
                    CSCI2240_A3_CreateTempInput(testCaseName);
            }

            try {
                ProcessBuilder pb = null;
                if (bCompile)
                    pb = new ProcessBuilder("gcc", "-Wall", "-ansi", "-pedantic", executableName + ".c");
                else
                    pb = new ProcessBuilder("java", CODE_TESTER_CLASS_NAME, testCaseName);
                pb.directory(new File(tempFolder));
                process = pb.start();
            } catch (IOException e) {
                log(e.toString());
                return null;
            }        
        }
        // This is the code prior to switch to use CodeTester to do the testing.
        /*
        else if (language == Language.C) {            
            sections[0] = System.getProperty("user.dir") + File.separator + TEMP_FOLDER_NAME + File.separator + getCExecutableName(); // Use absolute path
            if (bCompile) {
                //sections[0] = "gcc -Wall -ansi -pedantic " + executableName + ".c"; // Cannot get this to work.
                sections[0] = "gcc -Wall -pedantic " + executableName + ".c";
                
                /////////////////////
                // Code for specific assignments
                if (courseID == 2240 && assignmentNum == 5)
                    CSCI2240_A5_FixServerAddress();
            }

            log("Executing: " + sections[0]);
            
            // I need use dir with C because of launching arguments. If one of those were filenames, then the wrong launching dir will cause the C program to fail to find the file.
            File dir = new File(System.getProperty("user.dir") + File.separator + TEMP_FOLDER_NAME);
            try {
                if (bCompile)
                    process = Runtime.getRuntime().exec(sections[0], null, dir);
                else
                    process = Runtime.getRuntime().exec(sections, null, dir);
            } catch (IOException e) {
                log(e.toString());
                return null;
            }
        } */
        else {
            log("This programming language is not supported yet.");
            return null;
        }
        
        if (process == null) {
            log("Failed to start process");
            return null;
        }
        else {            
            log("Process started: " + process.toString());
        }

        List<String> lines = new ArrayList<String>();
        if (bCompile) {
            // See if there is any error output from the compiler, we want none.
            Scanner scanner = new Scanner(new InputStreamReader(process.getErrorStream()));
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                
                // Bug fix: The following output lines were considered normal:
                if (line.equals("Picked up JAVA_TOOL_OPTIONS: -javaagent:/usr/share/java/jayatanaag.jar "))
                    continue;
                
                lines.add(line);
            }
            
            // If there is no output from the compiler, we still need to verify if the output file was really created.
            if (lines.size() == 0) {
                //if (language == Language.Java)
                Path path = Paths.get(TEMP_FOLDER_NAME + File.separator + executableName + ".class");
                if (language == Language.C)
                    path = Paths.get(TEMP_FOLDER_NAME + File.separator + getCExecutableName());
                    
                if (!Files.exists(path))
                    lines.add("Verification failed: Cannot find the compiled file.");
            }
            
            scanner.close();
        }
        else {
            Scanner scanner = new Scanner(new InputStreamReader(process.getInputStream()));
            Scanner errorScanner = new Scanner(new InputStreamReader(process.getErrorStream()));

            // Collect all the output.
                    
            lines.add("===============================");
            lines.add("Output:");
            lines.add("-------------------------------");
        
            List<String> outputLines = new ArrayList<String>();
            boolean bResultSection = true;
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                lines.add(line);
                
                // Ignore match check done by the CodeTester
                if (line.startsWith("==========================="))
                    bResultSection = false;

                if (bResultSection)
                    outputLines.add(line);
            }

            // Compare result to a pre-defined result file
        
            /////////////////////
            // Code for specific assignments
            if (courseID == 1620 && assignmentNum == 3) {
                CSCI1620_A3_VerifyMaze(lines);
                for (String line : verifyOutput(outputLines, testResultFile))
                    lines.add(line);
            }
            else if (courseID == 2240 && assignmentNum == 4) {
                // For this assignment we need to verify two output files.
                String outputFile = TEMP_FOLDER_NAME + File.separator + sections[2] + ".out";
                String outputFile_Expected = testResultFile + ".out";                
                for (String line : verifyOutput(outputFile, outputFile_Expected))
                    lines.add(line);
                
                String wordsFile = TEMP_FOLDER_NAME + File.separator + sections[2] + ".words";
                String wordsFile_Expected = testResultFile + ".words";
                for (String line : verifyOutput(wordsFile, wordsFile_Expected))
                    lines.add(line);
            }
            else { // Compare with the result file
                for (String line : verifyOutput(outputLines, testResultFile))
                    lines.add(line);
            }
            
            // Add error output
            boolean bTitle = false;
            while (errorScanner.hasNextLine()) {
                if (!bTitle) {
                    lines.add("===============================");
                    lines.add("Error output:");
                    lines.add("-------------------------------");
                    bTitle = true;
                }
                String line = errorScanner.nextLine();
                lines.add(line);
            }
            
            scanner.close();
            errorScanner.close();
        }
        
        return lines.toArray(new String[lines.size()]);
    }
    
    private List<String> verifyOutput(String actualFile, String expectedFile) {
        if (!Files.exists(Paths.get(actualFile))) {
            List<String> tmpLines = new ArrayList<String>();
            tmpLines.add("Cannot find file: " + Paths.get(actualFile).getFileName());
            return tmpLines;
        }
        
        return verifyOutput(readFile(actualFile), expectedFile);
    }
    
    private List<String> verifyOutput(List<String> lines, String expectedFile) {
        if (!Files.exists(Paths.get(expectedFile)))
            return new ArrayList<String>();
        
        return verifyOutput(lines, readFile(expectedFile));
    }

    private List<String> verifyOutput(List<String> lines, List<String> expectedLines) {
        List<String> tmpLines = new ArrayList<String>();
        
        tmpLines.add("===============================");
        tmpLines.add("Compared to expected output:");
        tmpLines.add("-------------------------------");
    
        boolean error = false;
        
        // A simple comparison of number of lines.
        if (lines.size() > expectedLines.size()) {
            tmpLines.add("Output is longer than expected.");
            tmpLines.add("expectedLines.size() = " + expectedLines.size());
            tmpLines.add("outputLines.size() = " + lines.size());
            error = true;
        }
        if (lines.size() < expectedLines.size()) {
            tmpLines.add("Output is shorter than expected.");
            tmpLines.add("expectedLines.size() = " + expectedLines.size());
            tmpLines.add("outputLines.size() = " + lines.size());
            error = true;
        }
        tmpLines.add("");
        
        // Sometimes there are extra output lines before the expected output, we skip those, but they are an error nonetheless.
        String firstLine = expectedLines.get(0).trim();
        int j = 0;
        for (j = 0; j < lines.size(); j++) {
            if (firstLine.equals(lines.get(j).trim()))
                break;
                
            error = true;
        }
        if (j > 0) {
            tmpLines.add("There are " + j + " extra lines before expected output.");
            tmpLines.add("");
        }
        
        if (j == lines.size()) {
            //tmpLines.add("Cannot find the first line in the expect output.");
            //tmpLines.add("");
            error = true;
            
            // If we cannot find the matching first line, we try to start from the back and counting to the starting line.
            
            // First remove empty lines at the back
            for (j = lines.size() - 1; j >= 0; j--)
                if (lines.get(j).trim().length() > 0)
                    break;
                    
            // Then check is j is still valid.
            j -= expectedLines.size() - 1;
            if (j < 0) {
                tmpLines.add("Cannot find the first line in the expect output.");
                tmpLines.add("");
                j = lines.size();
            }
        }

        int mismatchCount = 0;
        int i = 0;
        for (i = 0; i < expectedLines.size() && i + j < lines.size(); i++) {
            String line = expectedLines.get(i).trim();
            if (!line.equals(lines.get(i + j).trim())) {
                mismatchCount++;
                tmpLines.add("Mismatch " + mismatchCount + ":");
                tmpLines.add("  Line     : " + (i + 1));
                tmpLines.add("  Expected : " + expectedLines.get(i));
                tmpLines.add("  Actual   : " + lines.get(i + j));
                tmpLines.add("");
                error = true;
            }
        }
        
        if (!error) {
            tmpLines.add("No error.");
        }
        tmpLines.add("===============================");
        
        return tmpLines;
    }
    
    // This is for CSCI-1620 A3(Maze) only
    // This method checks if the number of 'X's is equal to the number of steps
    // Returns false if maze is solved and steps don't match number of 'X's.
    private boolean CSCI1620_A3_VerifyMaze(List<String> lines) {
        String output = "";
        for (String line : lines)
            output += line;
        //String output = outputTextArea.getText();
        
        output = output.toLowerCase();
        
        // Find if it is solved
        if (output.indexOf("path") < 0)
            return true;
        
        int index1 = output.indexOf("solved");
        if (index1 < 0)
            return true;
        
        String maze = output.substring(index1); // Roughly the solved maze
        index1 = maze.indexOf("=====");
        if (index1 > 0)
            maze = maze.substring(0, index1);

        char[] a = maze.toCharArray();
        
        int count = 0;
        for (char c : a) {
            if (c == 'x' || c == 'X')
                count++;
        }
        
        // Now get the count that was printed out.
        int index2 = output.indexOf("took");
        int index3 = output.indexOf("steps");
        
        int steps = 0;
        try {
            steps = Integer.parseInt(output.substring(index2 + 4, index3).trim());
        } catch (Exception e) {}
        
        log("");
        log("========================");
        log("Verifying if(steps == number of 'x's)");
        log("");
        
        if (steps == count) {
            log("Match!");
            return true;
        }
        else {
            log(String.format("No"));
            log(String.format("Steps = %d", steps));
            log(String.format("Number of 'X's = %d", count));
            return false;
        }
    }

    // This is for CSCI-2240 A5(client/server) only
    // Some students hardcode loki as the address of the server. This
    // method replaces it with localhost.
    private void CSCI2240_A5_FixServerAddress() {
        String clientFile = TEMP_FOLDER_NAME + File.separator + "client.c";
        
        List<String> lines = readFile(clientFile);
        if (lines == null || lines.size() == 0) {
            log("CSCI2240_A5_FixServerAddress: Cannot find file \"client.c\"");
            return;
        }
        
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (!line.startsWith("/") && line.indexOf("gethostbyname") >= 0) {
                line = line.substring(0, line.indexOf("gethostbyname")) + "gethostbyname(\"localhost\");";
                lines.set(i, line);
                log("CSCI2240_A5_FixServerAddress: updated line " + (i + 1) + " to: " + line);
                break;
            }
        }
        
        try {
            Files.write(Paths.get(clientFile), lines, StandardCharsets.UTF_8);
        }
        catch (Exception e) {
            log("CSCI2240_A5_FixServerAddress: Failed to write back to file.");
        }
    }
    
    private void CSCI2240_A3_UseAlternateInputFile(String sourceCodeFileName) {
        String fileFullPath = TEMP_FOLDER_NAME + File.separator + sourceCodeFileName;
        List<String> lines = readFile(fileFullPath);
        if (lines == null || lines.size() == 0) {
            log("CSCI2240_A3_UseAlternateInputFile: Cannot find file: " + fileFullPath);
            return;
        }
        
        int count = 0;
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (!line.startsWith("/") && line.indexOf("/dev/tty") >= 0) {
                line = line.replace("/dev/tty", "tempinput");
                lines.set(i, line);
                log("CSCI2240_A3_UseAlternateInputFile: updated line " + (i + 1) + " to:");
                log(line);
                count++;
            }
        }
        
        try {
            Files.write(Paths.get(fileFullPath), lines, StandardCharsets.UTF_8);
        }
        catch (Exception e) {
            log("CSCI2240_A3_UseAlternateInputFile: Failed to write back to file.");
        }
    }
    
    private void CSCI2240_A3_CreateTempInput(String testCaseName) {
        // Read test case
        String testCaseFile = TEMP_FOLDER_NAME + File.separator + testCaseName;
        String tempInputFile = TEMP_FOLDER_NAME + File.separator + "tempinput";
        List<String> lines = readFile(testCaseFile);
        if (lines == null || lines.size() == 0) {
            log("CSCI2240_A3_CreateTempInput: Cannot find file: " + testCaseFile);
            return;
        }
        if (lines.size() < 4) {
            log("CSCI2240_A3_CreateTempInput: Testcase file does not contain user input lines: " + testCaseFile);
            return;
        }
        
        lines.remove(0);
        
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line == null || line.length() == 0 || line.startsWith("/")) {
                lines.remove(i);
                i--;
            }
        }
        
        try {
            Files.write(Paths.get(tempInputFile), lines, StandardCharsets.UTF_8);
            log("CSCI2240_A3_CreateTempInput: User input file for " + testCaseName + " was created.");
        }
        catch (Exception e) {
            log("CSCI2240_A3_CreateTempInput: Failed to write back to file.");
        }
    }
}
