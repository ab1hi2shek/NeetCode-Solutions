# Design Stock exchange (real-time pricing & alerts)

## Functional Requirements

1. Users should be able to see real-time prices of assets.
2. Users should be able to create alerts and get real-time notifications when criteria is met.
3. Users should be able to see the real-time average price of the assets.
4. User should be able to see the historical prices (and average prices) of the assets.

## Non-Functional requirements

1. Availability: 99.99% uptime.
2. Latency: Alert delivery in <200ms from price update and real-time price.
3. Scalability: Handle 100M DAU, 500B alerts in system total.
4. Cost: Minimize outbound connections to exchanges.

## Back-of-the-Envelope Calculation
**Assets and Price Updates:**
 - 10K assets × 1 update/sec = 10K updates/sec
 - Assume update payload size = 200B → 2MB/sec, manageable.

**Alerts:**
 - 100M users × 50 alerts = 5B alerts total
 - If 1% assets hit thresholds each second → ~100 assets/second
 - Assume 10M alerts attached to those 100 assets → 10M alerts evaluated/sec

**Notification Volume:**
 - If 0.01% alerts trigger every second → 1K notifications/sec
 - Notification payload ≈ 500B → 0.5MB/sec

 ## High level design

 ![image](/assets/real_time_pricing.png)

### Flow Steps

**1. Exchange Price Feed Ingestion**
 - Each exchange connection is handled by a small number of `update service`.
 - Each connector receives price updates (e.g., via WebSocket, FIX, or REST poll).
 - Feeds are published into Kafka topics partitioned by asset `asset-btc, asset-eth, ..., asset-aapl`.

**2. Kafka → Redis Price Updater**
 - Consumes Kafka topics.
 - For each update:
   - Writes to Redis key: price:BTC = {value, timestamp}.
   - Publishes to Redis Pub/Sub: `channel:price:BTC`

**3. Client Requests Real-Time Price**
 - Hits GET /realtime-price?asset=BTC endpoint.
 - API Service or WebSocket Gateway subscribes to:
 - Redis Pub/Sub `channel:price:BTC`.

**4. On Redis Publish**
 - Pricing Service receives message instantly.
 - Forwards to connected WebSocket/SSE clients.

## Deep Dives

### ❓ Some issues and fixes

| Issue                                    | Concern                                                                             | Solution                                                                  |
| ---------------------------------------- | ----------------------------------------------------------------------------------- | ------------------------------------------------------------------------- |
| Redis Pub/Sub is volatile              | Messages are lost if a service is down or reconnects late.                          | Use **Redis Streams** (instead of pure pub/sub) if you need durability. Same low-latency, but adds durability, backpressure handling, and replay.   |
| Redis Pub/Sub doesn’t scale infinitely | Millions of subscriptions can choke Redis.                                          | Horizontally partition Redis per asset class (BTC, STOCK, etc.).          |
| User to Channel Mapping               | Redis doesn't track subscribers — you need to manage sessions + mapping separately. | Maintain user-to-channel mapping in app memory or lightweight Redis sets. |
| No Replay of Missed Updates           | New connections miss events published before they subscribed.                       | Again, Redis Streams can fix this. |


### Why Cache Real-Time Prices in Redis if Using Pub/Sub?
We cache the latest prices in Redis in addition to using Pub/Sub so that non-streaming consumers (e.g. APIs, dashboards, alerting systems) can get the latest price instantly without waiting for the next pub/sub message.

1. Pub/Sub = push-based, low-latency updates to subscribed clients (e.g., WebSocket).
2. Cache = pull-based, fast lookups for APIs, alert checks, UI, or fallback.

------

### ❓ Where to Store User Alerts?

#### Option 1: DynamoDB (Recommended)
| Feature           | Benefit                                                       |
| ----------------- | ------------------------------------------------------------- |
| Low latency     | \~1–10ms per read/write                                       |
| Scalable       | Automatically handles 100M+ users, high write/read throughput |
| Cost-efficient | Cheaper than Elasticsearch or relational DBs at this scale    |
| TTL support    | Can auto-expire alerts (if alerts are time-bound)             |
| Query model    | Use GSI for `asset + price condition` lookup                  |


**Suggested schema**

