package de.kdi.junit.runners.thread;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Set;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.carrotsearch.junitbenchmarks.BenchmarkOptions;
import com.carrotsearch.junitbenchmarks.BenchmarkRule;

public class TestThreadMonitor {

	@Rule
	public TestRule benchmarkRun = new BenchmarkRule();

	@Test
	public void testSimpleComparation() throws Exception {
		ThreadMonitor testable = new ThreadMonitor();
		int sizeBefore = testable.currentThreadsId.size();
		createStubThread();
		Set<Thread> difference = testable.getDifference(Thread.getAllStackTraces().keySet());
		assertTrue(difference.size() > 0);
		assertEquals(1, difference.size());
		assertEquals(sizeBefore + 1, testable.currentThreadsId.size());
	}

	private static void createStubThread() {
		new Thread() {
			public void run() {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			};
		}.start();
	}

	@Test
	@BenchmarkOptions(benchmarkRounds = 100, warmupRounds = 0)
	public void benchmarkMonitor() {
		ThreadMonitor threadMonitor = new ThreadMonitor();
		createStubThread();
		threadMonitor.getDifference(Thread.getAllStackTraces().keySet());
	}

}
