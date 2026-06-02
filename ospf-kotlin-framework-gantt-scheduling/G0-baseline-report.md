# Phase G0 — Flt64 基线扫描报告

日期：2026-06-02（修订）

## 1. 总览

扫描范围：`ospf-kotlin-framework-gantt-scheduling` 全部 Kotlin 源文件（`*/main/*`）。

扫描命令（可复现）：

```bash
# Kotlin 文件总数
find ospf-kotlin-framework-gantt-scheduling -name "*.kt" -path "*/main/*" | wc -l

# Flt64 残留总行数
find ospf-kotlin-framework-gantt-scheduling -name "*.kt" -path "*/main/*" -exec grep -c "Flt64" {} + | awk -F: '{s+=$NF} END {print s+0}'

# Solver 模型泄漏总行数
find ospf-kotlin-framework-gantt-scheduling -name "*.kt" -path "*/main/*" -exec grep -cE "AbstractLinearMetaModel<Flt64>|LinearMetaModel<Flt64>|MetaModel<Flt64>|AbstractMetaModel<Flt64>|LinearIntermediate.*Flt64" {} + | awk -F: '{s+=$NF} END {print s+0}'

# 各子模块 Flt64 残留
for dir in ospf-kotlin-framework-gantt-scheduling/gantt-scheduling-*/; do
  name=$(basename "$dir")
  count=$(find "$dir" -name "*.kt" -path "*/main/*" -exec grep -c "Flt64" {} + | awk -F: '{s+=$NF} END {print s+0}')
  echo "$name: $count"
done
```

| 指标 | 数值 |
|------|------|
| Kotlin 文件总数 | **129** |
| Flt64 残留总行数 | **1251** |
| Solver 模型泄漏总行数 | **347** |
| 领域数量 Map<*, Flt64> | **12** |
| 返回 Flt64 的领域函数 | **2** |

## 2. 各子模块 Flt64 残留统计

| 子模块 | Flt64 行数 | 占比 |
|--------|-----------|------|
| gantt-scheduling-domain-task-compilation-context | 420 | 33.6% |
| gantt-scheduling-domain-bunch-compilation-context | 178 | 14.2% |
| gantt-scheduling-domain-resource-context | 173 | 13.8% |
| gantt-scheduling-domain-produce-context | 170 | 13.6% |
| gantt-scheduling-application | 125 | 10.0% |
| gantt-scheduling-domain-capacity-scheduling-context | 102 | 8.2% |
| gantt-scheduling-infrastructure | 50 | 4.0% |
| gantt-scheduling-domain-bunch-generation-context | 18 | 1.4% |
| gantt-scheduling-domain-task-context | 15 | 1.2% |
| gantt-scheduling-domain-task-generation-context | 0 | 0.0% |
| **合计** | **1251** | **100%** |

## 3. 各子模块 Solver 模型泄漏统计

| 子模块 | 泄漏行数 | 占比 |
|--------|---------|------|
| gantt-scheduling-domain-task-compilation-context | 161 | 46.4% |
| gantt-scheduling-domain-bunch-compilation-context | 59 | 17.0% |
| gantt-scheduling-domain-produce-context | 41 | 11.8% |
| gantt-scheduling-domain-resource-context | 39 | 11.2% |
| gantt-scheduling-application | 30 | 8.6% |
| gantt-scheduling-domain-capacity-scheduling-context | 17 | 4.9% |
| gantt-scheduling-domain-task-context | 0 | 0.0% |
| gantt-scheduling-domain-bunch-generation-context | 0 | 0.0% |
| gantt-scheduling-domain-task-generation-context | 0 | 0.0% |
| gantt-scheduling-infrastructure | 0 | 0.0% |
| **合计** | **347** | **100%** |

## 4. G1 已完成的泛型化类型

以下类型已被另一个会话部分泛型化，有 `V : RealNumber<V>` 和 `Flt64` typealias，且当前编译通过：

| 类型 | 泛型签名 | Flt64 typealias |
|------|---------|----------------|
| CostItem | `CostItem<V : RealNumber<V>>` | `Flt64CostItem` |
| Cost | `Cost<V : RealNumber<V>>` | `Flt64Cost` |
| MutableCost | `MutableCost<V : RealNumber<V>>` | `Flt64MutableCost` |
| ImmutableCost | `ImmutableCost<V : RealNumber<V>>` | `Flt64ImmutableCost` |
| CapacityColumn | `CapacityColumn<E, A, V : RealNumber<V>>` | `Flt64CapacityColumn<E, A>` |
| SlotBasedCapacityResult | `SlotBasedCapacityResult<A, M, R, V>` | `Flt64SlotBasedCapacityResult<A, M, R>` |
| CapacityIntermediateValues | `CapacityIntermediateValues<A, M, R, V>` | `Flt64CapacityIntermediateValues<A, M, R>` |
| SlotConstraints | `SlotConstraints<M, R, V>` | `Flt64SlotConstraints<M, R>` |
| AbstractTaskBunch | `AbstractTaskBunch<..., V : RealNumber<V>>` | `TaskBunch<E, A>`（裸 Flt64 别名） |