| Field             | Type          | Notes                     |
| ----------------- | ------------- | ------------------------- |
| `user_id`         | Partition key |                           |
| `alert_id`        | Sort key      | UUID or timestamp-based   |
| `asset_symbol`    | String        | e.g., `BTC`, `AAPL`       |
| `operator`        | String        | `>=`, `<=`, etc.          |
| `threshold_price` | Number        | e.g., `100`               |
| `channel`         | String        | `email`, `push`, etc.     |
| `triggered`       | Boolean       | To avoid duplicate alerts |

You can also add a GSI on asset_symbol to find all alerts for BTC, etc.


#### Option 2: Elasticsearch (Only If Advanced Filtering Needed)

| Pros                                   | Cons                                          |
| -------------------------------------- | --------------------------------------------- |
| 🔍 Complex filters (e.g. `BTC <= 100`) | Costly at scale                             |
| ⚡ Fast numeric range scans             | Adds operational overhead                   |
| 🧠 Ideal for analytics & debugging     | Not needed unless search is a core use case |

Use Elasticsearch only if you need rich alert querying/filtering by price range + metadata — not ideal for real-time alert trigger flow due to cost.

#### Option 3: Redis/In-Memory (Temporary)

| Use Case                         | Recommended? |
| -------------------------------- | ------------ |
| Cache active alerts in memory | ✅ Yes        |
| Store all alerts permanently   | ❌ No         |

→ Keep active alerts (like for BTC, TSLA, etc.) in Redis/memory as a performance optimization only — fetch from DB on warm-up or cache-miss.


-----
-----


### ❓ Lifecycle of an Alert
1. User creates alert → Stored in DynamoDB.
2. Warm cache (Redis or in-memory) with relevant alerts per asset.
3. Price Evaluator reads prices from Kafka and evaluates alerts.
4. If price crosses threshold:
    - Mark alert as triggered in DB.
    - Send event to Notification Service.
5. Remove alert from cache.

-----
-----

## ❓ AvoidDe-duplication of Alerts
You handle alert removal post-trigger. Add a `triggered=true flag` in DB before sending alert to Notification Service to:
1. Avoid race condition (e.g. double firing if evaluator retries).
2. Prevent duplicate notifications.

----
-----

## ❓ Redis vs Apache Flink for real-time average + historical aggregate use case

Let’s compare Redis vs Apache Flink for real-time average + historical aggregate use case.

#### When to use Redis + Kafka Consumer (custom logic)

**Architecture:**
Kafka consumer maintains rolling window in Redis or in-memory. Periodically flushes aggregates to TimescaleDB or DynamoDB

**Pros**
1. Simple, cost-effective for <10K assets
2. Redis is blazing fast for pub/sub + real-time cache
3. Easy to deploy and scale horizontally

**Works well if:**
1. You have your own logic (e.g., multiple alerting strategies)
2. You want full control over aggregation and processing

**Cons**
1. Requires manual management of time windows
2. Not ideal for complex event processing (e.g., joins, pattern matching)

#### When to use Apache Flink
**Architecture**
1. Kafka → Flink job → Compute aggregates
2. Flink → Write to Redis (for real-time) + DB (for historical)

**Pros**
1. Built-in support for sliding, tumbling, session windows
2. Designed for high-throughput, low-latency stream processing
3. Scales better than Redis-only pipeline for large asset counts or complex logic

**Ideal for:**
1. Large-scale streaming (10M+ events/sec)
2. Complex aggregations, joins, watermarking
3. Stateful stream processing

**Cons**
1. More operational overhead (Flink cluster, state backend)
2. Slightly higher latency vs Redis
3. Requires Flink expertise

#### Recommendation for this Case
For usecase: 10K assets, 100M DAU, 1 tick/sec per asset → 10K ticks/sec

That’s very manageable.

👇 Start With: Redis + Kafka consumer, custom logic in a lightweight service (Go/Java)
1. Simpler, easier to deploy
2. Great for early/mid-scale
3. Store real-time rolling average in Redis
4. Dump minute aggregates to TimescaleDB

📈 Scale Up Later (if needed) i.e. Switch to Apache Flink when:
1. You need multi-stage stream pipelines
2. Or handle millions of price updates per second
3. Or want stateful stream processing at scale


**Use of Apache Flink**

```
In Flink Job – You Do Two Things:
1. Compute rolling window average. E.g., 1-min, 5-min, 15-min TWAP (time-weighted avg price)

2. Write results:
  - To Redis for fast access (e.g., APIs, pricing dashboards)
  - To TimescaleDB for storage (e.g., historical charts)
```

