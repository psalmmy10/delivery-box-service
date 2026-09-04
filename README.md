# Delivery Box Service

A Spring Boot REST API for managing delivery boxes (cameras + battery, carrying small items to
remote locations) as described in the assessment brief.

## Tech stack

- Java 21
- Spring Boot 3.3.4 (Web, Data JPA, Validation)
- H2 in-memory database
- JUnit 5 + Mockito + AssertJ (via `spring-boot-starter-test`)
- Maven

## Build / Run / Test

> Requires Java 21 and Maven 3.6+ (or use the included Maven if you have it; no `mvnw` wrapper
> jar is bundled here to keep the zip small — if you'd like one, run `mvn -N wrapper:wrapper`).

```bash
# Build (compiles, runs tests, packages the jar)
mvn clean install

# Run the app (starts on http://localhost:8080)
mvn spring-boot:run
# or, after `mvn clean package`:
java -jar target/delivery-box-service-1.0.0.jar

# Run tests only
mvn test
```

The app uses an in-memory H2 database that is recreated on every startup and seeded with sample
boxes from `src/main/resources/data.sql`. The H2 console is available at
`http://localhost:8080/h2-console` (JDBC URL `jdbc:h2:mem:deliverybox`, user `sa`, blank password)
if you want to inspect the data directly.

### Preloaded sample data

| txref   | weightLimit | batteryCapacity | state      |
|---------|-------------|------------------|------------|
| BOX-001 | 500         | 100              | IDLE       |
| BOX-002 | 300         | 80               | IDLE       |
| BOX-003 | 500         | 10               | IDLE (low battery — will reject loading) |
| BOX-004 | 250         | 60               | DELIVERING (not available for loading) |

> **Note on this build:** this project was assembled in a sandboxed environment without access to
> Maven Central, so I was not able to run `mvn clean install` myself to give it a final green
> build. I've compiled it "by eye" carefully (structure, imports, JPA mappings, and a brace/paren
> balance check all pass), but please run `mvn clean install` as your first step and let me know
> if anything doesn't compile — happy to fix immediately.

## API

Base path: `/api/boxes`

| Method | Path                     | Description                                      |
|--------|--------------------------|---------------------------------------------------|
| POST   | `/api/boxes`             | Create a box                                       |
| GET    | `/api/boxes/{txref}`     | Get a single box                                   |
| POST   | `/api/boxes/{txref}/items` | Load a box with one or more items                |
| GET    | `/api/boxes/{txref}/items` | List items currently loaded on a box             |
| GET    | `/api/boxes/available`   | List boxes currently available for loading         |
| GET    | `/api/boxes/{txref}/battery` | Check battery level for a box                 |

### Create a box

```bash
curl -X POST http://localhost:8080/api/boxes \
  -H "Content-Type: application/json" \
  -d '{"txref": "BOX-100", "weightLimit": 500, "batteryCapacity": 100}'
```

`batteryCapacity` is optional and defaults to `100` (a freshly deployed box).

### Load a box with items

```bash
curl -X POST http://localhost:8080/api/boxes/BOX-001/items \
  -H "Content-Type: application/json" \
  -d '{
        "items": [
          {"name": "med-kit_1", "weight": 200, "code": "MED_KIT_01"},
          {"name": "water-bottle", "weight": 100, "code": "H2O_01"}
        ]
      }'
```

Rejected if:
- the box isn't `IDLE`/`LOADING` (e.g. it's already `DELIVERING`) → `409 Conflict`
- the box's battery is below 25% → `400 Bad Request`
- the combined weight would exceed the box's `weightLimit` → `400 Bad Request`

### Check loaded items

```bash
curl http://localhost:8080/api/boxes/BOX-001/items
```

### Check available boxes for loading

```bash
curl http://localhost:8080/api/boxes/available
```

### Check battery level

```bash
curl http://localhost:8080/api/boxes/BOX-001/battery
```

## Design assumptions

Since the brief explicitly invites assumptions, here's what I decided and why:

1. **`txref` is the box's business key and primary identifier.** It's used directly as the path
   variable (`/api/boxes/{txref}`) instead of a separate surrogate ID, since the brief describes
   it as the box's reference.
2. **Weights are in grams**, matching the brief's "500gr max" and item "weight" fields — both
   modeled as integers.
3. **`batteryCapacity` is an integer percentage (0–100).**
4. **A new box defaults to `state = IDLE` and `batteryCapacity = 100`** if battery isn't supplied
   at creation (a freshly deployed/charged box), matching the enum's natural starting point.
5. **Loading is a single atomic request**: a client posts a list of items to `/items`; the box
   moves `IDLE`/`LOADING` → `LOADING` (battery is checked at this point) → `LOADED` once every
   item is validated and persisted. If any item would push the total over the weight limit, the
   *entire* request is rejected — no partial loads.
6. **Loading is allowed incrementally**: a box already in `LOADING` (mid-load) can be loaded
   again; a box in `LOADED`, `DELIVERING`, `DELIVERED`, or `RETURNING` cannot be loaded until it
   returns to `IDLE` (that reset transition — e.g. after delivery completes — is outside this
   task's scope, so no endpoint mutates a box back to `IDLE`).
7. **"Available boxes for loading"** = boxes currently `IDLE` **and** with battery ≥ 25%, since
   that's the functional requirement gating the `LOADING` state.
8. **Validation** for `Item.name` (`^[a-zA-Z0-9_-]+$`) and `Item.code` (`^[A-Z0-9_]+$`) is enforced
   via Bean Validation `@Pattern` annotations at the API boundary, returning `400` with a field-
   level message on failure.
9. **No authentication/authorization** — out of scope for this exercise.
10. **H2 in-memory database** is used for simplicity and to keep the project trivially runnable;
    swapping in Postgres/MySQL would just mean changing `spring.datasource.*` and adding the
    relevant driver dependency — the JPA entities are otherwise portable.
11. **Communication with the physical box hardware is out of scope**, as stated in the brief — this
    service only manages box/item *state*, not device communication.

## Project structure

```
src/main/java/com/example/deliverybox/
├── model/          Box, Item, BoxState (JPA entities/enum)
├── repository/      BoxRepository, ItemRepository (Spring Data JPA)
├── dto/             Request/response payloads (with Bean Validation)
├── service/         BoxService — business rules live here
├── controller/       BoxController — REST endpoints
└── exception/        Domain exceptions + @RestControllerAdvice error mapping
src/main/resources/
├── application.yml   H2 + JPA config
└── data.sql          Seed data
src/test/java/...     BoxServiceTest — unit tests for the business rules
```

SWAGGER API URL
http://localhost:8080/swagger-ui.html