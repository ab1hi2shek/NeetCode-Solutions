# Design a stock exchange

To start, let's define these problem statements:

## Problem statement/Functional requirement
1. Clients/Users should be able to place sell and buy orders to the exchange. 
2. Exchange should match buyers and sellers of the stock.
3. Exchange should provide an ordered list of Asks and Bids (Order book) that everyone can agree to.
    - Maintain real-time order books per stock/symbol.
    - Data structures to insert/cancel/modify quickly.
4. Exchange should provide an ordered list of all activities for a client back to the client when asked.

### Below the line for now
1. Exchange should provide Market Data Feed to clients/brokers.
    - Push live price ticks (top of book, trades, depth)
    - Low latency broadcast to brokers.
2. Trade Settlement: Clearing and settlement after match
3. Risk Checks & Circuit Breakers
    - Prevent market manipulation (price bands, volume caps)
    - Per-user/broker limits

## Basic financial terms 101
### Order book
 - An order book lists the number of shares being bid on or offered at each price point or market depth.
 - Order books are used by almost every exchange for various assets like stocks, bonds, currencies, and even cryptocurrencies.
 - There are three parts to an order book: buy orders, sell orders, and order history.
    - Buy orders contain buyer information including all the bids and the amount they wish to purchase.
    - Sell orders resemble buy orders, but instead include all the offers (or asking prices) or where people are willing to sell.
    - Market order histories show all the transactions that have taken place in the past.
    - The top of the book is where you'll find the highest bid and lowest ask prices.

* We will see one or more trades executed when there exists both bid and ask such that bid price >= ask price.
 - Ask price: The minimum price on which someone is willing to sell a stock (minumum asking price for a asset).
 - Bid price: max price someone wants to pay to buy that asset.

 Ask price can be like $120 x 100 -> meaning this person is willing to sell 100 stocks at $120 or above.

### Other basic terms
- **Symbol:** An abbreviation used to uniquely identify a stock (e.g. META, AAPL). Also known as a "ticker".
- **Order:** An order to buy or sell a stock. Can be a market order or a limit order.
- **Market Order:** An order to trigger immediate purchase or sale of a stock at the current market price. Has no price target and just specifies a number of shares.
- **Limit Order:** An order to purchase or sell a stock at a specified price. Specifies a number of shares and a target price, and can sit on an exchange waiting to be filled or cancelled by the original creator of the order.

 ## High level design

 ![image](/assets/stock_exchange_HLD.png)

## HLD Component Breakdown

### 1. **Broker / Client**

* External entity (e.g., trading platform, user UI) interacting with the exchange.
* Sends **order requests** (create/update/cancel) and **subscribes to events** (order status, trade notifications).


### 2. **API Gateway & Load Balancer**

* Acts as the entrypoint for all clients.
* Performs:

  * **Authentication & Authorization**
  * **Rate limiting**
  * **Basic request validation**
  * **Routing** to the correct instance of Order Service


### 3. **Order Service**

* Core gateway for managing orders.
* Stateless and horizontally scalable.
* Responsibilities:

  * Validates the order fields.
  * Does **risk checks** (in combination with Risk Check Service).
  * Generates **unique `orderId`**.
  * Persists the order to DB with **status = `Pending`**.
  * Routes the request to **Matching Service** for execution.


### 4. **Risk Check Service**

* Ensures the order meets safety criteria.

  * Ex: Not breaching user limits, circuit breakers, or anomaly detection.
* Can be expanded with ML for fraud/malicious pattern detection.
* Stateless and scalable.
* Fast checks: price band, quantity cap, user exposure limits.


### 5. **Orders DB (DynamoDB)**

* Stores all orders and their statuses.
* Key Fields:

  ```
  orderId (PK)
  userId
  symbol
  quantity
  price
  orderType (Limit/Market)
  status (Pending/Success/Failed/Cancelled)
  validUntil (optional TTL)
  timestamp, lastUpdated
  ```
* Use DynamoDB Global Secondary Index (GSI) on `userId` or `symbol` for querying order history.
* `orderId` = partition key ensures update hits a single partition.


### 6. **Matching Service**

* Retrieves the **symbol's order book**.
* Attempts to **match the order** using order matching logic.
* Updates quantities or removes orders if fully matched.
* Generates **trades** and updates the order status in DB.


### 7. **Order Book**

* In-memory store per symbol.
* Data Structures:

  * `TreeMap<price, LinkedList<Order>>` for both bids (max heap) and asks (min heap)
  * `HashMap<orderId, Order>` for fast lookup (e.g., during cancel or modify)


### 8. **Data Publisher**

* Consumes execution/trade events and pushes to clients.
* Can publish to Kafka or directly over WebSockets/SSE/FIX.
* Pushes:

  * Order status updates
  * Trade events
  * Market data (top of book, depth)



## Order Flows in Detail

