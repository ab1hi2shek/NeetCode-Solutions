# Java Memory Management and Garbage Collection (GC) — Complete Guide

Java manages memory automatically to help developers avoid manual memory allocation and deallocation issues such as memory leaks and dangling pointers. This guide covers the key concepts, components, GC algorithms, JVM tuning, and best practices.

---

## 1. Java Memory Model Overview

Java runtime memory is divided into several logical parts:

| Memory Area      | Purpose                                            | Notes                            |
|------------------|--------------------------------------------------|---------------------------------|
| **Heap**         | Stores all Java objects and arrays                | Shared among all threads         |
| **Stack**        | Stores local variables, method call frames        | One per thread                   |
| **Method Area**   | Stores class metadata (runtime constant pool, static variables, method info) | Part of Permanent Generation / Metaspace |
| **Program Counter (PC) Register** | Keeps track of JVM instruction execution | One per thread                  |
| **Native Method Stack** | Used for native code execution (e.g., JNI calls) | Platform-dependent               |

---

## 2. Java Heap Structure

The heap is the main area of concern for memory management and garbage collection. It is divided into:

### Young Generation (New Gen)

- **Eden Space:** Where new objects are allocated.
- **Survivor Spaces (S0 and S1):** Objects that survive initial GC in Eden are moved here.

### Old Generation (Tenured Gen)

- Objects that survive several GC cycles in Young Gen get promoted here.
- Stores long-lived objects.

### Metaspace (Java 8+)

- Stores class metadata, method definitions, and other reflective data.
- Replaced Permanent Generation (PermGen) from earlier Java versions.
- Resides in native memory outside the heap.

---

## 3. Garbage Collection Process

Garbage Collection (GC) identifies and removes objects no longer referenced by the program.

### Key Concepts:

- **Reachability:** Objects reachable through references starting from "GC roots" (e.g., stack variables, static fields) are considered alive.
- **Mark and Sweep:**  
  - **Mark:** Traverse object graph and mark live objects.  
  - **Sweep:** Collect unmarked (dead) objects.
- **Compaction:** Optional process to reduce heap fragmentation by moving live objects together.

---

## 4. Common Garbage Collectors in JVM

| GC Algorithm        | Description                                     | Use Case                            |
|---------------------|------------------------------------------------|-----------------------------------|
| **Serial GC**       | Single-threaded GC for Young and Old generation | Simple, best for small apps       |
| **Parallel GC**     | Multi-threaded GC focused on throughput         | Suitable for multiprocessor systems |
| **CMS (Concurrent Mark Sweep) GC** | Low-pause GC that does most work concurrently | Interactive applications needing low latency |
| **G1 (Garbage-First) GC** | Divides heap into regions, does parallel and concurrent GC | General purpose, default since Java 9 |
| **ZGC (Z Garbage Collector)** | Scalable low-latency GC                          | Large heaps, low pause (<10ms)    |
| **Shenandoah GC**   | Another low-pause GC alternative                  | Low latency applications           |

---

## 5. Detailed GC Phases (G1 Example)

1. **Initial Mark:** Pause-the-world; marks objects directly reachable from roots.
2. **Root Region Scanning:** Concurrent marking of roots in survivor regions.
3. **Concurrent Marking:** Traverses object graph concurrently, marks live objects.
4. **Final Mark:** Pause-the-world; completes marking process.
5. **Cleanup:** Identifies free space, prepares regions for collection.
6. **Evacuation (Copying):** Moves live objects to new regions and reclaims old ones.

---

## 6. JVM Heap Tuning Parameters

| Parameter             | Description                                    | Example                        |
|-----------------------|------------------------------------------------|-------------------------------|
| `-Xms`                | Initial heap size                              | `-Xms512m`                    |
| `-Xmx`                | Maximum heap size                              | `-Xmx2g`                      |
| `-XX:NewSize`         | Initial young generation size                  | `-XX:NewSize=256m`            |
| `-XX:MaxNewSize`      | Maximum young generation size                  | `-XX:MaxNewSize=512m`         |
| `-XX:SurvivorRatio`   | Ratio between Eden and Survivor spaces         | `-XX:SurvivorRatio=8`         |
| `-XX:+UseG1GC`        | Enables G1 Garbage Collector                    |                               |
| `-XX:+UseConcMarkSweepGC` | Enables CMS GC                             |                               |
| `-XX:MaxMetaspaceSize`| Limits Metaspace size                           | `-XX:MaxMetaspaceSize=256m`   |

---

## 7. Detecting and Avoiding Memory Leaks

### Common causes:

- Holding references in static fields.
- Unbounded caches or collections.
- Listeners or callbacks not deregistered.
- ThreadLocals not removed properly.

### Tools to detect leaks:

- **Heap dumps + Eclipse Memory Analyzer Tool (MAT)**
- **VisualVM / JConsole**
- Profilers like YourKit or JProfiler

---

## 8. Best Practices for Memory Management

- Minimize object creation inside loops.
- Use primitive types where possible.
- Prefer StringBuilder/StringBuffer for string concatenation in loops.
- Use weak references for caches (`WeakReference`, `SoftReference`).
- Explicitly null references no longer needed in long-running methods.
- Tune GC parameters based on profiling data.
- Monitor application with JVM monitoring tools.

---

## 9. Example: Simple JVM Tuning for a Web App

```bash
java -Xms512m -Xmx2g -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -jar myapp.jar
