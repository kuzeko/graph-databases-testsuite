package com.graphbenchmark.common;

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;

import java.util.Random;
import java.util.concurrent.Callable;

public class RetryTransactionCtx {

    final static int
            defaultRetry = Runtime.getRuntime().availableProcessors() * 2,
            defaultNoDelay = 3,
            defaultDelay = 2; // effective max sleep: (defaultDelay + 1) ms
    static Random rnd = new Random();


    /**
     *
     * @param times
     * @param gts
     * @param func
     * @return true if the functions succeeds, false if the retry are exhausted.
     */
    public static boolean retry(int times, int delay, GraphTraversalSource gts, Callable<Void> func) {
        long thread_id = Thread.currentThread().getId();
        if (times <= 0)
            throw new IllegalArgumentException(String.format(
                    "[T%d] `times` must be a strictly positive integer, %d was provided.", thread_id, times));
        for (int tries=0; tries< times; tries++) {
            try {
                func.call();
                return true;
            } catch (Exception e) {
                switch (e.getClass().getCanonicalName()) {
                    case "org.janusgraph.core.JanusGraphException":
                        // PermanentLockingException
                        GdbLogger.getLogger().warning("[T%d] janus: %s", thread_id, e.getCause().getMessage());
                    case "com.orientechnologies.orient.core.exception.OConcurrentModificationException":
                        // OConcurrentModificationException
                    case "com.orientechnologies.orient.core.exception.OSchemaException":
                        // (should never happen.. but)
                    case "org.neo4j.kernel.DeadlockDetectedException":
                        // https://neo4j.com/developer/kb/explanation-of-error-deadlockdetectedexception-forseticlient-0-cant-acquire-exclusivelock/
                        break;
                    case "java.lang.RuntimeException":
                        if (e.getCause().getClass().getCanonicalName().equals("org.postgresql.util.PSQLException"))
                            break;

                        GdbLogger.getLogger().debug("[T%d] Got RuntimeException caused by %s",
                                thread_id, e.getCause().getClass().getCanonicalName());
                    default:
                        e.printStackTrace();
                        GdbLogger.getLogger().fatal("[T%d] Abort due to unexpected exception: %s.",
                                thread_id, e.getClass().getCanonicalName());
                }
                // If try again, rollback
                new TxContext(gts, false).rollback();

                if (delay > 0 && tries > defaultNoDelay) {
                    try {
                        long sleep_ms = rnd.nextInt(delay);
                        int sleep_ns = rnd.nextInt(1000) * 1000;
                        GdbLogger.getLogger().debug("[T%d] Going to sleep for %dms + %dns", thread_id, sleep_ms, sleep_ns);
                        Thread.sleep(sleep_ms, sleep_ns);
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        }
        return false;
    }

    public static void retryOrDie(GraphTraversalSource gts, long thread_id, Callable<Void> func) {
        GdbLogger.getLogger().ensure(retry(defaultRetry, defaultDelay, gts, func),
                "[T%d] Query abort due to transaction retries exhaustion", thread_id);
    }
}
