# MechanismModel 边界现状分析（2026-04-16）

> C4-1 边界现状分析报告，为 C4 阶段规划提供基线

---

## 1. MetaModel → MechanismModel 转换入口分析

### 1.1 主入口分布

**单入口模式，但内部有分支**

| 入口类型 | 文件 | 行号 | 触发条件 |
|----------|------|------|----------|
| Linear 入口 | MechanismModel.kt | 106-211 | `LinearMechanismModelOf.Companion.invoke()` |
| Quadratic 入口 | MechanismModel.kt | 508-620 | `QuadraticMechanismModelOf.Companion.invoke()` |

### 1.2 内部分支逻辑

```
invoke() 入口
    │
    ├── concurrent = false
    │       └── 同步顺序处理
    │
    ├── concurrent = true + blocking = true
    │       └── runBlocking { dumpAsync() }
    │
    └── concurrent = true + blocking = false
            └── coroutineScope { dumpAsync() }
```

**关键发现**：
- 无多入口并存问题（单入口模式）
- 但 `concurrent` 配置导致内部分化为同步/异步两个分支
- 两分支逻辑相似，只是执行方式不同

---

## 2. MechanismModel 内部类型使用

### 2.1 Cell 类型层次

```
Cell 层次结构
├── LinearCell (Cell.kt:134)
│       ├── LinearConstraintCell
│       └── LinearObjectiveCell
│
└── QuadraticCell (Cell.kt:135)
        ├── QuadraticConstraintCell
        └── QuadraticObjectiveCell
```

### 2.2 与 math.symbol 的转换点

| 转换点 | 输入类型 | 输出类型 | 文件 | 行号 |
|--------|----------|----------|------|------|
| **LinearConstraint 创建** | `MathLinearInequality` | `LinearConstraint` | Constraint.kt | 112-144 |
| **QuadraticConstraint 创建** | `MathQuadraticInequality` | `QuadraticConstraint` | Constraint.kt | 169-207 |
| **LinearSubObject 创建** | `LinearFlattenData` | `LinearSubObject` | SubObject.kt | 81-101 |
| **QuadraticSubObject 创建** | `QuadraticFlattenData` | `QuadraticSubObject` | SubObject.kt | 156-181 |

### 2.3 FlattenData 类型定义

| 类型别名 | 泛型定义 | 文件位置 |
|----------|----------|----------|
| `LinearFlattenData` | `LinearFlattenDataOf<Flt64>` | TokenCacheContext.kt |
| `QuadraticFlattenData` | `QuadraticFlattenDataOf<Flt64>` | TokenCacheContext.kt |

**内部使用**: `UtilsLinearMonomial<T>`, `UtilsQuadraticMonomial<T>` (来自 math.symbol)

---

## 3. Linear/Quadratic 边界分析

### 3.1 接口继承关系

```
MechanismModelOf<V> (sealed interface)
    ↑
AbstractLinearMechanismModelOf<V> (interface)
    ↑
SingleObjectMechanismModelOf<V> (interface)
    ↑
LinearMechanismModelOf<V> (class)
    │
    └── AbstractQuadraticMechanismModelOf<V> (interface)
            ↑
            QuadraticMechanismModelOf<V> (class)
```

### 3.2 边界清晰度评估

| 方面 | 状态 | 说明 |
|------|------|------|
| 接口隔离 | 较清晰 | Linear/Quadratic 有明确的父子关系 |
| 实现类分离 | 清晰 | 两个独立类 |
| 类型参数 | Phase 1 兼容模式 | 使用 `Of<V>` 泛型，内部仍用 Flt64 |
| 转换逻辑 | **有重复** | Quadratic 需处理 Linear 约束转换 |

### 3.3 Quadratic 处理 Linear 的问题

**文件**: MechanismModel.kt:788-803, 806-820

```kotlin
// QuadraticMechanismModel 需同时处理 Linear 和 Quadratic 约束
val linearConstraints = metaModel.constraints
    .filter { it is LinearInequality && it.category != Quadratic }
    .map { ... }

val quadraticConstraints = metaModel.constraints
    .filter { it is LinearInequality && it.category == Quadratic }
    .map { (it as LinearInequality).toQuadraticInequality() }
    .map { QuadraticConstraint.invoke(it, tokens, ...) }
```

**问题**: 增加了 QuadraticMechanismModel 的复杂度

---

## 4. 重复逻辑分析

### 4.1 问题点清单

| 优先级 | 问题 | 文件 | 行号 | 重复代码量 |
|--------|------|------|------|-----------|
| **P1** | LinearInequality 重复转换 | MechanismModel.kt | 794-803, MetaModel.kt:1152-1157 | ~20行 |
| **P2** | dumpAsync 逻辑重复 | MechanismModel.kt | 214-328 vs 622-731 | ~100行 |
| **P3** | Constraint 创建逻辑相似 | Constraint.kt | 112-144 vs 169-207 | ~30行 |
| **P4** | SubObject 创建逻辑重复 | SubObject.kt | 81-101 vs 156-181 | ~50行 |
| **P5** | Triad/Tetrad dump 重复 | LinearTriadModel.kt, QuadraticTetradModel.kt | 大量 | ~200行 |

### 4.2 P1 详细分析

**重复位置**:
- MetaModel.kt:1152-1157 (LinearInequality → QuadraticInequality)
- MechanismModel.kt:794-803 (LinearInequality → QuadraticConstraint)

**重复逻辑**:
```kotlin
// 两处都有类似逻辑
.filter { it is LinearInequality }
.map { (it as LinearInequality).toQuadraticInequality() }
```

### 4.3 P2 详细分析

**重复位置**:
- LinearMechanismModelOf.dumpAsync (line 214-328)
- QuadraticMechanismModelOf.dumpAsync (line 622-731)