**Here, what redis data structure to use?** 

Use Normal Redis Key-Value as only storing aggregates per window. Flink already handles the aggregation window internally, so storing all individual ticks in Redis may be redundant

```
SET avg:BTC:1min 105.23
SET avg:BTC:5min 104.87
HSET avg:BTC current 105.5 1min 105.23 5min 104.87
```


-----
-----

## ❓ Why use redis pub/sub if kafka can directly update the pricing service?

You can — and in some cases, Kafka → Pricing Service directly is good enough.

But Redis Pub/Sub is often used in addition to Kafka for performance and simplicity in real-time fanout to many clients (e.g., WebSocket users). Let’s break it down:

| Feature                   | Kafka                                                             | Redis Pub/Sub                                        |
| ------------------------- | ----------------------------------------------------------------- | ---------------------------------------------------- |
| **Best for**              | Persistent event log, stream processing                           | Ultra-fast real-time message delivery                |
| **Latency**               | \~10–50ms (pull-based or consumer lag possible)                   | \~1–2ms (push-based)                                 |
| **Fanout**                | Not ideal for 1M+ users (each needs Kafka consumer or state mgmt) | Very efficient pub/sub to 100K+ subscribers          |
| **Persistence**           | Durable (log-based)                                               | Ephemeral (no history, not persisted)                |
| **Client types**          | Complex to use Kafka on browsers or WebSockets                    | Easy to connect Redis pub/sub to WebSocket service   |
| **Backpressure handling** | Designed for durability, not tight real-time SLAs                 | Best-effort delivery, low-latency                    |
| **Scaling**               | Kafka clients need to track offsets, partitions, etc.             | Redis pub/sub is simple to scale for frontend fanout |


**Redis Pub/Sub Advantages Here:**
1. WebSocket/SSE-based clients can listen to Redis directly
2. Decouples pricing UI delivery from Kafka internals
3. Redis handles fast fanout to thousands/millions of users
4. No need for each client/web socket to have its own Kafka consumer
5. Low latency & less operational overhead compared to Kafka for this use case

**Summary**

| Scenario                                                     | Recommendation                    |
| ------------------------------------------------------------ | --------------------------------- |
| You want **durable, persistent stream processing**           | Use **Kafka**                     |
| You want **fast, real-time push to UI clients (WebSockets)** | Use **Redis Pub/Sub**             |
| You want both                                                | Kafka → Processor → Redis Pub/Sub |

----
-----

## ❓ WebSocket Pricing Service Scalability

You’ll eventually hit scaling limits with a single service doing:
1. Managing 1M+ concurrent WebSocket connections
2. Receiving updates
3. Pushing updates to correct clients

**To fix this, split responsibilities(Split into 2 Components)**

#### 1. Connection Manager
- Handles WebSocket connections.
- Keeps track of:
    - Client ID
    - Subscribed asset topics
    - WebSocket channel

- Stores this mapping in an in-memory registry (or Redis/Shared MemStore in cluster).

Can scale horizontally with sticky sessions + load balancer OR use something like NATS JetStream / WebSocket Gateway with Redis Stream as backend.

#### 2. Message Distributor
- Subscribes to Redis pub/sub (or Kafka topic).
- On receiving new price for Asset X:
    - Looks up subscribers for X (via in-memory or Redis lookup)
    - Sends data to the relevant WebSocket connections

**Benefits of Separation**
| Concern                | Impact                                                         |
| ---------------------- | -------------------------------------------------------------- |
| Horizontal scalability | Connection Manager can scale independently of pub/sub workload |
| Reduced latency        | Distributor can be optimized purely for fast delivery          |
| Failure isolation      | One doesn’t take down the other                                |
| Easier sharding        | Per-asset or per-user partitioning becomes possible            |


----
-----

## ❓ Flink Fault Tolerance (High Availability & Exactly-Once Guarantees)

1. Enable Checkpointing
- Flink supports stateful operations — to make them fault-tolerant:
- Enable checkpointing (e.g., every 30s or 60s).
2. Use Durable State Backend
 - RocksDB with S3 or HDFS for durability.
 - This stores state (e.g., windowed averages) reliably across restarts.

----
----

 ## ❓ How do you scale?

 Here’s a comprehensive breakdown of how to **scale each component** of your real-time pricing, average, alerts, and historical data system for **100M Daily Active Users (DAU)**, **thousands of assets**, and **billions of price ticks and alerts**:


