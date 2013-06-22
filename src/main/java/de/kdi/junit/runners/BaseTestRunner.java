package de.kdi.junit.runners;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.Runner;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;
import org.junit.runner.notification.RunNotifier;

public class BaseTestRunner extends Runner {

	private List<Method> testMethods = new ArrayList<Method>();
	private Map<Class<? extends Annotation>, Method> junitBasicAnnotationMap = new HashMap<Class<? extends Annotation>, Method>();
	private final Class<?> testClass;

	public BaseTestRunner(java.lang.Class<?> testClass) {
		this.testClass = testClass;
		Method[] classMethods = testClass.getDeclaredMethods();
		extractTestMethods(classMethods);
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
			Object testClassInstance;
			try {
				testClassInstance = testClass.newInstance();
				try {
					runJunitMethod(testClassInstance, Before.class);
					runNotifier.fireTestStarted(spec);
					runNotifier.addListener(listener);
					method.invoke(testClassInstance);
					runNotifier.fireTestRunFinished(result);
					runNotifier.fireTestFinished(spec);
				} catch (IllegalAccessException e) {
					runNotifier.fireTestFailure(new Failure(spec, e));
				} catch (IllegalArgumentException e) {
					runNotifier.fireTestFailure(new Failure(spec, new IllegalArgumentException("no parameters on test methods are supported", e)));
				} catch (InvocationTargetException e) {
					checkForExpectedFailure(runNotifier, method, spec, e);
				} finally {
					runNotifier.removeListener(listener);
					runJunitMethod(testClassInstance, After.class);
				}
			} catch (Exception e) {
				runNotifier.fireTestFailure(new Failure(spec, e));
			}
		}
		callAfterClass();
	}

	private void callBeforeClass() {
		try {
			runJunitMethod(null, BeforeClass.class);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void runJunitMethod(Object testClassInstance, Class<? extends Annotation> key) throws IllegalAccessException, IllegalArgumentException,
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
			runJunitMethod(null, AfterClass.class);
		} catch (Exception e){
			e.printStackTrace();
		}
	}

}