**重复结构**:
```kotlin
// 两版本都有相同的流程
suspend fun dumpAsync(...) {
    // 1. 分段处理 constraints
    // 2. 分段处理 subObjects
    // 3. 合并结果
}
```

### 4.4 P3/P4 详细分析

**Constraint 创建模式**:
```kotlin
// Linear 版本
monomials.forEach { monomial ->
    val token = tokens.find(monomial.symbol)
    cells.add(LinearConstraintCell(token, monomial.coefficient))
}

// Quadratic 版本
monomials.forEach { monomial ->
    val token1 = tokens.find(monomial.symbol1)
    val token2 = monomial.symbol2?.let { tokens.find(it) }
    cells.add(QuadraticConstraintCell(token1, token2, monomial.coefficient))
}
```

**差异**: 仅在 token 数量（单 vs 双）和 Cell 类型

---

## 5. 文件清单

| 文件 | 行数 | 主要内容 | 问题点 |
|------|------|----------|--------|
| MechanismModel.kt | 857 | Linear/Quadratic MechanismModel 定义 | P1, P2 |
| MetaModel.kt | 1344 | 前端模型，包含转换逻辑 | P1 |
| Constraint.kt | ~300 | Constraint 创建逻辑 | P3 |
| SubObject.kt | ~200 | SubObject 创建逻辑 | P4 |
| LinearTriadModel.kt | 2182 | Linear 三元组模型 | P5 |
| QuadraticTetradModel.kt | 1496 | Quadratic 四元组模型 | P5 |

---

## 6. 转换入口分布图（ASCII）

```
┌─────────────────────────────────────────────────────────────────────────┐
│                        MetaModel → MechanismModel 转换全景图               │
└─────────────────────────────────────────────────────────────────────────┘

[MetaModel 层 - frontend]
├── LinearMetaModelOf<V>
│       │
│       └── LinearMechanismModelOf.Companion.invoke()
│               │
│               ├── [同步分支]
│               │       ├── 直接创建 constraints
│               │       └── 直接创建 subObjects
│               │
│               └── [异步分支 - dumpAsync()]
│                       ├── coroutineScope { dumpConstraintsAsync() }
│                       │       └── 分段处理 constraints (并行)
│                       └── coroutineScope { dumpSubObjectsAsync() }
│                               └── 分段处理 subObjects (并行)
│
└── QuadraticMetaModelOf<V>
        │
        └── QuadraticMechanismModelOf.Companion.invoke()
                │
                ├── [同步分支]
                │       ├── 处理 LinearInequality → QuadraticConstraint ⚠️ P1
                │       ├── 处理 QuadraticInequality → QuadraticConstraint
                │       └── 处理 subObjects
                │
                └── [异步分支 - dumpAsync()]
                        ├── dumpConstraintsAsync()
                        │       ├── 处理 LinearInequality ⚠️ P1
                        │       └── 处理 QuadraticInequality
                        └── dumpSubObjectsAsync()

═════════════════════════════════════════════════════════════════════════

[MechanismModel 层 - intermediate]
│
├── LinearMechanismModelOf<V>
│       ├── constraints: List<LinearConstraint>
│       ├── subObjects: List<LinearSubObject>
│       └── dumpAsync() ⚠️ P2 (与 Quadratic 版本重复)
│
└── QuadraticMechanismModelOf<V>
        ├── constraints: List<QuadraticConstraint>
        │       ├── 包含 Linear 转换来的 ⚠️ P1
        │       └── 包含原生 Quadratic
        ├── subObjects: List<QuadraticSubObject>
        └── dumpAsync() ⚠️ P2 (与 Linear 版本重复)

═════════════════════════════════════════════════════════════════════════

[Constraint 创建层]
│
├── LinearConstraint.invoke (Constraint.kt:112-144) ⚠️ P3
│       ├── 输入: MathLinearInequality
│       ├── 遍历 monomials
│       └── 创建 LinearConstraintCell
│
└── QuadraticConstraint.invoke (Constraint.kt:169-207) ⚠️ P3
        ├── 输入: MathQuadraticInequality
        ├── 遍历 monomials
        └── 创建 QuadraticConstraintCell (token1, token2)

[SubObject 创建层]
│
├── LinearSubObject.invoke (SubObject.kt:81-101) ⚠️ P4
│       ├── 输入: LinearFlattenData
│       └── 遍历 flattenData.monomials
│
└── QuadraticSubObject.invoke (SubObject.kt:156-181) ⚠️ P4
        ├── 输入: QuadraticFlattenData
        └── 遍历 flattenData.monomials
```

---

## 7. C4 阶段规划建议

### 7.1 立即处理（C4 阶段）

| 问题 | 建议方案 |
|------|----------|
| P1 | 提取 `LinearInequality.toQuadraticConstraint()` 扩展函数 |
| P2 | 创建 `AbstractMechanismModelOf` 处理通用 dumpAsync 逻辑 |
| P3 | 提取 `processMonomials()` / `processQuadraticMonomials()` 工厂函数 |
| P4 | 使用 P3 工厂函数简化 SubObject 创建 |

### 7.2 后续处理（C5 阶段）

| 问题 | 建议方案 |
|------|----------|
| P5 | Triad/Tetrad 模型统一 |

### 7.3 终态处理（C6 阶段）

| 问题 | 说明 |
|------|------|
| Polynomial.kt 删除 | Private cache key 消失 |
| Expression.kt 删除 | 缓存归属统一 |

---

## 8. 文档签署

| 角色 | 签署 |
|------|------|
| **C4-1 分析人** | Claude Code Agent |
| **审核人** | 用户 |
| **日期** | 2026-04-16 |