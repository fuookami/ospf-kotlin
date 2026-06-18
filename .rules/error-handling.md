# 错误处理规范

## 1. 核心原则

**整个项目统一使用返回错误（Result 模式），不抛出异常。**

所有可能失败的操作都应返回 `Result<T, C, E>` 或其变体（`ExResult`、`Try`、`Ret<T>` 等），而不是抛出异常。

## 2. 错误类型体系

### 2.1 基础类型（ospf-kotlin-utils）

- `ErrorCode` - 错误码枚举，定义所有标准错误码
- `Error<C>` - 错误基类（sealed class）
  - `Err<C>` - 基本错误，包含 code 和 message
  - `LazyErr<C>` - 惰性消息错误，延迟消息构造
  - `ExErr<C, T>` - 带关联值的错误
  - `LazyExErr<C, T>` - 惰性带关联值错误

### 2.2 结果类型（ospf-kotlin-utils）

- `Result<T, C, E>` - 基础结果类型（sealed interface）
  - `Ok<T, C, E>` - 成功结果，包含值
  - `Failed<T, C, E>` - 失败结果，包含单个错误
  - `Fatal<T, C, E>` - 致命结果，包含多个错误

- `ExResult<T, C, E>` - 扩展结果类型（sealed interface）
  - `Ok<T, C, E>` - 成功结果
  - `Failed<T, C, E>` - 失败结果
  - `Fatal<T, C, E>` - 致命结果
  - `Warn<T, C, E>` - 警告结果，同时包含值和警告

### 2.3 类型别名

- `Try` - 无返回值的结果：`Result<Success, ErrorCode, Error<ErrorCode>>`
- `Ret<T>` - 带返回值的结果：`Result<T, ErrorCode, Error<ErrorCode>>`
- `ExTry` - 无返回值的扩展结果
- `ExRet<T>` - 带返回值的扩展结果

## 3. 使用规范

### 3.1 函数签名

```kotlin
// 正确：返回 Result
fun parse(input: String): Ret<ParsedData> {
    return if (isValid(input)) {
        Ok(parseData(input))
    } else {
        Failed(ErrorCode.IllegalArgument, "Invalid input format")
    }
}

// 正确：返回 Try（无有意义返回值）
fun save(data: Data): Try {
    return if (repository.save(data)) {
        ok
    } else {
        Failed(ErrorCode.ApplicationFailed, "Save failed")
    }
}

// 错误：抛出异常
fun parse(input: String): ParsedData {
    if (!isValid(input)) {
        throw IllegalArgumentException("Invalid input format")
    }
    return parseData(input)
}
```

### 3.2 错误传播

使用 `run`、`exRun` 等函数顺序执行多个可能失败的操作：

```kotlin
fun process(input: String): Ret<Output> {
    return run(
        { validate(input) },
        { transform(input) },
        lastBlock = { output -> save(output) }
    )
}
```

### 3.3 错误映射

使用 `map` 转换成功值，使用 `ifFailed` 处理失败：

```kotlin
val result: Ret<String> = parse(input)
    .map { it.toString() }
    .ifFailed { error -> log.error("Parse failed: ${error.message}") }
```

### 3.4 工厂函数

使用提供的工厂函数创建结果：

```kotlin
// 成功
Ok(value)
ok  // Try 的成功实例
ok(value)  // Ret<T> 的成功实例

// 失败
Failed(ErrorCode.IllegalArgument, "message")
Failed(ErrorCode.IllegalArgument, "message", additionalValue)

// 致命
Fatal(ErrorCode.ApplicationError, "fatal message")
Fatal(listOf(error1, error2))

// 警告
Warn(value, ErrorCode.Other, "warning message")
```

## 4. 禁止的模式

### 4.1 禁止抛出异常

```kotlin
// 禁止
throw IllegalArgumentException("...")
throw UnsupportedOperationException("...")
throw IllegalStateException("...")
throw RuntimeException("...")
```

### 4.2 禁止使用 ApplicationException

`ApplicationException` 存在是为了与外部库交互的兼容性，不应在业务代码中使用。

### 4.3 禁止在 Result 处理中抛异常

```kotlin
// 禁止
when (result) {
    is Failed -> throw IllegalStateException(result.error.message)
    // ...
}
```

## 5. 允许的例外情况

### 5.1 测试代码

测试代码中可以使用异常来：
- 模拟失败场景
- 断言预期行为
- 测试 stub 实现

```kotlin
// 测试中允许
override fun method(): Type = throw UnsupportedOperationException("stub")
```

### 5.2 外部库交互

与不支持 Result 模式的外部库交互时，可以在边界处捕获异常并转换为 Result：

```kotlin
fun externalCall(): Ret<Response> {
    return try {
        Ok(externalLibrary.doSomething())
    } catch (e: ExternalException) {
        Failed(ErrorCode.Other, "External call failed: ${e.message}")
    }
}
```

### 5.3 基础设施层

极少数基础设施代码（如并行计算工具库）可能保留异常，但必须：
- 提供 Result 版本的替代 API
- 在文档中明确说明
- 逐步迁移

## 6. 迁移策略

### 6.1 识别需要迁移的代码

使用以下命令搜索项目中的 throw 语句：

```bash
grep -r "throw\s+" --include="*.kt" --exclude-dir=test
```

### 6.2 迁移优先级

1. **高优先级**：应用层、领域服务层
2. **中优先级**：领域模型层、基础设施层
3. **低优先级**：工具库、并行计算库

### 6.3 迁移步骤

1. 识别函数的失败路径
2. 定义适当的 ErrorCode
3. 修改返回类型为 Result 变体
4. 将 throw 替换为返回 Failed/Fatal
5. 更新所有调用点
6. 添加测试覆盖

## 7. ErrorCode 扩展

当现有 ErrorCode 不足以表达错误类型时：

1. 首先检查是否可以复用现有 ErrorCode
2. 如果需要新的领域特定错误码，在相应模块定义扩展枚举
3. 确保错误码值不与现有 ErrorCode 冲突

## 8. 错误消息规范

- 使用简洁明了的中英双语消息
- 包含足够的上下文信息（如参数值、状态）
- 避免暴露内部实现细节
- 格式：`"操作失败：原因 / Operation failed: reason"`
