package de.kdi.junit.runners.thread;

import static org.junit.Assert.assertTrue;

import java.util.Map;
import java.util.Set;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.carrotsearch.junitbenchmarks.BenchmarkOptions;
import com.carrotsearch.junitbenchmarks.BenchmarkRule;

public class TestThreadDifferenceMonitor {

	@Rule
	public TestRule benchmarkRun = new BenchmarkRule();

	@BenchmarkOptions(benchmarkRounds = 100, warmupRounds = 0)
	@Test
	public void testThreadMonitoring() throws Exception {
		Map<Long, Throwable> threadIdToThrowable = testDifferenceMonitor(new Thread() {

			public void run() {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		});
		assertTrue(threadIdToThrowable.isEmpty());
	}

	@BenchmarkOptions(benchmarkRounds = 1, warmupRounds = 0)
	@Test
	public void testTheadMonitoringWithExceptions() throws Exception {
		Map<Long, Throwable> threadIdToThrowable = testDifferenceMonitor(new Thread() {
			public void run() {
				throw new IllegalArgumentException("Fast enough?");
			}
		});
		System.out.println("map in test: " + threadIdToThrowable);
		assertTrue(threadIdToThrowable.size() == 1);
	}
	
	@Ignore //TODO Fix test!
	@BenchmarkOptions(benchmarkRounds = 1, warmupRounds = 0)
	@Test
	public void testTheadMonitoringWithExceptionsAndSleep() throws Exception {
		Map<Long, Throwable> threadIdToThrowable = testDifferenceMonitor(new Thread() {
			public void run() {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				throw new IllegalArgumentException("Fast enough?");
			}
		});
		System.out.println("map in test: " + threadIdToThrowable);
		assertTrue(threadIdToThrowable.size() == 1);
	}

	private Map<Long, Throwable> testDifferenceMonitor(Thread threadToStart) throws InterruptedException {
		ThreadDifferenceMonitor monitor = new ThreadDifferenceMonitor();
		Thread monitoringThread = new Thread(monitor);
		monitoringThread.start();
		Thread.sleep(10);
		threadToStart.start();
		monitoringThread.interrupt();
		monitoringThread.join();
		Set<Thread> createdThreads = monitor.getCreatedThreads();
		assertTrue(createdThreads.size() == 1 || createdThreads.size() == 2);
		return monitor.getThreadIdAndCorrespondingException();
	}
}
