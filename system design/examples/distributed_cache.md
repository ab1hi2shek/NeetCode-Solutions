## Design a distributed cache
*What strategies would you use to ensure fault tolerance and data persistence in a distributed cache?*

---

## **1. High-Level Goals**

* Fast access to frequently-used data
* Horizontal scalability
* Low latency and high throughput
* Resilient to node failures
* durable storage for recovery after crash

---

## 🧠 Core Components

| Component                    | Responsibility                                        |
| ---------------------------- | ----------------------------------------------------- |
| Cache Clients                | Connect to and read/write from cache nodes            |
| Cache Nodes                  | Store in-memory data                                  |
| Coordination Layer           | Handles node discovery, partitioning, leader election |
| Optional: Backend Store      | Fallback to DB for cache misses                       |
| Persistent Storage | Write-ahead log (WAL) or snapshotting for durability  |

---

## 2. Strategies for Fault Tolerance

### **1. Data Partitioning (Sharding)**

> *“Use consistent hashing to evenly distribute keys across cache nodes.”*

#### 🔹 How it helps fault tolerance:

* When a node fails, **only a subset** of the data is affected (not the whole cache).
* With consistent hashing, the system can **gracefully redistribute** the lost partitions to surviving nodes.
* Minimizes impact of node failure and enables smooth **rebalancing** without rehashing the entire keyspace.

> 🔄 Example: If Node A dies, consistent hashing ensures that only a small slice of the keyspace is remapped to other nodes.


### **2. Replication**

> *“Replicate each partition across multiple nodes (e.g., master + replica(s))”*

#### 🔹 How it helps fault tolerance:

* **Redundancy**: If the master node fails, a replica is promoted and continues serving requests.
* **Read scalability**: Replicas can serve reads if masters are under pressure.
* **No single point of failure**.

#### Sync vs Async:

* **Synchronous replication**: Ensures data is safely written to replicas before acknowledging success. Safer but introduces latency.
* **Asynchronous replication**: Lower latency, but may **lose the latest writes** if master crashes before replicas catch up.

> 🛠 Choose based on latency vs durability tradeoff.

---

### **3. Failure Detection and Recovery**

> *“Use health checks to detect node failures and recover using leader election or failover.”*

#### 🔹 How it helps fault tolerance:

* **Quick detection**: Heartbeats detect dead or slow nodes quickly.
* **Automated recovery**: Systems like Raft, ZooKeeper, or etcd can:

  * Elect a new leader/master.
  * Trigger partition rebalancing.
  * Ensure quorum decisions for consistency.

> Helps the system **heal itself automatically** with minimal manual intervention.

---

### **4. Load Balancing**

> *“Clients use partition-aware routing or coordinator service to balance load.”*

### 🔹 How it helps fault tolerance:

* **Avoids overloading healthy nodes** when others go down — traffic is spread intelligently.
* **Smart clients** know which nodes are alive and reroute traffic dynamically.
* **Retries** and **fallback logic** ensure that temporary network issues or node failures don’t bring down the whole system.

> Think of this as *preventative fault tolerance* — it reduces cascading failures.

---

## Summary Table

| Strategy              | Fault Tolerance Benefit                     |
| --------------------- | ------------------------------------------- |
| **Partitioning**      | Isolates failure scope; easy redistribution |
| **Replication**       | Data redundancy and fast recovery           |
| **Failure Detection** | Automatic failover and self-healing         |
| **Load Balancing**    | Prevents overload and redirects traffic     |


---

## Strategies for Data Persistence

### **1. Write-Ahead Log (WAL)**

* Before applying changes to memory, write them to disk.
* Helps recover from crashes.

### **2. Periodic Snapshots**

* Periodically persist in-memory data to disk.
* Upon restart, replay the latest snapshot and WAL.

### **3. Hybrid Approach (Like Redis)**

* Append-only file + RDB snapshots (configurable).

### **4. Tiered Storage**

* Hot data in memory, warm data on SSD, cold data on disk (optional).

---

## Cache Invalidation & Consistency

* **Write-through**: Update cache and DB together.
* **Write-around**: Write to DB, update cache on read miss.
* **Write-back**: Write to cache first, flush to DB asynchronously (risk of loss).
* Use TTLs or event-driven invalidation (e.g., Kafka change streams) to avoid stale data.

