package com.bugsnag.android;

import android.support.annotation.NonNull;

import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;

/**
 * Capture and serialize the state of all threads at the time of an exception.
 */
class ThreadState implements JsonStream.Streamable {
    private static final String THREAD_TYPE = "android";

    private final Configuration config;
    private final Thread[] threads;
    private final Map<Thread, StackTraceElement[]> stackTraces;
    private final long currentThreadId;

    ThreadState(Configuration config, Thread currentThread, Map<Thread, StackTraceElement[]> allStackTraces) {
        this.config = config;
        stackTraces = allStackTraces;

        // API 24/25 don't record the currentThread, add it in manually
        // https://issuetracker.google.com/issues/64122757
        if (!stackTraces.containsKey(currentThread)) {
            stackTraces.put(currentThread, currentThread.getStackTrace());
        }

        currentThreadId = currentThread.getId();
        threads = sanitiseThreads(stackTraces);
    }

    /**
     * Returns an array of threads including the current thread, sorted by thread id
     *
     * @param liveThreads all live threads
     */
    private Thread[] sanitiseThreads(Map<Thread, StackTraceElement[]> liveThreads) {
        Set<Thread> threadSet = liveThreads.keySet();

        Thread[] threads = threadSet.toArray(new Thread[threadSet.size()]);
        Arrays.sort(threads, new Comparator<Thread>() {
            public int compare(@NonNull Thread lhs, @NonNull Thread rhs) {
                return Long.valueOf(lhs.getId()).compareTo(rhs.getId());
            }
        });
        return threads;
    }

    @Override
    public void toStream(@NonNull JsonStream writer) throws IOException {
        writer.beginArray();
        for (Thread thread : threads) {
            writer.beginObject();
            writer.name("id").value(thread.getId());
            writer.name("name").value(thread.getName());
            writer.name("type").value(THREAD_TYPE);

            StackTraceElement[] stacktrace = stackTraces.get(thread);
            writer.name("stacktrace").value(new Stacktrace(config, stacktrace));

            if (currentThreadId == thread.getId()) {
                writer.name("errorReportingThread").value(true);
            }
            writer.endObject();
        }
        writer.endArray();
    }
}
