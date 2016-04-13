import java.util.Scanner;

public class EmployeeDriver
{

    /*  public static int menu(String... options)
        {
        int choice;
        do
        {
        for(int line = 0; line < options.length; line++)
        System.out.printf("%d. %s\n", line+1,options[line]);

        System.out.println("\nEnter Choice: ");
        choice = in.nextInt();
        }while(choice > 0 && choice <= options.length);

        return choice;
        }*/

    public static void main(String args[])
    {
        int mainInput;//Input for main menu
        int subInput1;//Input for submenu
        int subInput2;//Input for sub-submenu
        int en;         //Inputting an employee number
        int index;
        double amount;
        EmployeeManager em = new EmployeeManager(); //The EmployeManager object
        Scanner in = new Scanner(System.in);

        //Main control loop, keep coming back to the
        //Main menu after each selection is finished
        do
        {
            do
            {
                //This is the main menu. Displays menu 
                //and asks for a choice, validaties that
                //what is entered is a valid choice
                System.out.println("\n\nMain Menu\n");
                em.listAll();
                System.out.println("\n1. Employee Submenu");
                System.out.println("2. Add Employee");
                System.out.println("3. Remove Employee");
                System.out.println("4. Sort Employees");
                System.out.println("5. Calculate Weekly Payout");
                System.out.println("6. Calculate Bonus");
                System.out.println("7. Annual Raises");
                System.out.println("8. Reset Week");
                System.out.println("9. Quit");

                System.out.print("\nEnter Choice: ");
                mainInput = in.nextInt();
                if(mainInput < 1 || mainInput > 9)
                {
                    System.out.println("Invalid Choice! Choose Again.");
                }
            }while(mainInput < 1 || mainInput > 9);

            //Perform the correct action based upon Main menu input
            switch(mainInput)
            {
                //Employee Submenu
                case 1:
                    do
                    {
                        do
                        {
                            System.out.println("\n1. Hourly Employees");
                            System.out.println("2. Salary Employees");
                            System.out.println("3. Commission Employees");
                            System.out.println("4. Back");
                            System.out.print("\nEnter Choice: ");

                            subInput1 = in.nextInt();
                            if(subInput1 < 1 || subInput1 > 4)
                            {
                                System.out.println("Invalid Choice! Choose Again.");
                            }
                        }while(subInput1 < 1 || subInput1 > 4);

                        switch(subInput1)
                        {
                            case 1:
                                do
                                {
                                    do
                                    {
                                        System.out.println();
                                        em.listHourly();
                                        System.out.println("1. Add Hours");
                                        System.out.println("2. Back");
                                        System.out.print("\nEnter Choice: ");
                                        subInput2 = in.nextInt();
                                        if(subInput2 < 1 || subInput2 > 2)
                                        {
                                            System.out.println("Invalid Choice! Choose Again.");
                                        }
                                    }while(subInput2 < 1 || subInput2 > 2);

                                    if( subInput2 == 1)
                                    {
                                        System.out.print("\nEmployee Number: ");
                                        en = in.nextInt();
                                        index = em.getIndex(en);
                                        if(index != -1)
                                        {
                                            System.out.print("Enter Hours: ");
                                            amount = in.nextDouble();
                                            em.increaseHours(index, amount);
                                        }
                                        else
                                        {
                                            System.out.println("Employee not found!");
                                        }

                                    }
                                }while(subInput2 != 2);
                                break;

                            case 2:
                                do
                                {
                                    System.out.println();
                                    em.listSalary();
                                    System.out.println("1. Back");
                                    System.out.print("\nEnter Choice: ");
                                    subInput2 = in.nextInt();
                                    if(subInput2 != 1)
                                    {
                                        System.out.println("Invalid Choice! Choose Again.");
                                    }
                                }while(subInput2 != 1);
                                break;

                            case 3:
                                do
                                {
                                    do
                                    {
                                        System.out.println();
                                        em.listCommission();
                                        System.out.println("1. Add Sales");
                                        System.out.println("2. Back");
                                        System.out.print("\nEnter Choice: ");
                                        subInput2 = in.nextInt();
                                        if(subInput2 < 1 || subInput2 > 2)
                                        {
                                            System.out.println("Invalid Choice! Choose Again.");
                                        }
                                    }while(subInput2 < 1 || subInput2 > 2);

                                    if( subInput2 == 1)
                                    {
                                        System.out.println("Employee Number: ");
                                        en = in.nextInt();
                                        index = em.getIndex(en);
                                        if(index != -1)
                                        {
                                            System.out.print("Enter Sales: ");
                                            amount = in.nextDouble();
                                            em.increaseSales(index, amount);
                                        }
                                        else
                                        {
                                            System.out.println("Employee not found!");
                                        }

                                    }
                                }while(subInput2 != 2);
                                break;
                        }
                    }while(subInput1 != 4);
                    break;

                    //Add Employee
                case 2:
                    String fn, ln;
                    char mi, g, f;
                    boolean ft = true;
                    do
                    {
                        System.out.println("\n1. Hourly");
                        System.out.println("2. Salary");
                        System.out.println("3. Commission");
                        System.out.print("\nEnter Choice: ");
                        subInput1 = in.nextInt();

                        if(subInput1 < 1 || subInput1 > 3)
                        {
                            System.out.println("Invalid Choice! Choose again.");
                        }
                    }while(subInput1 < 1 || subInput1 > 3);

                    System.out.print("\nEnter Last Name: ");
                    ln = in.next();
                    System.out.print("Enter First Name: ");
                    fn = in.next();
                    System.out.print("Enter Middle Initial: ");
                    mi = in.next().charAt(0);
                    System.out.print("Enter Gender: ");
                    g = in.next().charAt(0);
                    System.out.print("Enter Employee Number: ");
                    en = in.nextInt();
                    System.out.print("Full Time? (y/n): ");
                    f = in.next().charAt(0);
                    if(f == 'n' || f == 'N')
                    {
                        ft = false;
                    }

                    if(subInput1 == 1)
                    {
                        System.out.print("Enter wage: ");
                    }
                    else if(subInput1 == 2)
                    {
                        System.out.print("Enter salary: ");
                    }
                    else
                    {
                        System.out.print("Enter rate: ");
                    }
                    amount = in.nextDouble();

                    em.addEmployee(subInput1, fn , mi, ln, g, en, ft, amount);
                    em.removeRedundancies();
                    break;

                    //Remove Employee
                case 3:
                    System.out.print("Enter Employee Number to Remove: ");
                    en = in.nextInt();
                    index = em.getIndex(en);
                    if(index != -1)
                    {
                        em.removeEmployee(index);
                    }
                    break;

                    //Sort Employees
                case 4:
                    do
                    {
                        System.out.println("\n1. Sort by Employee Number");
                        System.out.println("2. Sort by Name");
                        System.out.print("\nEnter Choice: ");
                        subInput1 = in.nextInt();
                        if(subInput1 < 1 || subInput1 > 2)
                        {
                            System.out.println("Invalid Input! Choose again.");
                        }
                    }while(subInput1 < 1 || subInput1 > 2);

                    if(subInput1 == 1)
                    {
                        em.sortNumber();
                    }
                    else
                    {
                        em.sortName();
                    }
                    break;

                    //Calculate Weekly Payout
                case 5:
                    System.out.printf("Total weekly payout is %.2f\n", em.calculatePayout());
                    break;

                    //Calculate Bonus
                case 6:
                    amount = em.holidayBonuses();
                    System.out.printf("Total holiday bonus payout is %.2f\n", amount);
                    break;
                    //Apply Annual Raises
                case 7:
                    em.annualRaises();
                    System.out.println("Annual Raises applied.");
                    break;

                    //Reset the weeks values
                case 8:
                    em.resetWeek();
                    System.out.println("Weekly values reset.");
                    break;

                    //Exit
                case 9:
                    System.out.println("\nThank you for using the Employee Manager!\n");
            }
        }while(mainInput != 9);
    }
}

