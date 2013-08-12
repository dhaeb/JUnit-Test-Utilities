package de.kdi.junit.runners;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import de.kdi.junit.runners.annotation.ThreadShutdownTimeout;
import de.kdi.junit.runners.exception.AsynchronousTestRunnerException;
import de.kdi.junit.runners.exception.ThreadsStillAliveException;

@RunWith(AsyncTestRunner.class)
public class TestAsyncTestRunner {

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

	@Test(expected=RuntimeException.class)
	public void testMultipleThreads() throws Exception {
		new Thread(){
			@Override
			public void run() {
				throw new RuntimeException("got that?");
			}
		}.start();
	}

	@Test(expected=AsynchronousTestRunnerException.class) // will be thrown if more than one threads are throwing an exception
	public void testMultipleThreadsAndExceptions() throws Exception {
		new Thread(){
			@Override
			public void run() {
				throw new RuntimeException("got that?");
			}
		}.start();
		new Thread(){
			@Override
			public void run() {
				throw new IllegalArgumentException("got that?");
			}
		}.start();
	}
	
	@Test(expected=ThreadsStillAliveException.class)  // will be thrown if one or more threads are still alive after the invocation of the test method 																		
	public void testMultipleThreadsAndDuration() throws Exception {
		new Thread(){
			@Override
			public void run() {
				try {
					Thread.sleep(5000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				throw new IllegalArgumentException("this exception should not be thrown due to timeout");
			}
		}.start();
	}
	
	@ThreadShutdownTimeout(10000)
	@Test(expected=AsynchronousTestRunnerException.class)  // will be thrown if one or more threads are still alive after the invocation of the test method 
	public void testMultipleThreadsAndDurationWithTSTAnnotation() throws Exception {
		new Thread(){
			@Override
			public void run() {
				try {
					Thread.sleep(5000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				throw new IllegalArgumentException("this exception should be thrown due higher timeout");
			}
		}.start();
	}

}
