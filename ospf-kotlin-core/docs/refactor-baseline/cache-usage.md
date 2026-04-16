# 缓存点分布清单（2026-04-16）

> 生成方式：grep -rn 关键词扫描 core/src/main
> 目的：为 C3 缓存上收阶段提供完整的调用点清单，确保缓存归属和生命周期管理清晰

---

## 1. 写入点（Write）

### 1.1 Value 缓存写入

| 文件 | 行号 | 调用 | Key类型 | 上下文 |
|------|------|------|---------|--------|
| IntermediateSymbol.kt | 84 | `cache(cacheKey=symbol, solution=null)` | Symbol | prepareAndCache 内部 |
| IntermediateSymbol.kt | 91 | `cache(cacheKey=symbol, fixedValues)` | Symbol | prepareAndCache 内部 |
| TokenTable.kt | 717-719 | `prepareAndCache(null/fixedValues)` | Symbol | **同步注册**（B2修复后批量写入） |
| TokenTable.kt | 1391-1418 | `cache(symbols=map)` | Symbol | **并发注册**批量写入 |

**路径对比**：
- **同步注册**：`prepareAndCache()` → 内部调用 `cache(cacheKey=symbol)` [单符号] + `cache(symbols=map)` [批量 B2新增]
- **并发注册**：`prepare()` + `cache(symbols=map)` [批量]

---

### 1.2 LinearFlatten 缓存写入

| 文件 | 行号 | 调用 | Key类型 | 上下文 |
|------|------|------|---------|--------|
| **Symbol key（主路径）** |||||
| TokenTable.kt | 658 | `cacheLinearFlatten(symbol, symbol.flattenedMonomials)` | Symbol | cacheSymbolContext |
| TokenTable.kt | 684 | `cacheSymbolContexts(emptySymbols)` | Symbol | 同步注册-空符号 |
| TokenTable.kt | 756 | `cacheSymbolContexts(readySymbols)` | Symbol | 同步注册-批量 |
| TokenTable.kt | 1346 | `cacheSymbolContexts(emptySymbols)` | Symbol | 并发注册-空符号 |
| TokenTable.kt | 1427 | `cacheSymbolContexts(thisReadSymbol)` | Symbol | 并发注册-分批 |
| TokenTable.kt | 1462 | `cacheSymbolContexts(readySymbols)` | Symbol | 并发注册-单批 |
| TokenTable.kt | 1504 | `cacheSymbolContexts(readySymbols)` | Symbol | 并发注册-单线程 fallback |
| **Private key（Polynomial内部）** |||||
| Polynomial.kt | 546 | `cacheLinearFlatten(flattenCacheKey, flattenData)` | Private | AbstractLinearPolynomial.range getter |
| **Interface 实现（接口层）** |||||
| TokenTable.kt | 157, 373, 571, 859, 1205 | `cacheLinearFlatten(cacheKey, flatten)` | Any | 四类TokenTable实现 |

**Key 类型说明**：
- **Symbol key**: `IntermediateSymbol` 实例，由 `cacheSymbolContext` 写入
- **Private key**: `flattenCacheKey = "__linear_polynomial_flatten_cache__..."`，由 Polynomial 内部写入

---

### 1.3 QuadraticFlatten 缓存写入

| 文件 | 行号 | 调用 | Key类型 | 上下文 |
|------|------|------|---------|--------|
| **Symbol key（主路径）** |||||
| TokenTable.kt | 662 | `cacheQuadraticFlatten(symbol, symbol.flattenedMonomials)` | Symbol | cacheSymbolContext |
| **Private key（Polynomial内部）** |||||
| Polynomial.kt | 1032 | `cacheQuadraticFlatten(flattenCacheKey, flattenData)` | Private | AbstractQuadraticPolynomial.range getter |
| **Interface 实现（接口层）** |||||
| TokenTable.kt | 173, 390, 588, 884, 1230 | `cacheQuadraticFlatten(cacheKey, flatten)` | Any | 四类TokenTable实现 |

---

