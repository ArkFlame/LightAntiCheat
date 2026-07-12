package me.vekster.lightanticheat.player.violation;

import me.vekster.lightanticheat.check.CheckName;
import me.vekster.lightanticheat.player.violation.PlayerViolations.NotificationChannel;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerViolationsTest {

    @Test
    void newCounterStartsAtZero() {
        PlayerViolations pv = new PlayerViolations();
        assertEquals(0, pv.getViolations(CheckName.FLIGHT_A));
    }

    @Test
    void setReplacesValue() {
        PlayerViolations pv = new PlayerViolations();
        pv.setViolations(CheckName.FLIGHT_A, 7);
        assertEquals(7, pv.getViolations(CheckName.FLIGHT_A));
        pv.setViolations(CheckName.FLIGHT_A, 0);
        assertEquals(0, pv.getViolations(CheckName.FLIGHT_A));
    }

    @Test
    void increaseAddsValue() {
        PlayerViolations pv = new PlayerViolations();
        pv.increaseViolations(CheckName.FLIGHT_A, 3);
        pv.increaseViolations(CheckName.FLIGHT_A, 5);
        assertEquals(8, pv.getViolations(CheckName.FLIGHT_A));
    }

    @Test
    void concurrentIncreaseDoesNotLoseUpdates() throws Exception {
        final int workers = 8;
        final int perWorker = 10000;
        final PlayerViolations pv = new PlayerViolations();
        final CheckName check = CheckName.FLIGHT_A;
        ExecutorService executor = Executors.newFixedThreadPool(workers);
        try {
            final CountDownLatch start = new CountDownLatch(1);
            final CountDownLatch done = new CountDownLatch(workers);
            List<Future<?>> futures = new ArrayList<Future<?>>();
            for (int i = 0; i < workers; i++) {
                Callable<Void> task = new Callable<Void>() {
                    @Override
                    public Void call() {
                        try {
                            start.await();
                            for (int j = 0; j < perWorker; j++) {
                                pv.increaseViolations(check, 1);
                            }
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        } finally {
                            done.countDown();
                        }
                        return null;
                    }
                };
                futures.add(executor.submit(task));
            }
            start.countDown();
            done.await();
            for (Future<?> f : futures) {
                f.get();
            }
            assertEquals(workers * perWorker, pv.getViolations(check));
        } finally {
            executor.shutdown();
            executor.awaitTermination(10, TimeUnit.SECONDS);
        }
    }

    @Test
    void cooldownAllowsFirstAcquisition() {
        PlayerViolations pv = new PlayerViolations();
        assertTrue(pv.tryAcquire(NotificationChannel.VIOLATION_LOG, 1000L, 100L));
    }

    @Test
    void cooldownRejectsSameWindow() {
        PlayerViolations pv = new PlayerViolations();
        assertTrue(pv.tryAcquire(NotificationChannel.VIOLATION_LOG, 1000L, 100L));
        assertFalse(pv.tryAcquire(NotificationChannel.VIOLATION_LOG, 1050L, 100L));
    }

    @Test
    void cooldownAllowsAfterStrictlyGreaterWindow() {
        PlayerViolations pv = new PlayerViolations();
        assertTrue(pv.tryAcquire(NotificationChannel.VIOLATION_LOG, 1000L, 100L));
        assertTrue(pv.tryAcquire(NotificationChannel.VIOLATION_LOG, 1101L, 100L));
    }

    @Test
    void cooldownRejectsExactlyEqualBoundary() {
        PlayerViolations pv = new PlayerViolations();
        assertTrue(pv.tryAcquire(NotificationChannel.VIOLATION_LOG, 1000L, 100L));
        assertFalse(pv.tryAcquire(NotificationChannel.VIOLATION_LOG, 1100L, 100L));
    }

    @Test
    void cooldownChannelsAreIndependent() {
        PlayerViolations pv = new PlayerViolations();
        assertTrue(pv.tryAcquire(NotificationChannel.VIOLATION_LOG, 1000L, 100L));
        assertTrue(pv.tryAcquire(NotificationChannel.PUNISHMENT_LOG, 1000L, 100L));
        assertFalse(pv.tryAcquire(NotificationChannel.VIOLATION_LOG, 1050L, 100L));
        assertFalse(pv.tryAcquire(NotificationChannel.PUNISHMENT_LOG, 1050L, 100L));
    }
}