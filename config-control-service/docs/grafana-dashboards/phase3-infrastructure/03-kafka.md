# Kafka Dashboard (USE Method)

This dashboard monitors Kafka broker performance using the USE method: Utilization, Saturation, and Errors.

## 📊 Overview

### Purpose
Monitor Kafka broker health, CPU/memory usage, disk usage, consumer lag, and errors using the USE method.

### Target Audience
- **SRE teams** - Kafka performance and capacity monitoring
- **Operations teams** - Kafka troubleshooting and optimization
- **Data engineers** - Message throughput and lag monitoring

### USE Method
- **Utilization**: Broker CPU %, Memory %, Disk usage %
- **Saturation**: Consumer lag, queue size, partition count
- **Errors**: Producer errors, consumer errors, topic errors

### Prerequisites
- Kafka running and accessible
- Kafka JMX exporter or Kafka exporter installed
- Prometheus scraping Kafka metrics
- Understanding of PromQL (see [PromQL Basics](../../03-promql-basics.md))

## 🚀 Quick Start (Fast Track)

### Step 1: Create Dashboard from Documentation

1. Follow the **Step-by-Step Tutorial** section below
2. Build USE method sections (Utilization, Saturation, Errors)
3. Adjust metric names based on available Kafka metrics

---

## 📚 Step-by-Step Tutorial (Build from Scratch)

### Step 1: Create New Dashboard

1. In Grafana, click **Dashboards** → **New Dashboard**
2. Set up variables if needed (environment, kafka_broker)

### Step 2: Create Row 1 - Utilization

#### Panel 1: Broker CPU Usage %

1. Click **Add visualization**
2. **Query**:
   ```promql
   kafka_server_broker_cpu_usage_percent{broker=~"$kafka_broker"}
   ```
   Or if using node exporter:
   ```promql
   100 - (avg(rate(node_cpu_seconds_total{mode="idle",instance=~".*kafka.*"}[5m])) * 100)
   ```
3. **Visualization**: `Stat`
4. **Title**: `Kafka Broker CPU Usage %`
5. **Unit**: `percent (0-100)`
6. **Thresholds**: Green (0-70%), Yellow (70-90%), Red (90-100%)
7. Click **Apply**

#### Panel 2: Broker Memory Usage %

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   100 * (
     kafka_server_broker_memory_used_bytes{broker=~"$kafka_broker"}
     /
     kafka_server_broker_memory_max_bytes{broker=~"$kafka_broker"}
   )
   ```
3. **Visualization**: `Stat`
4. **Title**: `Kafka Broker Memory Usage %`
5. **Unit**: `percent (0-100)`
6. **Thresholds**: Green (0-70%), Yellow (70-90%), Red (90-100%)
7. Click **Apply**

#### Panel 3: Disk Usage %

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   100 * (
     kafka_server_log_dir_size_bytes{broker=~"$kafka_broker"}
     /
     node_filesystem_size_bytes{mountpoint="/var/lib/kafka"}
   )
   ```
3. **Visualization**: `Stat` or `Time series`
4. **Title**: `Kafka Disk Usage %`
5. **Unit**: `percent (0-100)`
6. **Thresholds**: Green (0-70%), Yellow (70-90%), Red (90-100%)
7. Click **Apply**

### Step 3: Create Row 2 - Saturation

#### Panel 1: Consumer Lag

1. **Add row** → `Saturation`
2. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   sum(kafka_consumer_lag_sum{broker=~"$kafka_broker"}) by (topic, partition)
   ```
   Or if using Spring Kafka metrics:
   ```promql
   sum(spring_kafka_consumer_lag_sum) by (topic, partition)
   ```
3. **Visualization**: `Time series`
4. **Title**: `Consumer Lag`
5. **Legend**: `{{topic}} - partition {{partition}}`
6. **Unit**: `messages`
7. **Thresholds**: Y-axis at 1000 (warning), 10000 (critical)
8. Click **Apply**

#### Panel 2: Total Consumer Lag

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   sum(kafka_consumer_lag_sum{broker=~"$kafka_broker"})
   ```
3. **Visualization**: `Stat`
4. **Title**: `Total Consumer Lag`
5. **Unit**: `messages`
6. **Thresholds**: Green (0-1000), Yellow (1000-10000), Red (10000-100000)
7. Click **Apply**

#### Panel 3: Queue Size

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   sum(kafka_server_producer_queue_size{broker=~"$kafka_broker"}) by (topic)
   ```
3. **Visualization**: `Time series`
4. **Title**: `Producer Queue Size`
5. **Legend**: `{{topic}}`
6. **Unit**: `messages`
7. Click **Apply**

### Step 4: Create Row 3 - Errors

#### Panel 1: Producer Errors

1. **Add row** → `Errors`
2. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   sum(rate(kafka_producer_errors_total{broker=~"$kafka_broker"}[5m])) by (error_type)
   ```
   Or if using Spring Kafka:
   ```promql
   sum(rate(spring_kafka_producer_errors_total[5m])) by (topic, error_type)
   ```
