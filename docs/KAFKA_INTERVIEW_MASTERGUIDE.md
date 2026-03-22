# Kafka Complete Interview Master Guide
# From Zero to Expert — Even a 5th Grader Can Understand

---

## TABLE OF CONTENTS

1. [What is Kafka? (The 5th Grade Explanation)](#1-what-is-kafka)
2. [Core Building Blocks](#2-core-building-blocks)
3. [Broker & Cluster — Deep Dive](#3-broker--cluster)
4. [Topics — Deep Dive](#4-topics)
5. [Partitions — Deep Dive](#5-partitions)
6. [Replication — How Kafka Never Loses Your Data](#6-replication)
7. [Producers — Deep Dive](#7-producers)
8. [Consumers & Consumer Groups — Deep Dive](#8-consumers--consumer-groups)
9. [Offsets — How Kafka Remembers Where You Left Off](#9-offsets)
10. [Zookeeper vs KRaft](#10-zookeeper-vs-kraft)
11. [Kafka Configuration Reference](#11-kafka-configuration-reference)
12. [Spring Boot Kafka Integration](#12-spring-boot-kafka-integration)
13. [Design Patterns in Kafka](#13-design-patterns)
14. [Your Project Explained with Kafka Concepts](#14-your-project-explained)
15. [Advanced Topics](#15-advanced-topics)
16. [Top 100 Interview Questions & Answers](#16-top-100-interview-questions--answers)
17. [Common Mistakes & Gotchas](#17-common-mistakes--gotchas)
18. [Quick Revision Cheat Sheet](#18-quick-revision-cheat-sheet)

---

## 1. What is Kafka?

### The 5th Grade Explanation

Imagine your school has a **notice board** in the hallway.

- A **teacher** (Producer) sticks a notice (message) on the board.
- Any **student** (Consumer) can walk up and read the notice.
- The notice stays on the board for some time — it doesn't disappear the moment one student reads it.
- **Multiple students** can read the **same notice** independently.
- Notices are organized into **sections** (Topics) — Sports, Academics, Events.

That notice board is **Kafka**.

### The Real Definition

Apache Kafka is a **distributed event streaming platform** used to:
- **Publish** (produce) events/messages
- **Subscribe** (consume) events/messages
- **Store** events durably (like a log/history)
- **Process** events in real time

### Why Was Kafka Created?

LinkedIn built Kafka in 2011 because they needed to handle **millions of activity events per second** (page views, clicks, likes) and existing tools couldn't handle it. They later open-sourced it under Apache.

### What Problems Does Kafka Solve?

**Without Kafka (Direct Service-to-Service Communication):**
```
UserService --HTTP--> PaymentService
UserService --HTTP--> NotificationService
UserService --HTTP--> AuditService

Problems:
- If PaymentService is down, UserService fails too
- If NotificationService is slow, UserService waits
- Tight coupling — change one, break others
- No history of what happened
```

**With Kafka:**
```
UserService --> [Kafka Topic] --> PaymentService
                              --> NotificationService
                              --> AuditService

Benefits:
- UserService doesn't care if others are down
- Each service works at its own pace
- Message history is preserved
- Add a new consumer anytime without changing UserService
```

---

## 2. Core Building Blocks

### Visual Map of Kafka

```
+--------------------------------------------------+
|                  KAFKA CLUSTER                   |
|                                                  |
|  +----------+  +----------+  +----------+        |
|  | Broker 1 |  | Broker 2 |  | Broker 3 |        |
|  |          |  |          |  |          |        |
|  | Topic A  |  | Topic A  |  | Topic A  |        |
|  | Part 0   |  | Part 1   |  | Part 2   |        |
|  | (Leader) |  | (Leader) |  | (Leader) |        |
|  | Part 1   |  | Part 2   |  | Part 0   |        |
|  | (Replica)|  | (Replica)|  | (Replica)|        |
|  +----------+  +----------+  +----------+        |
+--------------------------------------------------+
        ^                           |
        |                           v
  +----------+                +----------+
  | Producer |                | Consumer |
  | (writes) |                | (reads)  |
  +----------+                +----------+
```

### Terminology at a Glance

| Term | Simple Meaning | Real Definition |
|---|---|---|
| **Event/Message** | A notice on the board | A piece of data with key, value, timestamp, headers |
| **Topic** | A section on the notice board | Named channel/category where messages are published |
| **Partition** | Pages within a section | Ordered, immutable sequence of messages within a topic |
| **Broker** | One notice board server | A single Kafka server that stores and serves messages |
| **Cluster** | All notice boards together | A group of brokers working together |
| **Producer** | The teacher posting notices | Application that writes messages to Kafka |
| **Consumer** | The student reading notices | Application that reads messages from Kafka |
| **Consumer Group** | A class of students | Group of consumers sharing the work of reading a topic |
| **Offset** | Page number of the notice | Unique position/ID of a message within a partition |
| **Replication** | Photocopying the notice | Kafka copies data to multiple brokers for safety |
| **Leader** | The original notice | The primary partition that handles all reads/writes |
| **Replica/Follower** | The photocopy | Copy of the partition on another broker |
| **ZooKeeper** | The school principal | Manages and coordinates Kafka brokers (legacy) |
| **ISR** | Trusted photocopiers | In-Sync Replicas — replicas fully caught up with leader |

---

## 3. Broker & Cluster

### What is a Broker?

A **broker** is simply a **Kafka server** — a machine/process running Kafka.

Think of it like a post office. One post office = one broker.

```
kafka-server-start.sh server.properties
         |
         +--> This starts ONE broker
```

Each broker:
- Has a unique ID (`KAFKA_BROKER_ID=1`)
- Stores partitions of topics
- Handles producer and consumer requests
- Communicates with other brokers

### What is a Cluster?

A **cluster** is a **group of brokers** working together.

```
Cluster = Broker1 + Broker2 + Broker3
```

Why multiple brokers?
- **Fault tolerance**: if one broker crashes, others take over
- **Scalability**: spread the load across multiple machines
- **High throughput**: more brokers = more parallel writes/reads

### From Your docker-compose.yml

```yaml
kafka:
  image: confluentinc/cp-kafka:7.5.0
  environment:
    KAFKA_BROKER_ID: 1          # This broker's unique ID
    KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181  # Coordinator
```

Your project runs **1 broker** (development setup). In production, you'd have 3+ brokers.

### How Do Brokers Communicate?

```
KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: PLAINTEXT:PLAINTEXT,PLAINTEXT_HOST:PLAINTEXT
KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:9092,PLAINTEXT_HOST://localhost:29092
```

**Two listeners explained:**

| Listener | Address | Who Uses It |
|---|---|---|
| `PLAINTEXT://kafka:9092` | Internal Docker network | Other containers (services inside Docker) |
| `PLAINTEXT_HOST://localhost:29092` | Your laptop | You, connecting from outside Docker |

This is why your services use `kafka:9092` but you'd use `localhost:29092` from your machine.

### Controller Broker

In a cluster, one broker is elected as the **Controller**. It:
- Manages partition leadership
- Detects broker failures
- Assigns new leaders when a broker dies

Only one controller exists at a time. If it dies, another broker becomes controller.

### Interview Questions on Broker/Cluster

**Q: Can Kafka work with just one broker?**
Yes, but it's not fault-tolerant. One broker going down = everything stops.

**Q: What happens when a broker crashes?**
- ZooKeeper (or KRaft) detects the failure
- Controller elects new leaders for partitions that were on the crashed broker
- Producers/consumers reconnect to new leaders automatically

**Q: What is the minimum recommended cluster size for production?**
3 brokers minimum. This allows replication factor of 3 and tolerates 1 broker failure.

**Q: How many brokers can a Kafka cluster have?**
Hundreds to thousands. LinkedIn runs clusters with 1000+ brokers.

---

## 4. Topics

### What is a Topic?

A **topic** is like a **named folder or category** where messages are stored.

```
Kafka is like a post office.
Topics are like different mail slots:
- "ticket-events" slot
- "payment-events" slot
- "notification-events" slot
```

Producers write TO a topic. Consumers read FROM a topic.

### Topic Characteristics

- **Named**: every topic has a unique name
- **Durable**: messages are stored on disk (not lost when consumer reads)
- **Multi-subscriber**: multiple consumers can read the same topic
- **Configurable retention**: how long messages are kept (default: 7 days)

### From Your docker-compose.yml

```bash
kafka-topics --bootstrap-server kafka:9092 \
  --create --if-not-exists \
  --topic ticket-events \
  --partitions 3 \
  --replication-factor 1
```

You create topics with:
- `--topic`: name of the topic
- `--partitions`: how many partitions (parallel lanes)
- `--replication-factor`: how many copies of data

### Topic Naming Convention (Best Practice)

```
{domain}.{entity}.{event-type}
Examples:
- ticket.booking.reserved
- payment.transaction.completed
- user.profile.registered

Or flat (as in your project):
- ticket-events
- payment-events
```

### Message Structure

Every Kafka message has:

```
+-------------------+
| Key    (optional) |  --> Used for partitioning
| Value  (required) |  --> The actual payload (JSON, Avro, etc.)
| Headers(optional) |  --> Metadata (like HTTP headers)
| Timestamp         |  --> When message was created
| Offset            |  --> Position in partition (assigned by Kafka)
+-------------------+
```

From your `TicketEventProducer.java`:
```java
kafkaTemplate.send(ticketEventsTopic, event.getTicketId(), event);
//                  ^topic name       ^key (ticketId)     ^value (event object)
```

The `ticketId` is the **key** — this ensures all events for the same ticket go to the same partition.

### Topic Retention Policy

```
# Keep messages for 7 days (default)
retention.ms=604800000

# Keep messages up to 1GB per partition
retention.bytes=1073741824

# Keep messages forever (log compaction)
cleanup.policy=compact
```

**Log Compaction**: Kafka keeps only the LATEST message for each key. Like a hashmap — older values are garbage collected.

### Interview Questions on Topics

**Q: Can a topic have 0 partitions?**
No. Minimum is 1 partition.

**Q: Can you decrease the number of partitions?**
No. You can only increase partitions. Decreasing requires deleting and recreating the topic (data loss).

**Q: What happens to messages after retention period?**
They are deleted. Consumers that haven't read them in time will miss them (unless using log compaction).

**Q: Is Kafka a queue or pub-sub?**
Both. It works as a queue (within a consumer group — one consumer gets each message) and as pub-sub (multiple consumer groups each get all messages).

---

## 5. Partitions

### What is a Partition? (ELI5)

Imagine a highway. One lane = slow. Multiple lanes = cars move faster in parallel.

A **partition** is one lane in the highway (topic). More partitions = more parallelism.

```
Topic: ticket-events (3 partitions)

Partition 0: [msg0] [msg3] [msg6] [msg9]  ...
Partition 1: [msg1] [msg4] [msg7] [msg10] ...
Partition 2: [msg2] [msg5] [msg8] [msg11] ...
```

### Why Partitions Matter

1. **Parallelism**: 3 partitions → 3 consumers can read in parallel
2. **Scalability**: spread data across multiple brokers
3. **Ordering**: within a partition, order is GUARANTEED. Across partitions, it is NOT.

### How Does Kafka Decide Which Partition to Use?

```
If key is provided:
  partition = hash(key) % numPartitions

If no key:
  Round-robin across partitions (default)
  OR Sticky partitioning (sends batch to same partition for efficiency)
```

**From your code:**
```java
kafkaTemplate.send(ticketEventsTopic, event.getTicketId(), event);
//                                    ^key = ticketId
```

Since `ticketId` is the key:
- All events for ticket `abc-123` always go to the **same partition**
- This guarantees **ordering** — reserved → confirmed → released, in that sequence

### Partition Assignment to Brokers

With 3 brokers and topic with 3 partitions:
```
Broker 1: Partition 0 (Leader), Partition 1 (Replica), Partition 2 (Replica)
Broker 2: Partition 1 (Leader), Partition 2 (Replica), Partition 0 (Replica)
Broker 3: Partition 2 (Leader), Partition 0 (Replica), Partition 1 (Replica)
```
Kafka spreads partitions evenly across brokers automatically.

### How Many Partitions Should You Have?

**Rule of thumb:**
```
partitions = max(throughput_needed / throughput_per_partition, number_of_consumers)
```

Practical guidelines:
- For high throughput: more partitions (100s)
- For ordering requirements: fewer partitions
- For low latency: fewer partitions (less overhead)
- Production recommendation: 3x the number of brokers as a starting point

**Your project's choices:**
```
ticket-events:       3 partitions (high traffic, core flow)
payment-events:      3 partitions (high traffic, one per ticket event)
notification-events: 2 partitions (medium traffic)
user-events:         2 partitions (medium traffic)
audit-events:        1 partition  (ordered audit trail, less traffic)
dead-letter-queue:   1 partition  (low traffic, failed messages)
```

### Partition Leader & Follower

Every partition has ONE **leader** and zero or more **followers** (replicas).

```
Partition 0:
  Leader   --> Broker 1 (handles all reads & writes)
  Follower --> Broker 2 (copies data from leader)
  Follower --> Broker 3 (copies data from leader)
```

- **Producers write to leader only**
- **Consumers read from leader** (default) or follower (with `fetch.min.bytes` config)
- **Followers sync from leader** continuously

### Interview Questions on Partitions

**Q: Can two messages with the same key go to different partitions?**
No. `hash(key) % numPartitions` is deterministic. Same key always goes to same partition (unless you increase partition count — then the mapping changes!).

**Q: What happens to ordering if I add more partitions?**
Adding partitions changes `hash(key) % numPartitions`, so a key that used to go to partition 0 might now go to partition 2. This breaks ordering guarantees for in-flight messages. Plan partitions carefully upfront.

**Q: Is there a limit to partition count per topic?**
No hard limit, but too many partitions increases overhead (leader election time, memory, file handles). LinkedIn recommends no more than 4000 partitions per broker and 200,000 per cluster.

**Q: Can one consumer read from multiple partitions?**
Yes. If there are more partitions than consumers, one consumer handles multiple partitions.

---

## 6. Replication — How Kafka Never Loses Your Data

### The Photocopier Analogy

You write an important letter. You make 3 photocopies and store them in 3 different buildings. If one building burns down, you still have 2 copies. That's **replication**.

### How Replication Works

```
replication-factor = 3 means:
  1 Leader (original) + 2 Followers (copies)
  Stored on 3 different brokers
```

```
Producer writes "msg1" to Partition 0

Step 1: msg1 written to Leader (Broker 1)
Step 2: Leader sends msg1 to Follower on Broker 2
Step 3: Leader sends msg1 to Follower on Broker 3
Step 4: Both followers acknowledge
Step 5: Leader acknowledges to Producer (if acks=all)
```

### ISR — In-Sync Replicas

**ISR** = the set of replicas that are fully caught up with the leader.

```
ISR = {Broker1, Broker2, Broker3}  --> All in sync (healthy)
ISR = {Broker1, Broker2}           --> Broker3 lagged behind (maybe slow or crashed)
ISR = {Broker1}                    --> Only leader remaining (dangerous!)
```

A replica is removed from ISR if it hasn't fetched data from leader within `replica.lag.time.max.ms` (default 30 seconds).

### What Happens When a Leader Dies?

```
Before failure:
  Partition 0: Leader=Broker1, Followers=Broker2, Broker3
  ISR = {Broker1, Broker2, Broker3}

Broker1 crashes!
  Controller detects failure (via ZooKeeper heartbeat)
  Controller picks new leader from ISR (e.g., Broker2)

After recovery:
  Partition 0: Leader=Broker2, Followers=Broker1(recovered), Broker3
  ISR = {Broker2, Broker3}  (Broker1 rejoins after catching up)
```

Producers/consumers reconnect to Broker2 automatically. **No data is lost.**

### Replication Factor Configuration

```bash
# Your project creates topics with:
--replication-factor 1   # Development: no redundancy (single broker)

# Production recommendation:
--replication-factor 3   # Tolerates 2 broker failures
```

**Formula: `replication-factor` must be <= number of brokers**

If you have 1 broker, replication-factor can only be 1. This is why your dev setup uses 1 (you only have 1 broker).

### Key Replication Settings

```properties
# Minimum ISR — producer gets error if fewer replicas are in sync
min.insync.replicas=2

# How long a follower can be behind before removed from ISR
replica.lag.time.max.ms=30000

# Transaction log replication (internal Kafka topic)
KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR=1  # your docker-compose
```

### acks Setting (Producer Side)

This controls when the producer considers a write "done":

| acks value | Meaning | Risk | Speed |
|---|---|---|---|
| `0` | Fire and forget. No wait. | Message can be lost | Fastest |
| `1` | Wait for leader only | Lost if leader dies before replicas copy | Fast |
| `all` or `-1` | Wait for all ISR replicas | No loss (strongest guarantee) | Slowest |

**From your `KafkaProducerConfig.java`:**
```java
configProps.put(ProducerConfig.ACKS_CONFIG, "all"); // Maximum safety
```

### Interview Questions on Replication

**Q: What is the difference between replication factor and ISR?**
Replication factor is the CONFIGURED number of copies. ISR is the ACTUAL set of replicas currently in sync. ISR <= replication factor.

**Q: What is an unclean leader election?**
When ISR is empty (all replicas crashed), Kafka can elect a non-ISR replica as leader. This risks data loss. Controlled by `unclean.leader.election.enable` (default: false — data safety over availability).

**Q: If replication-factor=3 and min.insync.replicas=2, how many brokers can fail?**
1 broker can fail. With 2 remaining in ISR, producers with acks=all still work. If 2 brokers fail, ISR < min.insync.replicas, and producers get `NotEnoughReplicasException`.

**Q: Does replication affect performance?**
Yes. Higher replication = more network I/O and disk writes. But the safety benefit far outweighs the cost in production.

---

## 7. Producers

### What is a Producer?

A producer is any application that **writes messages to Kafka topics**.

```
Your App (Producer) --> [serialize] --> Kafka Broker --> Topic Partition
```

### Producer Internals — The Journey of a Message

```
1. Application calls kafkaTemplate.send(topic, key, value)
2. Serializer converts Java object → bytes
3. Partitioner decides which partition
4. Message added to RecordAccumulator (in-memory buffer)
5. Sender thread batches messages and sends to broker
6. Broker writes to leader partition
7. Leader replicates to followers (based on acks setting)
8. Broker sends acknowledgment back
9. CompletableFuture completes (success or failure)
```

### Producer Configuration Deep Dive

From your `KafkaProducerConfig.java`:

```java
// 1. BOOTSTRAP SERVERS
// Entry point to the cluster. Producer connects here first,
// then discovers all other brokers.
configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
// You only need ONE broker here — Kafka auto-discovers the rest.
// Best practice: list 2-3 for redundancy.
// e.g., "broker1:9092,broker2:9092,broker3:9092"

// 2. SERIALIZERS
// Key serializer: converts the message key (String) to bytes
configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
// Value serializer: converts Java object to JSON bytes
configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

// 3. ACKS (Acknowledgment)
// "all" = wait for all ISR replicas to confirm write
// Provides strongest durability guarantee
configProps.put(ProducerConfig.ACKS_CONFIG, "all");

// 4. RETRIES
// How many times to retry if send fails
configProps.put(ProducerConfig.RETRIES_CONFIG, 3);

// 5. IDEMPOTENCE
// Prevents duplicate messages during retries
// If producer retries, Kafka deduplicates using sequence numbers
configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
// Note: requires acks=all and retries > 0 (auto-configured)

// 6. MAX IN FLIGHT REQUESTS
// How many unacknowledged requests per connection
// With idempotence=true, can be up to 5 (Kafka guarantees ordering)
configProps.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5);

// 7. COMPRESSION
// Compresses message batches before sending
// Options: none, gzip, snappy, lz4, zstd
// snappy: good balance of speed and compression ratio
configProps.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy");

// 8. LINGER_MS
// How long to wait before sending a batch (to accumulate more messages)
// 0 = send immediately, higher = better batching but more latency
configProps.put(ProducerConfig.LINGER_MS_CONFIG, 1); // 1ms for low latency

// 9. BATCH_SIZE
// Max size of a batch in bytes
// Larger batch = better throughput, more memory
configProps.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384); // 16KB
```

### KafkaTemplate — Spring's Producer Abstraction

```java
// Inject it anywhere
@Autowired
private KafkaTemplate<String, Object> kafkaTemplate;

// Send message (fire and forget)
kafkaTemplate.send("topic-name", "key", eventObject);

// Send with callback (async)
CompletableFuture<SendResult<String, Object>> future =
    kafkaTemplate.send("topic-name", "key", eventObject);

future.whenComplete((result, ex) -> {
    if (ex == null) {
        // Success
        int partition = result.getRecordMetadata().partition();
        long offset = result.getRecordMetadata().offset();
        System.out.println("Sent to partition " + partition + " at offset " + offset);
    } else {
        // Handle failure
        System.err.println("Failed: " + ex.getMessage());
    }
});

// Send and wait (synchronous — blocks thread, avoid in production)
SendResult<String, Object> result = kafkaTemplate.send(...).get();
```

### Idempotent Producer — Exactly Once on Producer Side

**Problem without idempotence:**
```
Producer sends message → Network timeout
Producer retries → Message arrives twice (duplicate!)
```

**With `enable.idempotence=true`:**
```
Producer assigns sequence number to each message
Kafka broker tracks sequence numbers per producer
If duplicate arrives → broker silently discards it
```

### Producer Partitioning Strategies

```java
// Default: hash-based (if key present)
// partition = murmur2(key) % numPartitions

// Custom Partitioner (implement Partitioner interface)
public class CustomPartitioner implements Partitioner {
    @Override
    public int partition(String topic, Object key, byte[] keyBytes,
                         Object value, byte[] valueBytes, Cluster cluster) {
        // Your logic here
        // e.g., VIP users to partition 0
        if (key.toString().startsWith("VIP")) return 0;
        return (key.hashCode() & Integer.MAX_VALUE) % cluster.partitionCountForTopic(topic);
    }
}

// Register it:
configProps.put(ProducerConfig.PARTITIONER_CLASS_CONFIG, CustomPartitioner.class);
```

### Interview Questions on Producers

**Q: What is the difference between `linger.ms` and `batch.size`?**
`batch.size` is the MAX size — batch sends when full. `linger.ms` is the MAX WAIT — batch sends after this time even if not full. The batch sends when EITHER condition is met first.

**Q: How does idempotent producer work with retries?**
Each producer gets a unique PID (Producer ID). Each message gets a sequence number. Broker stores `{PID, partition, sequence}`. If a retry arrives with the same sequence, broker discards it.

**Q: Can you guarantee exactly-once delivery end-to-end?**
Yes, using Kafka Transactions. Producer wraps sends in `beginTransaction()` / `commitTransaction()`. Combined with transactional consumers (`isolation.level=read_committed`), you get exactly-once semantics.

**Q: What is `buffer.memory`?**
Total memory for the producer's send buffer. If full (broker too slow), `send()` blocks for `max.block.ms` then throws `BufferExhaustedException`. Default: 32MB.

---

## 8. Consumers & Consumer Groups

### What is a Consumer?

A consumer is any application that **reads messages from Kafka topics**.

```
Kafka Topic Partition --> [deserialize] --> Consumer Application
```

### The Poll Loop — How Consumers Work

Unlike most message queues that "push" messages, Kafka consumers **pull/poll**:

```java
// Under the hood (Spring abstracts this for you)
while (true) {
    ConsumerRecords<String, Object> records = consumer.poll(Duration.ofMillis(100));
    for (ConsumerRecord<String, Object> record : records) {
        // process record
    }
    consumer.commitSync(); // or commitAsync()
}
```

**Why pull instead of push?**
- Consumers control their own pace (no overwhelm)
- Consumers can batch-process at their own rate
- Simpler backpressure handling

### Consumer Groups — The Magic of Parallel Processing

**Consumer Group** = A group of consumers that share the work of reading a topic.

**Rules:**
- Each partition is assigned to exactly ONE consumer in a group
- One consumer can handle multiple partitions
- Number of active consumers <= number of partitions

```
Topic: ticket-events (3 partitions)
Consumer Group: payment-service-group

Scenario 1: 1 consumer
  Consumer A --> reads Partition 0, 1, 2

Scenario 2: 2 consumers
  Consumer A --> reads Partition 0, 1
  Consumer B --> reads Partition 2

Scenario 3: 3 consumers (ideal)
  Consumer A --> reads Partition 0
  Consumer B --> reads Partition 1
  Consumer C --> reads Partition 2

Scenario 4: 4 consumers (over-provisioned)
  Consumer A --> reads Partition 0
  Consumer B --> reads Partition 1
  Consumer C --> reads Partition 2
  Consumer D --> IDLE (no partition to read)
```

### Multiple Consumer Groups = Pub/Sub

Different consumer groups read INDEPENDENTLY:

```
Topic: ticket-events
  |
  +--> payment-service-group   --> PaymentService reads ALL messages
  |
  +--> notification-service-group --> NotificationService reads ALL messages
  |
  +--> audit-service-group    --> AuditService reads ALL messages
```

Each group maintains its own offset — they don't interfere with each other.

**From your project:**
```yaml
# ticket-service/application.yml
consumer:
  group-id: ticket-service-group

# payment-service/application.yml
consumer:
  group-id: payment-service-group

# audit-service/application.yml
consumer:
  group-id: audit-service-group
```

### @KafkaListener — Spring's Consumer Abstraction

```java
// Simple listener
@KafkaListener(topics = "ticket-events", groupId = "payment-service-group")
public void handleTicketEvent(TicketReservedEvent event) {
    paymentService.processPayment(event);
}

// With manual acknowledgment (your project uses this)
@KafkaListener(
    topics = "${kafka.topics.ticket-events}",
    groupId = "${spring.kafka.consumer.group-id}",
    containerFactory = "kafkaListenerContainerFactory"
)
public void handleTicketReserved(
    @Payload TicketReservedEvent event,
    Acknowledgment acknowledgment) {

    try {
        paymentService.processPayment(event);
        acknowledgment.acknowledge(); // Mark as processed
    } catch (Exception e) {
        // Don't acknowledge -> message will be redelivered
    }
}

// Multiple topics (your audit service)
@KafkaListener(topics = {"ticket-events", "payment-events", "user-events"})
public void auditAll(@Payload BaseEvent event,
    @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
    @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
    @Header(KafkaHeaders.OFFSET) long offset) {
    auditService.log(event, topic, partition, offset);
}

// Polymorphic handling (your ticket service PaymentEventConsumer)
@KafkaListener(topics = "${kafka.topics.payment-events}", ...)
public class PaymentEventConsumer {

    @KafkaHandler  // Called when message is PaymentCompletedEvent
    public void handleCompleted(PaymentCompletedEvent event, Acknowledgment ack) { ... }

    @KafkaHandler  // Called when message is PaymentFailedEvent
    public void handleFailed(PaymentFailedEvent event, Acknowledgment ack) { ... }
}
```

### Concurrency — Parallel Consumer Threads

```java
// From your KafkaConsumerConfig.java
factory.setConcurrency(3);
// This creates 3 consumer threads per @KafkaListener
// Each thread handles one partition (if 3 partitions)
```

With `concurrency=3` and `ticket-events` having 3 partitions:
```
Thread 1 --> Partition 0
Thread 2 --> Partition 1
Thread 3 --> Partition 2
```
Messages processed in parallel across partitions!

### Consumer Configuration Deep Dive

```java
// AUTO OFFSET RESET — Where to start reading if no offset stored
// "earliest": from the very first message (beginning of topic)
// "latest": only new messages from now (ignore old ones)
// "none": throw exception if no offset found
configProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

// ENABLE AUTO COMMIT — Should Kafka auto-commit offsets?
// false = you manually call acknowledgment.acknowledge()
// true = Kafka auto-commits every auto.commit.interval.ms
configProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

// MAX POLL RECORDS — Max messages fetched per poll()
// Default: 500. Lower this if processing is slow.
configProps.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 500);

// FETCH MIN BYTES — Minimum data to fetch per request
// Broker waits until this much data is available (or timeout)
// Higher = fewer requests, more batching, slightly more latency
configProps.put(ConsumerConfig.FETCH_MIN_BYTES_CONFIG, 1024); // 1KB

// MAX POLL INTERVAL — Max time between poll() calls
// If consumer takes longer to process, it's considered dead
// Kafka rebalances (reassigns its partitions)
// Default: 300000ms (5 minutes)
configProps.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, 300000);

// SESSION TIMEOUT — Heartbeat timeout
// Consumer sends heartbeats to broker
// If no heartbeat in this time, considered dead
configProps.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 45000);
```

### Consumer Rebalancing

When consumers join or leave a group, Kafka **rebalances** — reassigns partitions.

```
Initial state:
  Consumer A --> P0, P1
  Consumer B --> P2

Consumer C joins:
  [REBALANCING — brief pause in consumption]
  Consumer A --> P0
  Consumer B --> P1
  Consumer C --> P2

Consumer A crashes:
  [REBALANCING]
  Consumer B --> P0, P1
  Consumer C --> P2
```

**Impact of Rebalancing:**
- Brief stop-the-world pause (all consumers stop, rejoin, get new assignments)
- Can cause duplicate processing if offsets weren't committed before rebalance

**Strategies to minimize rebalance impact:**
1. **Incremental Cooperative Rebalancing** (Kafka 2.4+): only affected partitions move
2. **Static membership** (`group.instance.id`): Consumer keeps its partition assignment across restarts

### Interview Questions on Consumers

**Q: What happens if a consumer dies without committing an offset?**
The partition is reassigned to another consumer. That consumer starts from the last committed offset. Messages since the last commit may be reprocessed (at-least-once delivery).

**Q: What is the difference between auto-commit and manual commit?**
Auto-commit (`enable.auto.commit=true`) commits offsets automatically every `auto.commit.interval.ms`. Risk: commits before processing completes → message loss if consumer crashes. Manual commit gives you control: commit AFTER successful processing → at-least-once, or use transactions for exactly-once.

**Q: Can a consumer read from a specific partition?**
Yes, using `assign()` instead of `subscribe()`. But you lose automatic rebalancing.

**Q: What is `max.poll.interval.ms` and why does it matter?**
If your processing takes longer than this, Kafka considers the consumer dead and triggers a rebalance. Another consumer gets the partition and reprocesses the same messages → duplicates. Set it higher than your maximum processing time.

**Q: What is the difference between `session.timeout.ms` and `max.poll.interval.ms`?**
`session.timeout.ms` = heartbeat timeout (consumer sends heartbeats from background thread). `max.poll.interval.ms` = how long between `poll()` calls (processing timeout). Both can trigger rebalance if exceeded.

---

## 9. Offsets

### What is an Offset?

An **offset** is the unique sequential ID of a message within a partition.

```
Partition 0:
  Offset 0: {"event": "RESERVED", "ticketId": "t1"}
  Offset 1: {"event": "RESERVED", "ticketId": "t2"}
  Offset 2: {"event": "BOOKED", "ticketId": "t1"}
  Offset 3: {"event": "RESERVED", "ticketId": "t3"}
  ...
```

Offsets are:
- **Always increasing** (never reset within a partition)
- **Per partition** (Partition 0 and Partition 1 both have offset 0)
- **Stored in Kafka** itself (in `__consumer_offsets` internal topic)

### How Kafka Tracks Progress

```
Consumer Group "payment-service-group" reads ticket-events:

Partition 0: messages at offsets 0,1,2,3,4 (last read: 3, committed: 2)
             ^ committed offset means "I have processed up to here"

If consumer restarts: starts reading from offset 3 (committed + 1)
```

### Commit Strategies

**1. Auto-commit (simple but risky):**
```java
enable.auto.commit=true
auto.commit.interval.ms=5000
// Commits every 5 seconds regardless of processing status
// Risk: commits offset 5, consumer crashes processing 3 → messages 3,4 lost
```

**2. Manual sync commit (safe but slow):**
```java
// From your code: AckMode.MANUAL_IMMEDIATE
acknowledgment.acknowledge();
// Commits IMMEDIATELY after successful processing
// Safe but blocks until commit confirmed
```

**3. Manual async commit (faster, slight risk):**
```java
consumer.commitAsync((offsets, exception) -> {
    if (exception != null) log.error("Commit failed", exception);
});
// Non-blocking, but order of commits not guaranteed
```

### Offset Reset Scenarios

```
# "earliest" — consumer joins fresh and reads from the beginning
# Use case: replay all history, audit systems

# "latest" — consumer joins and reads only new messages
# Use case: real-time processing, only care about current state

# Manual reset (kafka-consumer-groups CLI):
kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --group payment-service-group \
  --topic ticket-events \
  --reset-offsets --to-earliest \
  --execute
```

### AckMode in Spring Kafka

```java
// From your KafkaConsumerConfig.java:
factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
```

| AckMode | Behavior |
|---|---|
| `RECORD` | Commit after each record processed |
| `BATCH` | Commit after each batch from poll() |
| `TIME` | Commit every N milliseconds |
| `COUNT` | Commit every N records |
| `MANUAL` | You call `ack.acknowledge()`, committed on next poll |
| `MANUAL_IMMEDIATE` | You call `ack.acknowledge()`, committed immediately |

### Interview Questions on Offsets

**Q: Where are consumer offsets stored?**
In Kafka itself, in the internal topic `__consumer_offsets`. Not in ZooKeeper (changed in Kafka 0.9).

**Q: What is log-end offset vs committed offset?**
Log-end offset: latest offset in the partition (latest message). Committed offset: offset the consumer has acknowledged processing. The difference is **consumer lag**.

**Q: What is consumer lag?**
`consumer lag = log-end offset - committed offset`. High lag = consumer is falling behind. Monitor with `kafka-consumer-groups --describe` or tools like Burrow.

**Q: Can you replay messages in Kafka?**
Yes! Just reset the consumer group offset to an earlier position. Unlike traditional queues (RabbitMQ), Kafka retains messages and allows replaying.

---

## 10. Zookeeper vs KRaft

### What is ZooKeeper?

ZooKeeper is a separate service Kafka traditionally used for:
- Tracking which brokers are alive
- Storing cluster metadata (topic configs, partition assignments)
- Leader election for partitions

```yaml
# From your docker-compose.yml
zookeeper:
  image: confluentinc/cp-zookeeper:7.5.0
  environment:
    ZOOKEEPER_CLIENT_PORT: 2181
    ZOOKEEPER_TICK_TIME: 2000  # Heartbeat interval in ms

kafka:
  environment:
    KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181  # Kafka connects to ZooKeeper
```

**ZooKeeper's TICK_TIME:**
- `TICK_TIME=2000` means heartbeat every 2 seconds
- Session timeout = 2 * TICK_TIME = 4 seconds (default)
- If ZooKeeper doesn't hear from a broker in this time → considers it dead

### Problems with ZooKeeper

1. **Operational complexity**: Two systems to manage and monitor
2. **Scaling bottleneck**: ZooKeeper struggles with millions of partitions
3. **Slow metadata operations**: Controller reads/writes from ZooKeeper are slow
4. **Restarting** requires careful ZooKeeper cleanup

### KRaft — ZooKeeper Replacement (Kafka 2.8+, Default in 3.x)

**KRaft** (Kafka Raft) = Kafka manages its own metadata using Raft consensus.

```
ZooKeeper mode:           KRaft mode:
  ZooKeeper               Kafka Broker (KRaft Controller)
    |                       |
  Kafka Controller -------> Kafka Broker
    |                       |
  Kafka Broker           Kafka Broker
```

**Benefits of KRaft:**
- Single system to manage
- Can handle millions of partitions
- Faster leader election (milliseconds vs seconds)
- Better scalability

**Your project uses ZooKeeper** (Confluent 7.5.0 — still common in enterprise). KRaft is production-ready from Kafka 3.3+.

### Interview Questions on ZooKeeper/KRaft

**Q: What does ZooKeeper do for Kafka?**
Stores broker metadata, manages leader elections, tracks consumer group membership (legacy), stores ACLs and configs.

**Q: Is ZooKeeper required in new Kafka versions?**
No. KRaft mode (Kafka 3.3+) is the new standard. ZooKeeper is deprecated and will be removed in Kafka 4.0.

**Q: What is the Raft consensus algorithm?**
Raft is a consensus algorithm that allows a distributed group of nodes to agree on values (like leader election) even when some nodes fail. It's simpler than Paxos and more efficient.

---

## 11. Kafka Configuration Reference

### Complete Producer Config

| Property | Your Value | Meaning |
|---|---|---|
| `bootstrap.servers` | `localhost:9092` | Entry point brokers |
| `key.serializer` | `StringSerializer` | Key → bytes |
| `value.serializer` | `JsonSerializer` | Object → JSON bytes |
| `acks` | `all` | Wait for all ISR replicas |
| `retries` | `3` | Retry 3 times on failure |
| `enable.idempotence` | `true` | No duplicate messages |
| `max.in.flight.requests.per.connection` | `5` | 5 unacked requests per connection |
| `compression.type` | `snappy` | Compress batches |
| `linger.ms` | `1` | Wait 1ms to batch |
| `batch.size` | `16384` | 16KB max batch |
| `buffer.memory` | `33554432` (default) | 32MB send buffer |
| `max.block.ms` | `60000` (default) | Block for 60s if buffer full |
| `request.timeout.ms` | `30000` (default) | Timeout per request |

### Complete Consumer Config

| Property | Your Value | Meaning |
|---|---|---|
| `bootstrap.servers` | `localhost:9092` | Entry point brokers |
| `group.id` | `ticket-service-group` | Consumer group name |
| `key.deserializer` | `StringDeserializer` | Bytes → String key |
| `value.deserializer` | `ErrorHandlingDeserializer` | JSON bytes → Object |
| `auto.offset.reset` | `earliest` | Start from beginning |
| `enable.auto.commit` | `false` | Manual commit |
| `max.poll.records` | `500` | Fetch max 500 per poll |
| `fetch.min.bytes` | `1024` | Min 1KB per fetch |
| `max.poll.interval.ms` | `300000` (default) | 5min max processing time |
| `session.timeout.ms` | `45000` (default) | 45s heartbeat timeout |
| `heartbeat.interval.ms` | `3000` (default) | Heartbeat every 3s |

### Topic Configuration

| Property | Meaning |
|---|---|
| `num.partitions` | Default partition count for new topics |
| `replication.factor` | Default replication factor |
| `retention.ms` | How long to keep messages (default 7 days) |
| `retention.bytes` | Max size per partition before deletion |
| `cleanup.policy` | `delete` or `compact` |
| `min.insync.replicas` | Min replicas that must be in sync |
| `compression.type` | Compression for stored messages |
| `max.message.bytes` | Max message size (default 1MB) |

---

## 12. Spring Boot Kafka Integration

### Dependencies (pom.xml)

```xml
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
    <!-- Version managed by Spring Boot parent -->
</dependency>
```

### Auto-Configuration vs Manual Configuration

**Auto-config (simple, uses application.yml):**
```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9092
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
    consumer:
      group-id: my-group
      auto-offset-reset: earliest
```

**Manual config (your approach — more control):**
```java
@Configuration
public class KafkaProducerConfig {
    @Bean
    public ProducerFactory<String, Object> producerFactory() { ... }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
}
```

**Which to use?** Manual when you need custom serializers, error handling, or multiple different templates.

### Error Handling in Consumers

**Your current approach (basic):**
```java
try {
    process(event);
    acknowledgment.acknowledge();
} catch (Exception e) {
    log.error("Error", e);
    // Don't ack → will be retried
}
```

**Production approach (with retry + DLQ):**
```java
@Bean
public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory() {
    factory.setCommonErrorHandler(new DefaultErrorHandler(
        new DeadLetterPublishingRecoverer(kafkaTemplate), // Send to DLQ after max retries
        new FixedBackOff(1000L, 3L) // Wait 1s, retry 3 times
    ));
    return factory;
}
```

### Message Headers — Type Information

When using `JsonSerializer`, Spring adds a header:
```
Header: __TypeId__ = com.ticketbooking.common.event.TicketReservedEvent
```

This tells the consumer which Java class to deserialize into.

From your `KafkaConsumerConfig.java`:
```java
configProps.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, true);
// Reads __TypeId__ header to determine class
configProps.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
// Allows deserializing classes from any package
// In production: specify exact packages for security
// e.g., "com.ticketbooking.common.event"
```

### Sending to Specific Partition

```java
// Send to specific partition (partition 0)
kafkaTemplate.send(new ProducerRecord<>(topic, 0, key, value));

// Send with specific timestamp
kafkaTemplate.send(new ProducerRecord<>(topic, partition, timestamp, key, value));
```

### Listening to Multiple Topics with Pattern

```java
// Your AuditEventConsumer explicitly lists topics
@KafkaListener(topics = {"ticket-events", "payment-events", "user-events"})

// Alternative: regex pattern (catches any new *-events topics automatically)
@KafkaListener(topicPattern = ".*-events")
```

### Transaction Support

```java
@Bean
public KafkaTransactionManager<String, Object> kafkaTransactionManager() {
    return new KafkaTransactionManager<>(producerFactory());
}

@Transactional
public void processAndPublish(Event event) {
    // Database operation
    repository.save(someEntity);
    // Kafka publish — both committed or both rolled back
    kafkaTemplate.send("topic", event);
}
```

---

## 13. Design Patterns

### Pattern 1: SAGA Pattern (Your Project)

**Problem**: How to do distributed transactions across microservices without two-phase commit?

**Solution**: Chain of events with compensating transactions.

```
Ticket Service                Payment Service         Notification Service
     |                              |                        |
     |-- reserve ticket             |                        |
     |-- publish TicketReserved --> |                        |
     |                        process payment               |
     |                        -- publish PaymentCompleted-->|
     |<-- PaymentCompleted ------   |                   send email
     |-- confirm booking            |
     |-- publish TicketBooked       |

On Failure:
     |<-- PaymentFailed ---------   |
     |-- release ticket             |
     |-- publish TicketReleased     |
```

**Your code implements this exactly:**
- `TicketService.reserveTicket()` → publishes `TicketReservedEvent`
- `PaymentService` consumes → publishes `PaymentCompletedEvent` or `PaymentFailedEvent`
- `TicketService.confirmBooking()` or `releaseTicket()` → handles the result

### Pattern 2: Event Sourcing

Instead of storing current state, store every state-changing event.

```
Traditional DB:
  ticket_table: {id: "t1", status: "BOOKED", seat: "A1"}

Event Sourcing:
  event_log: [
    {type: "RESERVED", ticketId: "t1", timestamp: ...}
    {type: "PAYMENT_COMPLETED", ticketId: "t1", ...}
    {type: "BOOKED", ticketId: "t1", ...}
  ]
  Current state = replay all events
```

Your audit service uses this pattern: every event is stored in MongoDB as a log.

### Pattern 3: CQRS (Command Query Responsibility Segregation)

Separate write model (commands) from read model (queries).

```
Write side: POST /tickets → publishes event → updates write DB
Read side:  GET /tickets  → reads from read-optimized DB (updated by consumer)

ticket-events topic → Consumer → Read DB (denormalized, optimized for queries)
```

### Pattern 4: Outbox Pattern

**Problem**: How to atomically write to DB and publish to Kafka?

```java
// WRONG: What if Kafka fails after DB save?
ticketRepository.save(ticket);           // Step 1: DB save
kafkaTemplate.send("topic", event);      // Step 2: Kafka send — could fail!
// DB has ticket but Kafka never got the event!

// OUTBOX PATTERN:
// Step 1: Save ticket + outbox record in SAME DB transaction
ticketRepository.save(ticket);
outboxRepository.save(new OutboxEvent(event));  // Same transaction

// Step 2: Separate process reads outbox and publishes to Kafka
@Scheduled(fixedDelay = 1000)
public void processOutbox() {
    List<OutboxEvent> events = outboxRepository.findUnpublished();
    for (OutboxEvent e : events) {
        kafkaTemplate.send("topic", e.getPayload());
        e.setPublished(true);
        outboxRepository.save(e);
    }
}
```

### Pattern 5: Dead Letter Queue (DLQ)

Failed messages after max retries go to DLQ for inspection.

```
Normal flow:
  ticket-events → consumer → success

On repeated failure:
  ticket-events → consumer → fail x3 → dead-letter-queue

Operations team:
  dead-letter-queue → inspect message → fix bug → republish to ticket-events
```

Your project has `dead-letter-queue` topic defined in docker-compose.yml but not yet wired up in code — a good addition for production.

---

## 14. Your Project Explained

### Full Event Flow with Kafka Concepts

```
1. User hits POST /tickets (TicketController.java)
   |
2. TicketService.reserveTicket() runs
   - Saves Ticket to PostgreSQL (status=RESERVED)
   - Creates TicketReservedEvent
   |
3. TicketEventProducer.publishTicketReserved()
   - kafkaTemplate.send("ticket-events", ticketId, event)
   - Key=ticketId → consistent partition
   - acks=all → waits for all ISR replicas
   - CompletableFuture → async confirmation
   |
4. Kafka stores message in ticket-events topic
   - Serialized to JSON by JsonSerializer
   - __TypeId__ header = TicketReservedEvent
   - Replicated (if replication-factor > 1)
   |
5. Multiple consumers read from ticket-events (different groups):
   |
   +-> payment-service-group (TicketEventConsumer.java in payment-service)
   |   - Reads TicketReservedEvent from partition assigned to it
   |   - paymentService.processPayment(event)
   |   - Simulates payment (success/failure)
   |   - Publishes PaymentCompletedEvent or PaymentFailedEvent to payment-events
   |   - acknowledgment.acknowledge() → commits offset
   |
   +-> notification-service-group (reads ticket-events)
   |   - Sends "your ticket is reserved" notification
   |
   +-> audit-service-group (AuditEventConsumer.java)
       - Logs event to MongoDB with topic, partition, offset metadata
       - @Header(KafkaHeaders.RECEIVED_TOPIC) gives topic name
       - @Header(KafkaHeaders.OFFSET) gives message position

6. payment-service publishes to payment-events topic
   |
7. ticket-service-group reads payment-events (PaymentEventConsumer.java)
   - @KafkaHandler routes to correct method by payload type
   - PaymentCompletedEvent → ticketService.confirmBooking()
   - PaymentFailedEvent → ticketService.releaseTicket()
   - Each case acknowledges after successful processing
   |
8. TicketService updates DB and publishes another event (TicketBooked/Released)
   |
9. notification-service, audit-service consume TicketBooked/Released
```

### Why Each Configuration Choice Was Made

| Config | Value | Why |
|---|---|---|
| `acks=all` | Maximum durability | Ticket data is critical — can't lose it |
| `enable.idempotence=true` | No duplicates | Don't want to charge twice or double-book |
| `retries=3` | Retry on transient failure | Network blip shouldn't lose booking |
| `auto.offset.reset=earliest` | Read from beginning | New consumer instances should process old events |
| `enable.auto.commit=false` | Manual commit | Control exactly when "processed" is marked |
| `AckMode.MANUAL_IMMEDIATE` | Commit immediately | Instant feedback after processing |
| `concurrency=3` | 3 consumer threads | Match the 3 partitions of ticket-events |
| `linger.ms=1` | Low latency | Event-driven system — don't want batching delay |

---

## 15. Advanced Topics

### Exactly-Once Semantics (EOS)

Kafka delivery guarantees:

| Guarantee | Description | Risk | Config |
|---|---|---|---|
| At-most-once | Message delivered 0 or 1 times | Data loss | acks=0, no retry |
| At-least-once | Message delivered 1 or more times | Duplicates | acks=all, retry, no idempotence |
| Exactly-once | Message delivered exactly once | Complex setup | Transactions + idempotence |

**Exactly-once setup:**
```java
// Producer
configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
configProps.put(ProducerConfig.TRANSACTIONAL_ID_CONFIG, "tx-ticket-service-1");

// Consumer (only read committed transactions)
configProps.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed");

// Usage
kafkaTemplate.executeInTransaction(t -> {
    t.send("topic-a", event1);
    t.send("topic-b", event2);
    return true;
    // Both sent or neither sent
});
```

### Log Compaction

Instead of deleting old messages by time, Kafka keeps the **last message per key**.

```
Before compaction:          After compaction:
Key=user1: {"name":"John"}  Key=user1: {"name":"John Smith"}  (latest)
Key=user1: {"name":"John Smith"}
Key=user2: {"email":"x@x.com"}  Key=user2: {"email":"y@y.com"}  (latest)
Key=user2: {"email":"y@y.com"}
```

Use case: User profiles, account balances, configuration — you only need the latest state.

```bash
kafka-topics --create --topic user-profiles \
  --config cleanup.policy=compact
```

### Kafka Streams (Brief Overview)

Kafka Streams is a library for processing events **within Kafka**:

```java
// Count bookings per movie in real-time
KStream<String, TicketReservedEvent> bookings = builder.stream("ticket-events");
KTable<String, Long> bookingCounts = bookings
    .groupBy((key, event) -> event.getMovieName())
    .count();
bookingCounts.toStream().to("booking-counts");
```

### Schema Registry

In production, use **Confluent Schema Registry** + Avro instead of JSON:

```
Benefits:
- Schema validation (can't send wrong data type)
- Schema evolution (backward/forward compatibility)
- Smaller messages (binary Avro vs text JSON)

JSON:  {"ticketId": "abc", "amount": 150.00}  = ~40 bytes
Avro:  [binary]                                = ~10 bytes
```

### Kafka Connect

Kafka Connect moves data between Kafka and external systems without code:

```
MySQL → Kafka: mysql-source-connector
Kafka → Elasticsearch: elasticsearch-sink-connector
Kafka → PostgreSQL: jdbc-sink-connector
```

### Multi-Region Kafka

For global applications:

```
Region US-East:         Region EU-West:
  Kafka Cluster A  <-->  Kafka Cluster B
  (using MirrorMaker 2 for replication)

Users in US → write to Cluster A → replicated to Cluster B
Users in EU → write to Cluster B → replicated to Cluster A
```

---

## 16. Top 100 Interview Questions & Answers

### Beginner Level (1-30)

**Q1: What is Apache Kafka?**
Apache Kafka is an open-source distributed event streaming platform designed for high-throughput, fault-tolerant, real-time data pipelines and event-driven applications. It was originally developed by LinkedIn and open-sourced in 2011.

**Q2: What are the main components of Kafka?**
Broker, Topic, Partition, Producer, Consumer, Consumer Group, ZooKeeper/KRaft, and Offset.

**Q3: What is a Kafka topic?**
A topic is a named, ordered, immutable log of messages. Producers write to topics; consumers read from them. Topics are split into partitions for scalability.

**Q4: What is a partition?**
A partition is an ordered, append-only sequence of messages within a topic. It is the fundamental unit of parallelism and scalability in Kafka.

**Q5: What is a broker?**
A broker is a Kafka server. A cluster consists of multiple brokers. Each broker stores partitions of topics and serves producer/consumer requests.

**Q6: What is a producer?**
An application that publishes (writes) messages to Kafka topics.

**Q7: What is a consumer?**
An application that subscribes to (reads) messages from Kafka topics.

**Q8: What is a consumer group?**
A named group of consumers that cooperatively consume a topic. Each partition is assigned to exactly one consumer in the group, enabling parallel processing.

**Q9: What is an offset?**
A unique, sequential number identifying each message within a partition. Consumers track which messages they've processed using offsets.

**Q10: What is the role of ZooKeeper in Kafka?**
ZooKeeper manages broker metadata, coordinates leader election for partitions, stores topic configurations, and tracks consumer group membership (in older versions).

**Q11: What is replication in Kafka?**
Kafka copies partition data to multiple brokers. The original copy is the leader; copies are followers. Replication provides fault tolerance — if a broker fails, a follower becomes the new leader.

**Q12: What is a leader partition?**
The primary copy of a partition that handles all reads and writes. There is exactly one leader per partition at any time.

**Q13: What is a follower/replica?**
A copy of a partition stored on a different broker. Followers sync data from the leader but don't serve read/write requests by default.

**Q14: What is ISR?**
In-Sync Replicas — the set of replicas fully caught up with the partition leader. The leader tracks ISR; followers that lag too much are removed.

**Q15: What does `acks=all` mean?**
The producer waits for ALL replicas in the ISR to acknowledge the write before considering it successful. Provides maximum durability.

**Q16: What is `auto.offset.reset`?**
Determines where a consumer starts reading when no committed offset exists: `earliest` (beginning), `latest` (newest messages only), or `none` (throw exception).

**Q17: What is `enable.auto.commit`?**
If true, Kafka automatically commits the offset every `auto.commit.interval.ms`. If false, the application must manually commit.

**Q18: What is the difference between Kafka and RabbitMQ?**
Kafka: log-based, messages persisted and replayable, high throughput, consumer pulls. RabbitMQ: queue-based, messages deleted after consumption, push model, lower latency for small messages. Kafka excels at event streaming; RabbitMQ at task queues.

**Q19: What is Kafka retention?**
How long Kafka keeps messages. Default: 7 days (`retention.ms=604800000`). After retention, messages are deleted. Consumers that haven't read in time will miss messages.

**Q20: What is the maximum message size in Kafka?**
Default: 1MB (`max.message.bytes`). Can be increased but large messages reduce throughput. Better to use a reference pattern (store large data externally, send reference in Kafka).

**Q21: Can Kafka lose messages?**
Yes, under certain configurations (acks=0, unclean leader election). With `acks=all`, `min.insync.replicas=2`, `unclean.leader.election.enable=false`, data loss is practically eliminated.

**Q22: What is a Kafka cluster?**
A group of Kafka brokers working together. They share topic partitions, replicate data, and coordinate through ZooKeeper (or KRaft in newer versions).

**Q23: How does Kafka achieve high throughput?**
Sequential disk writes (faster than random), zero-copy data transfer, message batching, compression, and consumer pull model.

**Q24: What is the `bootstrap.servers` config?**
Initial list of broker addresses the client uses to connect to the cluster. After connecting, the client discovers all other brokers automatically.

**Q25: What serializer/deserializer does your project use?**
`JsonSerializer` for values (Java objects → JSON → bytes), `StringSerializer` for keys. On consumer side, `ErrorHandlingDeserializer` wrapping `JsonDeserializer`.

**Q26: What is `@KafkaListener`?**
Spring annotation that marks a method (or class) as a Kafka message listener. Spring creates a `ConcurrentMessageListenerContainer` that polls Kafka and invokes the annotated method.

**Q27: What is `KafkaTemplate`?**
Spring's high-level abstraction for sending messages to Kafka. Wraps the native `KafkaProducer` with Spring conveniences like callbacks and transaction support.

**Q28: What is `@KafkaHandler`?**
Used with class-level `@KafkaListener`. Routes incoming messages to the correct method based on payload type (polymorphic dispatch).

**Q29: What is log compaction?**
A retention strategy where Kafka keeps only the latest message per key (deletes older values for the same key). Useful for storing current state (like a changelog).

**Q30: What is a Dead Letter Queue (DLQ) in Kafka?**
A special topic where messages are sent after exhausting retry attempts. Allows inspection and manual reprocessing of failed messages without blocking the main topic.

---

### Intermediate Level (31-60)

**Q31: Explain the Kafka consumer poll loop.**
The consumer continuously calls `poll(timeout)` to fetch records from assigned partitions. Spring Kafka abstracts this loop — you only write the handler method. The loop also sends heartbeats to the broker.

**Q32: What is consumer rebalancing?**
When consumers join/leave a group, Kafka reassigns partitions among active consumers. During rebalancing, consumption pauses temporarily.

**Q33: What triggers a rebalance?**
Consumer joins the group, consumer leaves (graceful shutdown), consumer crashes (session timeout), partition count changes, consumer group config changes.

**Q34: What is static group membership?**
Setting `group.instance.id` on a consumer. This consumer retains its partition assignment even after restart (without triggering a rebalance), useful for stateful stream processing.

**Q35: What is idempotent producer?**
With `enable.idempotence=true`, each message gets a sequence number. If the producer retries, the broker deduplicates using `{producerId, partition, sequenceNumber}`. Prevents duplicate messages from retries.

**Q36: What is the difference between `batch.size` and `linger.ms`?**
`batch.size` is the size trigger — send batch when it reaches this size. `linger.ms` is the time trigger — send batch after waiting this long. Whichever comes first triggers the send.

**Q37: What is sticky partitioning?**
When a producer sends without a key, sticky partitioning keeps sending to the same partition until the batch is full or `linger.ms` expires, then switches. Reduces the number of small batches vs pure round-robin.

**Q38: Explain `max.poll.records` and when you'd change it.**
Maximum records returned per `poll()` call. Lower it if your processing is slow (to avoid `max.poll.interval.ms` violations). Default: 500.

**Q39: What is the __consumer_offsets topic?**
An internal Kafka topic that stores committed consumer group offsets. Compacted (keeps only latest offset per group+topic+partition). Not for direct consumption.

**Q40: What is the `__TypeId__` header?**
A Kafka message header added by Spring's `JsonSerializer`. Contains the fully qualified Java class name. The `JsonDeserializer` uses it to know which class to deserialize into.

**Q41: How does Spring route messages to @KafkaHandler methods?**
By checking the Java type of the deserialized payload. It matches the payload type to the method parameter type and invokes the correct `@KafkaHandler` method.

**Q42: What is `ContainerProperties.AckMode.MANUAL_IMMEDIATE`?**
When the consumer calls `acknowledgment.acknowledge()`, the offset is committed immediately (synchronously) rather than waiting for the next poll cycle.

**Q43: What happens if you don't acknowledge a message?**
The offset is not committed. If the consumer restarts, it will re-read from the last committed offset and reprocess the unacknowledged message. This is at-least-once delivery.

**Q44: How do you send a message to a specific partition?**
```java
kafkaTemplate.send(new ProducerRecord<>(topic, partitionIndex, key, value));
```

**Q45: What is a consumer lag and how do you monitor it?**
Consumer lag = latest offset - committed offset per partition. Monitor with `kafka-consumer-groups --describe`, Kafka's JMX metrics, or tools like Burrow, Grafana + Prometheus.

**Q46: What is Kafka's zero-copy optimization?**
Kafka uses `sendfile()` system call on Linux to transfer data from disk directly to the network socket without copying into user space. Dramatically reduces CPU usage for consumers.

**Q47: What is the purpose of the `correlationId` in your events?**
To trace a single booking request across multiple services. All events for one booking share the same `correlationId`, making distributed tracing possible (e.g., in Zipkin/Jaeger).

**Q48: Explain the SAGA pattern in your project.**
Distributed transaction across services: reserve ticket → process payment → confirm/release ticket. Each step publishes an event; the next service reacts. On failure, compensating events roll back (release ticket).

**Q49: What is the outbox pattern and why isn't it in your project?**
The outbox pattern ensures atomic DB write + Kafka publish. Your project saves to DB then publishes — if Kafka fails, DB has the record but Kafka doesn't. The outbox pattern fixes this by saving the event to a DB outbox table in the same transaction.

**Q50: What is `KAFKA_AUTO_CREATE_TOPICS_ENABLE`?**
If true, Kafka creates a topic automatically when a producer first sends to it or a consumer first reads from it. Your project has this enabled (`'true'`). In production, disable it and create topics with proper configs.

**Q51: How does compression work in Kafka?**
The producer compresses batches of messages. The compressed batch is stored and transferred. The consumer decompresses. Compression is end-to-end — the broker stores compressed data as-is. Your project uses `snappy` (fast compression, good ratio).

**Q52: What is `min.insync.replicas`?**
Minimum number of replicas that must acknowledge a write when `acks=all`. If ISR < min.insync.replicas, the broker rejects the write with `NotEnoughReplicasException`. Protects against data loss.

**Q53: How many brokers can you lose with replication-factor=3, min.insync.replicas=2?**
You can lose 1 broker. With 2 remaining, ISR (2) >= min.insync.replicas (2) → writes succeed. If you lose 2 brokers, ISR (1) < min.insync.replicas (2) → writes fail (but data is safe on surviving broker).

**Q54: What is `unclean.leader.election.enable`?**
If true, Kafka can elect a non-ISR replica as leader (when ISR is empty). This avoids downtime but risks data loss (the replica might be behind). Default: false (safety over availability).

**Q55: Explain partition assignment strategies.**
`RangeAssignor` (default): assigns partitions consecutively to consumers. `RoundRobinAssignor`: distributes partitions evenly. `StickyAssignor`: minimizes partition movement during rebalance. Configured with `partition.assignment.strategy`.

**Q56: What is `fetch.min.bytes` and `fetch.max.wait.ms`?**
`fetch.min.bytes`: broker waits until this much data is available before responding to fetch request. `fetch.max.wait.ms`: max time broker waits regardless of data size. Balances latency vs throughput.

**Q57: What is the difference between `commitSync` and `commitAsync`?**
`commitSync`: blocks until commit is confirmed, retries on failure. Slower but safe. `commitAsync`: non-blocking, uses callback for result. Faster but order of commits not guaranteed in retries.

**Q58: How does `ErrorHandlingDeserializer` work in your project?**
Wraps `JsonDeserializer`. If deserialization fails (corrupt/incompatible message), it doesn't throw an exception. Instead, it stores the exception in the record header and returns a null/error record. Spring can then route it to the DLQ instead of crashing the consumer.

**Q59: What is `KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1` in your docker-compose?**
Sets the replication factor for the internal `__consumer_offsets` topic. Your single-broker dev setup can only support replication-factor of 1. In production with 3+ brokers, set to 3.

**Q60: What is KafkaTemplate's `executeInTransaction()`?**
Executes a series of Kafka sends within a transaction. All succeed or all fail. Requires `transactional.id` on the producer factory.

---

### Advanced Level (61-90)

**Q61: How do Kafka Transactions work?**
Kafka transactions use a two-phase commit between producer and broker. Producer calls `beginTransaction()`, sends messages, calls `commitTransaction()`. Broker marks messages as committed. Transactional consumers (isolation.level=read_committed) only see committed messages.

**Q62: What is exactly-once semantics end-to-end?**
Combining idempotent producer + Kafka transactions + transactional consumer. A message is consumed exactly once and the result is published exactly once, even with failures and retries.

**Q63: How does Kafka handle backward compatibility of schemas?**
Natively, Kafka doesn't — it's just bytes. In practice, use Schema Registry with Avro or Protobuf. Schema Registry enforces compatibility rules: backward (new schema can read old data), forward (old schema can read new data), full (both).

**Q64: What is Kafka Streams and how does it differ from consumer + producer?**
Kafka Streams is a Java library for stateful stream processing. It provides higher-level abstractions: KStream (event stream), KTable (changelog/materialized view), joins, windowing, aggregations. A consumer+producer is stateless I/O; Streams handles state internally with RocksDB.

**Q65: What is a KTable?**
A KTable represents a changelog stream interpreted as a key-value store. Each new record for a key updates (upserts) the table. Backed by a state store (RocksDB by default).

**Q66: What is a changelog topic?**
Kafka Streams stores state in local RocksDB and backs it up to a Kafka changelog topic. If the application restarts on a new machine, it replays the changelog to restore state.

**Q67: What is windowing in Kafka Streams?**
Grouping events into time windows for aggregation. Types: Tumbling (fixed non-overlapping), Hopping (fixed overlapping), Session (gap-based, variable size), Sliding (event-time based).

**Q68: What is Kafka Connect?**
A framework for streaming data between Kafka and external systems using connectors. Source connectors (DB/API → Kafka), Sink connectors (Kafka → DB/index/storage). No custom code needed for common integrations.

**Q69: What is MirrorMaker 2?**
Kafka's built-in tool for replicating topics across Kafka clusters (multi-region or disaster recovery). Uses Kafka Connect internally.

**Q70: What is the difference between at-least-once and exactly-once in consumers?**
At-least-once: commit offset AFTER processing. Duplicates possible if consumer crashes after processing but before commit. Exactly-once: use transactions to atomically process and commit offset, with output also in the same transaction.

**Q71: What is Kafka's storage model?**
Kafka stores data as **log segments** on disk. Each partition is a directory of segment files. Active segment is appended to. Old segments are deleted (or compacted) based on retention policy.

**Q72: What is the segment file structure?**
Each segment has three files: `.log` (actual messages), `.index` (offset → file position mapping), `.timeindex` (timestamp → offset mapping). Enables O(1) message lookup by offset.

**Q73: How does Kafka achieve sequential I/O?**
Producers always append to the end of the log. Consumers read sequentially. No random seeks needed. Sequential disk I/O is much faster than random I/O (even compared to SSDs).

**Q74: What is the page cache advantage Kafka uses?**
Kafka relies on OS page cache (RAM) instead of maintaining its own in-memory cache. Recent messages are typically in page cache, making consumer reads from page cache (fast RAM read) not disk.

**Q75: What is a controller broker?**
One broker in the cluster is elected controller. It monitors broker liveliness, manages partition leadership, reassigns partitions on broker failure. Controlled by ZooKeeper (or KRaft).

**Q76: What happens during a preferred leader election?**
Each partition has a "preferred leader" (first broker in the replica list). Over time, leadership may shift to other brokers due to failures. Preferred leader election reassigns leadership back to original brokers for load balancing. Triggered by `kafka-preferred-replica-election` tool.

**Q77: What is rack awareness?**
Kafka can be configured with broker rack IDs (`broker.rack`). When assigning replicas, Kafka ensures replicas are on different racks (different data centers/AZs). This protects against entire rack/AZ failure.

**Q78: What is the impact of increasing partition count?**
Pros: more parallelism, higher throughput. Cons: more file handles, more memory, longer leader election time on failure, longer rebalance time. Message ordering guarantees change (key's partition assignment changes).

**Q79: How does Kafka handle backpressure?**
Consumers pull at their own pace — natural backpressure. If consumer is slow, messages accumulate in Kafka (up to retention limit). Producer can be throttled with `buffer.memory` and `max.block.ms`.

**Q80: What is the `__transaction_state` topic?**
Internal Kafka topic that stores transaction coordinator state. Tracks active transactions, ensures transactional producers commit or abort. Compacted.

**Q81: Explain Kafka's Raft (KRaft) consensus mechanism.**
KRaft uses a Raft variant where one controller is elected leader among controller nodes. All metadata operations go through the leader. Followers replicate metadata log. On leader failure, new leader elected from followers. No ZooKeeper needed.

**Q82: What is cooperative rebalancing vs eager rebalancing?**
Eager (default): all consumers stop, rejoin, get new assignments. Full stop-the-world. Cooperative (incremental): only partitions that need to move are revoked and reassigned. Consumers not affected continue processing during rebalance.

**Q83: How do you implement a custom partitioner?**
Implement `org.apache.kafka.clients.producer.Partitioner`, override `partition()` method, register with `partitioner.class` config. Example use: route VIP users to dedicated partition.

**Q84: What is `interceptor.classes` in Kafka?**
Producer interceptors (`ProducerInterceptor`) intercept `send()` calls — add headers, log, modify records. Consumer interceptors (`ConsumerInterceptor`) intercept `poll()` results. Useful for cross-cutting concerns like tracing.

**Q85: What is Kafka's security model?**
Authentication: SSL/TLS (certificates), SASL (PLAIN, SCRAM, Kerberos, OAuth). Authorization: ACLs (`kafka-acls.sh`) or RBAC (Confluent). Encryption: TLS in transit, filesystem encryption at rest.

**Q86: What is a compacted topic's guarantee?**
For each key, at least the most recent value is retained forever. Old values for a key may be deleted during compaction. Null value (tombstone) signals deletion of a key.

**Q87: What is the difference between `poll` timeout and `max.poll.interval.ms`?**
`poll(Duration timeout)` timeout: how long to wait for records if none available. Returns empty if no records. `max.poll.interval.ms`: maximum time between consecutive `poll()` calls before consumer is considered dead.

**Q88: How does Kafka handle time — event time vs ingestion time vs processing time?**
Event time: when event occurred (in message payload). Ingestion time: when Kafka received the message (timestamp in record). Processing time: when consumer processes it. Kafka Streams supports event-time processing with watermarks.

**Q89: What is a high watermark?**
The highest offset that has been replicated to all ISR replicas. Consumers can only read up to the high watermark. This ensures consumers never read data that might be lost in a leader election.

**Q90: What is leader epoch?**
A monotonically increasing number assigned each time a new leader is elected for a partition. Used by followers to detect and recover from leader failovers without data inconsistency.

---

### Scenario-Based (91-100)

**Q91: A consumer is processing slowly and messages are piling up. How do you fix it?**
1. Increase consumer instances (scale out) — limited by partition count
2. Increase partition count (allows more consumers)
3. Optimize processing logic (reduce latency per message)
4. Increase `max.poll.records` if batch processing helps
5. Use async processing within consumer
6. Check if DB/external service is the bottleneck

**Q92: How do you prevent duplicate processing after consumer crash?**
Use idempotent processing: before processing, check if message was already handled (using correlation ID or event ID in DB). If processed, skip. This is the "idempotent consumer" pattern.

**Q93: Your service receives 10 million events/day. How do you size Kafka?**
```
10M events/day = ~115 events/second
Message size: 1KB average = 115 KB/s throughput
Add safety margin 3x = 345 KB/s

Partitions: 3-6 (based on consumer count)
Brokers: 3 minimum (replication-factor=3)
Disk: events/day * message size * retention * replication
     = 10M * 1KB * 7days * 3 = ~210 GB
Memory: 6GB per broker for page cache (rule of thumb)
```

**Q94: How do you ensure message ordering for a specific user's events?**
Use `userId` as the Kafka message key. All events with the same `userId` hash to the same partition. Kafka guarantees ordering within a partition.

**Q95: A message in Kafka is corrupt/unprocessable. How do you handle it?**
1. Retry N times (fixed or exponential backoff)
2. After max retries, send to Dead Letter Queue topic
3. Alert operations team
4. Fix bug and replay from DLQ
Never block the partition — skip bad messages to DLQ.

**Q96: Two services publish to the same topic. One publishes TypeA, another TypeB. How does the consumer handle both?**
Use `@KafkaHandler` with class-level `@KafkaListener` (your project does this). Spring reads `__TypeId__` header and routes to the correct handler method. Or use `@KafkaListener` on a method accepting `Object` and `instanceof` check.

**Q97: You need to replay all events from 3 days ago. How?**
```bash
# Reset consumer group offset to 3 days ago
kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --group my-group \
  --topic ticket-events \
  --reset-offsets \
  --to-datetime 2026-03-19T00:00:00.000 \
  --execute
# Then restart consumers — they'll read from that point
```
Requires retention period to still have those messages.

**Q98: How do you monitor Kafka in production?**
- **Consumer lag**: most important metric. Use Kafka built-in consumer groups describe, Burrow, or Prometheus exporter.
- **Broker metrics**: under-replicated partitions, leader election rate, request latency — via JMX.
- **Producer metrics**: record-send-rate, error-rate, batch-size-avg.
- **Tools**: Confluent Control Center, Kafka UI (your project), Grafana + kafka-exporter.

**Q99: How do you do a rolling upgrade of Kafka brokers without downtime?**
1. Update one broker at a time
2. Stop broker, upgrade, restart
3. Wait for partition reassignment and ISR to stabilize
4. Move to next broker
Requires replication-factor >= 2 so other brokers serve traffic while one is down.

**Q100: How would you design a Kafka-based system that can handle failures and ensure no data loss?**
```
1. Producer: acks=all, enable.idempotence=true, retries=Integer.MAX_VALUE
2. Topics: replication-factor=3, min.insync.replicas=2
3. Consumer: manual commit (MANUAL_IMMEDIATE), idempotent processing
4. Error handling: retry with backoff, DLQ after max retries
5. Monitoring: lag alerts, under-replicated partition alerts
6. Ops: 3+ brokers across racks/AZs, regular backup of configs
7. Schema: Schema Registry with compatibility enforced
8. Exactly-once: Kafka transactions if business requires it
```

---

## 17. Common Mistakes & Gotchas

### Mistake 1: Too Few Partitions
```
Problem: Created topic with 1 partition, have 10 consumers. 9 consumers are idle!
Fix: Create with enough partitions for expected consumer count. Can increase later.
```

### Mistake 2: Too Many Partitions
```
Problem: Created 1000 partitions per topic "just in case"
Result: Slow leader election (seconds instead of ms), high memory usage
Fix: Start conservative, increase based on actual throughput needs
```

### Mistake 3: Using `acks=0` in Production
```
Problem: Maximum throughput but messages lost silently
Fix: Use acks=1 (tolerate some loss) or acks=all (no loss)
```

### Mistake 4: Auto-commit with Slow Processing
```
Problem: enable.auto.commit=true, processing takes 10s
Kafka commits offset at 5s mark, consumer crashes at 8s
→ Message marked as processed but it wasn't!
Fix: Disable auto-commit, manually commit after processing
```

### Mistake 5: Consuming Without Consumer Group
```
Problem: Two consumers in same group both read same partition? No!
One reads, other sits idle. They share the work.
Fix: Understand consumer group semantics. Use different groups for independent consumers.
```

### Mistake 6: Not Handling Rebalance
```
Problem: Consumer crash during rebalance → uncommitted offsets reprocessed
Fix: Implement ConsumerRebalanceListener to commit before rebalance
```

### Mistake 7: Increasing Partition Count Breaking Ordering
```
Problem: Hash(key) % 3 → partition 0. After adding partition: Hash(key) % 4 → partition 1
Messages out of order!
Fix: Plan partitions upfront. If you must increase, handle the transition window carefully.
```

### Mistake 8: Not Setting `min.insync.replicas`
```
Problem: acks=all but min.insync.replicas=1
Broker accepts write with only 1 replica (the leader)
Leader dies → data lost (no replica has it)
Fix: min.insync.replicas=2 (for replication-factor=3)
```

### Mistake 9: `TRUSTED_PACKAGES="*"` in Production
```
Problem: Anyone can send any class name in __TypeId__ header
Deserialization of arbitrary classes = security risk
Fix: Set specific trusted packages: "com.yourcompany.events"
```

### Mistake 10: Large Messages in Kafka
```
Problem: Sending 50MB images as Kafka messages
Result: Memory pressure, slow brokers, slow consumers
Fix: Store large objects in S3/blob store, send reference (URL) in Kafka
```

### Mistake 11: Blocking in Consumer Handler
```
Problem: Consumer method calls a slow external API (30s timeout)
max.poll.interval.ms=300000 (5 min default)
30 consumers x 10 slow messages = 300s > 5min = REBALANCE STORM
Fix: Async processing, increase max.poll.interval.ms, or use separate thread pool
```

### Mistake 12: Ignoring Consumer Lag
```
Problem: Consumer lag growing quietly → consumers hours behind → stale data in production
Fix: Alert on consumer lag > threshold (e.g., > 10,000 messages or > 5 minutes behind)
```

---

## 18. Quick Revision Cheat Sheet

### Core Concepts in One Line

```
Kafka     = Distributed, durable, high-throughput message log
Topic     = Named category of messages (like a table)
Partition = Ordered slice of a topic (parallelism unit)
Broker    = Kafka server (one node in the cluster)
Cluster   = Group of brokers working together
Producer  = Writes messages to topics
Consumer  = Reads messages from topics
Consumer Group = Set of consumers sharing a topic's partitions
Offset    = Message position within a partition
ISR       = In-Sync Replicas (replicas caught up with leader)
Replication = Copies of partitions across brokers
ZooKeeper = Cluster coordinator (legacy, replaced by KRaft)
```

### Key Numbers to Remember

```
Default retention:          7 days (604800000 ms)
Default max message size:   1 MB
Default partition count:    1 (configure explicitly!)
Default replication:        1 (configure explicitly!)
Default auto.commit.interval: 5000 ms (5 seconds)
Default session.timeout:    45000 ms (45 seconds)
Default max.poll.interval:  300000 ms (5 minutes)
Default batch.size:         16384 bytes (16 KB)
Default fetch.min.bytes:    1 byte
Default linger.ms:          0 (send immediately)
Heartbeat every:            3000 ms (3 seconds)
Min recommended brokers:    3 (production)
Min recommended partitions: num_consumers (at minimum)
```

### Delivery Guarantee Summary

```
At-most-once:  acks=0 OR acks=1 without retry, auto-commit before processing
At-least-once: acks=all, retries>0, manual commit AFTER processing (YOUR PROJECT)
Exactly-once:  idempotence=true + Kafka transactions + read_committed consumer
```

### Producer Settings for Reliability

```java
acks=all
retries=Integer.MAX_VALUE (or 3+)
enable.idempotence=true
max.in.flight.requests.per.connection=5
```

### Consumer Settings for Reliability

```java
enable.auto.commit=false
auto.offset.reset=earliest (for new groups that need history)
max.poll.interval.ms > your processing time
// Use MANUAL_IMMEDIATE ack mode
// Implement idempotent processing logic
```

### Spring Annotations Quick Reference

```java
@EnableKafka              // Enable Kafka listeners (on @Configuration class)
@KafkaListener            // Mark method/class as consumer
@KafkaHandler             // Route by type within class-level @KafkaListener
@Payload                  // Bind parameter to message value
@Header(KafkaHeaders.X)  // Bind parameter to Kafka header

KafkaHeaders.RECEIVED_TOPIC     // Topic name
KafkaHeaders.RECEIVED_PARTITION // Partition number
KafkaHeaders.OFFSET             // Message offset
KafkaHeaders.RECEIVED_TIMESTAMP // Message timestamp
```

### Your Project's Consumer Groups

```
ticket-service-group    → reads payment-events (confirms/releases tickets)
payment-service-group   → reads ticket-events (processes payments)
notification-service-group → reads ticket-events, payment-events, user-events
audit-service-group     → reads ticket-events, payment-events, user-events
```

### Common CLI Commands

```bash
# List all topics
kafka-topics --bootstrap-server localhost:9092 --list

# Describe a topic (partitions, replicas, ISR)
kafka-topics --bootstrap-server localhost:9092 --describe --topic ticket-events

# Create a topic
kafka-topics --bootstrap-server localhost:9092 \
  --create --topic my-topic --partitions 3 --replication-factor 1

# Consume from beginning
kafka-console-consumer --bootstrap-server localhost:9092 \
  --topic ticket-events --from-beginning

# List consumer groups
kafka-consumer-groups --bootstrap-server localhost:9092 --list

# Check consumer lag
kafka-consumer-groups --bootstrap-server localhost:9092 \
  --describe --group ticket-service-group

# Reset offset to beginning
kafka-consumer-groups --bootstrap-server localhost:9092 \
  --group ticket-service-group --topic ticket-events \
  --reset-offsets --to-earliest --execute
```

### Interview Golden Rules

1. Always mention **trade-offs** (acks=all is safe but slower)
2. Talk about **ordering** — guaranteed per partition, not across
3. Mention **consumer groups** when asked about scaling
4. Bring up **ISR and min.insync.replicas** for durability
5. Know the difference between **at-least-once vs exactly-once**
6. Understand **when rebalancing happens** and its impact
7. Know **consumer lag** as the key operational metric
8. Mention **DLQ** for error handling in production
9. Know that Kafka is a **log, not a queue** — messages persist after reading
10. Correlate **partition count = maximum consumer parallelism**

---

*Last Updated: 2026-03-22*
*Project: sb-kafka-ticket — Event-Driven Ticket Booking System*
*Stack: Spring Boot + Apache Kafka + PostgreSQL + MongoDB + Angular*
