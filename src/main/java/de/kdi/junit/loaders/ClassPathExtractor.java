package de.kdi.junit.loaders;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ClassPathExtractor {


	static final String JAVA_CLASS_PATH_PROP_ACCESSOR = "java.class.path";
	static final String CLASSPATH_ENTRY_SEPARATOR = System.getProperty("path.separator");
	
	private String classPath;

	public ClassPathExtractor() {
		this(System.getProperty(JAVA_CLASS_PATH_PROP_ACCESSOR));
	}

	ClassPathExtractor(String classPathAsString) {
		this.classPath = classPathAsString;
	}

	public Collection<URL> createUrlClassPath() throws IOException {
		String[] split = classPath.split(CLASSPATH_ENTRY_SEPARATOR);
		List<URL> result = new ArrayList<URL>();
		for (String currentPathAsString : split) {
			result.add(new File(currentPathAsString).getCanonicalFile().toURI().toURL());
		}
		return result;
	}

}