### 1.4 Range 缓存写入

| 文件 | 行号 | 调用 | Key类型 | 上下文 |
|------|------|------|---------|--------|
| **Symbol key（主路径）** |||||
| TokenTable.kt | 665 | `cacheRange(symbol, symbol.range)` | Symbol | cacheSymbolContext |
| **Private key（Polynomial内部）** |||||
| Polynomial.kt | 526 | `cacheRange(rangeCacheKey, range)` | Private | AbstractLinearPolynomial.range getter |
| Polynomial.kt | 1004 | `cacheRange(rangeCacheKey, range)` | Private | AbstractQuadraticPolynomial.range getter |
| **Interface 实现（接口层）** |||||
| TokenTable.kt | 189, 407, 605, 909, 1255 | `cacheRange(cacheKey, range)` | Any | 四类TokenTable实现 |

---

### 1.5 批量预热入口（cacheSymbolContexts）

| 文件 | 行号 | 调用 | 上下文 |
|------|------|------|--------|
| TokenTable.kt | 654 | `cacheSymbolContext(symbol)` | 单符号预热函数定义 |
| TokenTable.kt | 668 | `cacheSymbolContexts(symbols)` | 批量预热函数定义 |
| TokenTable.kt | 670 | `cacheSymbolContext(symbol)` [循环内] | 函数内部实现 |
| TokenTable.kt | 684 | `cacheSymbolContexts(emptySymbols)` | 同步注册-空符号预热 |
| TokenTable.kt | 756 | `cacheSymbolContexts(readySymbols)` | 同步注册-批量预热 |
| TokenTable.kt | 1346 | `cacheSymbolContexts(emptySymbols)` | 并发注册-空符号预热 |
| TokenTable.kt | 1427 | `cacheSymbolContexts(thisReadSymbol)` | 并发注册-分批预热 |
| TokenTable.kt | 1462 | `cacheSymbolContexts(readySymbols)` | 并发注册-单批预热 |
| TokenTable.kt | 1504 | `cacheSymbolContexts(readySymbols)` | 并发注册-单线程 fallback |

---

## 2. 读取点（Read）

### 2.1 Value 缓存读取（cached/cachedValue）

| 文件 | 行号 | 调用 | Key类型 | 上下文 |
|------|------|------|---------|--------|
| IntermediateSymbol.kt | 187, 189 | `cached(cacheKey)` | Symbol | shouldPrepare 判断 |
| IntermediateSymbol.kt | 205 | `cached(cacheKey)` | Symbol | shouldPrepareWithFixedCacheKey |
| IntermediateSymbol.kt | 278 | `cachedValue(cacheKey)` | Symbol | evaluateWithCachedTokenTable |
| IntermediateSymbol.kt | 305 | `cachedValue(this, results)` | Symbol | evaluate 结果缓存 |
| IntermediateSymbol.kt | 342 | `cachedValue(this, values)` | Symbol | evaluate fixedValues缓存 |
| TokenTable.kt | 220, 231 | `cachedValue(cacheKey)` | Any | cacheIfNotCached |
| TokenTable.kt | 235, 246 | `cachedValue(cacheKey, fixedValues)` | Any | cacheIfNotCached |
| TokenCacheContext.kt | 113 | `cached(cacheKey, solution)` | Any | ValueCacheContext 接口 |
| TokenCacheContext.kt | 117 | `cached(cacheKey, fixedValues)` | Any | ValueCacheContext 接口 |
| TokenCacheContext.kt | 168, 175 | `solutionCache[cacheKey to solution]` | Any | getOrPut 内部 |
| TokenCacheContext.kt | 179, 186 | `fixedValueCache[cacheKey to fixedValues]` | Any | getOrPut 内部 |

---

### 2.2 LinearFlatten 缓存读取（cachedLinearFlatten/cachedLinearFlattenValue）

