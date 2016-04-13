# GradingTool
A grading tool for programming assignments. It streamlines the process of compiling, testing, reviewing source code and giving feedback when grading submissions for an assignment. Currently it can handle both Java and C assignments.

![Screenshot](https://github.com/mairen/GradingTool/blob/master/Screenshots/1.png?raw=true)

This program itself is to show Java students what can be done with what they will learn in the Java course. 

This program was created in a simple text editor. This is to show Java students that it is possible to create a relatively complicated GUI program without using an IDE.

The documentation about how to use it is in the beginning of GradingTool.java. Currently I have uploaded configurations for some Java(1620) and C(2240) assignments. You may create your own test cases for them.

The GradingTool uses CodeTester to test target program, so CodeTester must be compiled first.

There are two CodeTesters: CodeTester and CodeTester-old.

CodeTester-old feeds input to the target program and collects output from the target program as fast as possible. However, it cannot simulate what a user would see when he/she was manually running the program, that is to have the input lines properly positioned in the terminal window. 

CodeTester, the new one, solves this problem by adding a timestamp to all input and output characters, and later uses this timestamp to reconstruct the proper input and output sequence, same as what a manual run would look like. The reason we need this is to auto-compare the result of auto-testing with the example run on the assignment sheet. The drawback of this solution is that a delay was added after feeding each input line to the target program, so that it has time to finish all output lines for this input, and if a test cast has many input lines, the test run may take too much time, so in this case you may want to switch back to use CodeTester-old if you only want to see the result.
