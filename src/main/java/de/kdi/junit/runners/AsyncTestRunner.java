package de.kdi.junit.runners;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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
 * This test runner supports the normal junit annotations: {@link Before},
 * {@link After}, {@link BeforeClass}, {@link AfterClass}, {@link Ignore},
 * {@link Test}<br/>
 * <br/>
 * Note: The {@link Rule} annotation has not been tested together with this test
 * runner.<br/>
 * 
 * @author Dan Häberlein
 * 
 */
public class AsyncTestRunner extends Runner {

	/**
	 * Default timeout duration.
	 */
	private static final int DEFAULT_TIMEOUT = 3000;

	private Map<Class<? extends Annotation>, Method> junitBasicAnnotationMap = new HashMap<Class<? extends Annotation>, Method>();
	private int timeout;
	private Description rootDescription;
	List<Method> testMethods = new ArrayList<Method>();

	private final Class<?> testClass;
	private Object testClassInstance;

	public AsyncTestRunner(java.lang.Class<?> testClass) {
		this.testClass = testClass;
		Method[] classMethods = testClass.getDeclaredMethods();
		rootDescription = Description.createSuiteDescription(testClass.getName(), testClass.getAnnotations());
		extractTestMethods(classMethods);
		sortTestMethods();
	}

	private void sortTestMethods() {
		Collections.sort(testMethods, new AscMethodNameComparator());
		Collections.sort(rootDescription.getChildren(), new AscDescriptionComparator());
	}

	static class AscDescriptionComparator implements Comparator<Description> {

		@Override
		public int compare(Description comparable1, Description comparable) {
			return new StringComparator().compare(comparable1.getMethodName(), comparable.getMethodName());
		}
		
	}
	
	static class AscMethodNameComparator implements Comparator<Method> {

		@Override
		public int compare(Method comparable1, Method comparable2) {
			String comparableMethodName1 = comparable1.getName();
			String comparableMethodName2 = comparable2.getName();
			return new StringComparator().compare(comparableMethodName1, comparableMethodName2);
		}

	}
	
	static class StringComparator implements Comparator<String> {

		@Override
		public int compare(String comparable1, String comparable2) {
			return comparable1.compareTo(comparable2);
		}
		
	}

