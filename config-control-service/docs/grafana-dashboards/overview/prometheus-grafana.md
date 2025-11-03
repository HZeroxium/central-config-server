Below is a practical, end-to-end guide to Grafana—geared for a Java Spring + Prometheus stack you already have running. I’ll cover the core concepts, how to build durable, reusable dashboards, how to wire alerting, how to organize at scale, and the pitfalls to avoid. I’ll sprinkle in ready-to-use PromQL and a bit of provisioning-as-code so you can standardize quickly.

---

# 1) What Grafana is (for a Spring + Prometheus setup)

Grafana is the UI and control plane where you:

* **Query** your Prometheus (and other) data sources.
* **Visualize** metrics using rich panels and dashboards.
* **Templatize** dashboards with **variables** for reuse.
* **Transform**/join data (without changing Prometheus) before visualizing.
* **Drill down** via dashboard/panel/data links and **Explore** for ad-hoc analysis.
* **Alert** using Grafana’s unified alerting (rules, contact points, routing).
  Core docs: data sources, panels, variables, alerting, and Explore. ([Grafana Labs][1])

> Why Grafana on top of Prometheus? Prometheus is your metrics **store** and query engine (PromQL). Grafana is the multi-data-source **UI**, templating, and alerting surface you and your team live in day-to-day. For long-term storage or multi-tenant scale you can later add Grafana Mimir via `remote_write`. ([Grafana Labs][2])

---

# 2) Core building blocks (fast overview)

* **Data sources**: Prometheus will be your default; add more later (Loki for logs, Tempo for traces, etc.). Grafana has built-in support for Prometheus. ([Grafana Labs][3])
* **Dashboards & panels**: A dashboard is a grid of **panels**. Choose a visualization per panel (Time series, Stat, Table, Heatmap, State timeline, etc.). ([Grafana Labs][4])
* **Variables (templating)**: Make one dashboard reusable across services, instances, envs, regions by parameterizing label filters (e.g., `$service`, `$instance`). ([Grafana Labs][5])
* **Transformations**: In-UI operations (join, group, calc, rename) to prepare the query result for display. Handy when you can’t or shouldn’t change PromQL. ([Grafana Labs][6])
* **Annotations**: Overlay events (deploys, incidents) on charts. Manual or from data sources. ([Grafana Labs][7])
* **Links & drilldowns**: Dashboard/panel/data links let you jump to “service detail” or to logs/traces with the same time/labels. ([Grafana Labs][8])
* **Explore**: Ad-hoc PromQL queries, side-by-side comparisons, and “follow the trail” workflows without editing dashboards. ([Grafana Labs][9])
* **Alerting**: Unified alerting = alert rules + contact points + notification policies (routing). All manageable via UI **or** provisioning files. ([Grafana Labs][10])
* **Roles & permissions**: Viewers, Editors, Admins; plus folder and dashboard-level permissions for governance. ([Grafana Labs][11])

---

# 3) Wire Grafana to Prometheus (one-time)

**Add data source (UI)**
Connections → **Add new connection** → Prometheus → enter your Prometheus URL → **Save & test**. Grafana ships with Prometheus support out of the box. ([Grafana Labs][3])

**Provisioning (code)** — recommended for repeatable environments

```yaml
# /etc/grafana/provisioning/datasources/prometheus.yaml
apiVersion: 1
datasources:
  - name: Prometheus
    type: prometheus
    access: proxy
    url: http://prometheus:9090
    isDefault: true
    jsonData:
      httpMethod: POST
```

Dashboards can also be provisioned from disk:

```yaml
# /etc/grafana/provisioning/dashboards/spring.yaml
apiVersion: 1
providers:
  - name: 'spring'
    orgId: 1
    folder: 'Spring'
    type: file
    disableDeletion: false
    allowUiUpdates: true
    options:
      path: /var/lib/grafana/dashboards/spring
      foldersFromFilesStructure: true
```

