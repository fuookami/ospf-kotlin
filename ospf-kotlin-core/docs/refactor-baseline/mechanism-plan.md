# C4 MechanismModel 边界规划（2026-04-16）

> 基于 C4-1 边界现状分析，规划 MechanismModel 边界统一实施步骤

---

## 1. 边界现状分析总结

### 1.1 转换入口分布

**当前状态：单入口模式，内部有分支**

```
MetaModel (frontend)
    │
    ├── LinearMetaModelOf<V>
    │       └── LinearMechanismModelOf.Companion.invoke()
    │               ├── [同步分支] 直接创建
    │               └── [异步分支] dumpAsync()
    │
    └── QuadraticMetaModelOf<V>
            └── QuadraticMechanismModelOf.Companion.invoke()
                    ├── [同步分支] 直接创建
                    └── [异步分支] dumpAsync()
```

**关键发现**：
- 无多入口并存问题
- 但内部根据 `concurrent` 配置分化为同步/异步分支
- QuadraticMechanismModel 需同时处理 Linear 和 Quadratic 约束（增加复杂度）

---

### 1.2 Token/Cell 类型使用

| 层次 | Linear 类型 | Quadratic 类型 | 文件位置 |
|------|------------|---------------|----------|
| Constraint Cell | `LinearConstraintCell` | `QuadraticConstraintCell` | Constraint.kt |
| Objective Cell | `LinearObjectiveCell` | `QuadraticObjectiveCell` | LinearTriadModel.kt:35, QuadraticTetradModel.kt:35 |
| Mechanism Cell | `LinearCell` | `QuadraticCell` | Cell.kt:134-135 |

**与 math.symbol 转换点**：

| 转换点 | 文件 | 行号 | 输入类型 | 输出类型 |
|--------|------|------|----------|----------|
| LinearConstraint 创建 | Constraint.kt | 112-144 | `MathLinearInequality` | `LinearConstraint` |
| QuadraticConstraint 创建 | Constraint.kt | 169-207 | `MathQuadraticInequality` | `QuadraticConstraint` |
| LinearSubObject 创建 | SubObject.kt | 81-101 | `LinearFlattenData` | `LinearSubObject` |
| QuadraticSubObject 创建 | SubObject.kt | 156-181 | `QuadraticFlattenData` | `QuadraticSubObject` |

---

### 1.3 问题点清单（优先级排序）

| 优先级 | 问题 | 文件 | 行号 | 影响 | C4 处理策略 |
|--------|------|------|------|------|-------------|
| **P1** | LinearInequality 重复转换 | MechanismModel.kt | 794-803 | Quadratic 模型需处理 Linear 约束转换 | 提取公共转换函数 |
| **P2** | dumpAsync 逻辑重复 | MechanismModel.kt | 214-328, 622-731 | Linear/Quadratic 版本高度相似 | 提取 AbstractMechanismModel |
| **P3** | Constraint 创建逻辑相似 | Constraint.kt | 112-207 | monomial 遍历逻辑可通用化 | 提取 Cell 工厂函数 |
| **P4** | SubObject 创建逻辑重复 | SubObject.kt | 81-181 | flattenData.monomials 遍历相似 | 提取通用处理函数 |
| **P5** | Triad/Tetrad dump 重复 | LinearTriadModel.kt, QuadraticTetradModel.kt | 大量 | 约束处理代码重复 | C5 阶段处理 |

---

## 2. C4 实施目标

### 2.1 目标定义

| 目标 | 描述 | 验收标准 |
|------|------|----------|
| **统一边界入口** | 确保 MetaModel → MechanismModel 转换路径清晰 | 无重复转换逻辑 |
| **简化转换链路** | 减少 Linear/Quadratic 分支的代码重复 | 重复代码量降低 50%+ |
| **明确职责边界** | MechanismModel 仅负责 math → core 转换 | 无数学层逻辑渗透 |

### 2.2 非目标（本次不处理）

| 非目标 | 原因 |
|--------|------|
| Triad/Tetrad 模型重构 | C5 阶段处理 |
| 删除 Polynomial/Expression | C6 阶段处理 |
| 全量测试通过 | C2 泛型化遗留阻塞 |

---

## 3. 实施步骤

### 3.1 C4-2: 提取公共转换函数（P1）

**问题**: QuadraticMechanismModel 中 LinearInequality → QuadraticInequality 转换重复

**当前代码** (MechanismModel.kt:794-803):
```kotlin
// LinearInequality 转换逻辑重复出现
val quadraticConstraints = metaModel.constraints
    .filter { it is LinearInequality }
    .map { (it as LinearInequality).toQuadraticInequality() }
```

**解决方案**: 提取为公共扩展函数

```kotlin
// 在 MetaModel.kt 或 Constraint.kt 中添加
fun LinearInequality.toQuadraticConstraint(
    tokens: AbstractTokenTable,
    name: String = this.name
): QuadraticConstraint {
    val quadraticInequality = this.toQuadraticInequality()
    return QuadraticConstraint.invoke(quadraticInequality, tokens, name)
}
```

**影响范围**: MechanismModel.kt:794-803, MetaModel.kt:1152-1157

**预估工时**: 0.5h

---

### 3.2 C4-3: 提取 AbstractMechanismModel（P2）