---

## 🛡️ Ensuring High Availability

* Use **quorum-based** reads/writes (like DynamoDB, Cassandra) for tunable consistency.
* Deploy cache nodes across multiple availability zones/regions.
* Auto-rebalance partitions on node joins/leaves.

---

## ⚙️ Tools/Tech Stack Examples

| Requirement      | Tool                        |
| ---------------- | --------------------------- |
| In-Memory Cache  | Redis, Memcached, Hazelcast |
| Coordination     | etcd, ZooKeeper, Consul     |
| Persistence      | Redis AOF/RDB, RocksDB      |
| Sharding/Hashing | Consistent Hash Ring        |

---

## Let's dive into how **Redis Cluster** handles **partition-aware routing**:


### **How Redis Cluster Handles Partitioning and Routing**

Redis Cluster splits your keyspace into **16384 hash slots** (0–16383).

* Each node in the cluster is assigned a subset of these hash slots.
* When a key is set or retrieved, Redis computes a **CRC16 hash** of the key and maps it to a slot:

  ```
  hash_slot = CRC16(key) % 16384
  ```

---

### **Partitioning in Redis Cluster**

* Suppose you have 3 Redis nodes:

  * Node A handles slots 0–5460
  * Node B handles slots 5461–10922
  * Node C handles slots 10923–16383
* A key `"user:42"` is hashed to slot 7345 → Node B handles it.

---

### **Partition-Aware Routing**

Redis clients (like `JedisCluster`, `Lettuce`, `StackExchange.Redis`) are **partition-aware**.

#### Here’s what they do:

1. **Fetch the slot-node mapping** from the cluster (via `CLUSTER SLOTS` command).
2. When the app sends a `GET user:42`:

   * Client hashes `"user:42"` → slot 7345
   * Routing logic: “Oh, this belongs to Node B”
   * Sends the request directly to Node B (no coordinator needed)

---

### What if data is moved?

Redis Cluster may **reshard** (e.g., slot 7345 moves from B → A).

* If a client hits the old node (Node B), it returns a `MOVED` or `ASK` redirection.
* The client follows that redirection and **updates its routing table**.

---

### **Replication in Redis Cluster**

* Each master node can have **replicas** (slaves).
* If a master fails, the cluster promotes a replica using **Gossip Protocol** + **election**.
* Clients reconfigure routing table after failover.

---

### Fault Tolerance via Routing

Partition-aware clients + replication means:

* Clients route directly → lower latency.
* If a node dies, clients re-learn routing table and continue.
* No centralized bottleneck.

---

### Summary

| Feature              | Redis Cluster Implementation                                    |
| -------------------- | --------------------------------------------------------------- |
| Partitioning         | 16384 hash slots distributed across nodes                       |
| Routing              | Done by **partition-aware client**                              |
| Failover handling    | Redis Cluster handles, clients re-learn routing via redirection |
| Replication          | Master + replica(s) per shard                                   |
| Coordinator service? | ❌ No — clients are responsible for routing                      |

## HLD

1. Here’s a partition-aware client HLD for a Redis Cluster–like setup:

```
      +-------------------+
      |   Application     |
      |   Client (Jedis)  |
      +--------+----------+
               |
  +------------|-------------+
  | Uses hash(key) to route |
  +------------|-------------+
               |
    +----------+-----------+
    |                      |
+---------------+      +---------------+
| Redis Node A  |      | Redis Node B  |
| Slots 0–5460  |      | Slots 5461–10922 |
| Replica: A'   |      | Replica: B'   |
+---------------+      +---------------+
     |                      |
     v                      v
+---------------+      +---------------+
| Redis Node C  |      | Redis Node D  |
| Slots 10923–16383|   | (future scale)|
| Replica: C'   |      | Replica: D'   |
+---------------+      +---------------+
```

2. If clients aren’t smart enough:

```
Client → Load Balancer → Redis Proxy (e.g., Twemproxy) → Redis Nodes
```

 - Useful if you don’t want clients to know about cluster topology.
 - Adds latency but provides abstraction and centralized control.



---
