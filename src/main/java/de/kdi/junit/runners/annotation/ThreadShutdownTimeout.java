package de.kdi.junit.runners.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * Annotation designed for test methods to specify the maximum shutdown time a <br/>
 * thread should have after finishing the invocation of the test method.
 *  
 * The default timeout duration is set in: {@link de.kdi.junit.runners.AsyncTestRunner}
 *  
 * @author Dan Häberlein
 *
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ThreadShutdownTimeout {
	int value();
}