## 5. G1 未完成项

以下属于 G1 范围但尚未完成：

| 项 | 当前残留 | 位置 |
|----|---------|------|
| 领域数量 Map<*, Flt64> | 12 处 | CapacityActionProduce.kt、ProductionTask.kt、BunchCapacitySchedulingProduce.kt、CapacitySchedulingResourceUsage.kt、Produce.kt、Consumption.kt、StorageResource.kt、Resource.kt |
| 返回 Flt64 的领域函数 | 2 处 | Iteration.kt、BranchAndPriceAlgorithm.kt 中的 reduced cost / shadow price |
| ShadowPriceMap | 仍固定 `Map<T, Flt64>`，未泛型化为 `Map<T, V>` | ShadowPriceMap.kt |

### 5.1 Map<*, Flt64> 完整清单

```
gantt-scheduling-domain-capacity-scheduling-context/.../CapacityActionProduce.kt:  val quantity: Map<P, Flt64>
gantt-scheduling-domain-capacity-scheduling-context/.../CapacityActionProduce.kt:  val lessQuantity: Map<P, Flt64>
gantt-scheduling-domain-capacity-scheduling-context/.../CapacityActionProduce.kt:  val overQuantity: Map<P, Flt64>
gantt-scheduling-domain-capacity-scheduling-context/.../BunchCapacitySchedulingProduce.kt:  val quantity: Map<P, Flt64>
gantt-scheduling-domain-capacity-scheduling-context/.../BunchCapacitySchedulingProduce.kt:  val lessQuantity: Map<P, Flt64>
gantt-scheduling-domain-capacity-scheduling-context/.../BunchCapacitySchedulingProduce.kt:  val overQuantity: Map<P, Flt64>
gantt-scheduling-domain-capacity-scheduling-context/.../CapacitySchedulingResourceUsage.kt:  val usage: Map<C, Flt64>
gantt-scheduling-domain-capacity-scheduling-context/.../CapacitySchedulingResourceUsage.kt:  val overQuantity: Map<C, Flt64>
gantt-scheduling-domain-capacity-scheduling-context/.../CapacitySchedulingResourceUsage.kt:  val lessQuantity: Map<C, Flt64>
gantt-scheduling-domain-produce-context/.../Produce.kt:  val quantity: Map<P, Flt64>
gantt-scheduling-domain-produce-context/.../Consumption.kt:  val quantity: Map<C, Flt64>
gantt-scheduling-domain-resource-context/.../StorageResource.kt:  val quantity: HashMap<ResourceTimeSlot<R, C>, Flt64>
```

## 6. 允许保留 Flt64 的文件清单

以下文件因处于 solver adapter 边界或算法内部状态管理，允许保留 Flt64（后续可包进 `SchedulingModelBoundary<V>` / `SchedulingSolverValueAdapter<V>`）：

| 文件 | 理由 |
|------|------|
| `application/service/bunch/BranchAndPriceAlgorithm.kt` | solver 模型创建和列生成算法边界 |
| `application/service/task/BranchAndPriceAlgorithm.kt` | 同上（task 版本） |
| `application/model/bunch/Iteration.kt` | 算法迭代状态 |
| `application/model/task/Iteration.kt` | 同上（task 版本） |
| `bunch-compilation/service/SlotBasedCapacityPreSolver.kt` | 预求解器直接操作 solver 模型 |
| `bunch-compilation/service/BunchSolutionAnalyzer.kt` | 从 solver 模型中抽取解 |
| `bunch-compilation/service/TaskSolutionAnalyzer.kt` | 同上 |
| `task-compilation/service/SolutionAnalyzer.kt` | 同上 |

## 7. 门禁命令

后续每个 Phase 完成后，运行以下命令对比残留数：

```bash
# Flt64 总残留
find ospf-kotlin-framework-gantt-scheduling -name "*.kt" -path "*/main/*" -exec grep -c "Flt64" {} + | awk -F: '{s+=$NF} END {print "Flt64 total:", s+0}'

# Solver 模型泄漏
find ospf-kotlin-framework-gantt-scheduling -name "*.kt" -path "*/main/*" -exec grep -cE "AbstractLinearMetaModel<Flt64>|LinearMetaModel<Flt64>|MetaModel<Flt64>|AbstractMetaModel<Flt64>|LinearIntermediate.*Flt64" {} + | awk -F: '{s+=$NF} END {print "Solver leak total:", s+0}'

# 领域 Map<*, Flt64>
grep -rn "Map<.*Flt64>\|HashMap<.*Flt64>\|MutableMap<.*Flt64>" --include="*.kt" ospf-kotlin-framework-gantt-scheduling/ | grep "/main/" | grep -v "import\|typealias" | wc -l
```

基线值（本报告）：**Flt64 = 1251 行，Solver 泄漏 = 347 行，Map<*, Flt64> = 12 处**。
