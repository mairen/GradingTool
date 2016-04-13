import java.util.Scanner;

public class AccountDriver
{
    public static void main(String[] args)
    {
        Scanner in = new Scanner(System.in);
        String recordsFile;
        String transFile;

        AccountManager accts = new AccountManager();

        System.out.print("Enter name of records file: ");
        recordsFile = in.next();

        accts.readRecords(recordsFile);

        //Uncomment this to check that your readRecords
        //work properly
        //System.out.println("Current records are: ");
        //accts.printRecords();

        System.out.print("Enter name of transactions file: ");
        transFile = in.next();

        accts.updateTransactions(transFile);

        accts.printRecords();

        System.out.println("Writing records to file...");

        accts.writeRecords(recordsFile);
    }
}