### 1. **API Gateway & Load Balancer**

* **Horizontal Scaling**: Deploy multiple gateway instances behind a global load balancer (like AWS ALB + Route53, Cloudflare).
* **Rate Limiting & Auth**: Use distributed rate limiters (e.g., Envoy, Kong with Redis) and token-based auth (JWT + CDN edge caching).
* **Edge Locations**: Use CDN (Cloudflare Workers, Fastly, Lambda\@Edge) for request routing and caching static/near-static data.


### 2. **Pricing Service (WebSocket Real-time Updates)**

* **Connection Sharding**: Distribute connections by asset ID or region across pricing service pods.
* **WebSocket Fanout**: Use systems like **NATS**, **Socket.IO Redis adapter**, or **Kafka + WS fanout layer** to scale WebSocket broadcasts.
* **Backpressure Management**: Implement dropping/throttling strategies or queue buffers to prevent overload.
* **Stateless Design**: Store connection/session metadata in Redis or DynaCache (e.g., for reconnect logic).


### 3. **Kafka**

* **Topic Partitioning**: Partition by asset or asset+exchange combo to parallelize processing.
* **Cluster Scaling**: Scale brokers, controller quorum (KRaft mode), disk throughput (SSD), and network bandwidth.
* **Retention Strategy**: Keep only short-term raw ticks (1–3 days) if not needed forever; use Flink to materialize aggregates elsewhere.


### 4. **Apache Flink**

* **Horizontal Scaling**: Increase task managers and parallelism.
* **State Backend**: Use RocksDB with incremental checkpointing + external durable storage (S3/GCS) to recover state.
* **Checkpointing**: Configure 5–10s interval for fast failure recovery.
* **Job Segregation**: Separate jobs for average computation, aggregates, alert pre-processing to isolate failure domains.


### 5. **Redis (Real-time Prices + Pub/Sub + Alert Cache)**

* **Sharding**: Use Redis Cluster to shard by asset or alert ID.
* **Persistence**: Turn on RDB or AOF depending on importance of recovery.
* **Scaling Pub/Sub**: Redis Pub/Sub is limited; use Redis Streams or a dedicated pub/sub system (NATS, Kafka) if scale hits limits.
* **Eviction Policy**: Use LRU for real-time prices; TTL for alerts.


### 6. **TimeScaleDB (Historical Data)**

* **Time Partitioning**: Use time + asset-based partitioning and hypertables.
* **Compression**: Enable native compression for old data.
* **Write Scaling**: Use parallel COPY or batching via Kafka Connect → Timescale.
* **Read Scaling**: Read replicas for chart rendering, trends, or exports.


### 7. **Price Evaluator Service**

* **Horizontal Scaling**: Stateless service – scale based on partitions or asset load.
* **Efficient Polling**: Use Redis ZSET or LIST with blocking pop for new ticks.
* **Batch Evaluation**: Process multiple alerts per tick in one shot.
* **Worker Partitioning**: Route alerts by user ID hash or asset ID to scale evaluation.


### 8. **Alert Service**

* **High-Volume Write Support**: Use batched writes to DynamoDB with write throughput tuning.
* **Query Patterns**: Keep active alerts in Redis for fast access; batch refresh from DB every few minutes.
* **Async Processing**: Queue heavy operations (like alert cleanup, stats) to background workers (SQS + Lambda or Kafka).


### 9. **DynamoDB (Alert Storage)**

* **Partition Key Design**: Use `userId#assetId` or `alertId` pattern to spread partitions.
* **Auto Scaling**: Enable on-demand mode or provisioned with auto-scaling.
* **Streams**: Use DynamoDB Streams to react to alert updates or audit logs.


### 10. **Notification Service (Push + Email)**

* **Queue-based Fanout**: Use SQS/Kafka to buffer notifications per channel.
* **Deduplication**: Alert ID + timestamp de-duplication before pushing.
* **Provider Scaling**: Use managed push/email providers (SNS, SES, Firebase) with rate control.
* **Retries**: Implement retry queues for failed notifications.


### Scaling Strategy Summary by Load Domain