| 文件 | 行号 | 调用 | Key类型 | 上下文 |
|------|------|------|---------|--------|
| **Symbol key（主路径）** |||||
| TokenTable.kt | 365, 369 | `cachedLinearFlatten/Value(cacheKey)` | Any | Token 实现 |
| TokenTable.kt | 563, 567 | `cachedLinearFlatten/Value(cacheKey)` | Any | MutableTokenTable 实现 |
| TokenTable.kt | 847, 853 | `cachedLinearFlatten/Value(cacheKey)` | Any | ConcurrentTokenTable 实现 |
| TokenTable.kt | 1193, 1199 | `cachedLinearFlatten/Value(cacheKey)` | Any | ConcurrentMutableTokenTable 实现 |
| **Private key（Polynomial内部）** |||||
| Polynomial.kt | 543 | `cachedLinearFlattenValue(flattenCacheKey)` | Private | AbstractLinearPolynomial.range getter |
| Polynomial.kt | 558 | `cachedLinearFlatten(flattenCacheKey)` | Private | AbstractLinearPolynomial.cached getter |

---

### 2.3 QuadraticFlatten 缓存读取（cachedQuadraticFlatten/cachedQuadraticFlattenValue）

| 文件 | 行号 | 调用 | Key类型 | 上下文 |
|------|------|------|---------|--------|
| **Symbol key（主路径）** |||||
| TokenTable.kt | 382, 386 | `cachedQuadraticFlatten/Value(cacheKey)` | Any | Token 实现 |
| TokenTable.kt | 580, 584 | `cachedQuadraticFlatten/Value(cacheKey)` | Any | MutableTokenTable 实现 |
| TokenTable.kt | 872, 878 | `cachedQuadraticFlatten/Value(cacheKey)` | Any | ConcurrentTokenTable 实现 |
| TokenTable.kt | 1218, 1224 | `cachedQuadraticFlatten/Value(cacheKey)` | Any | ConcurrentMutableTokenTable 实现 |
| **Private key（Polynomial内部）** |||||
| Polynomial.kt | 1029 | `cachedQuadraticFlattenValue(flattenCacheKey)` | Private | AbstractQuadraticPolynomial.range getter |
| Polynomial.kt | 1044 | `cachedQuadraticFlatten(flattenCacheKey)` | Private | AbstractQuadraticPolynomial.cached getter |

---

### 2.4 Range 缓存读取（cachedRange/cachedRangeValue）

| 文件 | 行号 | 调用 | Key类型 | 上下文 |
|------|------|------|---------|--------|
| **Symbol key（主路径）** |||||
| TokenTable.kt | 181, 185 | `cachedRange/Value(cacheKey)` | Any | AbstractTokenTable 接口 |
| TokenTable.kt | 399, 403 | `cachedRange/Value(cacheKey)` | Any | Token 实现 |
| TokenTable.kt | 597, 601 | `cachedRange/Value(cacheKey)` | Any | MutableTokenTable 实现 |
| TokenTable.kt | 897, 903 | `cachedRange/Value(cacheKey)` | Any | ConcurrentTokenTable 实现 |
| TokenTable.kt | 1243, 1249 | `cachedRange/Value(cacheKey)` | Any | ConcurrentMutableTokenTable 实现 |
| **Private key（Polynomial内部）** |||||
| Polynomial.kt | 523, 524 | `cachedRangeValue(rangeCacheKey)` | Private | AbstractLinearPolynomial.range getter |
| Polynomial.kt | 590, 591 | `cachedRangeValue(rangeCacheKey)` | Private | AbstractLinearPolynomial.flush |
| Polynomial.kt | 1001, 1002 | `cachedRangeValue(rangeCacheKey)` | Private | AbstractQuadraticPolynomial.range getter |
| Polynomial.kt | 1096 | `cachedRangeValue(rangeCacheKey)` | Private | AbstractQuadraticPolynomial.flush |

---

## 3. 清理点（Clear/Remove）

### 3.1 单点清理（clearLinearFlatten/clearQuadraticFlatten/clearRange/clearValue）

