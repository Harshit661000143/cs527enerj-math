package test;

import static org.junit.Assert.*;
import enerj.lang.*;
import org.junit.Test;

public class MultiplyTest {

	@Test
	public void testMultiply() {
		
		Junit test = new Junit();
                @Approx double a=3;
		@Approx double b=4;
		double result= test.multiply(a, b);
		assertEquals(12, result,0);
	}

}