| Load Domain            | Strategy                                                             |
| ---------------------- | -------------------------------------------------------------------- |
| **100M DAU**           | WebSocket sharding, CDN for APIs, distributed pricing/alert service  |
| **10K Assets**         | Kafka topic partitioning, Redis clustering, asset-wise pricing state |
| **Billions of Ticks**  | Flink + Kafka, short retention in Kafka, rollup aggregates           |
| **Billions of Alerts** | Evaluate in batches via Flink or workers, Redis + DynamoDB combo     |


----
----

## ❓ Latency: Serve real-time pricing, alerts, and average prices with ultra-low latency

Here’s a **deep dive into how to serve real-time pricing, alerts, and average prices with ultra-low latency**, based on your HLD:


## 1. **Real-time Price Serving (Ultra-low Latency)**

### Flow:

1. **Exchange → Kafka**:
   Price ticks per second per asset pushed to Kafka (partitioned by asset).

2. **Kafka → Redis** (via Flink or lightweight consumer):

   * Update **latest price** in Redis (`SET assetId currentPrice`) for fast lookup.
   * Use **Redis Pub/Sub** (`PUBLISH assetId price`) to fanout to pricing services.

3. **Client → Pricing Service (WebSocket/SSE)**:

   * User subscribes to asset stream.
   * Pricing service subscribes to Redis Pub/Sub channel for that asset.
   * On new price → send it directly over WebSocket to client.

### Why This Is Ultra-fast:

| Layer     | Tech Used | Reason for Low Latency                 |
| --------- | --------- | -------------------------------------- |
| Ingestion | Kafka     | High-throughput, horizontally scalable |
| Cache     | Redis     | Sub-millisecond read + pub/sub         |
| Delivery  | WebSocket | Persistent low-latency connection      |

### Optimization Tips:

* **Avoid REST APIs** for live prices.
* Use **Gzip or Protobuf** for WebSocket payloads.
* Tune Redis with `tcp_nodelay` and optimized eviction policy.


## 2. **Real-time Alert Evaluation**

### Flow:

1. **Price Tick → Kafka**: as before.
2. **Flink Job (or custom Kafka consumer)**:

   * Subscribes to price ticks.
   * Pulls **active alerts** from Redis (`ZSET: assetId -> [alertThresholds]`).
   * If `currentPrice <= alert.threshold`, mark alert as triggered.
3. **Send to Notification Service** via Kafka or SQS for downstream fanout (Push/Email).
4. **Deduplication & TTL**: Track sent alerts in Redis (`SET alertId:triggered`) to avoid duplicates.

### Storage Strategy:

* **DynamoDB**: Persistent alert definitions (indexed by `userId`, `assetId`).
* **Redis**: Hot cache of active alerts per asset for fast access during evaluation.

### Low Latency Guarantees:

* Redis read time for alerts <1ms.
* Kafka-to-Flink-to-notification pipeline end-to-end target latency: **<300ms**.
* Alerts sent within sub-second of price condition match.

### Best Practices:

* Keep alerts in **sorted sets** (`ZADD assetId alertThreshold`) → fast range queries.
* In Flink or consumer, **batch evaluate per asset tick**.
* **Use local state or in-memory alert store** (backed by Redis snapshot) to avoid DynamoDB lookup per tick.

## 3. **Real-time and Historical Averages**

### Flow:

1. **Flink Job (Aggregation)**:

   * Subscribes to Kafka price ticks.
   * Maintains a sliding window (1-min, 5-min, hourly) for each asset.
   * Emits aggregates like average, high, low, volume to:

     * **Redis** for real-time queries.
     * **TimescaleDB** for historical queries.

2. **Client Requests Average**:

   * **Current average** from Redis (`GET assetId:avg:1m`, `5m`, etc.).
   * **Historical averages** from TimescaleDB using assetId and time range.

### Speed Factors:

| Data Type  | Store       | Latency               |
| ---------- | ----------- | --------------------- |
| 1-min Avg  | Redis       | \~1ms                 |
| Historical | TimescaleDB | \~100ms+ (for charts) |

### Redis Key Schema (examples):

```bash
SET btc:avg:1m 27450.32
SET btc:avg:5m 27388.12
```

### Optimization:

* Pre-compute common durations.
* Use **TimeWindowed Aggregates in Flink**.
* Set TTL in Redis (e.g., 30 minutes) to prevent bloating.

---

## Overall Real-Time Guarantees

