# Dispatch Load Balancer

Spring Boot REST API that assigns delivery orders to a fleet of vehicles. It respects vehicle capacity, serves **HIGH → MEDIUM → LOW** priority orders first, keeps travel distance low, and measures distance with the **Haversine formula**.

| | |
|---|---|
| Live API | `https://<your-app>.onrender.com`  *(fill in after deploying)* |
| Swagger UI | `https://<your-app>.onrender.com/swagger-ui/index.html` |
| OpenAPI JSON | `/v3/api-docs` |
| Loom demo | `<paste link>` |

---

## 1. How to run

**Requirements:** Java 17+, Maven 3.9+ (or Docker).

```bash
# run locally (H2 in-memory database, nothing to install)
mvn spring-boot:run

# run all tests + coverage report (target/site/jacoco/index.html)
mvn clean verify

# or with Docker
docker build -t dispatch-load-balancer .
docker run -p 8080:8080 dispatch-load-balancer
```

Then open **http://localhost:8080/swagger-ui/index.html**.

Test with Postman: import `postman/Dispatch_Load_Balancer.postman_collection.json` and run the requests in order (it contains tests). Sample payloads from the assignment are in `samples/`.

### Configuration (environment variables)

| Variable | Default | Purpose |
|---|---|---|
| `PORT` | `8080` | HTTP port (set automatically by most hosts) |
| `DB_URL` | `jdbc:h2:mem:dispatchdb` | JDBC URL. Use `jdbc:postgresql://host:5432/dbname` for PostgreSQL |
| `DB_USERNAME` | `sa` | DB user |
| `DB_PASSWORD` | *(empty)* | DB password |

---

## 2. API

Base path: `/api/dispatch`. Typical flow: **POST vehicles → POST orders → GET plan**.

| Method | Path | Description |
|---|---|---|
| POST | `/api/dispatch/orders` | Submit delivery orders (upsert by `orderId`) |
| POST | `/api/dispatch/vehicles` | Submit fleet (upsert by `vehicleId`) |
| GET | `/api/dispatch/plan` | Compute and return the dispatch plan |
| DELETE | `/api/dispatch/data` | Clear all data (convenience for testing) |

### POST /api/dispatch/orders
```json
{
  "orders": [
    { "orderId": "ORD001", "latitude": 12.9716, "longitude": 77.5946,
      "address": "MG Road, Bangalore, Karnataka, India",
      "packageWeight": 10, "priority": "HIGH" }
  ]
}
```
→ `200 {"message": "Delivery orders accepted.", "status": "success"}`

### POST /api/dispatch/vehicles
```json
{
  "vehicles": [
    { "vehicleId": "VEH001", "capacity": 100,
      "currentLatitude": 12.9716, "currentLongitude": 77.6413,
      "currentAddress": "Indiranagar, Bangalore, Karnataka, India" }
  ]
}
```
→ `200 {"message": "Vehicle details accepted.", "status": "success"}`

### GET /api/dispatch/plan
```json
{
  "dispatchPlan": [
    {
      "vehicleId": "VEH001",
      "capacity": 100,
      "totalLoad": 10,
      "remainingCapacity": 90,
      "currentAddress": "Indiranagar, Bangalore, Karnataka, India",
      "currentLatitude": 12.9716,
      "currentLongitude": 77.6413,
      "totalDistance": "5.06 km",
      "totalDistanceKm": 5.06,
      "assignedOrders": [
        { "orderId": "ORD001", "latitude": 12.9716, "longitude": 77.5946,
          "address": "MG Road, Bangalore, Karnataka, India",
          "packageWeight": 10, "priority": "HIGH" }
      ]
    }
  ],
  "unassignedOrders": [
    { "order": { "...": "..." }, "reason": "INSUFFICIENT_REMAINING_CAPACITY",
      "message": "No vehicle has enough remaining capacity for this order." }
  ],
  "summary": { "totalOrders": 30, "assignedOrders": 23, "unassignedOrders": 7,
               "totalVehicles": 5, "vehiclesUsed": 5, "totalLoad": 495,
               "totalDistance": "85.28 km" }
}
```
`assignedOrders` are listed **in visiting order**. Every vehicle appears in the plan, including idle ones (empty list, load 0).

### Errors
All errors share one shape:
```json
{
  "status": "error",
  "message": "Validation failed",
  "errors": [ { "field": "orders[0].latitude", "message": "latitude must be between -90 and 90" } ],
  "timestamp": "2026-11-19T10:00:00Z"
}
```
| Situation | HTTP |
|---|---|
| Missing/blank field, latitude ∉ [-90,90], longitude ∉ [-180,180], weight/capacity ≤ 0, bad priority, empty list, > 10 000 items | 400 |
| Duplicate `orderId` / `vehicleId` inside one request | 400 |
| Malformed JSON or wrong data type (e.g. `"latitude": "abc"`) | 400 |
| Wrong Content-Type / wrong HTTP method | 415 / 405 |
| Unexpected server error (details are logged, not leaked) | 500 |

**Unassignable orders are not errors**; the plan returns 200 and lists them with a reason:
`NO_VEHICLE_AVAILABLE`, `EXCEEDS_MAX_VEHICLE_CAPACITY` (heavier than the biggest vehicle), `INSUFFICIENT_REMAINING_CAPACITY` (fleet is full).

