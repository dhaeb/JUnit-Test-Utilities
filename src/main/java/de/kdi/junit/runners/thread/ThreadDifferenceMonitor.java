package de.kdi.junit.runners.thread;

import gnu.trove.set.hash.THashSet;

import java.util.Map;
import java.util.Set;

public class ThreadDifferenceMonitor implements Runnable {

	private ThreadMonitor monitor;
	private Set<Thread> createdThreads;
	private MappingUncaughtExceptionHandler exeptionHandler;

	public ThreadDifferenceMonitor() {
		monitor = new ThreadMonitor();
		createdThreads = new THashSet<Thread>();
		exeptionHandler = new MappingUncaughtExceptionHandler();
		Thread.setDefaultUncaughtExceptionHandler(exeptionHandler);
	}

	@Override
	public void run() {
		while (!Thread.currentThread().isInterrupted()) {
			Set<Thread> newThreads = monitor.getDifference(new THashSet<Thread>(Thread.getAllStackTraces().keySet()));
			createdThreads.addAll(newThreads);
		}
	}

	public Set<Thread> getCreatedThreads() {
		synchronized (createdThreads) {
			return createdThreads;
		}
	}

	public Map<Long, Throwable> getThreadIdAndCorrespondingException() {
		synchronized (exeptionHandler) {
			return exeptionHandler.getThreadIdToException();
		}
	}

}
