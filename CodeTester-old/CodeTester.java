/*
Mai Ren
2014-09-23

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
import java.util.Scanner;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class CodeTester {
    private static Charset _ENCODING = StandardCharsets.UTF_8;
    private static String _testCaseFile;
    private static String _testResultFile;
    
    public static void main(String[] args) throws IOException {
        // Read test case
        _testCaseFile = args[0];
        _testResultFile = _testCaseFile + "Result";
        Path path = Paths.get(_testCaseFile);
        List<String> inputLines = null;
        
        try {
            inputLines = Files.readAllLines(path, _ENCODING);
        } catch (IOException e) {}
        
        // Launch the program being tested, read and print its output from a separate thread
        Process cmd = Runtime.getRuntime().exec("java " + inputLines.get(0));

        final InputStream inStream = cmd.getInputStream();
        new Thread(new Runnable() {
            public void run() {
                InputStreamReader reader = new InputStreamReader(inStream);
                Scanner scan = new Scanner(reader);
                ArrayList<String> lines = new ArrayList<String>();
                while (scan.hasNextLine()) {
                    String line = scan.nextLine();
                    lines.add(line);
                    System.out.println(line);
                }
                CompareResult(lines.toArray(new String[lines.size()]));
            }
        }).start();

        // Send test case input lines to the program being tested.
        OutputStream outStream = cmd.getOutputStream();
        PrintWriter pWriter = new PrintWriter(outStream);
        
        for (int i = 1; i < inputLines.size(); i++) {
			String line  = inputLines.get(i);
			if (!line.startsWith("//") && line.trim().length() > 0) {
				pWriter.println(line);
			}
        }
        
        pWriter.flush();
        pWriter.close();
    }
    
    // Compare output lines collected from the process to lines in a result file.
    private static void CompareResult(String[] lines) {
        Path path = Paths.get(_testResultFile);
        
        if (Files.exists(path)) { // Compare with the result file
            List<String> inputLines = null;
            try {
                inputLines = Files.readAllLines(path, _ENCODING);
            } catch (IOException e) {}
            
            System.out.println("==============================");
            
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
            System.out.println("==============================");
        }
        else { // Write lines to the result file. Enable this section to create a result file.
            //try {
            //    Files.write(path, Arrays.asList(lines), _ENCODING);
            //} catch (IOException e) {}
        }
    }
}