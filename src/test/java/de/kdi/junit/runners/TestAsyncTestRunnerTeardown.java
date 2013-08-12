package de.kdi.junit.runners;

import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;


@RunWith(AsyncTestRunner.class)
public class TestAsyncTestRunnerTeardown {
	
	private static boolean teardownWasCalled;
	
	@Test(expected=RuntimeException.class)
	public void testTeardownIsCalledWhenAnExceptionIsThrown() throws Exception {
		throw new RuntimeException("Expected!");
	}
	
	@Test
	public void testTeardownWasCalled(){
		assertTrue(teardownWasCalled);
	}
	
	@After
	public void teardown(){
		teardownWasCalled = true;
	}
	
	
}