	private void extractTestMethods(Method[] classMethods) {
		for (Method currentMethod : classMethods) {
			Class<?> retClass = currentMethod.getReturnType();
			int length = currentMethod.getParameterTypes().length;
			int modifiers = currentMethod.getModifiers();
			boolean isWellformedTestMethod = retClass != null && length == 0 && !Modifier.isStatic(modifiers) && Modifier.isPublic(modifiers)
					&& !Modifier.isInterface(modifiers) && !Modifier.isAbstract(modifiers);
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
			rootDescription.addChild(Description.createTestDescription(method.getClass(), method.getName()));
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
		if (Modifier.isPublic(modifiers) && Modifier.isStatic(modifiers)) {
			if (currentMethod.getAnnotation(BeforeClass.class) != null) {
				junitBasicAnnotationMap.put(BeforeClass.class, currentMethod);
			}
			if (currentMethod.getAnnotation(AfterClass.class) != null) {
				junitBasicAnnotationMap.put(AfterClass.class, currentMethod);
			}
		}
	}

	@Override
	public Description getDescription() {
		return rootDescription;
	}

	@Override
	public void run(RunNotifier runNotifier) {
		callBeforeClass();
		for (int i = 0; i < testMethods.size(); i++) {
			Method method = testMethods.get(i);
			Description currentTestMethodDescription = rootDescription.getChildren().get(i);
			Result result = new Result();
			RunListener listener = result.createListener();
			try {
				startTest(runNotifier, currentTestMethodDescription, listener);	// needed to be started already here for retrieving failures out of before method!
				testClassInstance = tryToCreateTestClassInstance();
				invokeJunitMethod(testClassInstance, Before.class);
				runTest(runNotifier, method, currentTestMethodDescription, result);
			} catch (IllegalArgumentException e) {
				runNotifier.fireTestFailure(new Failure(currentTestMethodDescription, new IllegalArgumentException(
						"no parameters for test methods are supported", e)));
			} catch (InvocationTargetException e) {
				checkForExpectedFailure(runNotifier, method, currentTestMethodDescription, e);
			} finally {
				runNotifier.removeListener(listener);
				try {
					if (testClassInstance != null)
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

	private Object tryToCreateTestClassInstance() throws InvocationTargetException {
		try {
			return testClass.newInstance();
		} catch (Exception e){
			throw new InvocationTargetException(e);
		}
	}

	private void runTest(RunNotifier runNotifier, Method method, Description currentTestMethodDescription, Result result)
			throws InvocationTargetException {
		try {
			setTimeoutForMethod(method);
			// Prepare Thread monitoring
			ThreadDifferenceMonitor monitor = new ThreadDifferenceMonitor();
			Thread monitoringThread = new Thread(monitor);
			monitoringThread.start();
			Thread.sleep(10);
			// Invoke test method
			method.invoke(testClassInstance);
			Thread.sleep(10);
			monitoringThread.interrupt();
			monitoringThread.join();
			// check for still running threads
			Set<Thread> createdThreads = monitor.getCreatedThreads();
			for (Thread currentThread : createdThreads) {
				waitForFinishingThreads(currentThread);
			}
			checkForRecoredExceptions(monitor);
			// publish test results
			runNotifier.fireTestRunFinished(result);
			runNotifier.fireTestFinished(currentTestMethodDescription);
		} catch (InvocationTargetException e) {
			throw e;
		} catch (Throwable e) {
			throw new InvocationTargetException(e);
		}
	}

	private void startTest(RunNotifier runNotifier, Description currentTestMethodDescription, RunListener listener) {
		runNotifier.addFirstListener(listener);
		runNotifier.fireTestStarted(currentTestMethodDescription);
		runNotifier.addListener(listener);
	}

	private void setTimeoutForMethod(Method method) {
		ThreadShutdownTimeout annotatedValue = method.getAnnotation(ThreadShutdownTimeout.class);
		timeout = annotatedValue == null ? DEFAULT_TIMEOUT : annotatedValue.value();
	}

	private void waitForFinishingThreads(Thread currentThread) throws InvocationTargetException {
		try {
			long startTime = System.currentTimeMillis();
			boolean hasTimeouted = false;
			boolean hasStillRunningThreads = false;
			while (currentThread.isAlive() && !currentThread.isDaemon() && !hasTimeouted) {
				Thread.sleep(100);
				hasTimeouted = System.currentTimeMillis() - startTime > timeout;
				if (hasTimeouted) {
					System.err.println("[ERROR] " + currentThread.toString() + " is still running! Timeout exceeded...");
					hasStillRunningThreads = true;
				}
			}
			if (hasStillRunningThreads) {
				throw new ThreadsStillAliveException("Threre are threads still alive (" + currentThread + ")");
			}
		} catch (Exception e) {
			throw new InvocationTargetException(e);
		}
	}

	private void checkForRecoredExceptions(ThreadDifferenceMonitor monitor) throws Throwable {
		Map<Long, Throwable> threadIdToExceptions = monitor.getThreadIdAndCorrespondingException();
		switch (threadIdToExceptions.size()) {
			case 1:
				throw threadIdToExceptions.values().iterator().next();
			default:
				throw new AsynchronousTestRunnerException(threadIdToExceptions);
			case 0:
		}
	}

	private void invokeJunitMethod(Object testClassInstance, Class<? extends Annotation> key) throws InvocationTargetException {
		if (junitBasicAnnotationMap.containsKey(key)) {
			Method method = junitBasicAnnotationMap.get(key);
			try {
				method.invoke(testClassInstance);
			} catch (Exception e) {
				throw new InvocationTargetException(e);
			}
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
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}