### 1. Order Creation Flow

#### Flow:

1. Client sends order via API Gateway → Order Service.
2. Order Service validates and does risk check.
3. Generates `orderId`, sets status = `Pending`, inserts into DB.
4. Sends order to Matching Service.
5. Matching Service:

   * Inserts into Order Book
   * Tries to match order
   * If matched: creates trade, updates order quantities
   * If fully matched: marks status = `Success`
   * If partial: leaves order in order book, status remains `Pending`
6. Updates DB:

   * `status` to `Success` or keep as `Pending`
   * `filledQuantity`, `remainingQuantity`
7. Sends event to Data Publisher
8. Clients get real-time updates

#### DB Update Example:

```json
{
  "orderId": "O123",
  "userId": "U42",
  "symbol": "AAPL",
  "price": 180,
  "quantity": 100,
  "filledQuantity": 40,
  "remainingQuantity": 60,
  "status": "Pending",
  "timestamp": "...",
  "lastUpdated": "..."
}
```


### 2. Order Update Flow

#### Constraints:

* Can only update if `status = Pending`.
* Only modifiable fields: `price`, `quantity`, `validUntil`.

#### Flow:

1. Client sends update request with `orderId`.
2. Order Service:

   * Validates new fields
   * Checks if order is still in `Pending` state
3. Updates DB fields: `quantity`, `price`, `validUntil`, `lastUpdated`
4. Forwards to Matching Service.
5. Matching Service:

   * Updates order in order book (via `orderId → Order` HashMap)
   * Re-attempts matching
6. Updates `filledQuantity`, `remainingQuantity`, `status` as needed
7. Sends event to Data Publisher

#### DB Update Example:

```json
{
  "orderId": "O123",
  "price": 182,
  "quantity": 120,
  "status": "Pending",
  "lastUpdated": "..."
}
```

### 3. Order Cancel Flow

#### Constraints:

* Only `Pending` orders can be cancelled.

#### Flow:

1. Client sends cancel request with `orderId`.
2. Order Service:

   * Validates order is in `Pending`
   * Marks `status = Cancelled` in DB
3. Notifies Matching Service
4. Matching Service:

   * Removes order from in-memory Order Book
5. Sends cancel event to Data Publisher
6. Client receives confirmation

#### DB Update Example:

```json
{
  "orderId": "O123",
  "status": "Cancelled",
  "lastUpdated": "..."
}
```

---

Here's a **refined, detailed answer to all the deep dives** from your stock exchange system design — especially tailored for high-scale, low-latency, fault-tolerant architecture that can handle **billions of orders per day**.

---

## **Deep Dive 1: Order Matching Logic (Core of Exchange)**

### Matching Criteria:

* **Price-time priority**

  * Match if `highestBid >= lowestAsk`
  * Within same price, prioritize older order (FIFO)

### Data Structures:

```java
TreeMap<Double, Queue<Order>> bids = new TreeMap<>(Collections.reverseOrder());
TreeMap<Double, Queue<Order>> asks = new TreeMap<>();
HashMap<String, Order> orderMap = new HashMap<>();
```

* `TreeMap`: Sorted order matching by price
* `Queue`: FIFO for time-priority
* `orderMap`: O(1) for cancel/update

### Matching Algorithm:

```java
while (!bids.isEmpty() && !asks.isEmpty()) {
    if (bids.firstKey() >= asks.firstKey()) {
        Order buy = bids.get(bids.firstKey()).peek();
        Order sell = asks.get(asks.firstKey()).peek();

        int matchedQty = min(buy.qty, sell.qty);
        executeTrade(buy, sell, matchedQty);

        // Adjust or remove orders
        if ((buy.qty -= matchedQty) == 0) bids.get(...).poll();
        if ((sell.qty -= matchedQty) == 0) asks.get(...).poll();

        // Cleanup empty price levels
        if (bids.get(...).isEmpty()) bids.remove(...);
        if (asks.get(...).isEmpty()) asks.remove(...);
    } else break;
}
```

### Notes:

* Handle partial matches
* Support Market, Limit, IOC, FOK using `validUntil`, `orderType`, and flags


## **Deep Dive 2: Scaling the System to Billions of Orders**

### Horizontally Scalable Components:

| Component           | Scaling Strategy                                                              |
| ------------------- | ----------------------------------------------------------------------------- |
| **API Gateway**     | Stateless + Load-balanced                                                     |
| **Order Service**   | Stateless microservice; scaled via LB                                         |
| **Matching Engine** | **Sharded by symbol using consistent hashing**                                |
| **Database**        | DynamoDB or ScyllaDB; partitioned by `orderId` and indexed by `userId/symbol` |
| **Risk Checks**     | Stateless service cluster                                                     |
| **Data Publisher**  | Kafka + horizontal scaling (symbol-partitioned)                               |