| 文件 | 行号 | 调用 | Key类型 | 上下文 |
|------|------|------|---------|--------|
| **Symbol key（主路径）** |||||
| TokenTable.kt | 161, 378, 576, 866, 1212 | `clearLinearFlatten(cacheKey)` | Any | 四类TokenTable实现 |
| TokenTable.kt | 177, 395, 593, 891, 1237 | `clearQuadraticFlatten(cacheKey)` | Any | 四类TokenTable实现 |
| TokenTable.kt | 193, 412, 610, 916, 1262 | `clearRange(cacheKey)` | Any | 四类TokenTable实现 |
| TokenTable.kt | 197, 416, 614, 922, 1268 | `clearValue(cacheKey)` | Any | 四类TokenTable实现（B1新增） |
| **重绑清理（B1新增）** |||||
| TokenCacheContext.kt | 279 | `oldTokenTable.clearLinearFlatten(symbol)` | Symbol | bindTokenTableContext 重绑 |
| TokenCacheContext.kt | 280 | `oldTokenTable.clearQuadraticFlatten(symbol)` | Symbol | bindTokenTableContext 重绑 |
| TokenCacheContext.kt | 281 | `oldTokenTable.clearRange(symbol)` | Symbol | bindTokenTableContext 重绑 |
| TokenCacheContext.kt | 282 | `oldTokenTable.clearValue(symbol)` | Symbol | bindTokenTableContext 重绑 |
| **Private key（Polynomial内部）** |||||
| Polynomial.kt | 598 | `clearLinearFlatten(flattenCacheKey)` | Private | AbstractLinearPolynomial.flush |
| Polynomial.kt | 592 | `clearRange(rangeCacheKey)` | Private | AbstractLinearPolynomial.flush |
| Polynomial.kt | 1097 | `clearRange(rangeCacheKey)` | Private | AbstractQuadraticPolynomial.flush |
| Polynomial.kt | 1099 | `clearQuadraticFlatten(flattenCacheKey)` | Private | AbstractQuadraticPolynomial.flush |

---

### 3.2 批量清理（clearAll/clearFlatten/clearValue/clearRange）

| 文件 | 行号 | 调用 | 上下文 |
|------|------|------|--------|
| TokenCacheContext.kt | 244 | `clearLinearFlatten()` | TokenCacheContexts 方法定义 |
| TokenCacheContext.kt | 248 | `clearQuadraticFlatten()` | TokenCacheContexts 方法定义 |
| TokenCacheContext.kt | 252 | `clearFlatten()` | 合并清理 linear + quadratic |
| TokenCacheContext.kt | 254 | `clearQuadraticFlatten()` [内部调用] | clearFlatten 内部 |
| TokenCacheContext.kt | 257 | `clearValue()` | TokenCacheContexts 方法定义 |
| TokenCacheContext.kt | 261 | `clearRange()` | TokenCacheContexts 方法定义 |
| TokenCacheContext.kt | 265-268 | `clearAll()` | 全量清理 |
| TokenTable.kt | 338 | `cacheContexts.clearAll()` | Token.flush() |
| TokenTable.kt | 446 | `cacheContexts.clearAll()` | Token.close() |
| TokenTable.kt | 536 | `cacheContexts.clearAll()` | MutableTokenTable.flush() |
| TokenTable.kt | 644 | `cacheContexts.clearAll()` | MutableTokenTable.close() |
| TokenTable.kt | 807 | `cacheContexts.clearAll()` | ConcurrentTokenTable.flush() |
| TokenTable.kt | 975 | `cacheContexts.clearAll()` | ConcurrentTokenTable.close() |
| TokenTable.kt | 1153 | `cacheContexts.clearAll()` | ConcurrentMutableTokenTable.flush() |
| TokenTable.kt | 1321 | `cacheContexts.clearAll()` | ConcurrentMutableTokenTable.close() |

---

### 3.3 符号移除清理（remove(symbol)）

