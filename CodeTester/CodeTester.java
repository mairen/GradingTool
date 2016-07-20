/*
---------------
2016-01-27
Mai Ren

Add support for interactive programs.

Previously we were unable to insert the input lines into
the collected output lines, causing the output looks 
different than manually testing in a normal terminal.

By logging the timestamp for each input and output line,
and adding a delay after each input so that we can 
collect all the output for that input before feeding 
the next input, we are new able to insert the input 
lines to the correct position in the output collected.
The result is the exact same output a user would see if
he/she is testing the program in a normal terminal.

---------------
2014-09-23
Mai Ren

This is an automatic testing tool.

It reads test case from a file, feed it to the program 
being tested, read output from the program, and compare 
the output to the correct result.

To run this program:

"java CodeTester TestCase0"

"TestCase0" is the name of a test case file containing 
the name of the program to be tested and all the input 
lines for this test case.

The content of an example test case file:

SimpleListTest
2
y
3
n

Empty lines and lines started with "//" in the test 
case file will be ignored, so you can add comments in
your test case file.

If there is no correct result file, the output from 
current run will be used to create such a file, and 
results from future runs will be compared to result in 
this file. This feature is currently disabled, you can
enable it by uncommenting those lines.

The name of the currect result file for a test case 
must be the name of the test case file + "Result".
Because of this, you must make sure the name of your
test case file does not contain an extension, like 
".txt".

One of the best way to generate a correct result file 
is to run test cases on a reference program that was 
made sure to be correct. If you were required to match
a given output, the you can directly create a result
file from the given output.

The program being tested must only have one scanner 
instance.
*/

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Calendar;

class Line {
    public Date timestamp;
    public String text;
    
    public Line(Date timestamp, String text) {
        this.timestamp = timestamp;
        this.text = text;
    }
}

public class CodeTester {
    public enum Language {Java, C};
    
    private static Charset _ENCODING = StandardCharsets.UTF_8;
    private static String _testCaseFile;
    private static String _testResultFile;
    
    private static Language _language;
    
    private static int Input_Delay = 500; // In million seconds
    
    public static void main(String[] args) throws IOException {
        // Read test case
        _testCaseFile = args[0];
        _testResultFile = _testCaseFile + "Result";
        Path path = Paths.get(_testCaseFile);
        List<String> inputLines = null;
        final ArrayList<Line> inputTimestampedLines = new ArrayList<Line>();
        
        try {
            inputLines = Files.readAllLines(path, _ENCODING);
        } catch (IOException e) {}
        
        String[] sections = inputLines.get(0).split(" ");
        String executableName = sections[0];
        String cmd = executableName;
        
        if (Files.exists(Paths.get(executableName + ".java")) ||
            Files.exists(Paths.get(executableName + ".JAVA"))) {
            _language = Language.Java;
            cmd = "java " + executableName;
        }
        else {
            _language = Language.C;
            cmd = "./a.out";
            if (System.getProperty("os.name").toLowerCase().startsWith("windows"))
                cmd = "a.exe";
        }
        
        for (int i = 1; i < sections.length; i++) {
            cmd += " " + sections[i];
        }
        
        // Launch the program being tested, read and print its output from a separate thread
        Process process = Runtime.getRuntime().exec(cmd);

        Scanner errorScanner = new Scanner(new InputStreamReader(process.getErrorStream()));
        final InputStream inStream = process.getInputStream();
        
        // Refer to this page for this thread:
        // http://stackoverflow.com/questions/7071115/redirecting-standard-input-output-error-streams-with-nets-process-class
        new Thread(new Runnable() {
            public void run() {
                ArrayList<Line> outputTimestampedLines = new ArrayList<Line>();
                InputStreamReader reader = new InputStreamReader(inStream);
                try {
                    int current;
                    while ((current = reader.read()) >= 0) {
                        char c = (char)current;
                        outputTimestampedLines.add(new Line(getTime(), c + ""));
                        
                        //String line = "[" + getTimeString() + "] " + c + " " + (int)c;
                        //System.out.println(line);
                    }
                } catch (Exception e) {}
                
                String[] output = assembleOutput(outputTimestampedLines, inputTimestampedLines);
                
                for (int i = 0; i < output.length; i++) {
                    System.out.println(output[i]);
                }
                
                CompareResult(output);
            }
        }).start();

        // Send test case input lines to the program being tested.
        PrintWriter pWriter = new PrintWriter(process.getOutputStream());
        
        boolean bCommented = false; // true means we are in a comment section.
        for (int i = 1; i < inputLines.size(); i++) {
			String line  = inputLines.get(i);
            
            // Skip comments
            if (line.indexOf("//") >= 0) {
                line = line.substring(0, line.indexOf("//"));
            }
            else if (line.indexOf("/*") >= 0) {
                line = line.substring(0, line.indexOf("/*"));
                bCommented = true;
                continue;
            }
            else if (line.indexOf("*/") >= 0) {
                line = line.substring(line.indexOf("*/") + 2);
                bCommented = false;
                continue;
            }
            else if (bCommented) {
                continue;
            }
                 
            // Skip empty lines
            line = line.trim();
            if (line.length() == 0)
                continue;
            
            // We can add an extra pause in the test case file by adding a line like:
            // __pause__ 1000
            // It will cause a pause of 1000ms.
            if (line.startsWith("__pause__")) {
                int ms = Integer.parseInt(line.substring(line.indexOf(" ") + 1).trim());
                try {
                    Thread.sleep(ms);
                } catch (Exception e) {}
                
                continue;
            }
            
            if (line.startsWith("__global_pause__")) {
                Input_Delay = Integer.parseInt(line.substring(line.indexOf(" ") + 1).trim());
                continue;
            }

            // We add a default sleep. The duration of this sleep should be long enough 
            // so that the output for the previous input has finished. 
            // This way we can later organize the input and output in the correct order.
            
            // Currently this only works for Java, so we don't need this for C.
            if (_language == Language.Java) {
                try {
                    Thread.sleep(Input_Delay);
                } catch (Exception e) {}
            }
            
            // We must flush each input immediately, so that the output will be 
            // collected asap and have the most accurate timestamp.
            inputTimestampedLines.add(new Line(getTime(), line + ""));
            pWriter.println(line);
            pWriter.flush();
        }
        
        pWriter.close();
        
        // Add error output
        while (errorScanner.hasNextLine()) {
            String line = errorScanner.nextLine();
            System.err.println(line);
        }
    }

