package me.vekster.lightanticheat.player.cache.history;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerCacheHistoryTest {

    private static final HistoryElement[] ELEMENTS = HistoryElement.values();

    @Test
    void constructorFillsAllSlots() {
        PlayerCacheHistory<Integer> history = new PlayerCacheHistory<>(Integer.valueOf(0));
        for (int i = 0; i < ELEMENTS.length; i++) {
            assertEquals(Integer.valueOf(0), history.get(ELEMENTS[i]),
                    "Offset " + i + " should hold the initial value");
        }
    }

    @Test
    void singleAddUpdatesFrom() {
        PlayerCacheHistory<String> history = new PlayerCacheHistory<>("init");
        history.add("A");

        assertEquals("A", history.get(HistoryElement.FROM),
                "FROM should reflect the most recent add");

        HistoryElement[] older = {
                HistoryElement.FIRST, HistoryElement.SECOND, HistoryElement.THIRD,
                HistoryElement.FOURTH, HistoryElement.FIFTH, HistoryElement.SIXTH,
                HistoryElement.SEVENTH, HistoryElement.EIGHT, HistoryElement.NINTH,
                HistoryElement.TENTH
        };
        for (int i = 0; i < older.length; i++) {
            assertEquals("init", history.get(older[i]),
                    "Offset " + (i + 1) + " should still hold the initial value");
        }
    }

    @Test
    void orderedOffsetsRemainCorrect() {
        PlayerCacheHistory<String> history = new PlayerCacheHistory<>("Z");
        String[] values = {"A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K"};
        for (int i = 0; i < values.length; i++) {
            history.add(values[i]);
        }

        for (int i = 0; i < ELEMENTS.length; i++) {
            String expected = values[values.length - 1 - i];
            assertEquals(expected, history.get(ELEMENTS[i]),
                    "Offset " + i + " should map to values[" + (values.length - 1 - i) + "]");
        }
    }

    @Test
    void wrapRetainsLatestEleven() {
        PlayerCacheHistory<Integer> history = new PlayerCacheHistory<>(Integer.valueOf(-1));
        for (int v = 1; v <= 15; v++) {
            history.add(Integer.valueOf(v));
        }

        int[] expected = {15, 14, 13, 12, 11, 10, 9, 8, 7, 6, 5};
        for (int i = 0; i < ELEMENTS.length; i++) {
            assertEquals(Integer.valueOf(expected[i]), history.get(ELEMENTS[i]),
                    "Offset " + i + " should hold the newest-eleven value v=" + expected[i]);
        }
    }

    @Test
    void atRejectsNegativeIndex() {
        assertThrows(IndexOutOfBoundsException.class, () -> HistoryElement.at(-1));
    }

    @Test
    void atRejectsCountIndex() {
        assertThrows(IndexOutOfBoundsException.class,
                () -> HistoryElement.at(HistoryElement.count()));
    }

    @Test
    void concurrentAddAndGetDoesNotCorruptArray() throws InterruptedException {
        final int workers = 8;
        final int addsPerWorker = 100;
        final int totalAdds = workers * addsPerWorker;
        final PlayerCacheHistory<Integer> history = new PlayerCacheHistory<>(Integer.valueOf(0));

        ExecutorService pool = Executors.newFixedThreadPool(workers);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch workersDone = new CountDownLatch(workers);
        AtomicReference<Throwable> workerError = new AtomicReference<Throwable>(null);

        Thread reader = new Thread(() -> {
            try {
                start.await();
                while (workersDone.getCount() > 0) {
                    for (int s = 0; s < ELEMENTS.length; s++) {
                        Integer value = history.get(ELEMENTS[s]);
                        assertNotNull(value,
                                "Slot at offset " + s + " returned null under concurrent access");
                        int v = value.intValue();
                        if (v < 0 || v > totalAdds) {
                            throw new AssertionError("Out-of-range value observed: " + v);
                        }
                    }
                }
            } catch (Throwable t) {
                workerError.compareAndSet(null, t);
            }
        }, "pch-reader");
        reader.setDaemon(true);
        reader.start();

        try {
            for (int w = 0; w < workers; w++) {
                final int workerId = w;
                pool.submit(() -> {
                    try {
                        start.await();
                        int base = workerId * addsPerWorker;
                        for (int i = 0; i < addsPerWorker; i++) {
                            history.add(Integer.valueOf(base + i + 1));
                        }
                    } catch (Throwable t) {
                        workerError.compareAndSet(null, t);
                    } finally {
                        workersDone.countDown();
                    }
                });
            }

            start.countDown();
            assertTrue(workersDone.await(30, TimeUnit.SECONDS),
                    "Worker tasks did not complete within the timeout");
            reader.join(TimeUnit.SECONDS.toMillis(5));
        } finally {
            pool.shutdown();
            if (!pool.awaitTermination(5, TimeUnit.SECONDS)) {
                pool.shutdownNow();
            }
        }

        Throwable err = workerError.get();
        if (err != null) {
            throw new AssertionError("Concurrent corruption detected", err);
        }
    }
}