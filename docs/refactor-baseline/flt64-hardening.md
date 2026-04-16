# C0-2: core 主链路 Flt64/Double 固化点清单

**生成日期**: 2026-04-16
**扫描范围**: ospf-kotlin-core/src/main

---

## 概览

本清单记录 core 主链路中 `Flt64` 或 `Double` 类型固化的位置，用于后续泛型化改造。

### 统计摘要

| 类别 | 数量 | 可移除 | 必要边界 |
|------|------|--------|----------|
| 类定义固化 | 0 | 0 | 0 |
| 函数签名固化 | ~70 | ~50 | ~20 |
| 类型转换逻辑 | ~15 | ~10 | ~5 |

---

## 详细清单

### 1. 必要边界点 (保留)

这些固化点是求解器/数值计算的必要边界，不应移除：

#### Token.kt (变量结果存储)

**路径**: `variable/Token.kt`

| 位置 | 代码 | 行号 | 原因 |
|------|------|------|------|
| `__result` | `internal var __result: Flt64?` | 14-15 | 变量求解结果存储 |
| `random()` | `fun random(rng: Random): Flt64` | 47 | 随机值生成 |

**判定**: ✅ **保留** - 变量结果必须为数值类型

#### Cell.kt (单元格计算)

**路径**: `intermediate_model/Cell.kt`

| 位置 | 代码 | 行号 | 原因 |
|------|------|------|------|
| `LinearCell.coefficient` | `val coefficient: Flt64` | 15 | 单元格系数 |
| `LinearCell.evaluate()` | `fun evaluate(): Flt64?` | 8-10, 18-28 | 求值结果 |
| `QuadraticCell.coefficient` | `val coefficient: Flt64` | 43 | 单元格系数 |
| `QuadraticCell.evaluate()` | `fun evaluate(): Flt64?` | 47-69 | 求值结果 |

**判定**: ✅ **保留** - MechanismModel 内核基于 Token+Cell，系数/结果必须为数值

#### Constraint.kt (约束 RHS)

**路径**: `intermediate_model/Constraint.kt`

| 位置 | 代码 | 行号 | 原因 |
|------|------|------|------|
| `rhs` | `val rhs: Flt64` | 53, 79, 136 | 约束右侧值 |

**判定**: ✅ **保留** - 约束 RHS 必须为数值

#### SolverConfig/SolverOutput (求解器配置)

**路径**: `solver/config/SolverConfig.kt`, `solver/output/*.kt`

| 位置 | 代码 | 行号 | 原因 |
|------|------|------|------|
| `SolverConfig.gap` | `val gap: Flt64` | 21 | 求解器参数 |
| `SolverConfig.improveThreshold` | `val improveThreshold: Flt64` | 23 | 求解器参数 |
| `SolverOutput.obj` | `val obj: Flt64` | 24 | 求解结果 |
| `SolverOutput.bestBound` | `val bestBound: Flt64?` | 16-17 | 求解结果 |
| `SolvingStatus.*` | 多个 Flt64 字段 | 22-25, 29-30 | 求解状态 |

**判定**: ✅ **保留** - 求解器边界，数值类型合理

#### IIS (不可约不一致集)

**路径**: `solver/iis/*.kt`

| 位置 | 代码 | 行号 | 原因 |
|------|------|------|------|
| `QuadraticBound.lowerBound/upperBound` | `Flt64?` | 148-149 | 边界值 |
| `IISConfig.slackTolerance` | `Flt64` | 23 | 容差参数 |

**判定**: ✅ **保留** - IIS 分析需要数值精度

---

### 2. 主链路固化点 (需评估)

这些固化点位于主计算链路，需要评估是否可泛型化：

#### IntermediateSymbol.kt (中间符号)

**路径**: `intermediate_symbol/IntermediateSymbol.kt`

| 位置 | 代码 | 行号 | 评估 |
|------|------|------|------|
| `prepare()` 返回 | `Flt64?` | 79, 261, 290, 314, 698, 1000 | ⚠️ 符号预求值 |
| `evaluate()` 返回 | `Flt64?` | 390, 394, 403, 411, 421, 433, 1036, 1045, 1055 | ⚠️ 符号求值 |
| `calculateValue()` 返回 | `Flt64?` | 1065-1067 | ⚠️ 符号计算 |
| `Double` 参数 | `constant: Double` | 575, 878 | ⚠️ 常量参数 |

**评估**: 主链路符号求值返回 `Flt64?`，可能需要保持泛型 `V`

#### Expression.kt (表达式)

**路径**: `intermediate_model/Expression.kt`

