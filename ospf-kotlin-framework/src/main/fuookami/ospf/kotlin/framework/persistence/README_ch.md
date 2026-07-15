# Persistence Package

:us: [English](README.md) | :cn: 简体中文

持久化基础设施层，提供请求/响应记录存储、日志记录持久化、SQL 类型扩展和持久化 API 控制器。基于 Ktorm ORM 实现。

## 文件概览

| 文件 | 职责 |
| --- | --- |
| `Request.kt` | 请求/响应 DTO 接口（F-bounded 泛型） |
| `RequestRecord.kt` | 请求/响应记录的 Ktorm 表定义、DAO 和序列化感知 PO |
| `LogRecord.kt` | 日志记录的 Ktorm 表定义、DAO 和持久化保存实现 |
| `SqlType.kt` | Ktorm 自定义列类型扩展（数值/日期/枚举列表） |
| `PersistenceApiController.kt` | 持久化 API 控制器接口 |

## 请求/响应 DTO

`RequestDTO<T>` 和 `ResponseDTO<T>` 使用 F-bounded 泛型约束，确保类型安全的自引用：

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

## 请求/响应记录

`RequestRecordPO<T>` 和 `ResponseRecordPO<T>` 是序列化感知的持久化对象，自动将 DTO 序列化为字节数组存入数据库：

```kotlin
// 创建请求记录
val record = RequestRecordPO(
    app = "my-app",
    requester = "user",
    version = "1.0",
    request = myRequestDTO
)

// 插入数据库
RequestRecordDAO.insert(db, tableName = "requests", record = record)

// 查询请求记录
val records: List<RequestRecordPO<MyRequestDTO>> = RequestRecordDAO.get(
    db = db,
    tableName = "requests",
    app = "my-app"
)
```

### DAO 查询能力

`RequestRecordDAO` 和 `ResponseRecordDAO` 支持按以下条件查询：

- `id` — 请求标识
- `app` — 应用名
- `requester` — 请求者
- `time` — 时间范围（`Pair<LocalDateTime, LocalDateTime>`）

`ResponseRecordDAO.get()` 自动关联请求表进行 LEFT JOIN 查询。

## 日志记录持久化

`LogRecordPersistenceSaving` 实现了 `Saving` 接口，支持同步和异步两种模式：

- **同步模式**（`scope = null`）— 直接写入数据库。
- **异步模式**（`scope != null`）— 写入内存队列，定时批量刷盘（10 秒间隔，使用 `Mutex` 保证线程安全）。

SQLite 方言自动使用字符串存储（因 SQLite 不支持 BLOB 事务），并设置 `PRAGMA busy_timeout = 30000`。

```kotlin
val saving = LogRecordPersistenceSaving(
    db = db,
    tableName = "logs",
    scope = CoroutineScope(Dispatchers.IO)
)

// 保存日志
saving(logRecordPO, MyData::class.serializer())

// 关闭时刷盘剩余记录
saving.close()
```

`LogRecordDAO` 提供直接插入方法，支持字节数组和字符串两种存储格式。

## SQL 类型扩展

`SqlType.kt` 为 Ktorm 表定义提供自定义列类型映射：

| 扩展函数 | Kotlin 类型 | SQL 类型 | 说明 |
| --- | --- | --- | --- |
| `ui32(name)` | `UInt32` | INT | 无符号 32 位整数 |
| `i32(name)` | `Int32` | INT | 有符号 32 位整数 |
| `ui64(name)` | `UInt64` | BIGINT | 无符号 64 位整数 |
| `i64(name)` | `Int64` | BIGINT | 有符号 64 位整数 |
| `f32(name)` | `Flt32` | FLOAT | 32 位浮点数 |
| `f64(name)` | `Flt64` | DOUBLE | 64 位浮点数 |
| `fltx(name, scale)` | `FltX` | DECIMAL | 高精度浮点数，可指定精度和舍入模式 |
| `kotlinDatetime(name)` | `LocalDateTime` | DATETIME | Kotlin 日期时间 |
| `enums(name)` | `List<T : Enum<T>>` | VARCHAR | 枚举列表（逗号分隔存储） |

用法示例：

```kotlin
object MyTable : Table<MyEntity>("my_table") {
    val id = ui64("id").primaryKey()
    val name = varchar("name").bindTo { it.name }
    val amount = fltx("amount", scale = 4).bindTo { it.amount }
    val createdAt = kotlinDatetime("created_at").bindTo { it.createdAt }
    val tags = enums<MyEnum>("tags").bindTo { it.tags }
}
```

## Expression 子包

`persistence/expression/` 子包提供基于 KSP 注解处理的 SQL 谓词下推框架，详见专属文档：

- [expression/README_ch.md](expression/README_ch.md)