**问题**: LinearMechanismModel.dumpAsync 和 QuadraticMechanismModel.dumpAsync 逻辑高度重复

**当前代码对比**:
- LinearMechanismModel.dumpAsync (line 214-328)
- QuadraticMechanismModel.dumpAsync (line 622-731)

**解决方案**: 提取抽象基类处理通用 dumpAsync 逻辑

```kotlin
// 新增 AbstractMechanismModel.kt
abstract class AbstractMechanismModelOf<V : Value<V>>(
    val constraints: List<Constraint>,
    val subObjects: List<SubObject>,
    ...
) {
    protected abstract fun createConstraint(...)
    protected abstract fun createSubObject(...)

    suspend fun dumpAsync(
        tokens: AbstractTokenTable,
        ...
    ): MechanismModelResult {
        // 通用 dumpAsync 逻辑
        val constraintsDeferred = constraints.mapAsync { ... }
        val subObjectsDeferred = subObjects.mapAsync { ... }
        ...
    }
}
```

**影响范围**: MechanismModel.kt 全文件（857 行）

**预估工时**: 2h

---

### 3.3 C4-4: 提取 Cell 工厂函数（P3）

**问题**: Constraint 创建逻辑相似，monomial 遍历逻辑可通用化

**当前代码** (Constraint.kt:112-144):
```kotlin
// LinearConstraint.invoke
monomials.forEach { monomial ->
    val token = tokens.find(monomial.symbol)
    cells.add(LinearConstraintCell(token, monomial.coefficient))
}
```

**当前代码** (Constraint.kt:169-207):
```kotlin
// QuadraticConstraint.invoke
monomials.forEach { monomial ->
    val token1 = tokens.find(monomial.symbol1)
    val token2 = monomial.symbol2?.let { tokens.find(it) }
    cells.add(QuadraticConstraintCell(token1, token2, monomial.coefficient))
}
```

**解决方案**: 提取通用 monomial 处理函数

```kotlin
// 在 Cell.kt 中添加
fun <T> processMonomials(
    monomials: List<UtilsLinearMonomial<T>>,
    tokens: AbstractTokenTable,
    cellFactory: (Token, T) -> LinearCell
): List<LinearCell> {
    return monomials.map { monomial ->
        val token = tokens.find(monomial.symbol)
        cellFactory(token, monomial.coefficient)
    }
}

fun <T> processQuadraticMonomials(
    monomials: List<UtilsQuadraticMonomial<T>>,
    tokens: AbstractTokenTable,
    cellFactory: (Token, Token?, T) -> QuadraticCell
): List<QuadraticCell> {
    return monomials.map { monomial ->
        val token1 = tokens.find(monomial.symbol1)
        val token2 = monomial.symbol2?.let { tokens.find(it) }
        cellFactory(token1, token2, monomial.coefficient)
    }
}
```

**影响范围**: Constraint.kt, SubObject.kt

**预估工时**: 1h

---

### 3.4 C4-5: 提取 SubObject 通用处理（P4）

**问题**: SubObject 创建逻辑重复

**解决方案**: 使用 C4-4 提取的 Cell 工厂函数

```kotlin
// SubObject.kt 简化
operator fun invoke(
    category: ObjectCategory,
    flattenData: LinearFlattenData,
    tokens: AbstractTokenTable,
    name: String
): LinearSubObject {
    val cells = processMonomials(
        flattenData.monomials,
        tokens,
        { token, coef -> LinearCell(token, coef) }
    )
    return LinearSubObject(category, cells, name)
}
```

**影响范围**: SubObject.kt (81-181)

**预估工时**: 0.5h

---

## 4. 实施优先级

| 步骤 | 内容 | 优先级 | 预估工时 | 阻塞依赖 |
|------|------|--------|----------|----------|
| C4-2 | 提取公共转换函数 | P1 | 0.5h | 无 |
| C4-4 | 提取 Cell 工厂函数 | P3 | 1h | 无 |
| C4-5 | 提取 SubObject 通用处理 | P4 | 0.5h | 依赖 C4-4 |
| C4-3 | 提取 AbstractMechanismModel | P2 | 2h | 依赖 C4-2, C4-4, C4-5 |

**总预估工时**: 4h

---

## 5. 验收标准

| 标准 | 验证方法 |
|------|----------|
| 主代码编译通过 | `mvn -pl ospf-kotlin-core compile -q` |
| 无重复转换逻辑 | grep 搜索 LinearInequality 转换点仅一处 |
| dumpAsync 逻辑统一 | AbstractMechanismModel 覆盖 Linear/Quadratic |
| 文档交付 | mechanism-boundary.md, mechanism-plan.md |

---

## 6. 后续步骤

| 阶段 | 内容 | 状态 |
|------|------|------|
| C4-2 | 提取公共转换函数 | ⏳ 待执行 |
| C4-4 | 提取 Cell 工厂函数 | ⏳ 待执行 |
| C4-5 | 提取 SubObject 通用处理 | ⏳ 待执行 |
| C4-3 | 提取 AbstractMechanismModel | ⏳ 待执行 |
| C4-6 | 验收交付 | ⏳ 待前序步骤完成 |

---

## 7. 文档签署

| 角色 | 签署 |
|------|------|
| **C4 规划人** | Claude Code |
| **审核人** | 用户 |
| **日期** | 2026-04-16 |