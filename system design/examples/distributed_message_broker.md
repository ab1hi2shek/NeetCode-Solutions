# Distributed message broker

## 1. Requirements

### Functional

* **Publish/Subscribe model** (topics/queues)
* **At least once / exactly once** delivery guarantees
* **Durable message storage**
* **Consumer groups for load balancing**
* **Replay capability** (replay messages from an offset)
* **Topic partitioning**
* **Back-pressure handling**
* **Acknowledgments**

### Non-Functional

* **Low latency** (<10ms in region, <100ms cross-region)
* **High throughput** (billions/day ≈ millions/minute)
* **Scalability** (horizontally scalable producers, brokers, consumers)
* **High availability**
* **Geo-redundancy / disaster recovery**
* **Security** (authentication, authorization, encryption)

---

## 2. APIs

```http
POST /topics/{topic}/publish
Body: { key: string, value: string, headers?: Map }

GET /topics/{topic}/subscribe?group={group}&offset={offset}
Response: { messageId, key, value, headers }

POST /topics/{topic}/ack
Body: { messageId }

POST /topics
Body: { name, partitions, retentionPolicy }

DELETE /topics/{topic}
```

---

## 3. High-Level Architecture

```
        [Producers]             [Consumers]
             |                        |
             v                        ^
       +-----------+         +--------------+
       |  Ingress  | <-----> |  Egress/API  |
       +-----------+         +--------------+
             |                        ^
             v                        |
       +------------------------------+
       |       Distributed Broker     |
       | +----------+  +----------+   |
       | | Partition|  | Partition|...|
       | |   Leader |  |   Follower|  |
       | +----------+  +----------+   |
       +------------------------------+
             |
             v
       +--------------------+
       | Distributed Storage|
       +--------------------+
             |
             v
       +----------------------+
       |  Replication Engine  |
       +----------------------+
             |
             v
       +--------------------+
       |   Cross-Region     |
       | Replication Layer  |
       +--------------------+
```


### 1. Producers

* **Who they are**: Applications or services sending messages to topics.
* **Responsibilities**:

  * Choose a topic.
  * Optionally use a partitioning key.
  * Send data with low latency and retries for fault tolerance.


### 2. Ingress

* **Role**: First point of contact for producers.
* **Key Functions**:

  * Authentication / authorization.
  * Routing to the correct topic partition.
  * Apply backpressure control (rate limiting, rejecting large batches).
  * Validate and batch incoming messages.
  * Forward to partition leaders for writing.
* **Design Note**: Deployed across edge regions for low latency.


### 3. Egress/API Layer

* **Role**: Interface for **consumers** to pull messages or **admin users** to manage topics.
* **Key Functions**:

  * Handle `subscribe`, `ack`, and offset management.
  * Deliver messages to consumers based on offset & group.
  * Support replay and consumer group balancing.
* **Why separate from Ingress**:

  * Different traffic patterns.
  * Independent scaling.
  * Security separation.

### 4. Distributed Broker

* **Core Component**.
* Manages:

  * **Partitions**: A topic is divided into partitions for parallelism.
  * **Leaders**: Each partition has a leader responsible for writes and reads.
  * **Followers**: Maintain replicas for durability and failover.

#### Internals:

* Message indexing and log segmentation.
* Handling consumer offsets and acknowledgments.
* Transaction/state coordination for exactly-once delivery.


### 5. Partition (Leader/Follower)

* **Partition Leader**:

  * Handles **writes** and **reads**.
  * Responsible for ordering, batching, and replication.
* **Followers**:

  * Replicate logs from the leader.
  * Can serve **read-only** queries in geo-local regions.
* **Why Partitioning**:

  * Scale throughput horizontally.
  * Enable parallelism across consumers.


### 6. Distributed Storage

* **Purpose**: Store partition logs persistently and durably.
* **Design Choices**:

  * Local SSDs + tiered storage (e.g., S3/GCS for cold data).
  * Use LSM-based stores (e.g., RocksDB) or append-only file systems.
* **Data Lifecycle**:

  * Write → Replicate → Retain → Compaction → Archive.

### 7. Replication Engine

* **Role**: Maintain consistency and availability across replicas.
* **Techniques**:

  * Leader-based replication (like Kafka ISR or Raft).
  * Sync or async modes.
  * Catch-up for lagging followers.
