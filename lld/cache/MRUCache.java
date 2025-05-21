package lld.cache;

import java.util.*;

/**
 * Class to implement Cache with MRU (Most Recently Used) eviction policy.
 */
public class MRUCache {

    // Capacity of the cache.
    private final int capacity;

    /**
     * Static class for a cache node.
     */
    private static class CacheNode {
        private String key;
        private String value;
        private CacheNode prev;
        private CacheNode next;

        CacheNode(final String key, final String value) {
            this.key = key;
            this.value = value;
            this.prev = null;
            this.next = null;
        }
    }

    private final Map<String, CacheNode> store;
    private final CacheNode head;
    private final CacheNode tail;

    MRUCache(final int capacity) {

        this.capacity = capacity;
        this.store = new HashMap<>();

        head = new CacheNode(null, null);
        tail = new CacheNode(null, null);
        head.next = tail;
        tail.prev = head;
    }

    /**
     * Adds the key value pair to the cache.
     * 
     * @param key
     * @param value
     */
    private void put(String key, String value) {

        CacheNode newNode = new CacheNode(key, value);

        if (store.containsKey(key)) {
            // if store already contains the key, update the node value and it's position in
            // the DLL.
            CacheNode node = store.get(key);
            node.value = value;
            deleteNode(node); // delete node from current position.
            addNode(node); // add node to front.
            return;
        } else if (store.size() == capacity) {
            CacheNode nodeToRemove = head.next;
            deleteNode(nodeToRemove); // evict the node from front as it is most recently used.
            store.remove(nodeToRemove.key);
        }

        addNode(newNode); // then add node to start (after head).
        store.put(key, newNode);

    }

    /**
     * Gets the value of the key if it exists. otherwise return empty string.
     * 
     * @param key
     * @return
     */
    private String get(String key) {

        if (store.containsKey(key)) {
            CacheNode node = store.get(key);

            // Have to put this node in front of the DLL.
            deleteNode(node);
            addNode(node);

            return node.value;
        }

        return "";
    }

    /**
     * Helper function to add a node at the front of the doubly linked list.
     * 
     * @param node: cache node to be added.
     */
    private void addNode(CacheNode node) {

        CacheNode temp = head.next;

        head.next = node;
        node.prev = head;

        node.next = temp;
        temp.prev = node;
    }

    /**
     * Helper function to delete a node from doubly linked list.
     * 
     * @param node: cache node to be deleted.
     */
    private void deleteNode(CacheNode node) {

        CacheNode nodeNext = node.next;
        CacheNode nodePrev = node.prev;

        nodePrev.next = nodeNext;
        nodeNext.prev = nodePrev;

    }

    public static void main(String[] args) {
        MRUCache cache = new MRUCache(3);

        System.out.println("=== Initial Inserts ===");
        cache.put("product:001", "iPhone");
        cache.put("product:002", "Pixel");
        cache.put("product:003", "Galaxy");

        System.out.println("Get 'product:001': " + cache.get("product:001")); // iPhone → now MRU
        System.out.println("Get 'product:002': " + cache.get("product:002")); // Pixel → now MRU

        System.out.println("\n=== Insert Beyond Capacity (Should Evict Most Recently Used: product:002) ===");
        cache.put("product:004", "OnePlus");

        System.out.println("Get 'product:002': " + cache.get("product:002")); // Should be "" (evicted)
        System.out.println("Get 'product:003': " + cache.get("product:003")); // Should be Galaxy
        System.out.println("Get 'product:001': " + cache.get("product:001")); // Should be iPhone
        System.out.println("Get 'product:004': " + cache.get("product:004")); // Should be OnePlus

        System.out.println("\n=== Access a Key and Add Another (Evict Recently Used) ===");
        cache.get("product:003"); // Galaxy → now MRU
        cache.put("product:005", "Nothing Phone"); // Evict product:003

        System.out.println("Get 'product:003': " + cache.get("product:003")); // Should be "" (evicted)
        System.out.println("Get 'product:005': " + cache.get("product:005")); // Should be Nothing Phone
        System.out.println("Get 'product:001': " + cache.get("product:001")); // Should be iPhone
        System.out.println("Get 'product:004': " + cache.get("product:004")); // Should be OnePlus

        System.out.println("\n=== Re-insert an Existing Key (Updates and Makes MRU) ===");
        cache.put("product:004", "OnePlus Updated"); // Updates and becomes MRU
        cache.put("product:006", "Xiaomi"); // Evict MRU → product:004

        System.out.println("Get 'product:004': " + cache.get("product:004")); // Should be "" (evicted)
        System.out.println("Get 'product:006': " + cache.get("product:006")); // Should be Xiaomi
    }

}
