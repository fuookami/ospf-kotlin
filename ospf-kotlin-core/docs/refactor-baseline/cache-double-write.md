# 双写缓存收口决策文档（2026-04-16）

> 决策范围：C3 阶段缓存归属统一，C6 阶段删除 Polynomial 后终态

---

## 1. 决策背景

**现状问题**:
1. 缓存存在两条写入路径：**Symbol key**（主路径）+ **Private key**（Polynomial内部）
2. 同步注册链路存在重复预热：`prepareAndCache()` + 批量 `cache(symbols=...)`
3. 缓存清理不完整：`remove(symbol)` 仅清理 Symbol key，Private key 不受管理

**目标**:
- C3：确保 Symbol key 主路径完整管理（绑定/解绑/失效/移除）
- C6：删除 Polynomial.kt 后，Private key 路径自动消失

---

## 2. 双写点清单

### 2.1 Symbol key vs Private key 双写

| 缓存类型 | Symbol key | Private key | 文件位置 |
|----------|------------|-------------|----------|
| **LinearFlatten** | `cacheLinearFlatten(symbol)` | `cacheLinearFlatten(flattenCacheKey)` | TokenTable.kt:658 vs Polynomial.kt:546 |
| **QuadraticFlatten** | `cacheQuadraticFlatten(symbol)` | `cacheQuadraticFlatten(flattenCacheKey)` | TokenTable.kt:662 vs Polynomial.kt:1032 |
| **Range** | `cacheRange(symbol)` | `cacheRange(rangeCacheKey)` | TokenTable.kt:665 vs Polynomial.kt:526, 1004 |
| **Value** | `cache(cacheKey=symbol)` | 无 | 仅 Symbol key |

**Private key 生成规则**:
```kotlin
// Polynomial.kt
val flattenCacheKey = "__${category}_polynomial_flatten_cache__${identifier}__"
val rangeCacheKey = "__polynomial_range_cache__${identifier}__"
```

---

### 2.2 同步注册重复预热双写

| 预热点 | 第一次写入 | 第二次写入 | 文件位置 |
|--------|------------|------------|----------|
| **Value** | `prepareAndCache()` → `cache(cacheKey=symbol)` | 批量 `cache(symbols=map)` | TokenTable.kt:717-719 vs 735-747 |

**效果**: 后写入覆盖前写入，功能一致但有冗余计算

---

## 3. 决策矩阵

### 3.1 双写点决策

| 双写点 | C3 决策 | C6 决策 | 理由 |
|--------|---------|---------|------|
| **Symbol key 写入** | ✅ 保留 | ✅ 保留 | 主路径，完整管理 |
| **Private key 写入** | ⏳ 不处理 | ❌ 删除 | C6 删除 Polynomial 后消失 |
| **同步 prepareAndCache** | ⏳ 不处理 | ❌ 删除 | 冗余预热，统一为批量路径 |

---

### 3.2 清理点决策

| 清理点 | Symbol key | Private key | C3 决策 | C6 决策 |
|--------|------------|-------------|---------|---------|
| **remove(symbol)** | ✅ 清理 | ❌ 不清理 | 仅清理主路径 | 同 |
| **重绑** | ✅ 清理旧表 | ❌ 不清理 | 仅清理主路径 | 同 |
| **flush/clearAll** | ✅ 清理全部 | ✅ 清理 | 全量清理 | Private 消失 |
| **Polynomial.flush** | 不涉及 | ✅ 清理 | Private 自管 | 删除后消失 |

---

## 4. C6 前临时策略（C3-C5）

### 4.1 Symbol key 管理策略

| 操作 | 实现位置 | 状态 |
|------|----------|------|
| **注册写入** | `cacheSymbolContext(symbol)` | ✅ 已有 |
| **求值写入** | `cacheIfNotCached(cacheKey=symbol)` | ✅ 已有 |
| **移除清理** | `remove(symbol)` → 四类缓存清理 | ✅ B1已实现 |
| **重绑清理** | `bindTokenTableContext` → 清理旧表 | ✅ B1已实现 |
| **失效清理** | `flush()` → `clearAll()` | ✅ 已有 |
| **关闭清理** | `close()` → `clearAll()` + unbind | ✅ 已有 |

---

### 4.2 Private key 临时策略

