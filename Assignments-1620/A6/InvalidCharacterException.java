public class InvalidCharacterException extends Exception
{
	public InvalidCharacterException()
	{
		this("NONE");
	}

	public InvalidCharacterException(String string)
	{
		super("Invalid character: " + string);
	}

	public InvalidCharacterException(char character)
	{
		super("Invalid character: " + character);
	}
}