Provisioning docs overview. ([Grafana Labs][12])

---

# 4) Instrument Spring Boot correctly (Prometheus + Micrometer)

If you haven’t already:

**Gradle (Java 21 + Spring Boot 3.x)**

```gradle
dependencies {
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    runtimeOnly("io.micrometer:micrometer-registry-prometheus")
}
```

**application.yml**

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus
  endpoint:
    prometheus:
      enabled: true
  metrics:
    tags:
      application: orders-service
    distribution:
      percentiles-histogram:
        http.server.requests: true   # publish histogram buckets -> enables p90/p95/p99 via histogram_quantile
```

Note: Spring Boot 3.3.x manages Micrometer 1.13.x; Micrometer’s Prometheus registry moved to the Prometheus Java client 1.x with some breaking changes. If you manually override versions, ensure compatibility with your Boot line. ([GitHub][13])

**Custom metric example (Counter)**

```java
@RestController
@RequiredArgsConstructor
class OrdersController {
  private final MeterRegistry registry;
  private final Counter ordersCreated = Counter.builder("orders_created_total")
      .description("Total created orders")
      .register(registry);

  @PostMapping("/orders")
  public ResponseEntity<?> createOrder(@RequestBody CreateOrder req) {
    // ... business logic
    ordersCreated.increment();
    return ResponseEntity.accepted().build();
  }
}
```

---

# 5) Build a “golden signals” service dashboard (the right way)

The **Golden Signals** (Latency, Traffic, Errors, Saturation) is a great starting structure. Complement with the **RED method** (Rate, Errors, Duration) for HTTP services and the **USE method** at the infrastructure layer. ([YouTube][14])

## 5.1 Variables (templating) for reuse

Create variables to scope all panels:

* `$env` ← label values from `env`
* `$service` ← label values from `application` (Micrometer tag)
* `$instance` ← label values from `instance`

In Grafana: Dashboard settings → **Variables** → **Add variable**. For Prometheus, use `label_values()`:

* Name: `service`
  Type: Query
  Query: `label_values(http_server_requests_seconds_count, application)`
* Name: `instance`
  Type: Query
  Query: `label_values(http_server_requests_seconds_count{application="$service"}, instance)`

Templating docs. ([Grafana Labs][5])

## 5.2 Panels & PromQL (copy-paste ready)

**Traffic (RPS)**

```promql
sum by (method) (
  rate(http_server_requests_seconds_count{application="$service", instance=~"$instance"}[5m])
)
```

Visualize with **Time series**; legend `{{method}}`. ([Grafana Labs][4])

**Errors (error rate %)**

```promql
100 * sum(
  rate(http_server_requests_seconds_count{
    application="$service", status=~"5..|4..", instance=~"$instance"
  }[5m])
)
/
sum(
  rate(http_server_requests_seconds_count{application="$service", instance=~"$instance"}[5m])
)
```

Use a **Stat** panel with unit `%`.

**Latency (p95)** using Micrometer’s histogram buckets:

```promql
histogram_quantile(
  0.95,
  sum by (le) (
    rate(http_server_requests_seconds_bucket{
      application="$service", instance=~"$instance"
    }[5m])
  )
)
```

`histogram_quantile` and `rate()` are Prometheus functions designed for this. ([Cisco DevNet][15])

**Saturation (JVM heap)**

```promql
100 *
(sum(jvm_memory_used_bytes{application="$service", area="heap"}) by ()
 /
 sum(jvm_memory_max_bytes{application="$service", area="heap"}) by ())