---

## 3. How the optimization works

1. **Order precedence:** HIGH → MEDIUM → LOW. Within a priority, heavier packages first (first-fit-decreasing packs capacity better), then `orderId` so results are deterministic.
2. **Vehicle choice:** for each order, among vehicles with enough remaining capacity, pick the one **closest to the order** (measured from the vehicle's start, or from its last assigned stop). Ties go to the vehicle with the most remaining capacity, which spreads load across vehicles at the same depot.
3. **Unassignable orders** are collected with a reason instead of failing the request.
4. **Route sequencing:** each vehicle's stops are reordered with nearest-neighbour from the vehicle's current position.
5. **Distance:** `totalDistance` = Haversine path *start → stop 1 → … → stop n* (R = 6371 km).

Because HIGH orders are placed before any LOW order, a LOW order can never take capacity a HIGH order needs.

**Complexity:** assignment `O(orders × vehicles)`, sequencing `O(k²)` per vehicle with `k` stops. 20 000 orders across 300 vehicles are planned in roughly 0.3 s (see `handlesLargeDatasetQuickly`).

### Assumptions
* Vehicles do **not** return to their start; the distance is one-way along the route.
* Distances are straight-line (Haversine), not road distances, as the brief specifies.
* A vehicle's location is where it starts; each vehicle makes a single trip.
* Weights/capacities are positive numbers (decimals allowed); a vehicle may be loaded up to exactly its capacity.
* The plan is **computed on demand** from the stored orders/fleet, so `GET /plan` always reflects the latest data. Re-posting an existing id updates it.
* Priority is case-insensitive on input (`high` works) and always returned upper-case.
* **Sample dataset:** the 30 sample orders weigh 645 but the 5 vehicles carry only 500, so some orders are necessarily unassignable. The plan assigns all HIGH and MEDIUM orders (420 total) and reports the leftover LOW ones.

### Trade-offs
The assignment is a fast greedy heuristic, not an exact solver. Optimal vehicle routing is NP-hard, so this favours predictable speed and clear priority guarantees over a mathematically perfect minimum. The strategies are interfaces, so a better one (2-opt, savings algorithm, OR-Tools) can be swapped in without touching controllers or services.

---

## 4. Design

```
controller   REST endpoints + OpenAPI annotations (no logic)
dto          request/response records, Bean Validation rules
service      DispatchDataService (upserts), DispatchPlanService (orchestration)
optimizer    DistanceCalculator   <- HaversineDistanceCalculator     (Strategy)
             AssignmentStrategy   <- PriorityGreedyAssignmentStrategy (Strategy)
             RouteSequencer       <- NearestNeighborRouteSequencer    (Strategy)
domain       framework-free records: DeliveryOrder, FleetVehicle, Priority
entity       JPA entities (OrderEntity, VehicleEntity)
repository   Spring Data repositories
mapper       entity <-> domain <-> DTO
exception    InvalidRequestException + GlobalExceptionHandler (uniform errors)
```

* **Strategy pattern** for distance, assignment and sequencing; **constructor injection** throughout.
* The optimizer depends only on plain `domain` records, so it is unit-tested without Spring or a database.
* **Efficient upserts:** one `SELECT … WHERE id IN (…)` for the whole batch, then `saveAll` with JDBC batching (`hibernate.jdbc.batch_size=500`). Entities implement `Persistable`, so Spring Data does not issue an extra SELECT per row.
* `BigDecimal` for weights/capacity gives exact sums, with no floating-point drift on load checks.

---

## 5. Tests

`mvn clean verify` runs everything; JaCoCo coverage is in `target/site/jacoco/index.html`.

| Test class | What it covers |
|---|---|
| `HaversineDistanceCalculatorTest` | zero distance, known city pairs, symmetry, antipodal points |
| `NearestNeighborRouteSequencerTest` | ordering, empty route |
| `PriorityGreedyAssignmentStrategyTest` | priority order, capacity limits, exact fit, overweight, no vehicles, no orders, nearest vehicle, tie-break, route distance, determinism, the official 30-order sample, 20 000-order performance |
| `DispatchDataServiceUpsertTest` | insert vs update, no duplicates, case-insensitive priority, duplicate ids, clear |
| `DispatchControllerIntegrationTest` | full API flow with the spec example and the sample dataset, validation errors, malformed JSON, 405/415 |

---

## 6. Deploying (Render, Docker)

1. Push this repo to GitHub.
2. Render → **New → Web Service** → pick the repo → **Runtime: Docker** (the `Dockerfile` is used as-is).
3. *(Optional, for persistent data)* create a Render PostgreSQL database and add env vars `DB_URL` (JDBC form: `jdbc:postgresql://<host>:5432/<db>`), `DB_USERNAME`, `DB_PASSWORD`. Without them the app uses in-memory H2 and data resets when the service restarts.
4. Deploy, then open `https://<service>.onrender.com/swagger-ui/index.html`.

Free instances can sleep when idle, so the first request after a pause may be slow. Open the URL once shortly before reviewers are likely to try it.

---

## 7. Possible improvements
2-opt route improvement; optional return-to-depot distance; time windows; async plan generation with persisted plans; pagination for very large order sets.