| Use Case            | Target Latency  | Key Enabler                        |
| ------------------- | --------------- | ---------------------------------- |
| Live Price Display  | \~50ms – 100ms  | Kafka + Redis + WS                 |
| Alert Triggering    | \~200ms – 500ms | Redis + Flink/Kafka + Async Notify |
| Real-Time Avg Query | \~1ms           | Redis + Pre-aggregation            |
| Historical Query    | \~100ms – 300ms | TimescaleDB (read-optimized)       |


## When Not to Use Kafka → Redis → Pub/Sub

| Case                               | Recommendation                                                       |
| ---------------------------------- | -------------------------------------------------------------------- |
| Ultra-high volume topics (1M+/sec) | Use **Kafka Streams or Flink only**                                  |
| Pub/Sub scaling limits (Redis)     | Replace Redis Pub/Sub with **NATS, Apache Pulsar, or Kafka streams** |
| Complex stream joins/aggregates    | Use **Apache Flink or ksqlDB**                                       |

----
----


## ❓ How do you ensure 99.99% availability?

To ensure **99.99% availability** (\~52.6 minutes downtime/year) for a **real-time pricing and alerting system** at this scale (100M DAU, 10K assets, billions of alerts), you must design for **resilience, redundancy, isolation, and failover** across all layers.


## 1. **Infrastructure & Hosting**

### Multi-Region, Multi-AZ Deployment

* Deploy all services across **at least 3 Availability Zones**.
* Use **multi-region failover** (e.g., AWS Global Accelerator + Route53 for latency routing).
* Use **health checks + circuit breakers** for failover.

### Container Orchestration (K8s / ECS)

* Auto-healing pods
* Horizontal Pod Autoscaling (based on CPU/network usage)


## 2. **Data Layer Availability**

### **Redis (Real-Time Cache)**

* Use **Redis Cluster** (multi-node with partitioning + replication)
* Enable **Redis Sentinel** or AWS ElastiCache Multi-AZ
* Use **read replicas** for scale

### 🔹 **Kafka (Streaming Ingestion)**

* Deployed in **multi-AZ cluster** with minimum 3 brokers
* Replication factor of **≥3** for each topic
* Use **Kafka MirrorMaker** for region-to-region failover

### **Flink (Aggregations & Alert Evaluation)**

* Deployed in **high-availability mode** with checkpointing (HA job manager)
* Store state in **durable, replicated storage** (e.g., S3, GCS)

### **Persistent DB (DynamoDB / TimescaleDB)**

* **DynamoDB**: Multi-region global tables, automatic replication
* **TimescaleDB**: Deployed with streaming replication + read replicas across regions


## 3. **APIs & WebSocket Services**

* Use **load balancers** (e.g., ALB/NLB) with health checks and zone failover
* **WebSocket Gateway** (e.g., AWS API Gateway + Lambda or custom K8s WS services) should autoscale
* Use **retry policies** for all clients

---

## 4. **Alerting System Reliability**

* Alerts can’t be missed → **durable queueing + retry**
* Use Kafka + backup SQS for notifications
* Ensure **idempotency** and **deduplication** (Redis keys or UUIDs)
* Fall back to email if push fails


## 5. **Resilience Patterns**

| Pattern                  | Description                                               |
| ------------------------ | --------------------------------------------------------- |
| **Retry with Backoff**   | Retry transient failures (with circuit breaker)           |
| **Bulkheads**            | Isolate alert evaluation, pricing, storage systems        |
| **Rate Limiting**        | Protect downstream services                               |
| **Graceful Degradation** | If real-time data fails, serve last known data from Redis |
| **Dead Letter Queues**   | Catch alert delivery failures for later retry             |


## 6. **Monitoring & Auto-Healing**

* Use Prometheus + Grafana + AWS CloudWatch
* Alert on:

  * Kafka lag
  * Redis replication failure
  * WS connection error spike
  * Flink checkpoint failures
* Auto-scale components based on:

  * Kafka consumer lag
  * Redis CPU/memory
  * API/WebSocket QPS


## 7. **Backups and Disaster Recovery**

* Periodic snapshots of:

  * DynamoDB (or PostgreSQL/TimescaleDB)
  * Redis RDB/AOF dumps (with hourly backups)
  * Kafka topic data (mirror or cold backup to S3)
* RTO (Recovery Time Objective): < 5 mins
* RPO (Recovery Point Objective): < 1 min


## 8. **Testing for High Availability**

* Chaos Engineering (e.g., Netflix’s Chaos Monkey)
* Simulate:

  * Redis node loss
  * Kafka broker outage
  * Flink JobManager crash
  * Region-wide failover