---

### Matching Service Partitioning:

#### **Goal**: Avoid bottleneck by having multiple matching engines

#### **Consistent Hashing:**

* Hash(symbol) → virtual node → physical matcher
* Prevents massive reshuffling on scaling
* Use tools like **Ringpop**, **HashiCorp Consul**, or Zookeeper-based coordination

#### Registry:

* `symbol → matching_service_instance`
* Stored in Zookeeper / etcd
* Order Service queries this to route


### Matching Engine Responsibilities:

* Maintain **in-memory order book** for assigned symbols
* Process and match orders atomically
* Log trades to WAL or Kafka
* Communicate matches to:

  * Order DB
  * Data Publisher
  * Trade log


## **Deep Dive 3: Reducing Order Execution Latency**

### Design Goals:

1. Match orders in **microseconds**
2. Maintain fairness and ordering
3. Avoid inter-service hops in critical path


### Strategies:

#### 1. **In-Memory Book**

* Fastest read/write
* Per-symbol TreeMap/SkipList/LinkedHashMap

#### 2. **Thread Pinning**

* Each matching thread owns a fixed number of symbols
* Avoid locks; use single-threaded event loop (like Redis)

#### 3. **Bypass DB in hot path**

* Order status is async-updated in DB post match
* Trade execution → persist via Kafka + batch writer

#### 4. **Zero-Copy Messaging**

