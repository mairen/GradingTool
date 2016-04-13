public class EmployeeTest {
	public static void main(String args[]) {
		//Create references for our objects
		HourlyEmployee hourly;
		SalaryEmployee salary;
		CommissionEmployee commission;

		//The sum of all bonuses will be placed here
		double bonusPayout = 0;

		//Create and print a new HourlyEmployee
		hourly = HourlyFactory();
		System.out.printf("\n%s\n", hourly);

		//Create and print a new SalaryEmployee
		salary = SalaryFactory();
		System.out.printf("\n%s\n", salary);

		//Create and print a new CommissionEmployee
		commission = CommissionFactory();
		System.out.printf("\n%s\n", commission);

		//Alter some values of Hourly and Commission
		System.out.println("Increasing Hourly's hours worked by 50.");
		hourly.increaseHours(50);

		System.out.println("Increasing Commissions's sales by 150,000.\n");
		commission.increaseSales(150000);

		//Output weekly pay for each Employee
		System.out.printf("Hourly Payout: %.2f\n", hourly.calculateWeeklyPay());
		System.out.printf("Salary Payout: %.2f\n", salary.calculateWeeklyPay());
		System.out.printf("Commission Payout: %.2f\n\n", commission.calculateWeeklyPay());

		//Find total bonus payout
		System.out.println("Finding total bonus payout...");
		bonusPayout += hourly.holidayBonus();
		bonusPayout += salary.holidayBonus();
		bonusPayout += commission.holidayBonus();

		System.out.printf("Bonus Payout is %.2f\n\n", bonusPayout);

		//Reset the week and apply raises to all Employees
		System.out.println("Applying annual raises and resetting week...");
		hourly.resetWeek();
		hourly.annualRaise();
		salary.resetWeek();
		salary.annualRaise();
		commission.resetWeek();
		commission.annualRaise();

		//Output Employees after changes
		System.out.printf("%s\n%s\n%s\n", hourly, salary, commission);
	}

	//Retrieve input and create HourlyEmployee
	public static HourlyEmployee HourlyFactory() {
		System.out.println("Creating HourlyEmployee...");
		return new HourlyEmployee("Steve", "Rogers", 'A', 'M', 12345, true, 15.34);
	}

	//Retrieve input and create SalaryEmployee
	public static SalaryEmployee SalaryFactory() {
		System.out.println("Creating SalaryEmployee...");
		return new SalaryEmployee("Kitty", "Pryde", 'X', 'F', 54321, true, 75101.28);
	}

	//Retrieve input and create CommissionEmployee
	public static CommissionEmployee CommissionFactory() {
		System.out.println("Creating CommissionEmployee...");
		return new CommissionEmployee("Johnny", "Storm", 'F', 'L', 976499, false, 2.5);
	}
}