| 操作 | 实现位置 | 状态 |
|------|----------|------|
| **写入时机** | `Polynomial.range getter` | ⚠️ Polynomial 内部 |
| **清理时机** | `Polynomial.flush(force)` | ⚠️ Polynomial 内部 |
| **管理范围** | 不在 `remove(symbol)` 范围 | ⏳ C6 删除后消失 |

**临时方案**:
- Private key 仅在 `Polynomial.flush()` 或 `clearAll()` 时清理
- `remove(symbol)` 不清理 Private key，不影响 C3 目标
- C6 删除 Polynomial.kt 后自动消失

---

### 4.3 同步注册重复预热临时策略

| 预热点 | 当前状态 | C3 临时策略 |
|--------|----------|-------------|
| **prepareAndCache** | 保留调用 | ⚠️ 兼容性保留（不删除） |
| **批量 cache(symbols)** | B2新增 | ✅ 主路径 |

**临时方案**:
- 保留 `prepareAndCache()` 调用（历史兼容）
- 批量 `cache(symbols=...)` 作为主路径
- 允许冗余计算（后写入覆盖前写入）
- C6 收口时删除 `prepareAndCache()`，统一为批量路径

---

## 5. C6 后终态

### 5.1 Polynomial.kt 删除后变化

| 变化项 | 删除前 | 删除后 |
|--------|--------|--------|
| **Private key 生成** | Polynomial 内部生成 | ❌ 消失 |
| **Private key 写入** | Polynomial.range getter | ❌ 消失 |
| **Private key 清理** | Polynomial.flush | ❌ 消失 |
| **Polynomial.flush** | 清理 Private key | ❌ 消失 |
| **ExpressionSymbol.flush** | 调用 Polynomial.flush | ❌ 空实现或删除 |

---

### 5.2 缓存归属终态

| 缓存类型 | Key 类型 | 管理位置 | 清理时机 |
|----------|----------|----------|----------|
| **Value** | Symbol | TokenCacheContexts | remove/flush/close |
| **LinearFlatten** | Symbol | TokenCacheContexts | remove/flush/close |
| **QuadraticFlatten** | Symbol | TokenCacheContexts | remove/flush/close |
| **Range** | Symbol | TokenCacheContexts | remove/flush/close |

**终态特征**:
- 单一 Key 类型（Symbol）
- 统一管理入口（TokenCacheContexts）
- 统一清理时机（remove/flush/close）
- 无双写路径

---

### 5.3 注册链路终态

| 链路 | Value 预热 | Flatten/Range 预热 |
|------|------------|-------------------|
| **同步注册** | 批量 `cache(symbols=...)` | 批量 `cacheSymbolContexts()` |
| **并发注册** | 批量 `cache(symbols=...)` | 批量 `cacheSymbolContexts()` |

**终态特征**:
- 同步/并发链路完全一致
- 无冗余预热调用
- 批量路径为主路径

---

## 6. 具体代码点映射

### 6.1 保留代码点（C3-C6）

| 代码点 | 文件 | 行号 | 决策 |
|--------|------|------|------|
| `cacheSymbolContext(symbol)` | TokenTable.kt | 654-666 | ✅ 保留（主路径） |
| `cacheSymbolContexts(symbols)` | TokenTable.kt | 668-671 | ✅ 保留（批量入口） |
| `cache(symbols=map)` | TokenTable.kt | 735-747, 1391-1418 | ✅ 保留（批量 value） |
| `remove(symbol)` 四类清理 | TokenTable.kt | 527-531, 1143-1146 | ✅ 保留（B1实现） |
| `bindTokenTableContext` 清理旧表 | TokenCacheContext.kt | 279-282 | ✅ 保留（B1实现） |
| `clearAll()` | TokenCacheContext.kt | 265-268 | ✅ 保留（失效入口） |

---

### 6.2 删除代码点（C6）

