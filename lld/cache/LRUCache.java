package lld.cache;

import java.util.*;

import org.w3c.dom.Node;

class CacheNode {
    int value;
    String key;
    CacheNode next;
    CacheNode prev;

    CacheNode(final String key, final int value) {
        this.value = value;
        this.key = key;
    }
}

public class LRUCache {

    private final int capacity;
    private final Map<String, CacheNode> store;

    CacheNode head;
    CacheNode tail;

    LRUCache(final int capacity) {
        this.capacity = capacity;
        this.store = new HashMap<>();

        head = new CacheNode("-1", -1);
        tail = new CacheNode("-1", -1);
        head.next = tail;
        tail.prev = head;
    }

    private void put(String key, int value) {
        if (store.containsKey(key)) {
            CacheNode node = store.get(key);
            store.remove(key);
            deleteNode(node);
        }

        if (store.size() == capacity) {
            store.remove(tail.prev.key);
            deleteNode(tail.prev);
        }

        addNode(new CacheNode(key, value));
        store.put(key, head.next);
    }

    private int get(String key) {
        if (store.containsKey(key)) {
            CacheNode node = store.get(key);
            store.remove(key);
            deleteNode(node);
            addNode(node);

            store.put(key, head.next);
            return node.value;
        }

        return -1;
    }

    private void addNode(CacheNode node) {
        CacheNode temp = head.next;

        node.next = temp;
        node.prev = head;

        temp.prev = node;
        head.next = node;
    }

    private void deleteNode(CacheNode node) {
        node.prev.next = node.next;
        node.next.prev = node.prev;
    }

    public static void main(String[] args) {
        LRUCache lruCache = new LRUCache(3);

        lruCache.put("key1", 1);
        lruCache.put("key2", 2);

        System.out.println(lruCache.get("key1")); // shoould be 1
        System.out.println(lruCache.get("key2")); // should be 2

        lruCache.put("key3", 3);
        lruCache.put("key4", 4);

        System.out.println(lruCache.get("key1")); // should be -1;
        System.out.println(lruCache.get("key3")); // should be 3;

        lruCache.put("key2", 8);
        System.out.println(lruCache.get("key3")); // should be -1;
        lruCache.put("key5", 5);
        System.out.println(lruCache.get("key3")); // should be -1;
        System.out.println(lruCache.get("key5")); // should be 5;
    }
}