| 文件 | 行号 | 调用 | 上下文 |
|------|------|------|--------|
| MetaModel.kt | 435, 436 | `tokens.remove(symbol)` | MetaModel.remove 调用 |
| TokenTable.kt | 319 | `remove(symbol)` | AbstractMutableTokenTable 接口定义 |
| **MutableTokenTable（B1新增）** |||
| TokenTable.kt | 521-531 | `remove(symbol)` | 四类缓存清理 + unbind |
| TokenTable.kt | 527 | `unbindTokenTableContext(symbol, this)` | 解绑上下文 |
| TokenTable.kt | 528 | `cacheContexts.linearFlatten.remove(symbol)` | 清理 linear flatten |
| TokenTable.kt | 529 | `cacheContexts.quadraticFlatten.remove(symbol)` | 清理 quadratic flatten |
| TokenTable.kt | 530 | `cacheContexts.range.remove(symbol)` | 清理 range |
| TokenTable.kt | 531 | `cacheContexts.value.remove(symbol)` | 清理 value（B1新增） |
| **ConcurrentMutableTokenTable（B1新增）** |||
| TokenTable.kt | 1135-1146 | `remove(symbol)` | synchronized + 四类缓存清理 |
| TokenTable.kt | 1142 | `unbindTokenTableContext(symbol, this)` | 解绑上下文 |
| TokenTable.kt | 1143-1146 | `cacheContexts.*.remove(symbol)` | 清理四类缓存 |
| TokenCacheContext.kt | 289 | `symbolTokenTableContext.remove(symbol)` | unbindTokenTableContext 内部 |

---

## 4. 绑定/解绑点（Bind/Unbind）

| 文件 | 行号 | 调用 | 上下文 |
|------|------|------|--------|
| **绑定（bindTokenTableContext）** |||
| TokenCacheContext.kt | 276 | `bindTokenTableContext(symbol, tokenTable)` | 函数定义 |
| TokenCacheContext.kt | 279-282 | `oldTokenTable.clear*(symbol)` | 重绑时清理旧表（B1新增） |
| TokenTable.kt | 655 | `bindTokenTableContext(symbol, this)` | cacheSymbolContext 内部 |
| **解绑（unbindTokenTableContext）** |||
| TokenCacheContext.kt | 287 | `unbindTokenTableContext(symbol, tokenTable)` | 函数定义 |
| TokenCacheContext.kt | 289 | `symbolTokenTableContext.remove(symbol)` | 解绑实现 |
| TokenTable.kt | 448 | `unbindTokenTableContext(symbol, this)` | Token.close() |
| TokenTable.kt | 527 | `unbindTokenTableContext(symbol, this)` | MutableTokenTable.remove()（B1新增） |
| TokenTable.kt | 646 | `unbindTokenTableContext(symbol, this)` | MutableTokenTable.close() |
| TokenTable.kt | 979 | `unbindTokenTableContext(symbol, this)` | ConcurrentTokenTable.close() |
| TokenTable.kt | 1142 | `unbindTokenTableContext(symbol, this)` | ConcurrentMutableTokenTable.remove()（B1新增） |
| TokenTable.kt | 1325 | `unbindTokenTableContext(symbol, this)` | ConcurrentMutableTokenTable.close() |
| **查询（boundTokenTableContext）** |||
| TokenCacheContext.kt | 293 | `boundTokenTableContext(symbol)` | 函数定义 |
| Polynomial.kt | 516 | `boundTokenTableContext(it)` | AbstractLinearPolynomial 查询绑定表 |
| Polynomial.kt | 994 | `boundTokenTableContext(it)` | AbstractQuadraticPolynomial 查询绑定表 |

---

## 5. 缓存失效点（flush）

