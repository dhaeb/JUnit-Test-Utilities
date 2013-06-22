package de.kdi.junit.runners;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.Runner;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;
import org.junit.runner.notification.RunNotifier;

import de.kdi.junit.runners.annotation.ThreadShutdownTimeout;
import de.kdi.junit.runners.exception.AsynchronousTestRunnerException;
import de.kdi.junit.runners.exception.ThreadsStillAliveException;
import de.kdi.junit.runners.thread.ThreadDifferenceMonitor;

/**
 * 
 * JUnit 4 test runner to test asynchrous code. <br/>
 * Uncaught exceptions of created threads in a test method <br/>
 * will be catched by this class and presented in a junit report. <br/>
 * Remember to use this class only for integration test purposes. <br/>
 * Asynchronous code could be tested much more convincing by using mocks.<br/>
 * <br/>
 * This test runner supports the normal junit annotations: 
 * {@link Before}, {@link After}, {@link BeforeClass}, {@link AfterClass}, {@link Ignore}, {@link Test}<br/> 
 * <br/>
 * The {@link Rule} annotation has not been tested together with this test runner.<br/>
 * @author Dan Häberlein
 *
 */
public class AsyncTestRunner extends Runner {

	/**
	 * Default timeout duration. 
	 */
	private static final int DEFAULT_TIMEOUT = 3000;
	
	private List<Method> testMethods = new ArrayList<Method>();
	private final Class<?> testClass;
	private Map<Class<? extends Annotation>, Method> junitBasicAnnotationMap = new HashMap<Class<? extends Annotation>, Method>();
	private int timeout;

	public AsyncTestRunner(java.lang.Class<?> testClass) {
		this.testClass = testClass;
		Method[] classMethods = testClass.getDeclaredMethods();
		extractTestMethods(classMethods);
	}

	private void extractTestMethods(Method[] classMethods) {
		for (Method currentMethod : classMethods) {
			Class<?> retClass = currentMethod.getReturnType();
			int length = currentMethod.getParameterTypes().length;
			int modifiers = currentMethod.getModifiers();
			boolean isWellformedTestMethod = retClass != null && 
											 length == 0 && 
											 !Modifier.isStatic(modifiers) && 
											 Modifier.isPublic(modifiers) && 
											 !Modifier.isInterface(modifiers) && 
											 !Modifier.isAbstract(modifiers);
			if (isWellformedTestMethod) {
				boolean usedForTests = addToMethodsWhenTestMethodAndNotIgnored(currentMethod);
				if (!usedForTests) {
					retreiveMethodWithAnnotation(currentMethod);
				}
			} else {
				retriveMethodWithStaticJunitAnnoation(currentMethod);
			}
		}
	}

	private boolean addToMethodsWhenTestMethodAndNotIgnored(Method method) {
		String methodName = method.getName();
		boolean isMethodIgnored = method.getAnnotation(Ignore.class) != null;
		boolean isTestMethod = methodName.toUpperCase().startsWith("TEST") || method.getAnnotation(Test.class) != null;
		boolean isUsedForTests = isTestMethod && !isMethodIgnored;
		if (isUsedForTests) {
			testMethods.add(method);
		}
		return isUsedForTests;
	}

	private void retreiveMethodWithAnnotation(Method currentMethod) {
		if (currentMethod.getAnnotation(Before.class) != null) {
			junitBasicAnnotationMap.put(Before.class, currentMethod);
		}
		if (currentMethod.getAnnotation(After.class) != null) {
			junitBasicAnnotationMap.put(After.class, currentMethod);
		}
	}

	private void retriveMethodWithStaticJunitAnnoation(Method currentMethod) {
		int modifiers = currentMethod.getModifiers();
		if(Modifier.isPublic(modifiers) && Modifier.isStatic(modifiers)) {
			if(currentMethod.getAnnotation(BeforeClass.class) != null){
				junitBasicAnnotationMap.put(BeforeClass.class, currentMethod);
			}
			if(currentMethod.getAnnotation(AfterClass.class) != null){
				junitBasicAnnotationMap.put(AfterClass.class, currentMethod);
			}
		}
	}

	@Override
	public Description getDescription() {
		Description spec = Description.createSuiteDescription(this.testClass.getName(), this.testClass.getAnnotations());
		return spec;
	}

