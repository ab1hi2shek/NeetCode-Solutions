package lld.keyValueStore;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class WindowedKeyValueStoreSet {

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    private static class Cache {
        private final String key;
        private final long value;
        private final long expiryTime;

        Cache(final String key, final long value, final long expiryTime) {
            this.key = key;
            this.value = value;
            this.expiryTime = expiryTime;
        }
    }

    private AtomicLong count;
    private AtomicLong sum;

    private final long expiryWindowMillis;
    private final ConcurrentHashMap<String, Cache> store;
    private final ConcurrentSkipListMap<Long, Set<String>> expiryTree;

    WindowedKeyValueStoreSet(final long expiryWindowMillis) {
        this.expiryWindowMillis = expiryWindowMillis;
        this.store = new ConcurrentHashMap<>();
        this.expiryTree = new ConcurrentSkipListMap<>();
        this.sum = new AtomicLong(0);
        this.count = new AtomicLong(0);
    }

    // Stores the key with the given value and current time
    public void put(String key, long value) {

        lock.writeLock().lock();
        try {
            long timeNow = System.currentTimeMillis();
            clearExpiredValues(timeNow); // Clear expired values everytime we call put operation.

            // if key exists in store, remove key value from sum and decrease count.
            if (store.containsKey(key)) {
                Cache cache = store.get(key);
                sum.addAndGet(-cache.value);
                count.decrementAndGet();
                expiryTree.get(cache.expiryTime).remove(cache.key); // remove existing entry from TreeMap
            }

            count.incrementAndGet();
            sum.addAndGet(value);

            long expiryTime = timeNow + expiryWindowMillis;
            Cache cache = new Cache(key, value, expiryTime);

            store.put(key, cache);
            expiryTree.computeIfAbsent(expiryTime, k -> new HashSet<>()).add(key);
        } finally {
            lock.writeLock().unlock();
        }
    }

    // Retrieves the value if not expired, otherwise returns -1
    public long get(String key) {

        lock.readLock().lock();
        try {
            long timeNow = System.currentTimeMillis();
            clearExpiredValues(timeNow); // Clear expired values everytime we call get operation.

            if (store.containsKey(key)) {
                return store.get(key).value;
            }

            return -1;
        } finally {
            lock.readLock().unlock();
        }
    }

    // Returns the average of all non-expired values
    public double getAverage() {
        lock.readLock().lock();
        try {
            long timeNow = System.currentTimeMillis();
            clearExpiredValues(timeNow); // Clear expired values everytime we call getAverage operation.

            return count.get() == 0 ? 0.0 : (double) sum.get() / count.get();
        } finally {
            lock.readLock().unlock();
        }
    }

    private void clearExpiredValues(long timeNow) {

        // Check for expired keys
        Iterator<Map.Entry<Long, Set<String>>> it = expiryTree.entrySet().iterator();

        while (it.hasNext()) {
            Map.Entry<Long, Set<String>> entry = it.next();
            long expiryTime = entry.getKey();

            // Entry is not expired, so don't need to check more.
            if (expiryTime > timeNow) {
                break;
            }

            for (String key : entry.getValue()) {
                Cache expiredCache = store.remove(key);
                sum.addAndGet(-expiredCache.value);
                count.decrementAndGet();
            }

            it.remove();
        }

    }

    public static void main(String[] args) throws InterruptedException {
        // Expiry window: 15 sec = 15000 ms
        WindowedKeyValueStoreSet store = new WindowedKeyValueStoreSet(15000);

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

        System.out.println("get value of a " + store.get("a")); // Now expired
        System.out.println("get value of e " + store.get("e")); // Not expired
        Thread.sleep(10_000);
        System.out.println("get value of e " + store.get("e")); // Now expired
    }

}

/**
 * 
 * synchronized
 * It’s built into Java as a keyword.
 * Automatically locks and unlocks around a block of code.
 * Easy to use but less flexible.
 * Can’t try to get a lock without waiting, or be interrupted while waiting.
 * Good for simple locking needs.
 * 
 * ReentrantLock
 * A class you create and control manually.
 * You have to call lock() and unlock() yourself.
 * More flexible: you can try to get the lock without waiting, wait with
 * timeout, or be interrupted.
 * Supports features like “fairness” (locks given in order of requests).
 * Can be used as a read/write lock variant for better performance with many
 * readers.
 */
