package de.kdi.junit.runners;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AsyncTestRunner.class)
public class TestAsyncTestRunnerUsingBadConstructor {

	private String notAllowedParameter;

	public TestAsyncTestRunnerUsingBadConstructor(String notAllowedParameter) {
		this.notAllowedParameter = notAllowedParameter;
	}
	
	@Test(expected=java.lang.InstantiationException.class)
	public void testShouldFail() throws Exception {
		System.out.println("hallo?");
	}
}