	@Override
	public void run(RunNotifier runNotifier) {
		callBeforeClass();
		for (int i = 0; i < testMethods.size(); i++) {
			Method method = testMethods.get(i);
			Description spec = Description.createTestDescription(method.getClass(), method.getName());
			Result result = new Result();
			RunListener listener = result.createListener();
			runNotifier.addFirstListener(listener);
			Object testClassInstance = null;
				try {
					testClassInstance = testClass.newInstance();
				} catch (Exception e) {
					e.printStackTrace();
				}
				setTimeoutForMethod(method); 
				try {
					invokeJunitMethod(testClassInstance, Before.class);
					runNotifier.fireTestStarted(spec);
					runNotifier.addListener(listener);
					ThreadDifferenceMonitor monitor = new ThreadDifferenceMonitor();
					Thread monitoringThread = new Thread(monitor);
					monitoringThread.start();
					Thread.sleep(10);
					method.invoke(testClassInstance);
					Thread.sleep(10);
					monitoringThread.interrupt();
					Set<Thread> createdThreads = monitor.getCreatedThreads();
					for(Thread currentThread : createdThreads){
						waitForFinishingThreads(currentThread);
					}
					Map<Long, Throwable> threadIdToExceptions = monitor.getThreadIdAndCorrespondingException();
					switch(threadIdToExceptions.size()){
						case 1 :  throw new InvocationTargetException(threadIdToExceptions.values().iterator().next());
						default : throw new InvocationTargetException(new AsynchronousTestRunnerException(threadIdToExceptions));
						case 0:
					}
					runNotifier.fireTestRunFinished(result);
					runNotifier.fireTestFinished(spec);
				} catch (IllegalArgumentException e) {
					runNotifier.fireTestFailure(new Failure(spec, new IllegalArgumentException("no parameters for test methods are supported", e)));
				} catch (InvocationTargetException e) {
					checkForExpectedFailure(runNotifier, method, spec, e);
				} catch (Exception e) {
					runNotifier.fireTestFailure(new Failure(spec, e));
				} finally {
					runNotifier.removeListener(listener);
					try {
						invokeJunitMethod(testClassInstance, After.class);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			
		}
		callAfterClass();
	}
	
	private void callBeforeClass() {
		try {
			invokeJunitMethod(null, BeforeClass.class);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void setTimeoutForMethod(Method method) {
		ThreadShutdownTimeout annotatedValue = method.getAnnotation(ThreadShutdownTimeout.class);
		timeout = annotatedValue == null ? DEFAULT_TIMEOUT : annotatedValue.value();
	}

	private void waitForFinishingThreads(Thread currentThread) throws InterruptedException, InvocationTargetException {
		long startTime = System.currentTimeMillis();
		boolean hasTimeouted = false;
		boolean hasStillRunningThreads = false; 
		while(currentThread.isAlive() && !currentThread.isDaemon() && !hasTimeouted){
			Thread.sleep(100);
			hasTimeouted = System.currentTimeMillis() - startTime > timeout;
			if(hasTimeouted){
				System.err.println("[ERROR] " + currentThread.toString() + " is still running! Timeout exceeded...");
				hasStillRunningThreads = true;
			}
		}
		if(hasStillRunningThreads){
			throw new InvocationTargetException(new ThreadsStillAliveException("Threre are threads still alive (" + currentThread + ")"));
		}
	}

	private void invokeJunitMethod(Object testClassInstance, Class<? extends Annotation> key) throws IllegalAccessException, IllegalArgumentException,
			InvocationTargetException {
		if (junitBasicAnnotationMap.containsKey(key)) {
			Method method = junitBasicAnnotationMap.get(key);
			method.invoke(testClassInstance);
		}
	}

	private void checkForExpectedFailure(RunNotifier runNotifier, Method method, Description spec, InvocationTargetException e) {
		Throwable targetException = e.getTargetException();
		Test testAnnotation = method.getAnnotation(Test.class);
		if (testAnnotation == null) {
			runNotifier.fireTestFailure(new Failure(spec, targetException));
		} else {
			Class<? extends Throwable> expectedException = testAnnotation.expected();
			if (expectedException != null && expectedException.isInstance(targetException)) {
				runNotifier.fireTestFinished(spec);
			} else {
				runNotifier.fireTestFailure(new Failure(spec, targetException));
			}
		}
	}
	
	private void callAfterClass() {
		try {
			invokeJunitMethod(null, AfterClass.class);
		} catch (Exception e){
			e.printStackTrace();
		}
	}

}