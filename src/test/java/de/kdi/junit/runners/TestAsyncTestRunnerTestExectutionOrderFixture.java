package de.kdi.junit.runners;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AsyncTestRunner.class)
public class TestAsyncTestRunnerTestExectutionOrderFixture {
	
	@Test
	public void e(){}
	
	@Test
	public void b() {}
	
	@Test 
	public void a(){}
	
	@Test 
	public void c(){}
	
	@Test 
	public void d(){}
}
