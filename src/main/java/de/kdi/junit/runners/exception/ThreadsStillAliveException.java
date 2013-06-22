package de.kdi.junit.runners.exception;

public class ThreadsStillAliveException extends Exception {
	
	private static final long serialVersionUID = 1L;
	
	public ThreadsStillAliveException(String reason) {
		super(reason);
	}
}
