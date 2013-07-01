package de.kdi.junit.loaders;

import static org.junit.Assert.*;

import java.io.File;
import java.net.URL;
import java.util.Collection;

import org.junit.Test;

public class TestClassPathExtractor {

	@Test
	public void testSimpleClassPathExtraction() throws Exception {
		String fixture = createFixture();
		ClassPathExtractor testable = new ClassPathExtractor(fixture);
		Collection<URL> result = testable.createUrlClassPath();
		assertEquals(2, result.size());
	}

	private String createFixture() {
		StringBuilder fixtureBuilder = new StringBuilder();
		fixtureBuilder.append(new File(".").toString());
		fixtureBuilder.append(ClassPathExtractor.CLASSPATH_ENTRY_SEPARATOR);
		fixtureBuilder.append(new File("..").toString());
		return fixtureBuilder.toString();
	}
	
	@Test
	public void testCPAccessorConstant() throws Exception {
		assertEquals("java.class.path", ClassPathExtractor.JAVA_CLASS_PATH_PROP_ACCESSOR);
	}
}
