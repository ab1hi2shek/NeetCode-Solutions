package lld.keyValueStore;

import java.util.*;

public class WindowedKeyValueStore {

    private final long expiryWindowMillis;
    private Map<String, Cache> keyValueStore;
    private Queue<Cache> expiryQueue;
    private long sum;
    private long count;

    private static class Cache {
        private String key;
        long value;
        long expiringTime;

        Cache(final String key, final long value, final long expiryTime) {
            this.key = key;
            this.value = value;
            this.expiringTime = expiryTime;
        }
    }

    // Constructor: takes expiry window in milliseconds
    public WindowedKeyValueStore(long expiryWindowMillis) {
        this.expiryWindowMillis = expiryWindowMillis;
        keyValueStore = new HashMap<>();
        expiryQueue = new LinkedList<>();
        this.sum = 0;
        this.count = 0;
    }

    // Stores the key with the given value and current time
    public void put(String key, long value) {

        long now = System.currentTimeMillis();
        Cache newCache = new Cache(key, value, now + expiryWindowMillis);

        // If key already exists, remove old value and subtract from sum and count.
        if (keyValueStore.containsKey(key)) {
            sum -= keyValueStore.get(key).value;
            count--;
        }

        count++;
        sum += value;
        keyValueStore.put(key, newCache);
        expiryQueue.offer(newCache);

        clearExpiredValues();
    }

    // Retrieves the value if not expired, otherwise returns -1
    public long get(String key) {
        if (!keyValueStore.containsKey(key)) {
            return -1;
        }

        Cache cache = keyValueStore.get(key);
        if (cache.expiringTime < System.currentTimeMillis()) {
            // Evict expired entries on get
            clearExpiredValues();
            return -1;
        }

        return cache.value;
    }

    // Returns the average of all non-expired values
    public double getAverage() {
        clearExpiredValues();
        return count == 0 ? 0.0 : sum / count;
    }

    private void clearExpiredValues() {
        long now = System.currentTimeMillis();

        while (!expiryQueue.isEmpty() && expiryQueue.peek().expiringTime < now) {
            Cache expiredCache = expiryQueue.poll(); // Remove from deque
            keyValueStore.remove(expiredCache.key); // Remove from map
            sum -= expiredCache.value;
            count--;
        }
    }

    public static void main(String[] args) throws InterruptedException {
        // Expiry window: 15 sec = 15000 ms
        WindowedKeyValueStore store = new WindowedKeyValueStore(15000);

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
