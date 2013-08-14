package de.kdi.junit.runners;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AsyncTestRunner.class)
public class TestAsyncTestRunnerUsingBadConstructor {


	/**
	 * @param notAllowedParameter  
	 */
	public TestAsyncTestRunnerUsingBadConstructor(String notAllowedParameter) {}
	
	@Test(expected=java.lang.InstantiationException.class)
	public void testShouldFail() throws Exception {
		System.out.println("hallo?");
	}
	
}
