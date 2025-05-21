package lld.keyValueStore;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

public class WindowedKeyValueStoreConcurrent {

    private final long expiryWindowMillis;
    private final ConcurrentHashMap<String, Cache> keyValueStore;
    private final ConcurrentLinkedQueue<Cache> expiryQueue;
    private final AtomicLong sum;
    private final AtomicLong count;
    private final Object lock = new Object();

    private static class Cache {
        private final String key;
        private final long value;
        private final long expiringTime;

        Cache(final String key, final long value, final long expiryTime) {
            this.key = key;
            this.value = value;
            this.expiringTime = expiryTime;
        }
    }

    public WindowedKeyValueStoreConcurrent(long expiryWindowMillis) {
        this.expiryWindowMillis = expiryWindowMillis;
        this.keyValueStore = new ConcurrentHashMap<>();
        this.expiryQueue = new ConcurrentLinkedQueue<>();
        this.sum = new AtomicLong(0);
        this.count = new AtomicLong(0);
    }

    public void put(String key, long value) {
        long now = System.currentTimeMillis();
        long expiryTime = now + expiryWindowMillis;
        Cache newCache = new Cache(key, value, expiryTime);

        synchronized (lock) {
            Cache oldCache = keyValueStore.put(key, newCache);
            if (oldCache != null) {
                sum.addAndGet(-oldCache.value);
                count.decrementAndGet();
            }
            sum.addAndGet(value);
            count.incrementAndGet();
            expiryQueue.offer(newCache);
            clearExpiredValues(now);
        }
    }

    public long get(String key) {
        Cache cache = keyValueStore.get(key);
        if (cache == null) {
            return -1;
        }
        long now = System.currentTimeMillis();
        if (cache.expiringTime < now) {
            synchronized (lock) {
                Cache removed = keyValueStore.remove(key);
                if (removed != null) {
                    sum.addAndGet(-removed.value);
                    count.decrementAndGet();
                    expiryQueue.remove(removed);
                }
            }
            return -1;
        }
        return cache.value;
    }

    public double getAverage() {
        long now = System.currentTimeMillis();
        synchronized (lock) {
            clearExpiredValues(now);
            long currentCount = count.get();
            return currentCount == 0 ? 0.0 : (double) sum.get() / currentCount;
        }
    }

    private void clearExpiredValues(long now) {
        while (true) {
            Cache expiredCache = expiryQueue.peek();
            if (expiredCache == null || expiredCache.expiringTime >= now) {
                break;
            }
            expiryQueue.poll();
            Cache removed = keyValueStore.remove(expiredCache.key);
            if (removed != null) {
                sum.addAndGet(-removed.value);
                count.decrementAndGet();
            }
        }
    }

    public static void main(String[] args) throws InterruptedException {
        // Expiry window: 15 sec = 15000 ms
        WindowedKeyValueStoreConcurrent store = new WindowedKeyValueStoreConcurrent(15000);

        // Insert values at T
        store.put("a", 10); // T
        store.put("b", 20); // T
        store.put("c", 30); // T

        System.out.println("Average at T: " + store.getAverage());
        // Expected: (10 + 20 + 30) / 3 = 20.0

        Thread.sleep(10_000); // Wait 10 seconds (T + 10s)

        store.put("d", 40);

        System.out.println("Average at T + 10s: " + store.getAverage());
        // Expected: (10 + 20 + 30 + 40) / 4 = 25.0 (none expired yet)

        Thread.sleep(10_000); // Wait another 10 seconds (T + 20s)

        store.put("e", 50);

        System.out.println("Average at T + 20s: " + store.getAverage());
        // Expected: (40 + 50) / 2 = 45.0
        // Explanation: a, b, c inserted at T and now expired (T + 20 > T + 15)

        Thread.sleep(10_000); // Wait another 10 seconds (T + 30s)

        store.put("f", 60);

        System.out.println("Average at T + 30s: " + store.getAverage());
        // Expected: (50 + 60) / 2 = 55.0
        // Explanation: d (T + 10s) expired at T + 25s, only e and f remain
    }
}
