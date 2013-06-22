package de.kdi.junit.runners.exception;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;
import java.util.Map.Entry;


public class AsynchronousTestRunnerException extends Exception {

	private static final long serialVersionUID = 1L;
	
	private Map<Long, Throwable> threadIdAndCorrespondingException;

	public AsynchronousTestRunnerException(Map<Long, Throwable> threadIdAndCorrespondingException) {
		this.threadIdAndCorrespondingException = threadIdAndCorrespondingException;
	}
	
	@Override
	public String getMessage() {
		StringBuilder resultBuilder = new StringBuilder();
		for(Entry<Long, Throwable> currentEntry : threadIdAndCorrespondingException.entrySet()){
			resultBuilder.append("Exception recoreded in Thread with ID = ");
			resultBuilder.append(currentEntry.getKey());
			resultBuilder.append("\nmessage was: ");
			Throwable currentThrowable = currentEntry.getValue();
			resultBuilder.append(currentThrowable);
			resultBuilder.append("\nStacktrace:\n");
			StringWriter sw = new StringWriter();
			currentThrowable.printStackTrace(new PrintWriter(sw));
			resultBuilder.append(sw.toString());
			resultBuilder.append("\n");
		}
		return resultBuilder.toString();
	}
	
}