```

**In-flight requests (gauge)** (if exposed by your HTTP server/middleware):

```promql
sum by (uri, method) (
  http_server_active_requests{application="$service", instance=~"$instance"}
)
```

> Tip: lock high-cost expressions into **Prometheus recording rules** (e.g., `:svc:http_req_rate5m`) and query the rule from Grafana for both speed and consistency. ([GitHub][16])

## 5.3 Recommended visualizations

* **Time series** for RPS/Latency/Heap.
* **Stat** for SLIs (Error rate %, p95 latency).
* **Table** for per-endpoint drilldowns (method/uri, top N).
* **State timeline** for status/availability over time.
  Panel selection reference. ([Grafana Labs][4])

## 5.4 Page structure (one dashboard per service)

* **Row 1**: Service title, `$env/$service/$instance` variable bar, big KPIs (Stat): error %, p95, RPS.
* **Row 2**: Time series (RPS + error rate overlay), p95/p99 latency series, heap/GC metrics.
* **Row 3**: “Endpoints top-N” (Table) with data link to per-endpoint drilldowns.
* **Row 4**: Dependencies (DB calls, Kafka lag) if instrumented.

---

# 6) Cross-service navigation and drilldowns

* **Dashboard links**: top-level shortcuts (e.g., “Overview → Service → Instance”). Preserve time range and variables. ([Grafana Labs][8])
* **Panel links**: from a Stat/Time series to a deeper dashboard or docs (Runbook). ([Grafana Labs][8])
* **Data links**: from a **specific series/row** (e.g., URI) to logs (Loki) or traces (Tempo). ([Grafana Labs][8])
* **Explore**: open the same query in **Explore** to iterate quickly; split-view to compare staging vs prod. ([Grafana Labs][9])

---

# 7) Correlate metrics ↔ logs ↔ traces (exemplars & links)

If you add **Tempo** and **Loki**, you can:

* Show **exemplars** on Time series (dots linked to traces) when metrics and tracing are wired accordingly.
* Link via **data links** from metrics panels to logs/traces for the same labels/time window.
  Docs: exemplars and trace-metrics correlation. ([WorkingMouse][17])

---

# 8) Annotations (deploy markers & incidents)

Add annotations for deploys, feature flags, or incidents so your latency/error spikes are explained **on the charts**. Create manually, via APIs, or from a data source query. ([Grafana Labs][7])

---

# 9) Transformations (join, group, calculate) when PromQL alone is clumsy

Examples:

* **Merge** two query results (e.g., join an SLO budget table with current burn rate).
* **Group/aggregate** after pulling raw series (sum/mean/min/max at display time).
* **Rename/organize fields** for better legends and table columns.
  Transformation docs. ([Grafana Labs][6])

---

# 10) Alerting: rules, contact points, routing, and provisioning

**How you should think about it:**

1. Create **alert rules** that evaluate PromQL conditions (e.g., error rate > SLO for 5m).
2. Define **contact points** (Slack, Email, Webhook).
3. Build **notification policies** (routing tree by env/team/severity).
4. Optionally provision all of the above via files (YAML) checked into Git.

**Docs & concepts:** Start with unified alerting concepts, contact points, notification policies. ([Grafana Labs][10])

**Provisioning alerting (YAML)** — good for Kubernetes/Helm & CI

```yaml
# /etc/grafana/provisioning/alerting/contact-points.yaml
apiVersion: 1
contactPoints:
  - orgId: 1
    name: slack-platform
    receivers:
      - uid: slack-platform
        type: slack
        settings:
          url: ${SLACK_WEBHOOK}
```

```yaml
# /etc/grafana/provisioning/alerting/policies.yaml
apiVersion: 1
policies:
  - orgId: 1
    receiver: slack-platform   # default
    routes:
      - receiver: slack-payments
        object_matchers:
          - ["team", "=", "payments"]
      - receiver: email-sre
        object_matchers:
          - ["severity", "=", "critical"]