## Summary: What Enables 99.99%?

| Component                  | Strategy                                       |
| -------------------------- | ---------------------------------------------- |
| **Kafka**                  | Multi-AZ, replicated, failover via MirrorMaker |
| **Redis**                  | Cluster + replicas + Sentinel                  |
| **Flink**                  | HA mode, checkpointing                         |
| **DB (Dynamo/TSDB)**       | Multi-region, replication                      |
| **Pricing/Alert Services** | Auto-scaled, stateless, retries                |
| **Notifications**          | Queued + fallback paths                        |
| **Monitoring**             | Full-stack + self-healing triggers             |
| **Disaster Recovery**      | Automated, under 5 min RTO/RPO                 |


---
---

## ❓ Should pricing service (real-time pricing component) subscribe to kafa topics or redis topics

Great question. Let’s break down **how subscribing to Kafka vs Redis Pub/Sub actually works**, both **conceptually** and in terms of **implementation and behavior**.

### 🔄 1. **Redis Pub/Sub – How It Works**

### Concept

* Publisher sends a message to a **channel** (e.g., `"AAPL"`).
* All clients **currently subscribed** to that channel get the message.
* If a subscriber is **disconnected**, the message is **lost** for them.

### Features

* **Ephemeral**: No persistence.
* **One-to-many**: Message fan-out to all current subscribers.
* **No backpressure**, **no retries**, **no delivery guarantees**.
* Super low latency, but fragile under load or failures.

---

### 2. **Kafka – How It Works**

### Concept

* Producer writes to a **topic** (e.g., `"price.AAPL"`).
* Consumers read from the topic using a **consumer group** and **offset tracking**.
* Messages are **persistent**, **ordered**, and **replayable**.
* Consumers can be **stateless** and recover from crashes.

### Features

* **Durable**: Messages stored for hours/days/weeks.
* **Scalable**: Partitions allow horizontal scaling.
* **Replayable**: Re-read from any offset.
* **Backpressure-aware**: Consumers pull at their own pace.
* Ideal for large-scale systems.

## 🔍 Key Differences for Subscription Logic

| Feature              | Redis Pub/Sub                    | Kafka                                  |
| -------------------- | -------------------------------- | -------------------------------------- |
| **Connection type**  | TCP, keeps connection open       | TCP, but allows reconnect/retry        |
| **Message delivery** | Fire-and-forget (only live subs) | Reliable, guaranteed once-per-consumer |
| **Consumer scale**   | All consumers get same message   | Each consumer group gets its own copy  |
| **Persistence**      | ❌ No                             | ✅ Yes (disk-backed)                    |
| **Message replay**   | ❌ No                             | ✅ Yes (via offsets)                    |
| **Use case**         | Light, fast, small-scale fan-out | Reliable stream processing, high-scale |

---

## 🧠 When to Use Which?

| Situation                                             | Use Redis Pub/Sub | Use Kafka |
| ----------------------------------------------------- | ----------------- | --------- |
| Just need to push data fast to open WebSocket clients | ✅                 | ❌         |
| Need to persist and replay missed updates             | ❌                 | ✅         |
| Handle 100K+ messages/sec and durable fan-out         | ❌                 | ✅         |
| Keep infra light/simple                               | ✅                 | ❌         |
| Expect consumer crashes, retries, scaling             | ❌                 | ✅         |



### 🧠 Redis Pub/Sub vs Kafka — Summary of Use Cases for real-time pricing

| Feature                    | **Redis Pub/Sub**                    | **Kafka**                                        |
| -------------------------- | ------------------------------------ | ------------------------------------------------ |
| **Latency**                | Ultra-low (\~sub-millisecond)        | Low (<10ms with tuning)                          |
| **Durability**             | ❌ No                                 | ✅ Yes                                            |
| **Message replay**         | ❌ No                                 | ✅ Yes (offsets per consumer)                     |
| **Backpressure support**   | ❌ None                               | ✅ Built-in                                       |
| **Horizontal Scalability** | ❌ Poor (single shard)                | ✅ Excellent (partitioning + brokers)             |
| **Use Case**               | Best for short-lived, ephemeral data | Best for large-scale streaming, recovery, replay |

---

### What You Should Do in a Large Real-Time System

#### 🏗️ **Architecture: Kafka as Source of Truth, Redis for Speed**

