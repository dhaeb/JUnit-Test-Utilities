package de.kdi.junit.runners;

import static org.junit.Assert.*;

import java.lang.reflect.Method;
import java.util.List;

import org.junit.Test;
import org.junit.runner.Description;

public class TestAsyncTestRunnerTestExectutionOrder {

	@Test
	public void testOrderOfTests() throws Exception {
		AsyncTestRunner asyncTestRunner = new AsyncTestRunner(TestAsyncTestRunnerTestExectutionOrderFixture.class);
		Description mainTestDescription = asyncTestRunner.getDescription();
		List<Method> testMethods = asyncTestRunner.testMethods;
		for (int i = 0; i < testMethods.size(); i++) {
			Method currentMethod = testMethods.get(i);
			Description currentMethodDescription = mainTestDescription.getChildren().get(i);
			assertMethodEqualsDescription(currentMethod, currentMethodDescription);
		}
	}

	private void assertMethodEqualsDescription(Method m, Description currentMethodDescription) {
		assertEquals(m.getName(), currentMethodDescription.getMethodName());
	}
	
	
}