| 文件 | 行号 | 调用 | 上下文 |
|------|------|------|--------|
| **TokenTable.flush()（触发 clearAll）** |||
| TokenTable.kt | 337 | `override fun flush()` | Token 实现 |
| TokenTable.kt | 338 | `cacheContexts.clearAll()` | 清理全部缓存 |
| TokenTable.kt | 534 | `override fun flush()` | MutableTokenTable 实现 |
| TokenTable.kt | 535 | `tokenList.flush()` | 清理 tokenList |
| TokenTable.kt | 536 | `cacheContexts.clearAll()` | 清理全部缓存 |
| TokenTable.kt | 805 | `override fun flush()` | ConcurrentTokenTable 实现 |
| TokenTable.kt | 807 | `cacheContexts.clearAll()` | 清理全部缓存 |
| TokenTable.kt | 1150 | `override fun flush()` | ConcurrentMutableTokenTable 实现 |
| TokenTable.kt | 1152 | `tokenList.flush()` | 清理 tokenList |
| TokenTable.kt | 1153 | `cacheContexts.clearAll()` | 清理全部缓存 |
| **MetaModel.flush()（触发 tokens.flush()）** |||
| MetaModel.kt | 464 | `tokens.flush()` | MetaModel.flush 调用 |
| **Polynomial.flush()（Private key 清理）** |||
| Polynomial.kt | 588 | `override fun flush(force)` | AbstractLinearPolynomial 实现 |
| Polynomial.kt | 592 | `clearRange(rangeCacheKey)` | Private key 清理 |
| Polynomial.kt | 598 | `clearLinearFlatten(flattenCacheKey)` | Private key 清理 |
| Polynomial.kt | 1094 | `override fun flush(force)` | AbstractQuadraticPolynomial 实现 |
| Polynomial.kt | 1097 | `clearRange(rangeCacheKey)` | Private key 清理 |
| Polynomial.kt | 1099 | `clearQuadraticFlatten(flattenCacheKey)` | Private key 清理 |
| **Symbol.flush()（IntermediateSymbol）** |||
| IntermediateSymbol.kt | 378 | `override fun flush(force)` | ExpressionSymbol 实现 |
| **FunctionSymbol.flush()** |||
| FunctionSymbol.kt | 158 | `override fun flush(force)` | MathFunctionSymbol 空实现 |
| If.kt | 120 | `override fun flush(force)` | 空实现 |
| Masking.kt | 295 | `override fun flush(force)` | 空实现 |
| **Monomial.flush()** |||
| LinearMonomial.kt | 353 | `override fun flush(force)` | LinearMonomial 实现 |
| QuadraticMonomial.kt | 805 | `override fun flush(force)` | QuadraticMonomial 实现 |

---

## 6. 注册链路（Register）

### 6.1 同步注册链路（MutableTokenTable）

| 文件 | 行号 | 调用 | 上下文 |
|------|------|------|--------|
| TokenTable.kt | 684 | `cache(emptySymbols)` | 空符号 value 预热 |
| TokenTable.kt | 684 | `cacheSymbolContexts(emptySymbols)` | 空符号 flatten/range 预热 |
| TokenTable.kt | 717-719 | `prepareAndCache(null/fixedValues)` | FunctionSymbol value 预热（单符号）⚠️ |
| TokenTable.kt | 735-747 | `cache(symbols=map)` | **B2新增：批量 value 预热** |
| TokenTable.kt | 756 | `cacheSymbolContexts(readySymbols)` | 批量 flatten/range 预热 |

**B2修复前**：单符号 + 批量 flatten/range 预热各一次（重复）
**B2修复后**：移除单符号 flatten/range 重复调用

⚠️ **兼容性保留点**：同步链路保留 `prepareAndCache()` + 批量 `cache(symbols=...)`，函数符号 value 预热存在重复计算风险。
- `prepareAndCache()` 内部调用 `cache(cacheKey=symbol)` [单符号]
- 批量 `cache(symbols=...)` 再次写入同一 symbol 的 value 缓存
- 并发链路仅使用批量预热，无重复
- 当前行为：后写入覆盖前写入，功能一致但有冗余计算
- 建议在 C6 收口时统一为批量路径

---

### 6.2 并发注册链路（ConcurrentMutableTokenTable）