Use **Kafka as your central event backbone**, and optionally **Redis Pub/Sub or Redis cache for fast access** at the edge.

#### Example:

1. **Price Feed (e.g., Bloomberg, Coinbase, NYSE)** → Streams price ticks to **Kafka topics** (e.g., `price.AAPL`, `price.BTC`).
2. **Pricing Service**:

   * Subscribes to relevant **Kafka topics**.
   * Processes and stores the latest price, triggers alerts, etc.
   * Optionally **writes the latest price to Redis** for fast reads or local caching.
3. **Frontend / Alert Workers**:

   * Fetch latest price from Redis (low latency).
   * For events or triggers, consume from Kafka or receive push from a worker.

---

### 🔁 When to Add Redis Pub/Sub (Optional Layer)

You can still use **Redis Pub/Sub** for ultra-fast local broadcast — like this:

* Kafka → Pricing Service → publishes to Redis Pub/Sub for **local consumers** (e.g., frontend WebSocket workers).
* Redis delivers to active connections (WebSocket clients).
* This **reduces Kafka fan-out pressure** for user-facing systems.

**But:**

* Don't rely on Redis Pub/Sub as your primary bus.
* Don't expect it to handle persistence, replay, or fault tolerance.

---

### 🔧 Conclusion: Should Pricing Service Subscribe to Kafka Directly?

> ✅ **Yes. Your Pricing Service should subscribe directly to Kafka topics.**
> Kafka is your reliable, scalable event bus.

Then you can optionally push processed output to Redis (as:

* `SET price:AAPL 191.56`
* or publish to `redis.publish('AAPL', price)` for WebSocket delivery).

---
---

## What if we do this?:

* The **Pricing Service** subscribes to **Kafka topics** (1 per asset or asset class).
* Clients open **WebSocket connections** with the **Pricing Service**.
* Pricing Service pushes asset updates to users from Kafka.


## Why This Is *Not* Ideal

### 1. **Kafka is Pull-Based**

* Kafka is designed for **pulling** data with consumers.
* Your Pricing Service would need to **poll and buffer messages**, then push to WebSocket clients.
* This adds latency and complexity for near-real-time fan-out.

### 2. **Kafka ≠ Fan-out Layer**

* Kafka does **not natively support 100M+ fan-out**.
* You cannot have 1 Kafka consumer per user or per user group — that’s too expensive.
* Kafka consumers are meant to be **few in number**, not millions.

### 3. **Rebalancing Overhead**

* Kafka consumer group rebalancing can be disruptive.
* If you scale your pricing service up/down, Kafka may rebalance partitions across instances, causing **delays** or **missed ticks**.


## Better Pattern

Split your services by responsibility:

### **Kafka → Price Processor → Redis → Push Layer (WebSockets)**

1. **Kafka**:

   * Receives price ticks from exchanges (1/sec/asset)
   * Partitioned by asset

2. **Price Processor (Flink / Kafka Consumer)**:

   * Reads Kafka messages
   * Computes aggregates (if needed)
   * Updates **Redis** (HSET `asset:{id}` → latest price)
   * Publishes to **Pub/Sub layer** (e.g., Redis Pub/Sub, NATS)

3. **Pricing Service / WebSocket Push Layer**:

   * Clients connect via WebSocket
   * For each subscribed asset, the service subscribes to **pub/sub messages**
   * When price changes, it sends update to subscribed clients

---

## 🚀 Why This Works Better

| Feature              | Kafka → Push Layer | Kafka → Redis → Push Layer |
| -------------------- | ------------------ | -------------------------- |
| Scales to 100M users | ❌ No               | ✅ Yes                      |
| Latency              | ⚠ Medium (\~100ms) | ✅ Low (<10ms via Redis)    |
| Persistence          | ✅ Yes              | ✅ Yes (Kafka + Redis)      |
| Fan-out Optimization | ❌ Hard             | ✅ Pub/Sub fits well        |
| Fault tolerance      | ⚠ Rebalance issues | ✅ Decoupled components     |
| Maintainability      | ❌ Complex logic    | ✅ Clear separation         |

---

## So Final Answer

> No – Don't use Pricing Service to consume from Kafka directly and fan-out to clients.

> Yes – Use Kafka for ingestion and aggregation → Redis for latest prices → Pub/Sub (e.g., Redis or NATS) → WebSocket Push Layer for fan-out.

----
-----