3. **Visualization**: `Time series`
4. **Title**: `Producer Errors Rate`
5. **Legend**: `{{error_type}}` or `{{topic}} - {{error_type}}`
6. **Unit**: `errors/sec`
7. Click **Apply**

#### Panel 2: Consumer Errors

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   sum(rate(kafka_consumer_errors_total{broker=~"$kafka_broker"}[5m])) by (error_type)
   ```
   Or:
   ```promql
   sum(rate(spring_kafka_consumer_errors_total[5m])) by (topic, error_type)
   ```
3. **Visualization**: `Time series`
4. **Title**: `Consumer Errors Rate`
5. **Legend**: `{{error_type}}`
6. **Unit**: `errors/sec`
7. Click **Apply**

#### Panel 3: Topic Errors

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   sum(rate(kafka_topic_errors_total{broker=~"$kafka_broker"}[5m])) by (topic, error_type)
   ```
3. **Visualization**: `Time series` or `Table`
4. **Title**: `Topic Errors Rate`
5. **Legend**: `{{topic}} - {{error_type}}`
6. **Unit**: `errors/sec`
7. Click **Apply**

### Step 5: Create Row 4 - Performance

#### Panel 1: Message Publish Rate

1. **Add row** → `Performance`
2. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   sum(rate(kafka_server_broker_topic_messages_in_per_sec{broker=~"$kafka_broker"}[5m])) by (topic)
   ```
   Or:
   ```promql
   sum(rate(spring_kafka_producer_records_total[5m])) by (topic)
   ```
3. **Visualization**: `Time series`
4. **Title**: `Message Publish Rate`
5. **Legend**: `{{topic}}`
6. **Unit**: `messages/sec`
7. Click **Apply**

#### Panel 2: Message Consumption Rate

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   sum(rate(kafka_server_broker_topic_bytes_in_per_sec{broker=~"$kafka_broker"}[5m])) by (topic)
   ```
   Or:
   ```promql
   sum(rate(spring_kafka_consumer_records_total[5m])) by (topic)
   ```
3. **Visualization**: `Time series`
4. **Title**: `Message Consumption Rate`
5. **Legend**: `{{topic}}`
6. **Unit**: `messages/sec`
7. Click **Apply**

#### Panel 3: Throughput (Bytes/sec)

1. **Add panel** → **Add visualization**
2. **Query**:
   ```promql
   sum(rate(kafka_server_broker_topic_bytes_in_per_sec{broker=~"$kafka_broker"}[5m])) by (topic)
   ```
3. **Visualization**: `Time series`
4. **Title**: `Kafka Throughput (Bytes/sec)`
5. **Legend**: `{{topic}}`
6. **Unit**: `bytes/sec`
7. Click **Apply**

### Step 6: Save Dashboard

1. Click **Save dashboard**
2. **Name**: `Kafka - USE Method`
3. **Folder**: `2-Infrastructure`
4. **Tags**: `kafka`, `infrastructure`, `use-method`
5. Click **Save**

---

## 🔬 PromQL Deep Dive (Advanced)

### Kafka Metrics

Kafka metrics vary by exporter:
- **Kafka JMX Exporter**: `kafka_server_broker_*`, `kafka_consumer_*`, `kafka_producer_*`
- **Spring Kafka**: `spring_kafka_*`
- **Kafka Exporter**: `kafka_*`

Query available metrics:
```promql
{__name__=~".*kafka.*"}
```

### Consumer Lag Calculation

Consumer lag is typically:
```promql
kafka_consumer_lag_sum{topic="<topic>", partition="<partition>"}
```

High lag indicates consumers can't keep up with producers.

---

## ✅ Best Practices

### Kafka Monitoring

1. **Monitor consumer lag** - High lag indicates processing issues
2. **Watch disk usage** - Kafka stores all messages on disk
3. **Track producer/consumer errors** - Errors indicate connectivity or configuration issues
4. **Monitor throughput** - Ensure adequate capacity

---

## 🐛 Troubleshooting

### "No data" for Kafka metrics

**Check**:
1. Kafka exporter is installed and running
2. JMX is enabled in Kafka
3. Prometheus is scraping Kafka metrics

**Debug**:
```promql
{__name__=~".*kafka.*"}
```

### High consumer lag

**Possible causes**:
1. Consumers too slow
2. Not enough consumer instances
3. Consumer errors

**Solutions**:
1. Increase consumer instances
2. Optimize consumer processing
3. Check consumer errors

---

## 📚 References

### Related Dashboards
- [Config Control Service - Infrastructure Dependencies](../phase2-service-deep-dive/02-config-control-infrastructure-dependencies.md)

### External Documentation
- [Kafka Monitoring](https://kafka.apache.org/documentation/#monitoring)
- [Kafka JMX Metrics](https://kafka.apache.org/documentation/#remote_jmx)

---

**Next**: [Consul Dashboard](04-consul.md)  
**Previous**: [MongoDB Dashboard](02-mongodb.md)

