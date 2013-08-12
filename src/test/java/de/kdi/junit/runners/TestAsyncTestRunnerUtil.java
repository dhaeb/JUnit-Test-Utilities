package de.kdi.junit.runners;

import static org.junit.Assert.*;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.junit.Test;


public class TestAsyncTestRunnerUtil {

	@Test
	public void testMethodNameComparator() throws Exception {
		Comparator<Method> methodNameComparator = new AsyncTestRunner.AscMethodNameComparator();
		Method methodA = getClass().getMethod("a");
		Method methodB = getClass().getMethod("b");
		assertEquals(-1, methodNameComparator.compare(methodA, methodB));
		assertEquals(0, methodNameComparator.compare(methodA, methodA));
		assertEquals(1, methodNameComparator.compare(methodB, methodA));
		List<Method> fixture = Arrays.asList(new Method[]{methodB, methodA});
		Collections.sort(fixture, methodNameComparator);
		assertEquals(methodA, fixture.get(0));
		assertEquals(methodB, fixture.get(1));
	}
	
	public void a() {}
	
	public void b() {}
	
}
