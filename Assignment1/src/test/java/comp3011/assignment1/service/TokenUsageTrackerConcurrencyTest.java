package comp3011.assignment1.service;

import static org.assertj.core.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import comp3011.assignment1.model.GlobalStatResponse;

/**
 * Requirement: global token stats stay correct under concurrent transcriptions.
 *
 * 200 threads are released at the same instant by a latch so their recordUsage() calls genuinely overlap. 
 * With a plain long and +=, updates would be lost and the totals would come up short. LongAdder must lose none.
 */
class TokenUsageTrackerConcurrencyTest {

    private static final int THREADS = 200;
    private static final int CALLS_PER_THREAD = 1_000;

    @Test
    @DisplayName("200 threads recording usage at once lose no updates")
    void concurrentRecordUsageLosesNoUpdates() throws Exception {
        TokenUsageTracker tracker = new TokenUsageTracker();
        CountDownLatch startGate = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(THREADS);

        try {
            List<Future<Void>> futures = new ArrayList<>();
            for (int t = 0; t < THREADS; t++) {
                futures.add(pool.submit(() -> {
                    startGate.await();                 // every thread waits here
                    for (int i = 0; i < CALLS_PER_THREAD; i++) {
                        tracker.recordUsage(3, 5);
                    }
                    return null;
                }));
            }
            startGate.countDown();                     // release all 200 at once
            for (Future<Void> f : futures) {
                f.get(30, TimeUnit.SECONDS);           // rethrows any worker failure
            }
        } finally {
            pool.shutdownNow();
        }

        long totalCalls = (long) THREADS * CALLS_PER_THREAD;
        GlobalStatResponse stats = tracker.snapshot();
        assertThat(stats.inputTokens()).isEqualTo(totalCalls * 3);
        assertThat(stats.outputTokens()).isEqualTo(totalCalls * 5);
    }
}