* Use memory-mapped logs (`mmap` + ring buffers) for IPC
* Libraries: [Chronicle Queue](https://chronicle.software/), [Disruptor pattern](https://lmax-exchange.github.io/disruptor/)

#### 5. **Use Binary Protocols**

* Avoid JSON for internal communication
* Use SBE, FIX, or FlatBuffers for wire efficiency


## **Deep Dive 4: At-Most-Once Event Delivery**

### Problem:

Duplicate or missed delivery of order execution events

### Solution Stack:

#### 1. **Trade ID** (UUID or `orderId + nonce`)

* Every trade has a unique identifier
* Client deduplicates based on this

#### 2. **Event Store (Kafka)**:

* Matching Service publishes trade to Kafka topic `trades_<symbol>`
* Kafka ensures durable, ordered, fault-tolerant delivery

#### 3. **Reliable Publisher**:

* Data Publisher consumes Kafka and pushes via:

  * SSE
  * WebSocket
  * FIX gateway
* Tracks client ACKs

#### 4. **Idempotent Clients**:

* Clients apply trade if `tradeId` not in their local history

#### 5. **Fallback Endpoint**:

```http
GET /client/{id}/orders/status
```

* Pull-based recovery if client misses events


## **Deep Dive 5: Matching Service Crash Recovery**

### Problem:

Crash = Loss of in-memory order book


### Recovery Strategy:

#### 1. **WAL (Write-Ahead Log)**

* Every change to book (new, cancel, match) is logged
* Format: `timestamp, orderId, action, symbol, price, qty, side`

#### 2. **Periodic Snapshots**

* Every N seconds, dump full order book to disk/cloud (S3, EBS)
* On crash:

  * Load snapshot
  * Replay WAL after snapshot timestamp

#### 3. **Replication (Active-Passive)**

* All operations are mirrored to standby engine
* Use TCP or Kafka for mirroring
* Failover orchestrated by Zookeeper or Kubernetes

#### 4. **Recover From DB (Last Resort)**

* Pull `status = pending` orders from DynamoDB
* Rebuild book; slower but safe

---

## Deep dive 6: How to remove stale orders from order book?

In a real-world stock exchange or trading system, not all orders are meant to remain active indefinitely. The `validUntil` or **Time-to-Live (TTL)** field determines how long an order remains valid in the order book before it is automatically cancelled.

###  Why Is This Important?

1. **Reduce stale load**: Prevent accumulation of expired, unmatchable orders.
2. **Support advanced order types**: Such as:

   * **GTC (Good-Till-Cancelled)** – valid until explicitly cancelled.
   * **IOC (Immediate-Or-Cancel)** – cancel if not executed immediately.
   * **FOK (Fill-Or-Kill)** – cancel unless completely filled immediately.
   * **GTD (Good-Till-Date)** – valid until a specific time/date.
3. **Better user control**: Traders might want to cancel orders if not filled within a certain timeframe.

### How to Implement?

### 1. **`validUntil` Field**

Add to your order schema:

```json
{
  "orderId": "O123",
  "symbol": "AAPL",
  "price": 180.0,
  "quantity": 100,
  "type": "Limit",
  "validUntil": "2025-06-04T12:00:00Z",
  "status": "Pending"
}
```

### 2. **Order Book In-Memory Structure**

* Along with the `TreeMap<price, List<Order>>`, maintain a **min-heap of (validUntil, orderId)**.
* Periodically (e.g., every second), scan and **expire** all orders with `validUntil < now`.

### 3. **In DB (DynamoDB-specific)**

* Use **Time-to-Live (TTL)** attribute on DynamoDB.
* Automatically deletes expired orders from the DB after the TTL time.
* Helps avoid background jobs for cleanup.

```json
"ttl": 1725494383  // Unix epoch seconds
```

> Note: TTL in DynamoDB is not real-time – deletion can be delayed up to 48 hours, so don’t rely on it for order matching logic. Only for storage cleanup.

---

### Matcher Consideration

* The **Matching Service** must check `validUntil` before attempting to match:

```java
if (order.getValidUntil().isBefore(now)) {
    markOrderAsExpired(order);
    continue;
}
```

## Deep dive 7: Communication between order service and matching service for low latency.

We are in the **hot path** of an exchange:

```
Client → OrderService → MatchingEngine
```

Matching must happen in **single-digit milliseconds** or less, with **strict order**, **guaranteed durability**, and **no loss under crash**.

Kafka is **too slow for this path**, so you’ll often see **fast, lossless local IPC** options like the following.


## 1. Low-Latency RPC (e.g., gRPC, TCP, UNIX sockets)

###  Description:

Order Service makes a **direct network call** (often over loopback `127.0.0.1`) to Matching Service using a **lightweight RPC protocol**.

### How It Works:

* Matching engine runs as a **long-lived server process** on same machine or datacenter
* Order Service calls it using **gRPC, Flatbuffers, or raw TCP**
* Protocol buffers are used for serialization
* Can be **sync or async**

### Latency:

* \~0.2–1 ms if local
* \~5–20 ms if across nodes

### Pros:

* Easy to implement
* Language agnostic
* Matches microservices architecture

### Cons:

* Still involves kernel calls and network stack
* Slightly slower than memory-based communication

### Example:

```java
// OrderService (client)
OrderRequest req = new OrderRequest(orderId, symbol, price, qty);
matchingEngineClient.matchOrder(req);
```

## 2. Shared Memory Ring Buffer (e.g., Disruptor, mmap)

### Description:

Both OrderService and MatchingEngine share a **memory-mapped region** (via file or direct memory), and pass messages by writing into a **ring buffer**.

This is **extremely fast**, used in **high-frequency trading (HFT)**, where latency < 1µs matters.

### How It Works:

* OS-backed memory segment (`mmap`)
* Ring buffer has **preallocated fixed-size slots**
* Writer thread writes order at index `i`, sets a flag
* Reader thread sees the flag, reads, and processes

### Latency:

* \~0.1 µs to 10 µs
* Practically **near-zero latency**

### Pros:

* **Fastest method** possible
* No context switch, no kernel mode
* Bounded memory usage
* Used in LMAX Disruptor, Nasdaq ITCH, etc.

### Cons:

* Requires **same physical machine**
* More complex to manage (thread safety, memory fences)
* Harder to scale across nodes

### Example (Conceptual):

```c
// Producer
ringBuffer[writeIndex] = order;
writeFlag[writeIndex] = 1;

// Consumer
if (writeFlag[readIndex] == 1) {
    process(ringBuffer[readIndex]);
    writeFlag[readIndex] = 0;
}
```


## 3. In-Process Queue (Single Process Model)

### Description:

OrderService and MatchingEngine are just **different components in a single monolithic service**, communicating via **thread-safe queues**.

### How It Works:

* Main thread (OrderService) enqueues incoming orders into a `BlockingQueue`
* MatchingEngine runs in its own thread and polls the queue
* No serialization required

### Latency:

* Sub-millisecond — depends only on thread scheduling

### Pros:

* **No IPC overhead**
* Very simple and reliable
* Perfect for first version or single-node engine

### Cons:

* Single point of failure
* Scaling is limited to vertical scale
* Harder to split matching logic across many symbols

### Example (Java):

```java
// OrderService thread
queue.put(order);

// MatchingEngine thread
while (true) {
    Order order = queue.take();
    match(order);
}
```


##  Comparison Table

| Technique             | Latency  | Cross-machine? | Fault Tolerance   | Complexity |
| --------------------- | -------- | -------------- | ----------------- | ---------- |
| Low-latency RPC       | 0.5–5 ms | ✅              | ✅                 | ⭐⭐         |
| Shared Memory RingBuf | <10 µs   | ❌              | ❌ (unless logged) | ⭐⭐⭐⭐       |
| In-process Queue      | <1 ms    | ❌              | ❌ (no isolation)  | ⭐          |

---

## Real-World Usage:

| System Type                   | Technique                   |
| ----------------------------- | --------------------------- |
| High-Frequency Trading (HFT)  | Shared Memory (Disruptor)   |
| Modern exchanges (Coinbase)   | Low-latency RPC + WAL       |
| Early-stage exchange projects | In-process queue (monolith) |

---


