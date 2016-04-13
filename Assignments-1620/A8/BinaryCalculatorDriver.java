import javax.swing.JFrame;

public class BinaryCalculatorDriver {

	public static void main(String[] args)
	{
		BinaryCalculatorFrame calcFrame = new BinaryCalculatorFrame();
		calcFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		calcFrame.setSize(400, 100);
		calcFrame.setResizable(false);
		calcFrame.setVisible(true);
	}

}
