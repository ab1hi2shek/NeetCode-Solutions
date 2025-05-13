# Garbage collection basics

## 1. What is Garbage Collection? (Generic Explanation)

**Garbage collection (GC)** is the process of automatically identifying and reclaiming memory that is no longer in use by a program. In languages without garbage collection (like C/C++), developers must manually free memory, which can lead to memory leaks or dangling pointers if done incorrectly[^4][^6].

**Key Points:**

- **Purpose:** Prevent memory leaks by freeing unused memory.
- **How:** The system tracks which objects are still accessible (reachable) by the program. If an object cannot be reached (no references from code), it is considered garbage and its memory can be reclaimed.
- **Advantages:**
    - Simplifies coding (no need for manual memory management).
    - Reduces bugs related to memory leaks and invalid memory access.
- **Disadvantages:**
    - Can cause unpredictable pauses ("stop-the-world" events) as the collector runs.
    - Slight performance overhead due to the tracking and collection process.

---

## 2. How Does Java Garbage Collection Work?

**Java uses automatic garbage collection to manage memory in the heap.** The JVM (Java Virtual Machine) tracks object references and automatically deletes objects that are no longer reachable[^1][^2][^4][^5][^6].

### **Key Concepts:**

- **Heap Memory:** All Java objects are created in the heap[^5][^6].
- **Stack Memory:** Holds references to objects (variables, method calls)[^5].
- **Reachability:** If an object is accessible from any root (local variables, static fields, etc.), it is considered "in use." Otherwise, it is garbage[^2][^5][^6].
- **Garbage Collector:** The JVM component that performs garbage collection[^5].


### **Garbage Collection Process in Java:**

1. **Mark Phase:** The GC traverses all reachable objects starting from the roots and marks them as alive.
2. **Sweep Phase:** The GC scans the heap and deletes objects that were not marked (i.e., unreachable).
3. **Compaction (optional):** The GC may move objects to defragment memory.

**Algorithms Used:**

- **Mark-and-Sweep:** Standard approach-mark reachable, sweep unreachable[^5].
- **Generational GC:** Java divides the heap into "young" and "old" generations to optimize collection frequency and performance.
- **Other Algorithms:** Serial, Parallel, CMS (Concurrent Mark Sweep), G1 (Garbage First), each with different performance characteristics[^2][^7].


### **Benefits of Java GC:**

- **Automatic memory management:** Developers don’t need to explicitly free memory[^2][^4][^5].
- **Prevents memory leaks and dangling pointers.**
- **Handles circular references:** Even if two objects reference each other, if they are unreachable from roots, they will be collected[^2].


### **Drawbacks:**

- **Pause times:** GC can cause application pauses, especially in large heaps or with frequent object creation[^2].
- **Not fully predictable:** You can request GC with `System.gc()`, but JVM may ignore it[^2][^5].

------------------
------------------

# Implementation in Java sample

## Core concepts:
- Heap: Where all objects are stored.
- Roots: Entry points (like global variables, stack references) from which objects are reachable.
- Mark Phase: Traverse from roots, marking all reachable objects.
- Sweep Phase: Remove all unmarked (unreachable) objects from the heap.

```java
import java.util.ArrayList;
import java.util.List;

// Represents an object managed by our GC
class GCObject {
    boolean marked = false;
    List<GCObject> references = new ArrayList<>(); // Other objects this one references
}

// Memory manager with mark-and-sweep GC
class MemoryManager {
    private List<GCObject> heap = new ArrayList<>();
    private List<GCObject> roots = new ArrayList<>();

    // Allocate a new object
    public GCObject allocate() {
        GCObject obj = new GCObject();
        heap.add(obj);
        return obj;
    }

    // Add/remove root references (simulate stack/global variables)
    public void addRoot(GCObject obj) {
        roots.add(obj);
    }
    public void removeRoot(GCObject obj) {
        roots.remove(obj);
    }

    // Mark phase: recursively mark reachable objects
    private void mark(GCObject obj) {
        if (!obj.marked) {
            obj.marked = true;
            for (GCObject ref : obj.references) {
                mark(ref);
            }
        }
    }

    // Sweep phase: remove unmarked objects
    private void sweep() {
        List<GCObject> newHeap = new ArrayList<>();
        for (GCObject obj : heap) {
            if (obj.marked) {
                obj.marked = false; // Reset for next GC cycle
                newHeap.add(obj);
            }
            // else: unreachable, so not added (collected)
        }
        heap = newHeap;
    }

    // Run garbage collection
    public void collectGarbage() {
        // Mark phase
        for (GCObject root : roots) {
            mark(root);
        }
        // Sweep phase
        sweep();
    }

    // For demonstration: get current heap size
    public int getHeapSize() {
        return heap.size();
    }
}

// Example usage
public class SimpleGCExample {
    public static void main(String[] args) {
        MemoryManager mm = new MemoryManager();

        // Create objects and references
        GCObject a = mm.allocate();
        GCObject b = mm.allocate();
        GCObject c = mm.allocate();
        a.references.add(b); // a -> b
        b.references.add(c); // b -> c

        mm.addRoot(a); // Only 'a' is a root

        System.out.println("Heap size before GC: " + mm.getHeapSize()); // Should be 3

        mm.collectGarbage();
        System.out.println("Heap size after GC: " + mm.getHeapSize()); // Should still be 3

        // Remove root reference to 'a'
        mm.removeRoot(a);

        mm.collectGarbage();
        System.out.println("Heap size after removing root and GC: " + mm.getHeapSize()); // Should be 0
    }
}
```