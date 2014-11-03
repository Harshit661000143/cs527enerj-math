package test;
import enerj.lang.*;
public class Junit {

	public String concatenate(String one, String two)
	{
		return one + two;
		
	}
	public double multiply(@Approx double number1, @Approx double number2)
	{
		return Endorsements.endorse(number1*number2);
	}
}
