# Fraud detection service

## ✅ 1. Functional Requirements

1. **Real-time transaction risk scoring**

   * Evaluate deposits, withdrawals, transfers, trades, etc.
2. **Rule-based + ML-based risk detection**

   * Use static rules (e.g., amount > threshold) + anomaly detection.
3. **Alert generation**

   * Trigger risk alerts to compliance or fraud teams.
4. **Audit log**

   * Store all risk decisions for compliance and auditing.
5. **Human review workflow**

   * Queue risky transactions for manual review.
6. **Entity enrichment**

   * Enrich transaction with KYC data, user history, device fingerprinting.

---

## 📌 2. Non-Functional Requirements

* **Low latency**: Risk decision within <100ms per transaction.
* **High availability**: 99.99% uptime; transactions shouldn’t be blocked.
* **Scalability**: Handle millions of events per day.
* **Explainability**: All decisions (ML or rules) must be explainable (for audits).
* **Security**: PII data protection, encryption at rest and in transit.
* **Fault tolerance**: No single point of failure.
* **Compliance-friendly**: GDPR/FINRA/SOX-ready.

---

## 🧾 3. Core APIs

### Risk Evaluation API

```http
POST /risk/evaluate
Body:
{
  "userId": "u123",
  "transactionId": "tx789",
  "type": "withdrawal",
  "amount": 12000,
  "currency": "USD",
  "ip": "192.168.1.1",
  "deviceId": "abc123",
  "timestamp": "2025-06-01T10:10:00Z"
}

Response:
{
  "riskScore": 0.87,
  "decision": "flagged",
  "ruleMatches": ["large_amount", "new_device"],
  "reviewRequired": true
}
```

---

## 📚 4. Core Entities

* **Transaction**

  * `id`, `type`, `userId`, `amount`, `deviceId`, `timestamp`, `metadata`
* **User Profile**

  * `userId`, `KYC status`, `account age`, `location`, `device history`
* **Risk Rule**

  * `id`, `expression`, `action`, `severity`
* **Risk Alert**

  * `id`, `transactionId`, `reason`, `createdAt`, `status`
* **ML Model Metadata**

  * `version`, `featureSet`, `accuracy`, `lastUpdated`

---

## 🏗️ 5. High-Level Architecture (Text Diagram)

```
                 [Transaction Services]
                         |
                         v
                +-------------------+
                | Risk Evaluation API|
                +-------------------+
                         |
            +------------+------------+
            |                         |
            v                         v
+---------------------+     +---------------------+
|   Rules Engine      |     |    ML Scoring Engine|
+---------------------+     +---------------------+
            |                         |
            v                         v
       +-----------------------------------+
       |       Risk Decision Processor     |
       +-----------------------------------+
                         |
            +------------+-------------+
            |                          |
            v                          v
+--------------------+      +------------------------+
|  Alert & Review DB |      |   Audit + Event Store  |
+--------------------+      +------------------------+
            |
            v
     [Risk Review UI for Compliance Team]
```

---

## 🔍 6. Explanation of Each Component

### A. **Transaction Services**

* Source of truth for deposit/withdrawal/trade actions.
* Publishes transactions to risk evaluation system (via API or event stream).

### B. **Risk Evaluation API**

* Stateless API to receive incoming transaction events.
* Calls downstream rules engine and ML model in parallel.
* Combines results and generates risk score/decision.

### C. **Rules Engine**

* Real-time evaluation of pre-defined rules (e.g., amount > 10K, country = blacklist).
* Uses a DSL or expression language (e.g., CEL, JEXL, or custom).
* Easily modifiable by compliance officers.

### D. **ML Scoring Engine**

* Runs trained anomaly or fraud detection models.
* Uses real-time features (amount, velocity, user history).
* Deployed on a feature store with online inference.

### E. **Risk Decision Processor**

* Combines rule + ML outcomes.
* Applies thresholds or escalation logic.
* Sends alerts to Alert DB and persists audit logs.

### F. **Alert & Review DB**

* Stores alerts that require human intervention.
* Supports prioritization (high-risk first), review state, comments.

### G. **Audit/Event Store**

* Append-only store of all risk evaluations.
* Immutable logs for compliance (queryable for regulators).

### H. **Risk Review UI**

* Used by compliance/fraud teams.
* Shows alert metadata, scoring explanation, and transaction context.

---

## Sync or Async flow?

## Sync flow

In the core diagram I shared earlier, I opted for a **synchronous flow** (API-based evaluation) to demonstrate the system's **real-time, low-latency requirements** — i.e., **risk scoring before a transaction is processed** (e.g., blocking a withdrawal if flagged).

However, in **real-world large-scale systems**, especially at fintechs like **Coinbase**, **Kafka or similar event streaming platforms** are almost always used — either:

* **As the primary transport layer** between services.
* Or as a **side-channel for async processing, audit, replay**, etc.

---

## Async flow (Kafka)

Let’s add Kafka into the architecture and explain its role.



```
                [Transaction Services]
                         |
                         v
             +---------------------------+
             |  Kafka (transaction topic)|
             +---------------------------+
                         |
                         v
          +-----------------------------+
          |   Risk Evaluation Workers   |  ← scalable consumers
          +-----------------------------+
                         |
            +------------+------------+
            |                         |
            v                         v
+---------------------+     +---------------------+
|   Rules Engine      |     |    ML Scoring Engine|
+---------------------+     +---------------------+
            |                         |
            v                         v
       +-----------------------------------+
       |       Risk Decision Processor     |
       +-----------------------------------+
                         |
            +------------+-------------+
            |                          |
            v                          v
+--------------------+      +------------------------+
|  Alert & Review DB |      |   Audit + Event Store  |
+--------------------+      +------------------------+
            |
            v
     [Risk Review UI for Compliance Team]
```