    // Insert input lines to the correct positions in the output lines. 
    // The position to insert was determined by timestamp.
    private static String[] assembleOutput(List<Line> outputLines, List<Line> inputLines) {
        //ArrayList<String> lines = new ArrayList<String>();
        String line = "";

        //boolean unprintedLine = false;
        for (int i = 0; i < outputLines.size(); i++) {
            line += ((Line)outputLines.get(i)).text;
            
            // The last line
            if (i == outputLines.size() - 1) {
                break;
            }

            // Now we have at least two items in outputLines

            // If time difference is too large, we consider it a line break, and possible
            // point to insert an input line.
            Date d1 = ((Line)outputLines.get(i)).timestamp;
            Date d2 = ((Line)outputLines.get(i + 1)).timestamp;
            long diff = d2.getTime() - d1.getTime();
            if (diff > Input_Delay / 2) {
                // At this point, the first item in inputLines should be inserted here, but 
                // we need to verify the timestamp first.
                if (((Line)inputLines.get(0)).timestamp.getTime() <= d1.getTime() ||
                    ((Line)inputLines.get(0)).timestamp.getTime() > d2.getTime()) {
                    // Something went wrong.
                    System.out.println("Cannot match input and output lines.");
                    System.out.println("The last output line: " + ((Line)outputLines.get(i)).text);
                    System.out.println("The last input line: " + ((Line)inputLines.get(0)).text);
                    break;
                }
                
                //line += ((Line)inputLines.get(0)).text + System.getProperty("line.separator");
                line += ((Line)inputLines.get(0)).text + "\n";
                inputLines.remove(0);
                continue;
            }
        }
        
        return line.replace("\r\n", "\n").split("\n");
    }

    // Compare output lines collected from the process to lines in a result file.
    private static void CompareResult(String[] lines) {
        Path path = Paths.get(_testResultFile);
        
        if (Files.exists(path)) { // Compare with the result file
            List<String> inputLines = null;
            try {
                inputLines = Files.readAllLines(path, _ENCODING);
            } catch (IOException e) {}
            
            System.out.println("===============================");
            
            boolean error = false;
            
            if (lines.length > inputLines.size()) {
                System.out.println("Error: Output is longer than expected!");
                error = true;
            }
            
            int i = 0;
            for (; i < inputLines.size(); i++) {
                String line = inputLines.get(i).trim();
                if (!line.equals(lines[i].trim()))
                    break;
            }
            if (i < inputLines.size()) {
                System.out.println("Error: A mismatch was found!");
                System.out.println("  Line number : " + i);
                System.out.println("  Expected    : " + inputLines.get(i));
                System.out.println("  Actual      : " + lines[i]);
                error = true;
            }
            
            if (!error) {
                System.out.println("No error.");
            }
            System.out.println("===============================");
        }
        else { // Write lines to the result file. Enable this section to create a result file.
            //try {
            //    Files.write(path, Arrays.asList(lines), _ENCODING);
            //} catch (IOException e) {}
        }
    }

    private static String getTimeString() {
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        return sdf.format(cal.getTime());
    }
    
    private static Date getTime() {
        Calendar cal = Calendar.getInstance();
        return cal.getTime();
    }
}