```

```yaml
# /etc/grafana/provisioning/alerting/rules.yaml
apiVersion: 1
groups:
  - orgId: 1
    name: svc-latency
    folder: "Alerts/Spring"
    interval: 1m
    rules:
      - uid: svc_p95_high
        title: "[${env}] ${service} p95 latency high"
        condition: A
        data:
          - refId: A
            queryType: "classic_condition"
            datasourceUid: PROM
            model:
              expr: histogram_quantile(0.95, sum by (le) (rate(http_server_requests_seconds_bucket{application="$service"}[5m])))
              interval: ""
        annotations:
          runbook: "https://runbooks.internal/${service}/latency"
        labels:
          team: "payments"
          severity: "warning"
```

Official provisioning guide and example repo. ([Grafana Labs][18])

> Note: File provisioning targets Grafana’s built-in Alertmanager. External Alertmanager can be used too; check doc notes and edition caveats. ([Grafana Labs][19])

---

# 11) Organize at scale: folders, permissions, naming

**Folder layout suggestion**

* `/0-Overview` (global health, business KPIs)
* `/1-Apps/<team>/<service>` (service dashboards)
* `/2-Infra/<cluster|namespace>` (infra)
* `/3-SLOs/<team>` (SLO dashboards + burn alerts)
* `/Runbooks` (markdown panels linking to wiki)

**Permissions**

* Viewers: org-wide read.
* Editors: per-folder write (team ownership).
* Admins: org admin + server admin (ops).
  Roles/permissions docs. ([Grafana Labs][11])

**Naming & tags**

* Prefix with env: `[prod] Orders – Golden Signals`
* Tag dashboards: `team:payments`, `type:golden`, `env:prod` (helps dashboard links and search). ([Grafana Labs][8])

---

# 12) Reuse at scale: variables, repeated panels, library panels, dashboard-as-code

* **Variables**: `$env`, `$service`, `$instance`, `$namespace`—apply everywhere. ([Grafana Labs][5])
* **Repeated rows/panels**: Same visualization repeated per value of a variable (e.g., per instance).
* **Library panels**: Define common panels once and reuse across dashboards. If a panel changes, it propagates. ([Grafana Labs][20])
* **Provision dashboards** from JSON in Git, or adopt tools like Terraform provider / Jsonnet (`grafonnet`) as you mature. ([Grafana Labs][12])

---

# 13) Performance & correctness: how to avoid slow dashboards

**PromQL habits**

* Prefer **recording rules** for heavy expressions (quantiles, joins) and for business SLIs. ([GitHub][16])
* Avoid expensive regex on high-cardinality labels in every panel.
* Bound ranges: use `[5m]` not `[1h]` unless needed; keep step sizes reasonable.
* For percentiles use **histogram buckets** + `histogram_quantile()` (not `summary` if you need server-side aggregation). ([Cisco DevNet][15])

**Grafana panel settings**

* Set an appropriate **Min interval** (e.g., `> 15s`) to limit data points per query.
* Use **downsampling**/recording rules for long time ranges.
* Turn on **Reduce** transformations (max/avg/last) for expensive tables. ([Grafana Labs][6])

**Cardinality control**

* Keep `uri` label values bounded (consider templating or using normalized routes instead of raw paths with IDs).
* Use `topk()` sparingly.

---

# 14) SLO dashboards (recommended next)

Define **SLI metrics** and **error budget** (e.g., availability over 30d). In Prometheus, set recording rules for:

* `sli_availability` (good/valid requests ratio)
* `error_budget_burn_rate_1h` / `6h`
  Then build an SLO dashboard that shows burn rate + budget remaining and pages only on sustained burn. The golden signals and SRE model come from Google’s SRE materials; RED/USE methods complement them. ([YouTube][14])

---

# 15) From one service to many: Overview → Service → Instance

**Pattern**

1. **Overview dashboard**

   * Panels aggregated by `application`, `namespace`, `cluster`.
   * Click into **Service dashboard** via dashboard link.

2. **Service dashboard** (templated by `$service`)

   * Golden signals + dependency metrics (DB, Kafka).
   * Click into **Instance dashboard** via data/panel link.

3. **Instance dashboard**

   * JVM heap, GC, CPU, thread count, in-flight, container CPU/mem if available.

Use **dashboard links** (preserving time & variables) to travel across the stack. ([Grafana Labs][8])

---

# 16) Extras you’ll use sooner than you think

* **Playlists**: rotate dashboards on a TV in NOC/SRE rooms. ([Grafana Labs][21])
* **Snapshots**: share a read-only snapshot with colleagues or attach to incidents. ([thenewstack.io][22])
* **Grafana Explore** split view: compare prod vs staging using the same query side-by-side. ([Grafana Labs][9])

---

# 17) Example: service dashboard JSON as code (high-level workflow)

1. Design the dashboard in UI.
2. **Export JSON** and commit to Git under `grafana/dashboards/spring/orders-service.json`.
3. Provision with the `providers` YAML shown earlier. ([Grafana Labs][12])
4. For reusability, extract common KPIs into **library panels** and share them across services. ([Grafana Labs][20])
5. For larger orgs, adopt **Terraform** for contact points/policies and backing up dashboards. ([registry.terraform.io][23])

---

# 18) Case study: “Orders Service” done right

**Goal**: The team wants one canonical dashboard for every Spring Boot service with identical layout and behavior.

**Steps**

1. Create `service_base.json` with:

   * Variables: `$env`, `$service`, `$instance`.
   * Rows: KPIs, Traffic/Errors/Latency time series, JVM, endpoints table.
2. Convert key panels to **library panels** (KPI: p95, Errors, RPS). ([Grafana Labs][20])
3. Import for each service by **cloning** and adjusting defaults (e.g., `service=orders-service`).
4. Add panel/data links:

   * To **logs** (Loki) with `{application="$service", instance="$instance"}` and timeframe from variables.
   * To **traces** (Tempo) for p95 panels using exemplars. ([Dynatrace][24])
5. Add **annotations**: CD pipeline posts a deploy annotation each rollout. ([Grafana Labs][7])
6. Setup **alert rules**:

   * `svc:errors:rate5m > 2% for 10m` → Slack `team-orders`
   * `svc:p95_latency_seconds > 0.8 for 10m` (warning), `> 1.5 for 10m` (critical)
     Route by team/severity with notification policies. Provision via YAML. ([Grafana Labs][25])

---

# 19) Case study: Org-wide Overview dashboard

**Page 1: “Platform Overview”**

* Grid of **Stat** KPIs: total error rate, p95 latency, p50 RPS.
* Top 5 services by error rate (Table with data link → service dashboard).
* Infrastructure row: Node CPU, memory, disk (if you also scrape node_exporter/cAdvisor).
* A row of **SLO burn rates** across critical services.

Add **dashboard links** to discovery pages and team folders. ([Grafana Labs][8])

---

# 20) Case study: SLO burn & alerting

* Create recording rules for 30-day window SLIs and **multi-window multi-burn** (e.g., 1h and 6h). Use alert rules with labels `team`, `service`, `severity`.
* **Notification policies** route `severity=critical` to on-call, `warning` to Slack only.
* Contact points defined once per team; all provisioned. ([Grafana Labs][25])

---

# 21) Governance & collaboration

* **RBAC**: Editors per folder; Viewers org-wide; Admins restricted. ([Grafana Labs][11])
* **Code review**: dashboards and alert files are code—PR review for changes.
* **Naming conventions**: `[env] Service – Golden Signals`; labels `team`, `env`, `service`.
* **Runbooks**: panel links to runbooks; annotations for deploys/incidents. ([Grafana Labs][8])

---

# 22) Query and design best practices (quick checklist)

**PromQL**

* Use `rate()`/`irate()` on counters, **not** raw counters. ([Baeldung on Kotlin][26])
* Quantiles from histograms: `histogram_quantile()` on `_bucket`. ([Cisco DevNet][15])
* Precompute with **recording rules** for heavy expressions and long lookbacks. ([GitHub][16])

**Panels**

* Pick visualization based on the question: **Time series** for trends, **Stat** for SLOs, **Table** for top-N, **State timeline** for status. ([Grafana Labs][4])
* Always set **units** (seconds, %, bytes). Consistency matters.
* Use **thresholds** (green/yellow/red) on Stat/Gauge.

**UX**

* Keep a persistent, ordered **variable bar**: env → service → instance.
* Provide **drilldowns**: data link from endpoint rows to logs/traces. ([Grafana Labs][8])
* Add **annotations** for deploys. ([Grafana Labs][7])

**Org**

* Reuse via **library panels**; version in Git via provisioning. ([Grafana Labs][20])
* Use **Playlists** on wallboards; **Snapshots** for incident sharing. ([Grafana Labs][21])

---

# 23) Provision everything (so it’s reproducible)

* **Datasources/dashboards** via provisioning files (YAML + JSON). ([Grafana Labs][12])
* **Alerting** (rules, contact points, policies) via YAML or Terraform resources. ([Grafana Labs][18])
* Put them under `observability/grafana/` in your mono-repo; apply via Helm/Compose.

---

# 24) Long-term metrics & scale (optional but useful context)

If you later need longer retention, multi-tenancy, or global query fan-out, you can **remote_write** from Prometheus to **Grafana Mimir** (OSS). This keeps local Prometheus responsive while storing long-term data in object storage via Mimir. Be aware remote_write increases Prometheus memory; tune per Prometheus guidance. ([Grafana Labs][2])

---

# 25) Minimal “starter pack” for your current setup

**(A) Import a prebuilt Spring dashboard**
Try a community/official Spring dashboard as a seed (e.g., “Spring Boot Observability”) and adapt it to your label schema. ([Grafana Labs][27])

**(B) Create a service-template dashboard**

* Variables: `$env`, `$service`, `$instance`.
* Panels: RPS, Error %, p95 latency, Heap, GC, Top N endpoints.
* Links: To logs/traces/runbooks. ([Grafana Labs][8])

**(C) Add alerting**

* `svc:errors:rate5m` and `svc:p95_latency` with routes to team contact points. ([Grafana Labs][25])

**(D) Provision**

* Datasource YAML, Dashboard provider YAML, Alerting YAML. ([Grafana Labs][12])

**(E) Governance**

* Folder per team, Viewer/Editor roles set; Library panels for KPIs. ([Grafana Labs][11])

---

# 26) Example: “Orders — Golden Signals” (panel recipes)

**A. Error rate (%) — Stat**

```promql
100 * sum(rate(http_server_requests_seconds_count{
  application="$service", status=~"5..|4..", instance=~"$instance"
}[5m]))
/
sum(rate(http_server_requests_seconds_count{application="$service", instance=~"$instance"}[5m]))
```

* Unit `%`, thresholds: 1 (green), 2 (yellow), 5 (red).
* Data link to logs: `/explore?datasource=Loki&query={application="$service",instance="$instance"}&from=${__from}&to=${__to}`

**B. Latency p95 — Time series**

```promql
histogram_quantile(
  0.95,
  sum by (le) (
    rate(http_server_requests_seconds_bucket{application="$service", instance=~"$instance"}[5m])
  )
)
```

* Unit `s`, thresholds aligned to SLO. ([Cisco DevNet][15])

**C. RPS by method — Time series**

```promql
sum by (method)(
  rate(http_server_requests_seconds_count{application="$service", instance=~"$instance"}[5m])
)
```

**D. Top endpoints — Table**

```promql
topk(10, sum by (uri)(
  rate(http_server_requests_seconds_count{application="$service"}[5m])
))
```

(Prefer normalized route labels if available to avoid unbounded cardinality.)

**E. JVM heap — Time series**

```promql
sum(jvm_memory_used_bytes{application="$service", area="heap"})
/
sum(jvm_memory_max_bytes{application="$service", area="heap"})
```

---

# 27) FAQs & common pitfalls

**Q: My panels feel slow.**

* Add **recording rules** for heavy expressions; reduce lookbacks; set panel **Min interval**. ([GitHub][16])

**Q: We keep duplicating the same KPI panel across dashboards.**

* Convert to a **library panel** and reuse it (one source of truth). ([Grafana Labs][20])

**Q: How do we keep dashboards consistent across envs?**

* Use the same base dashboard + variables (`$env`) and **provision** JSON per env. ([Grafana Labs][12])

**Q: Can we manage alerting as code?**

* Yes—**file provisioning** for contact points/policies/rules (YAML) or **Terraform**. ([Grafana Labs][18])

**Q: How to quickly debug without touching dashboards?**

* Use **Explore** for ad-hoc analysis; compare two envs in split view. ([Grafana Labs][9])

---

# 28) Next steps (actionable)

1. **Lock in datasource & folder structure** with provisioning files. ([Grafana Labs][12])
2. **Create a “service template” dashboard** with variables and library panels. ([Grafana Labs][20])
3. **Add alerting**: define 2–3 high-value rules (errors, latency, saturation) and route by team. ([Grafana Labs][25])
4. **Establish SLOs** for your top services; create burn-rate alerts. (Grounded in Golden Signals & SRE.) ([YouTube][14])
5. **Wire logs/traces** and enable exemplars to accelerate root cause. ([WorkingMouse][17])
6. **Govern** with roles/permissions; PR review for dashboard/alert JSON. ([Grafana Labs][11])

---

## Appendix: More references you’ll find useful

* **Getting started with Grafana + Prometheus** (step-by-step walkthrough). ([Grafana Labs][28])
* **Prometheus in Grafana** (built-in). ([Grafana Labs][3])
* **PromQL functions** you’ll use daily (`rate`, `histogram_quantile`). ([Baeldung on Kotlin][26])
* **Explore** for ad-hoc & split-view. ([Grafana Labs][9])
* **Dashboard links** & **data links** for drilldowns. ([Grafana Labs][8])
* **Annotations** for deploys/incidents. ([Grafana Labs][7])
* **Alerting concepts & provisioning**. ([Grafana Labs][10])
* **Variables (templating)**—make everything reusable. ([Grafana Labs][5])
* **Visualizations** reference. ([Grafana Labs][4])
* **Library panels**—centralize reusable KPIs. ([Grafana Labs][20])
* **Roles & permissions**—governance at scale. ([Grafana Labs][11])
* **Prebuilt Spring Boot dashboard idea** to bootstrap. ([Grafana Labs][27])
* **Exemplars and traces**—close the metrics ↔ traces loop. ([WorkingMouse][17])
* **Remote_write to Mimir** for long-term scale. ([Grafana Labs][2])

---

If you want, I can turn the “service template” dashboard above into a ready-to-import JSON (with variables and library-panel stubs), plus the provisioning YAMLs for datasources and alerting—so you can drop them into your repo and be running in minutes.

[1]: https://grafana.com/docs/grafana/latest/datasources/?utm_source=chatgpt.com "Data sources | Grafana documentation"
[2]: https://grafana.com/docs/mimir/latest/introduction/?utm_source=chatgpt.com "Introduction to Grafana Mimir"
[3]: https://grafana.com/docs/grafana/latest/datasources/prometheus/configure/?utm_source=chatgpt.com "Configure the Prometheus data source"
[4]: https://grafana.com/docs/grafana/latest/panels-visualizations/visualizations/?utm_source=chatgpt.com "Visualizations | Grafana documentation"
[5]: https://grafana.com/docs/grafana/latest/dashboards/build-dashboards/annotate-visualizations/?utm_source=chatgpt.com "Annotate visualizations | Grafana documentation"
[6]: https://grafana.com/grafana/dashboards/19004-spring-boot-statistics/?utm_source=chatgpt.com "Spring Boot 3.x Statistics"
[7]: https://grafana.com/grafana/dashboards/17175-spring-boot-observability/?utm_source=chatgpt.com "Spring Boot Observability | Grafana Labs"
[8]: https://grafana.com/docs/grafana/latest/dashboards/build-dashboards/manage-dashboard-links/ "Manage dashboard links | Grafana documentation
"
[9]: https://grafana.com/docs/grafana/latest/explore/?utm_source=chatgpt.com "Explore | Grafana documentation"
[10]: https://grafana.com/docs/grafana/latest/panels-visualizations/visualizations/annotations/?utm_source=chatgpt.com "Annotations list | Grafana documentation"
[11]: https://grafana.com/docs/grafana/latest/administration/roles-and-permissions/ "Roles and permissions | Grafana documentation
"
[12]: https://grafana.com/docs/grafana/latest/administration/provisioning/?utm_source=chatgpt.com "Provision Grafana | Grafana documentation"
[13]: https://github.com/micrometer-metrics/micrometer/wiki/1.13-Migration-Guide "1.13 Migration Guide · micrometer-metrics/micrometer Wiki · GitHub"
[14]: https://www.youtube.com/watch?v=TJLpYXbnfQ4&utm_source=chatgpt.com "The RED Method: How To Instrument Your Services [B] - Tom ..."
[15]: https://developer.cisco.com/articles/what-are-the-golden-signals/what-are-the-golden-signals-that-sre-teams-use-to-detect-issues/?utm_source=chatgpt.com "What are the 'Golden Signals' that SRE teams use to detect ..."
[16]: https://github.com/micrometer-metrics/micrometer/wiki/1.13-Migration-Guide?utm_source=chatgpt.com "1.13 Migration Guide · micrometer-metrics ..."
[17]: https://www.workingmouse.com.au/blogs/the-four-golden-signals-of-monitoring/?utm_source=chatgpt.com "The Four Golden Signals Of Monitoring"
[18]: https://grafana.com/docs/grafana/latest/alerting/set-up/provision-alerting-resources/file-provisioning/?utm_source=chatgpt.com "Use configuration files to provision alerting resources"
[19]: https://grafana.com/docs/grafana/latest/alerting/configure-notifications/create-notification-policy/?utm_source=chatgpt.com "Configure notification policies | Grafana documentation"
[20]: https://grafana.com/docs/grafana/latest/panels-visualizations/query-transform-data/transform-data/?utm_source=chatgpt.com "Transform data | Grafana documentation"
[21]: https://grafana.com/grafana/dashboards/21308-http/?utm_source=chatgpt.com "Spring Boot Http (3.x)"
[22]: https://thenewstack.io/monitoring-microservices-red-method/?utm_source=chatgpt.com "The RED Method: A New Approach to Monitoring ..."
[23]: https://registry.terraform.io/providers/grafana/grafana/latest/docs/resources/notification_policy?utm_source=chatgpt.com "grafana_notification_policy | Resources | grafana/grafana"
[24]: https://www.dynatrace.com/knowledge-base/golden-signals/?utm_source=chatgpt.com "What are golden signals?"
[25]: https://grafana.com/docs/grafana/latest/alerting/configure-notifications/manage-contact-points/?utm_source=chatgpt.com "Configure contact points | Grafana documentation"
[26]: https://www.baeldung.com/spring-boot-actuators?utm_source=chatgpt.com "Spring Boot Actuator"
[27]: https://grafana.com/grafana/dashboards/17175-spring-boot-observability/ "Spring Boot Observability | Grafana Labs
"
[28]: https://grafana.com/docs/grafana/latest/getting-started/get-started-grafana-prometheus/?utm_source=chatgpt.com "Get started with Grafana and Prometheus"
