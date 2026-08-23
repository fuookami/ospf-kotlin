# Persistence Package

:us: English | :cn: [简体中文](README_ch.md)

Persistence infrastructure layer providing request/response record storage, log record persistence, SQL type extensions, and persistence API controllers. Built on Ktorm ORM.

## File Overview

| File | Responsibility |
| --- | --- |
| `Request.kt` | Request/response DTO interfaces (F-bounded generic) |
| `RequestRecord.kt` | Ktorm table definitions, DAOs, and serialization-aware POs for request/response records |
| `LogRecord.kt` | Ktorm table definitions, DAOs, and persistence saving implementation for log records |
| `SqlType.kt` | Ktorm custom column type extensions (numeric/date/enum list) |
| `PersistenceApiController.kt` | Persistence API controller interface |

## Request/Response DTO

`RequestDTO<T>` and `ResponseDTO<T>` use F-bounded generic constraints for type-safe self-referencing:

```kotlin
interface RequestDTO<T : RequestDTO<T>> {
    val id: String
}

interface ResponseDTO<T : ResponseDTO<T>> {
    val id: String
    val code: UInt64
    val msg: String
}
```

## Request/Response Records

`RequestRecordPO<T>` and `ResponseRecordPO<T>` are serialization-aware persistence objects that automatically serialize DTOs to byte arrays for database storage:

```kotlin
// Create request record
val record = RequestRecordPO(
    app = "my-app",
    requester = "user",
    version = "1.0",
    request = myRequestDTO
)

// Insert into database
RequestRecordDAO.insert(db, tableName = "requests", record = record)

// Query request records
val records: List<RequestRecordPO<MyRequestDTO>> = RequestRecordDAO.get(
    db = db,
    tableName = "requests",
    app = "my-app"
)
```

### DAO Query Capabilities

`RequestRecordDAO` and `ResponseRecordDAO` support querying by:

- `id` — Request identifier
- `app` — Application name
- `requester` — Requester
- `time` — Time range (`Pair<LocalDateTime, LocalDateTime>`)

`ResponseRecordDAO.get()` automatically performs a LEFT JOIN with the request table.

## Log Record Persistence

`LogRecordPersistenceSaving` implements the `Saving` interface, supporting both synchronous and asynchronous modes:

- **Synchronous mode** (`scope = null`) — Writes directly to the database.
- **Asynchronous mode** (`scope != null`) — Writes to an in-memory queue with periodic batch flushing (10-second interval, using `Mutex` for thread safety).

SQLite dialect automatically uses string storage (since SQLite does not support BLOB transactions) and sets `PRAGMA busy_timeout = 30000`.

```kotlin
val saving = LogRecordPersistenceSaving(
    db = db,
    tableName = "logs",
    scope = CoroutineScope(Dispatchers.IO)
)

// Save log record
saving(logRecordPO, MyData::class.serializer())

// Flush remaining records on close
saving.close()
```

`LogRecordDAO` provides direct insertion methods, supporting both byte array and string storage formats.

## SQL Type Extensions

`SqlType.kt` provides custom column type mappings for Ktorm table definitions:

| Extension Function | Kotlin Type | SQL Type | Description |
| --- | --- | --- | --- |
| `ui32(name)` | `UInt32` | INT | Unsigned 32-bit integer |
| `i32(name)` | `Int32` | INT | Signed 32-bit integer |
| `ui64(name)` | `UInt64` | BIGINT | Unsigned 64-bit integer |
| `i64(name)` | `Int64` | BIGINT | Signed 64-bit integer |
| `f32(name)` | `Flt32` | FLOAT | 32-bit floating point |
| `f64(name)` | `Flt64` | DOUBLE | 64-bit floating point |
| `fltx(name, scale)` | `FltX` | DECIMAL | High-precision float, configurable scale and rounding mode |
| `kotlinDatetime(name)` | `LocalDateTime` | DATETIME | Kotlin date-time |
| `enums(name)` | `List<T : Enum<T>>` | VARCHAR | Enum list (comma-separated storage) |

Usage example:

```kotlin
object MyTable : Table<MyEntity>("my_table") {
    val id = ui64("id").primaryKey()
    val name = varchar("name").bindTo { it.name }
    val amount = fltx("amount", scale = 4).bindTo { it.amount }
    val createdAt = kotlinDatetime("created_at").bindTo { it.createdAt }
    val tags = enums<MyEnum>("tags").bindTo { it.tags }
}
```

## Expression Sub-package

The `persistence/expression/` sub-package provides a KSP annotation processing-based SQL predicate pushdown framework. See the dedicated documentation:

- [expression/README.md](expression/README.md)

## Relational Query Plans

The framework persistence API also exposes a database-independent [relational query plan](query/README.md). It is intended for controlled backend adapters: adapters register sources and columns, while callers provide immutable join, predicate, projection, ordering, grouping, and pagination semantics.

`Inner`, `Left`, and `Exists` are distinct operations. `Exists` is a semi-join and is the preferred shape for one-to-many existence predicates because it preserves root cardinality. Root-granularity counting requires an explicit `rootKey`; unsupported composite-key strategies must be rejected rather than approximated.