| 位置 | 代码 | 行号 | 评估 |
|------|------|------|------|
| `evaluate()` 返回 | `Flt64?` | 30-31, 38-39, 47-48 | ⚠️ 表达式求值 |

**评估**: 与 IntermediateSymbol 类似，可能需要泛型化

#### FlattenUtility.kt (展平工具)

**路径**: `intermediate_symbol/flatten/FlattenUtility.kt`

| 位置 | 代码 | 行号 | 评估 |
|------|------|------|------|
| `constant` 参数 | `Flt64` | 30, 55, 72, 111 | ⚠️ 常量参数 |
| `mergedMonomials` 值 | `Flt64.zero` | 38, 94 | ⚠️ 合并逻辑 |

**评估**: 展平逻辑涉及系数合并，可能需要泛型化

---

### 3. 可移除固化点 (优先清理)

这些固化点可以改为泛型或移除：

#### CallBackModelInterface.kt (回调模型)

**路径**: `model/callback/CallBackModelInterface.kt`

| 位置 | 代码 | 行号 | 建议 |
|------|------|------|------|
| `defaultObjective` | `Flt64` | 87 | 改为泛型 `V` |
| `objectiveValue()` 返回 | `Flt64` | 94 | 改为泛型 `V` |
| `objectiveValue(obj)` 参数/返回 | `Flt64` | 98 | 改为泛型 `V` |
| `operation(lhs, rhs)` 参数/返回 | `Flt64` | 102 | 改为泛型 `V` |

**建议**: 回调模型接口应支持泛型 `V`

#### CallBackModel.kt

**路径**: `model/callback/CallBackModel.kt`

| 位置 | 代码 | 行号 | 建议 |
|------|------|------|------|
| `compareObjective(lhs, rhs)` 参数 | `Flt64` | 207, 211 | 改为泛型 `V` |

#### ModelBuildingStatus.kt

**路径**: `model/status/ModelBuildingStatus.kt`

| 位置 | 代码 | 行号 | 建议 |
|------|------|------|------|
| `progress` | `Flt64` | 13 | 可改为 Double 或保留 |

#### MultiObject.kt

**路径**: `model/MultiObject.kt`

| 位置 | 代码 | 行号 | 建议 |
|------|------|------|------|
| `weight` | `Flt64` | 8 | 可改为泛型 `V` |

#### Gap.kt

**路径**: `solver/Gap.kt`

| 位置 | 代码 | 行号 | 建议 |
|------|------|------|------|
| `gap(obj, possibleBestObj)` 参数/返回 | `Flt64` | 5 | 求解器边界，保留 |

---

### 4. heuristic (启发式算法)

**路径**: `solver/heuristic/*.kt`

| 文件 | 固化点 | 建议 |
|------|--------|------|
| `Mutation.kt` | `mutationRate: Flt64` | ⚠️ 保留，算法参数 |
| `MutationMode.kt` | `mutationRate: Flt64?` | ⚠️ 保留，算法参数 |
| `Selection.kt` | `truncationThreshold`, `initialTemperature`, `decayRate` | ⚠️ 保留，算法参数 |
| `Policy.kt` | `value: Flt64` 参数 | ⚠️ 评估是否需要泛型 |

**建议**: 启发式算法参数使用数值类型合理，保留

---

### 5. 类型转换逻辑

**路径**: `solver/value/SolveValueConversionContext.kt`

| 位置 | 代码 | 行号 | 原因 |
|------|------|------|------|
| `toSolverDouble()` | `fun Flt64.toSolverDouble(): Double` | 24 | 求解器适配边界 |

**判定**: ✅ **保留** - plugin 边界转换

---

## 固化点分类总结

### A 类: 必要边界 (保留)

- Token 结果存储
- Cell 系数与求值
- Constraint RHS
- Solver 配置与输出
- IIS 参数与结果
- Plugin 边界转换

### B 类: 主链路求值 (需评估)

- IntermediateSymbol 求值
- Expression 求值
- FlattenUtility 合并

### C 类: 可泛型化 (优先清理)

- CallBackModel 接口
- MultiObject.weight
- ModelBuildingStatus.progress (可选)

---

## 验收命令

```bash
# 扫描主链路 Flt64/Double 固化
grep -r ": Flt64\|: Double\|<Flt64>\|<Double>" \
  ospf-kotlin-core/src/main --include="*.kt" \
  | grep -v "solver/" | grep -v "Cell.kt" | grep -v "Constraint.kt"
```

---

## 下一步

进入 **C2 阶段**: 泛型化贯通 + plugin 边界下沉