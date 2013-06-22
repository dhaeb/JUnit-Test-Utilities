package de.kdi.junit.runners.thread;

import gnu.trove.map.hash.THashMap;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.Map;


public class MappingUncaughtExceptionHandler implements UncaughtExceptionHandler {

	private Map<Long, Throwable> threadIdToException = new THashMap<Long, Throwable>();
	
	@Override
	public void uncaughtException(Thread t, Throwable e) {
		threadIdToException.put(Long.valueOf(t.getId()), e);
	}
	
	public Map<Long, Throwable> getThreadIdToException() {
		return threadIdToException;
	}
	
}
