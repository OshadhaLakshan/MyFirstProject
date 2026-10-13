package chat;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Part 4a: the corrected counter, with no networking at all.
 * 50 threads each increment a shared counter 100,000 times.
 * Expected total: 5,000,000.
 *
 * AtomicInteger makes each increment atomic, preventing lost updates.
 */
public class CounterDemo {

    static final int THREADS = 50;
    static final int INCREMENTS = 100_000;

    static final AtomicInteger count = new AtomicInteger(0);

    public static void main(String[] args) throws InterruptedException {
        CountDownLatch startSignal = new CountDownLatch(1);
        Thread[] threads = new Thread[THREADS];

        for (int i = 0; i < THREADS; i++) {
            threads[i] = new Thread(() -> {
                try {
                    startSignal.await();               // all threads start together
                } catch (InterruptedException e) {
                    return;
                }
                for (int j = 0; j < INCREMENTS; j++) {
                    count.incrementAndGet();           // atomic read-modify-write
                }
            });
            threads[i].start();
        }

        long start = System.nanoTime();
        startSignal.countDown();
        for (Thread t : threads) {
            t.join();
        }
        long ms = (System.nanoTime() - start) / 1_000_000;

        int expected = THREADS * INCREMENTS;
        int actual = count.get();
        System.out.printf("Expected: %,d%n", expected);
        System.out.printf("Actual:   %,d%n", actual);
        System.out.printf("Lost:     %,d updates (%d ms)%n", expected - actual, ms);
    }
}