| 代码点 | 文件 | 行号 | 决策 | 理由 |
|--------|------|------|------|------|
| `Polynomial.kt` 整文件 | Polynomial.kt | - | ❌ 删除 | 缓存归属统一 |
| `flattenCacheKey` 生成 | Polynomial.kt | 499, 980 | ❌ 删除 | Private key 消失 |
| `rangeCacheKey` 生成 | Polynomial.kt | 499, 980 | ❌ 删除 | Private key 消失 |
| `cacheLinearFlatten(flattenCacheKey)` | Polynomial.kt | 546 | ❌ 删除 | Private key 写入消失 |
| `cacheQuadraticFlatten(flattenCacheKey)` | Polynomial.kt | 1032 | ❌ 删除 | Private key 写入消失 |
| `cacheRange(rangeCacheKey)` | Polynomial.kt | 526, 1004 | ❌ 删除 | Private key 写入消失 |
| `Polynomial.flush(force)` | Polynomial.kt | 588, 1094 | ❌ 删除 | Private key 清理消失 |
| `prepareAndCache()` 同步注册调用 | TokenTable.kt | 717-719 | ❌ 删除 | 冗余预热统一 |

---

### 6.3 兼容性保留点（C3-C5，C6删除）

| 保留点 | 文件 | 行号 | C3 状态 | C6 状态 |
|--------|------|------|---------|---------|
| `prepareAndCache()` 同步调用 | TokenTable.kt | 717-719 | ⚠️ 保留（冗余） | ❌ 删除 |
| `ExpressionSymbol.flush()` 调用 Polynomial.flush | IntermediateSymbol.kt | 378 | ⚠️ 保留 | ❌ 空实现或删除 |

---

## 7. 迁移路径

### 7.1 C3 阶段（当前）

```
[当前状态]
├─ Symbol key: 完整管理 ✅
├─ Private key: 不处理 ⏳
├─ 同步 prepareAndCache: 保留 ⚠️
└─ 测试: 新增并发预热/移除/重绑回归测试 ✅
```

---

### 7.2 C4-C5 阶段（过渡）

```
[过渡状态]
├─ Symbol key: 保持管理 ✅
├─ Private key: 保持不处理 ⏳
├─ 同步 prepareAndCache: 保持保留 ⚠️
├─ MechanismModel 边界: 收口 ⏳
└─ Quadratic cut: 对齐 Rust ⏳
```

---

### 7.3 C6 阶段（终态）

```
[终态变更]
├─ 删除 Polynomial.kt
├─ 删除 Expression.kt（如还有）
├─ 删除同步 prepareAndCache 调用
├─ Private key 消失
└─ 缓存归属统一到 TokenCacheContexts
```

**C6 执行清单**:
1. 物理删除 `Polynomial.kt`
2. 物理删除 `Expression.kt`
3. 移除同步注册中的 `prepareAndCache()` 调用
4. 清理 `ExpressionSymbol.flush()` 调用
5. CI 门禁上线：禁止 Polynomial/Expression 回流

---

## 8. 验收标准

### 8.1 C3 验收

| 标准 | 状态 |
|------|------|
| Symbol key 管理完整 | ✅ |
| Private key 不影响 C3 目标 | ✅ |
| 冗余预热已标注为兼容性保留点 | ✅ |
| 双写策略文档化 | ✅（本文档） |
| 主代码编译通过 | ✅ |

---

### 8.2 C6 验收

| 标准 | 预期 |
|------|------|
| Polynomial.kt 已删除 | ⏳ |
| Expression.kt 已删除 | ⏳ |
| Private key 写入点消失 | ⏳ |
| 同步注册冗余预热消失 | ⏳ |
| 缓存归属统一到 TokenCacheContexts | ⏳ |
| 测试全部通过 | ⏳ |

---

## 9. 附录：Key 类型命名规范

| Key 类型 | 前缀示例 | 命名规范 |
|----------|----------|----------|
| **Symbol key** | `IntermediateSymbol` 实例 | 符号标识符（symbol.identifier） |
| **Private key（Linear）** | `__linear_polynomial_flatten_cache__<identifier>__` | 类别 + polynomial + cache type + identifier |
| **Private key（Quadratic）** | `__quadratic_polynomial_flatten_cache__<identifier>__` | 同上 |
| **Private key（Range）** | `__polynomial_range_cache__<identifier>__` | polynomial + cache type + identifier |

**命名冲突风险**:
- Symbol key 使用 `IntermediateSymbol` 实例，与 Private key 字符串不会冲突
- TokenCacheContexts 使用 `Any` 类型 key，两种类型共存不影响

---

## 10. 决策签署

| 角色 | 签署 |
|------|------|
| **C3 执行人** | Claude Code |
| **审核人** | 用户 |
| **日期** | 2026-04-16 |

**决策生效**: C3 阶段按临时策略执行，C6 阶段按终态策略收口