---

## 📦 Where Kafka Is Used

| Component               | Kafka Role                                       |
| ----------------------- | ------------------------------------------------ |
| Transaction Services    | **Producers** of raw transaction events          |
| Risk Evaluation         | **Consumers** of transaction topic               |
| Audit/Event Store       | Use Kafka for **append-only log** architecture   |
| ML Feature Store        | Can consume Kafka to update rolling aggregates   |
| Rule + Model Evaluation | Kafka partitions allow **parallel risk scoring** |
| Replay Engine           | Reprocess old transactions via Kafka replays     |

---

## 🧠 Deep Dive: Why Kafka is Useful Here

### 1. **Decoupling**

* Producers (Transaction Systems) don't care if risk scoring is down.
* Consumers (Risk Workers) can scale independently.

### 2. **Backpressure Handling**

* If ML scoring or DB is slow, Kafka queues can buffer.
* Allows retries and timeouts without losing data.

### 3. **Audit & Replay**

* Store raw events forever (immutable log).
* Replay a partition for re-evaluating transactions with updated rules or ML models.

### 4. **Scalability**

* Partitioned by `userId`, `transactionId` or `region`.
* Each consumer group handles part of the stream (massively parallel).

---

### 🔄 Synchronous + Kafka: Best of Both Worlds?

* **Blocking operations** (e.g., before a withdrawal is allowed) → **sync path**:

  * Call a **Risk Scoring API** that queries **in-memory cache**, rules, or ML service quickly.
* **Async evaluations**, audits, or secondary detection (e.g., suspicious trends) → **Kafka-based stream**.

---

## 🔄 Dual Path Design (Sync + Kafka)

```
             [Withdrawal API]
                   |
                   v
      +-----------------------------+
      | Risk Evaluation API (sync)  | ← used to allow/block request
      +-----------------------------+
                   |
                   v
        [Immediate Transaction Decision]

                   |
                   v
     +------------------------------------+
     | Kafka (for async risk + audit log) |
     +------------------------------------+
                   |
                   v
        [Async Risk Evaluation Consumers]
```

---

## ✅ Summary

| Path                  | Use Case                             | Latency           | Kafka Used? |
| --------------------- | ------------------------------------ | ----------------- | ----------- |
| **Synchronous API**   | Block risky withdrawals in real time | <100ms            | Optional    |
| **Async Kafka-based** | Continuous evaluation, enrichment    | seconds to ms     | Yes         |
| **Replay/Audit**      | Retroactive model/rule updates       | batch/time-travel | Yes         |

---

## Deep dives

### 1. Scaling the System

* **Kafka-based architecture** scales linearly with number of partitions.
* **Risk Workers** scale horizontally to match event volume.
* **ML scoring** uses an online feature store (Redis or Feast) to prevent database bottlenecks.
* **Rules Engine** compiled to in-memory decision tree to handle thousands of TPS.
* Use **region-based sharding** to localize data access and latency.

---

### 2. Fault Tolerance

* Kafka provides **at-least-once delivery** — events won't be lost.
* ML models can **fail-open or fallback to default** if offline.
* Circuit breakers between services.
* **Audit log** ensures we can replay or reconstruct events if services crash.
* Use **chaos engineering** (e.g., Gremlin) to test resilience.

---

### 3. Machine Learning Pipeline

* **Model training**:

  * Use historical transactions (labeled good/bad).
  * Features: user velocity, account age, device fingerprinting, country patterns.
* **Model inference**:

  * Use lightweight gradient boosted trees or DNN for fast scoring.
  * Serve via TensorFlow Serving or custom Flask service.
* **Model versioning & rollback**:

  * Canary deploy with dual scoring and alert comparison.

---

### 4. Rule Engine Design

* Rules written in DSL (e.g., `"amount > 10000 && country != 'US'"`)
* Store rule metadata in DB and load into memory.
* Allow dynamic rule updates via UI or API.
* Evaluate in <10ms using interpretable rule tree.

---

### 5. Feature Enrichment & Feature Store

* Store live features like:

  * Transactions in past hour/day
  * New device or location
  * Historical fraud score
* Serve via Redis or real-time feature store (e.g., Tecton or Feast).
* TTL-based eviction for freshness.

---

### 6. Monitoring & Observability

* Metrics:

  * Risk evaluation latency
  * Alerts generated per minute
  * False positives/negatives
* Traces:

  * Transaction → Kafka → Risk Worker → Alert
* Dashboards:

  * Alert volume by region/user type
  * Model drift indicators

---

### 7. Compliance, Audit, GDPR

* All evaluations are logged (inputs + decisions).
* Log tamper-proof: use append-only Kafka + checksum validation.
* GDPR: right to be forgotten, access logs.
* Role-based access control (RBAC) for review UI.
* Integrate with SIEM systems for security alerts.

---

### 8. Sync vs Async Risk Evaluation

| Path  | Use Case                     | Tech Used      | Latency    |
| ----- | ---------------------------- | -------------- | ---------- |
| Sync  | Block withdrawals/trades     | API, in-memory | <100ms     |
| Async | Detection, alerts, ML models | Kafka          | ms–seconds |

Many real systems use **both**:

* Sync for front-line blocking (rules + ML lite).
* Async for enrichment, deep ML, fraud team review.

---

