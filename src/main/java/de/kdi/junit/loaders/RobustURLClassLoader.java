package de.kdi.junit.loaders;

import java.net.URL;
import java.net.URLClassLoader;


/**
 * Classloader that will not throw an exception when it's not possible to load a class.<br/> 
 * Useful for test cases with overlapping classnames.<br/>
 * Usage: <br/>
 * 	<p>
 * 	<i>Thread.currentThread().setContextClassLoader(instance)</i>
 * </p>
 * 
 * @author Dan Häberlein
 *
 */
public class RobustURLClassLoader extends URLClassLoader {

	public RobustURLClassLoader(URL[] urls) {
		super(urls);
	}
	
	@Override
	public Class<?> loadClass(String name) throws ClassNotFoundException {
		Class<?> result = Object.class; 
		try {
			result = super.loadClass(name);
		} catch (ClassNotFoundException e){
			System.err.println("Can't load class: " + e.getMessage());
		}
		return result;
	}

}