* **Failure Recovery**:

  * Detect leader failure and promote follower.
  * Commit log is quorum-based for durability.


### 8. Cross-Region Replication Layer

* **Why needed**:

  * Improve latency for global users.
  * Ensure disaster recovery and data sovereignty.
* **How it works**:

  * Read logs from origin region and replicate to remote regions.
  * Can support:

    * **Active-active**: Multiple leaders (more complex).
    * **Active-passive**: Single leader, replicated data read in other regions.
* **Optimizations**:

  * Deduplication, compression, delta syncs.


### 9. Consumers

* **Who they are**: Applications that read messages.
* **Pull model**:

  * Poll data from specific topic/partition and offset.
  * Can be in consumer groups (for horizontal scaling).
* **Offset Tracking**:

  * Managed by broker or external store (e.g., ZooKeeper, database).


### Summary Table

| Component           | Main Function                      | Scalability Model         |
| ------------------- | ---------------------------------- | ------------------------- |
| Producers           | Send messages                      | Horizontally scalable     |
| Ingress             | Authenticate, batch, route         | Deployed per region       |
| Broker + Partitions | Core message storage + replication | Partitioned by topic      |
| Storage Layer       | Durable storage of logs            | Tiered, append-only       |
| Replication Engine  | Ensure durability across replicas  | Quorum-based or async     |
| Cross-Region Layer  | Replicate across geographies       | Async, pluggable          |
| Egress/API          | Expose messages to consumers       | Scales with consumer load |
| Consumers           | Pull messages + manage offsets     | Scales with partitions    |

---

## 4. Deep Dives

### A. **Partitioning & Load Balancing**

* Each topic is partitioned.
* Each partition has a **leader** broker and **replicas**.
* **Hash(key) mod N** to route messages to partitions.
* Consumers can be in **consumer groups**, each assigned specific partitions.

### B. **Storage Engine**

* Append-only log per partition (like Kafka).
* Segmented files with index structures.
* Time-based or size-based **retention policy**.
* Snapshotting & compaction for cleanup.

### C. **Replication**

* Leader-follower model for each partition.
* Synchronous or asynchronous replication depending on durability SLA.
* Replication factor ≥ 3 (ideally across availability zones).

### D. **Cross-Region Replication**

* Async replication of partitions across regions.
* Used for failover and consumer locality.
* May support **active-active** or **active-passive** configs.

### E. **Delivery Guarantees**

* **At least once**: Store message, then ack to producer.
* **Exactly once**: Requires idempotence on producer + transactional semantics.

### F. **Back Pressure & Flow Control**

* Pull-based consumption with quotas.
* Throttling producers/consumers.
* Batching + compression to reduce load.

---

## 5. Strategies for Low Latency & High Throughput (Geo-Distributed)

### 1. **Edge Ingress Points**

* Deploy ingress nodes close to producers using global Anycast DNS.
* Local write-ahead before global replication.

### 2. **Partition Affinity**

* Route producers/consumers to brokers near their region.
* Use **geo-aware partition placement**.

### 3. **Batching & Compression**

* Batch small messages together.
* Use LZ4/Snappy for fast compression with minimal CPU cost.

### 4. **Zero Copy Techniques**

* Leverage OS page cache and memory-mapped files (mmap) for log read/write.

### 5. **Asynchronous Replication**

* Use async replication across regions to avoid cross-region latency.

### 6. **Consumer Locality**

* Geo-aware routing: consumers pull from the nearest available replica.

### 7. **Dedicated Replication Pipeline**

* Separate internal traffic (replication) from external traffic (client API).

---

## 6. Scalability to Billions of Requests / Day

* Use **horizontal scaling**: brokers, partitions, consumers, ingress nodes.
* **Sharded metadata storage** (e.g., ZooKeeper, Raft-based metadata store).
* **High-throughput storage systems** (e.g., RocksDB, custom log storage).
* Monitor with **Kafka-style offsets**, metrics collection, and autoscaling.

---

## 7. Technologies / Inspiration

* Kafka (log-oriented, pull-based)
* Pulsar (separate compute & storage)
* NATS Jetstream (low latency, lightweight)
* Redpanda (zero copy, native Raft)
* Custom Raft/Log-based implementation with S3 for infinite retention

---