| 文件 | 行号 | 调用 | 上下文 |
|------|------|------|--------|
| TokenTable.kt | 1346 | `cache(emptySymbols)` | 空符号 value 预热 |
| TokenTable.kt | 1346 | `cacheSymbolContexts(emptySymbols)` | 空符号 flatten/range 预热 |
| TokenTable.kt | 1391-1418 | `cache(symbols=map)` | 批量 value 预热 |
| TokenTable.kt | 1427 | `cacheSymbolContexts(thisReadSymbol)` | 分批 flatten/range 预热 |
| TokenTable.kt | 1462 | `cacheSymbolContexts(readySymbols)` | 单批 flatten/range 预热 |
| TokenTable.kt | 1504 | `cacheSymbolContexts(readySymbols)` | 单线程 fallback |

---

## 7. Key 类型说明

### 7.1 Symbol key（主路径）

- **来源**：`IntermediateSymbol` 实例
- **写入时机**：
  - 符号注册时 `cacheSymbolContext(symbol)`
  - 符号求值时 `prepareAndCache()` 内部
- **清理时机**：
  - `remove(symbol)` 清理四类缓存
  - `flush()` 触发 `clearAll()` 清理全部
  - 重绑时 `bindTokenTableContext` 清理旧表缓存

### 7.2 Private key（Polynomial内部）

- **来源**：`flattenCacheKey = "__linear_polynomial_flatten_cache__..."`、`rangeCacheKey = "__polynomial_range_cache__..."`
- **写入时机**：Polynomial.range getter 内部
- **清理时机**：Polynomial.flush(force) 内部
- **管理范围**：不在 `remove(symbol)` 清理范围内，仅 `clearAll()` 时清理
- **C6终态**：删除 Polynomial.kt 后自动消失

---

## 8. 统计汇总

### 8.1 统计规则说明

**统计口径定义**（避免歧义）:

| 统计项 | 统计规则 | 说明 |
|--------|----------|------|
| **写入点** | 实际调用点（不含接口定义） | 排除 `fun cacheLinearFlatten(...)` 接口声明，仅统计调用 |
| **读取点** | 实际调用点（含内部实现） | 含 `getOrPut` 内部读取，不含接口定义 |
| **清理点（单点）** | 实际调用点 + 重绑清理 | 含四类TokenTable实现调用、bind重绑清理 |
| **清理点（批量）** | 实际调用点（不含方法定义） | 仅统计 `clearAll()` 等实际调用，不含函数定义行 |
| **移除点** | remove(symbol) 实现体调用 | 含缓存清理 + unbind 调用 |
| **绑定/解绑** | 实际调用点 | 含 bind/unbind/bound 三类 |
| **失效点** | 实际调用点 + clearAll 调用 | 含 TokenTable.flush 内的 clearAll 调用 |

**3.2 批量清理统计说明**:
- 表格展开项含函数定义（如 `TokenCacheContext.kt:244 clearLinearFlatten()` 定义）
- 统计汇总仅计实际调用（如 `TokenTable.kt:338 cacheContexts.clearAll()`）
- 定义行不计入统计，仅作文档参考

### 8.2 统计汇总表

| 分类 | Symbol key | Private key | 总计 |
|------|------------|-------------|------|
| **写入点** | 12 | 4 | 16 |
| **读取点** | 40 | 8 | 48 |
| **清理点（单点）** | 24 | 4 | 28 |
| **清理点（批量）** | 10 | 0 | 10 |
| **移除点** | 8 | 0 | 8 |
| **绑定/解绑** | 12 | 0 | 12 |
| **失效点（flush）** | 4 | 4 | 8 |

---

## 9. C3 收口策略

### 9.1 当前状态

- **Symbol key**：由 TokenTable 统一管理，remove/flush 清理完整 ✅
- **Private key**：仅 Polynomial 内部使用，C6 删除后自动消失 ⏳

### 9.2 C3 临时方案

- `remove(symbol)` 仅清理 Symbol key ✅（B1已实现）
- `flush()` 清理全部（包括 Private key）✅
- Private key 不在 remove 管理范围，不影响 C3 目标

### 9.3 C6 终态

- 删除 Polynomial.kt 后 Private key 消失
- 仅保留 Symbol key 单路径
- 缓存归属完全统一到 TokenCacheContexts