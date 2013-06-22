package de.kdi.junit.runners;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(BaseTestRunner.class)
public class TestBaseTestRunner {

	private boolean isSetupCalled;
	private static boolean IS_TEARDOWN_CALLED;
	private static boolean BEFORE_CLASS_EXECUTED;
	
	@BeforeClass
	public static void testBeforeClass(){
		BEFORE_CLASS_EXECUTED = true;
	}
	
	@Before
	public void setup(){
		isSetupCalled = true;
	}
	
	@After
	public void teardown(){
		IS_TEARDOWN_CALLED = true;
	}
	
	@Test(expected=AssertionError.class)
	public void testAbilityOfExpectedExceptions(){
		fail("the runner should be expecting this error");
	}
	
	@Test
	public void testIsTeardownCalled() throws Exception {
		assertTrue(IS_TEARDOWN_CALLED);
	}
	
	@Test
	public void testNormal() throws Exception {}
	
	@Test
	public void testIsSetupCalled() throws Exception {
		assertTrue(isSetupCalled);
	}
	
	@Test
	public void beforeClassWasCalled(){
		assertTrue(BEFORE_CLASS_EXECUTED);
	}

}
