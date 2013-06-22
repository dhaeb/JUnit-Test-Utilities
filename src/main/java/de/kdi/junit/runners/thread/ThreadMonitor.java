package de.kdi.junit.runners.thread;

import gnu.trove.set.hash.THashSet;
import gnu.trove.set.hash.TLongHashSet;

import java.util.Iterator;
import java.util.Set;

public class ThreadMonitor {
	
	TLongHashSet currentThreadsId = new TLongHashSet();

	public ThreadMonitor() {
		Set<Thread> currentThreads = new THashSet<Thread>(Thread.getAllStackTraces().keySet());
		for(Thread currentThread : currentThreads){
			currentThreadsId.add(currentThread.getId());
		}
	}
	
	public Set<Thread> getDifference(Set<Thread> comparable){
		Set<Thread> result = new THashSet<Thread>();
		for (Iterator<Thread> it = comparable.iterator(); it.hasNext();) {
			Thread currentThreadComparable = it.next();
			long threadId = currentThreadComparable.getId();
			if(!currentThreadsId.contains(threadId)){
				result.add(currentThreadComparable);
				currentThreadsId.add(threadId);
			}
		}
		return result;
	}
}
