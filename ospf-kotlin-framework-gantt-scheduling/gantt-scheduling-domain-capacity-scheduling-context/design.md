# Capacity Scheduling Context Design

# 产能调度上下文设计

## Overview / 概述

This module provides a generic framework for capacity scheduling, supporting both discrete and continuous production actions with time-slot-based scheduling and optional in-slot
ordering.

本模块提供通用的产能调度框架，支持离散型和连续型生产动作，基于时隙调度，并可选支持时隙内顺序控制。

---

## Core Concepts / 核心概念

### Production Action / 生产动作

A production action represents a way to produce capacity. It can be:

- **Discrete**: Has fixed batch duration, decision variable represents batch count
- **Continuous**: No fixed batch duration, decision variable represents duration units

生产动作表示产能的生产方式，分为：

- **离散型**：有固定批次时长，决策变量表示批次数
- **连续型**：无固定批次时长，决策变量表示时长单位数

### Time Slot / 时隙

Uses `TimeSlot` interface from `infrastructure` module, defined by:

- Time range
- Duration

使用 `infrastructure` 模块的 `TimeSlot` 接口，由时间范围和时长定义。

---

## Capacity Interface / Capacity 抽象接口

```kotlin
/**
 * 产能编译抽象接口
 * Capacity Compilation Abstract Interface
 *
 * 提供统一的产能计算接口，用于约束和目标函数
 * Provides unified capacity calculation interface for constraints and objectives
 */
interface Capacity<A : ProductionAction> {
    /**
     * 动作-时隙的操作时间
     * Operation time per action-slot
     *
     * 表示每个动作在每个时隙分配的产能（时长）
     * Represents capacity (duration) allocated to each action in each time slot
     */
    val operationTime: LinearIntermediateSymbols2  // [action, slot] -> duration

    /**
     * 设备-时隙的总产能
     * Total capacity per executor-slot
     *
     * 表示每个设备在每个时隙的总产能（时长）
     * Represents total capacity (duration) per executor in each time slot
     */
    val capacity: LinearIntermediateSymbols2  // [executor, slot] -> duration

    /**
     * 解析解
     * Extract solution from model
     */
    fun extractSolution(model: AbstractLinearMetaModel): Ret<CapacitySchedulingSolution<A>>
}
```

---

## Three Compilation Classes / 三个编译类

### Class Overview / 类概览

| Class                          | Purpose                           | Variable Dimensions                                   |
|--------------------------------|-----------------------------------|-------------------------------------------------------|
| `CapacityCompilation`          | Non-column generation, no order   | `x[action, slot]` 2D                                  |
| `CapacityOrderCompilation`     | Non-column generation, with order | `x[action, slot, order]` 3D                           |
| `IterativeCapacityCompilation` | Column generation master problem  | `x[executor][iteration, columnIndex]` 2D per executor |

| 类                              | 用途       | 变量维度                                        |
|--------------------------------|----------|---------------------------------------------|
| `CapacityCompilation`          | 非列生成，无顺序 | `x[action, slot]` 二维                        |
| `CapacityOrderCompilation`     | 非列生成，有顺序 | `x[action, slot, order]` 三维                 |
| `IterativeCapacityCompilation` | 列生成主问题   | `x[executor][iteration, columnIndex]` 每设备二维 |

---

### CapacityCompilation (No Order) / 无顺序

```kotlin
/**
 * 产能编译决策对象（无顺序）
 * Capacity Compilation Decision Object (No Order)
 */
class CapacityCompilation<A : ProductionAction>(
    private val actions: List<A>,
    private val slots: List<TimeSlot>,
    private val timeWindow: TimeWindow
) : Capacity<A> {
    
    /**
     * 二维整型变量
     * 2D integer variable
     * x[action, slot] -> amount
     */
    lateinit var x: UIntVariable2
    
    override lateinit var operationTime: LinearIntermediateSymbols2  // [action, slot]
    override lateinit var capacity: LinearIntermediateSymbols2       // [executor, slot]
    
    fun register(model: LinearMetaModel): Try
    
    override fun extractSolution(model: AbstractLinearMetaModel): Ret<CapacitySchedulingSolution<A>>
}
```

---

### CapacityOrderCompilation (With Order) / 有顺序

```kotlin
/**
 * 产能编译决策对象（有顺序）
 * Capacity Compilation Decision Object (With Order)
 */
class CapacityOrderCompilation<A : ProductionAction>(
    private val actions: List<A>,
    private val slots: List<TimeSlot>,
    private val timeWindow: TimeWindow,
    private val maxOrderPerSlot: UInt64
) : Capacity<A> {
    
    /**
     * 三维整型变量
     * 3D integer variable
     * x[action, slot, order] -> amount
     */
    lateinit var x: UIntVariable3
    
    /**
     * 三维二元变量（顺序占用标记）
     * 3D binary variable for order occupation
     */
    lateinit var b: BinVariable3
    
    override lateinit var operationTime: LinearIntermediateSymbols2  // [action, slot]
    override lateinit var capacity: LinearIntermediateSymbols2       // [executor, slot]
    
    fun register(model: LinearMetaModel): Try
    
    override fun extractSolution(model: AbstractLinearMetaModel): Ret<CapacitySchedulingSolution<A>>
}
```

---

### IterativeCapacityCompilation (Column Generation) / 列生成

```kotlin
/**
 * 迭代产能编译决策对象（列生成主问题）
 * Iterative Capacity Compilation Decision Object (Column Generation Master Problem)
 */
class IterativeCapacityCompilation<E : Executor, A : ProductionAction>(
    private val executors: List<E>,
    private val actions: List<A>,
    private val slots: List<TimeSlot>,
    private val timeWindow: TimeWindow
) : Capacity<A> {
    
    /**
     * 每台设备的列聚合（按迭代分组）
     * Column aggregation per executor (grouped by iteration)
     */
    internal val columnsByExecutor: Map<E, CapacityColumnAggregation<E, A>>
    
    /**
     * 每台设备的二维整型变量
     * 2D integer variables per executor
     * x[executor][iteration, columnIndexInIteration] -> amount
     */
    lateinit var x: Map<E, UIntVariable2>
    
    override lateinit var operationTime: LinearIntermediateSymbols2  // [action, slot]
    override lateinit var capacity: LinearIntermediateSymbols2       // [executor, slot]
    
    fun register(model: MetaModel): Try
    
    suspend fun addColumns(
        iteration: UInt64,
        newColumns: List<CapacityColumn<E, A>>,
        model: AbstractLinearMetaModel
    ): Ret<List<CapacityColumn<E, A>>>
    
    override fun extractSolution(model: AbstractLinearMetaModel): Ret<CapacitySchedulingSolution<A>>
}
```

---

## Column Generation Support / 列生成支持

### Column Definition / 列定义

```kotlin
/**
 * 产能列
 * Capacity Column
 *
 * 一个产能列代表某台设备在某个时隙某个顺序位置的完整分配方案
 * A column represents a complete allocation plan for an executor at a specific slot and order
 */
data class CapacityColumn<E : Executor, A : ProductionAction>(
    val executor: E,
    val slotIndex: Int,
    val order: Int,
    val allocations: Map<A, UInt64>,  // Action allocations / 动作分配
    val cost: Flt64                    // Column cost / 列成本
)
```

### Column Aggregation / 列聚合

```kotlin
/**
 * 列聚合（按迭代分组）
 * Column Aggregation (grouped by iteration)
 */
class CapacityColumnAggregation<E : Executor, A : ProductionAction> {
    /**
     * 按迭代分组的列
     * Columns grouped by iteration
     */
    val columnsIteration: List<List<CapacityColumn<E, A>>> = ArrayList()
    
    /**
     * 所有列（扁平化）
     * All columns (flattened)
     */
    val columns: List<CapacityColumn<E, A>>
    
    /**
     * 最新迭代的列
     * Columns from last iteration
     */
    val lastIterationColumns: List<CapacityColumn<E, A>>
    
    /**
     * 添加新列
     * Add new columns
     */
    fun addColumns(iteration: UInt64, newColumns: List<CapacityColumn<E, A>>): List<CapacityColumn<E, A>>
}
```

### Variable Structure Illustration / 变量结构图示

```
设备 executor_1:
  iteration 0: x[0, 0]  x[0, 1]  x[0, 2]  ...  (初始列 / Initial columns)
  iteration 1: x[1, 0]  x[1, 1]           ...  (第1轮生成的列 / Columns from iteration 1)
  iteration 2: x[2, 0]                    ...  (第2轮生成的列 / Columns from iteration 2)
  ...

设备 executor_2:
  iteration 0: x[0, 0]  x[0, 1]  ...
  iteration 1: x[1, 0]  x[1, 1]  x[1, 2]  ...
  ...
```

### Column Generation Workflow / 列生成流程

```
┌─────────────────────────────────────────────────────────┐
│                    主问题 (Master Problem)                │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐       │
│  │ 设备1的列    │ │ 设备2的列    │ │ 设备3的列    │ ...   │
│  │ x₁[i, j]    │ │ x₂[i, j]    │ │ x₃[i, j]    │       │
│  └─────────────┘ └─────────────┘ └─────────────┘       │
│                        ↓ Solve / 求解                    │
│               Shadow Prices (π) / 影子价格              │
└─────────────────────────────────────────────────────────┘
                         ↓
┌─────────────────────────────────────────────────────────┐
│   子问题 (Subproblem) - Handled by other contexts       │
│   子问题 - 由其他上下文处理 (e.g., capacity-generation-context) │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐       │
│  │ 设备1子问题  │ │ 设备2子问题  │ │ 设备3子问题  │ ...   │
│  │ 生成新列    │ │ 生成新列    │ │ 生成新列    │       │
│  └─────────────┘ └─────────────┘ └─────────────┘       │
└─────────────────────────────────────────────────────────┘
                         ↓
               New columns added to master problem
                    新列加入主问题
```

---

## Order Constraints / 顺序约束

**Note**: Order constraints only apply to `CapacityOrderCompilation`.

**注意**：顺序约束仅适用于 `CapacityOrderCompilation`。

### Uniqueness Constraint / 唯一性约束

Each order position can have at most one action with non-zero allocation:

每个顺序位置最多只能有一个动作不为 0：

```
Σ_action b[action, slot, order] ≤ 1
```

### Linking Constraint / 关联约束

Binary variable `b` is linked to integer variable `x`:

二元变量 `b` 与整型变量 `x` 关联：

```
x[action, slot, order] ≥ b[action, slot, order]
x[action, slot, order] ≤ M × b[action, slot, order]
```

### Order Constraint Illustration / 顺序约束图示

```
Time Slot t:
  order 0: [action_1: x=3, b=1]  [action_2: x=0, b=0]  [action_3: x=0, b=0]  Σb=1 ✓
  order 1: [action_1: x=0, b=0]  [action_2: x=2, b=1]  [action_3: x=0, b=0]  Σb=1 ✓
  order 2: [action_1: x=0, b=0]  [action_2: x=0, b=0]  [action_3: x=1, b=1]  Σb=1 ✓

Execution order: action_1 -> action_2 -> action_3
```

---

## Directory Structure / 目录结构

```
gantt-scheduling-domain-capacity-scheduling-context/src/main/fuookami/ospf/kotlin/framework/gantt_scheduling/domain/capacity_scheduling/
├── CapacitySchedulingContext.kt          # Context interface / 上下文接口
├── Aggregation.kt                         # Aggregation classes / 聚合类
├── model/
│   ├── ProductionAction.kt               # Production action interface / 生产动作接口
│   ├── Capacity.kt                        # Capacity abstract interface / Capacity抽象接口
│   ├── CapacityCompilation.kt            # No order compilation / 无顺序编译
│   ├── CapacityOrderCompilation.kt       # With order compilation / 有顺序编译
│   ├── IterativeCapacityCompilation.kt   # Column generation / 列生成编译
│   ├── CapacityColumn.kt                 # Capacity column / 产能列
│   ├── CapacityColumnAggregation.kt      # Column aggregation / 列聚合
│   └── CapacitySchedulingSolution.kt      # Solution result / 解析结果
└── service/
    └── limits/
        ├── OrderConstraint.kt             # Order constraint / 顺序约束
        ├── ExecutorCapacityConstraint.kt  # Executor capacity constraint / 设备产能约束
        └── CapacityCostMinimization.kt    # Cost minimization / 成本最小化目标
```

---

## Core Classes / 核心类

### ProductionAction Interface / 生产动作接口

```kotlin
interface ProductionAction {
    /**
     * 动作唯一标识
     * Unique identifier for the action
     */
    val id: String

    /**
     * 动作名称
     * Name of the action
     */
    val name: String
    val displayName: String get() = name

    /**
     * 执行器
     * The executor that performs this action
     */
    val executor: Executor

    /**
     * 是否为离散型动作
     * Whether the action is discrete
     *
     * - true: discrete, x represents batch count
     * - false: continuous, x represents duration units
     *
     * - true: 离散型，x 表示批次数
     * - false: 连续型，x 表示时长单位数
     */
    val discrete: Boolean

    /**
     * 批次时长（仅离散型有效）
     * Batch duration (only for discrete actions)
     */
    val batchDuration: Duration? get() = null

    /**
     * 每个单位 x 对应的产能
     * Unit capacity per x value
     *
     * @param timeWindow Time window / 时间窗口
     * @return Unit capacity as Flt64 / 单位产能
     */
    fun unitCapacity(timeWindow: TimeWindow): Flt64

    /**
     * 单位成本
     * Unit cost
     *
     * @param time Time instant / 时间点
     * @return Unit cost / 单位成本
     */
    fun unitCost(time: Instant): Flt64

    /**
     * x 变量的上界
     * Upper bound for x variable
     *
     * @param slot Time slot / 时隙
     * @param timeWindow Time window / 时间窗口
     * @return Upper bound value / 上界值
     */
    fun upperBound(slot: TimeSlot, timeWindow: TimeWindow): UInt64
}
```

### OrderConstraint / 顺序约束

```kotlin
/**
 * 顺序约束（仅用于 CapacityOrderCompilation）
 * Order Constraint (only for CapacityOrderCompilation)
 */
class OrderConstraint<A : ProductionAction>(
    private val compilation: CapacityOrderCompilation<A>,
    private val actions: List<A>,
    private val slots: List<TimeSlot>,
    private val maxOrderPerSlot: UInt64,
    override val name: String = "order"
) : Pipeline<AbstractLinearMetaModel> {
    
    override fun invoke(model: AbstractLinearMetaModel): Try {
        val x = compilation.x
        val b = compilation.b
        
        for ((t, _) in slots.withIndex()) {
            for (o in 0u until maxOrderPerSlot.toUInt()) {
                // Constraint 1: Each order position has at most one action
                // 约束1: 每个顺序位置最多一个动作
                model.addConstraint(
                    constraint = sum(actions.indices.map { a -> b[a, t, o] }) leq Flt64.one,
                    name = "${name}_unique_${t}_$o"
                )
                
                for ((a, _) in actions.withIndex()) {
                    // Constraint 2: Link b and x
                    // 约束2: 关联 b 和 x
                    model.addConstraint(
                        constraint = x[a, t, o] geq b[a, t, o],
                        name = "${name}_link_lb_${a}_${t}_$o"
                    )
                    model.addConstraint(
                        constraint = x[a, t, o] leq Flt64(10000.0) * b[a, t, o],
                        name = "${name}_link_ub_${a}_${t}_$o"
                    )
                }
            }
        }
        
        return ok
    }
}
```

### ExecutorCapacityConstraint / 设备产能约束

```kotlin
/**
 * 设备产能约束（适用于所有 Capacity 实现）
 * Executor Capacity Constraint (works for all Capacity implementations)
 */
class ExecutorCapacityConstraint<A : ProductionAction>(
    private val capacity: Capacity<A>,
    private val slots: List<TimeSlot>,
    override val name: String = "executor_capacity"
) : Pipeline<AbstractLinearMetaModel> {
    
    override fun invoke(model: AbstractLinearMetaModel): Try {
        // Uses unified capacity interface
        // capacity.capacity[executor, slot] <= slot.duration
        return ok
    }
}
```

---

## Variable Semantics / 变量语义

### Discrete Action / 离散型动作

- `x` = batch count (批次数)
- Duration = `x × batchDuration`

### Continuous Action / 连续型动作

- `x` = duration units (时长单位数)
- Duration = `x × timeWindow.interval`

---

## Variable Summary / 变量总结

| Class                          | Variable        | Dimensions                 | Type                         | Description           |
|--------------------------------|-----------------|----------------------------|------------------------------|-----------------------|
| `CapacityCompilation`          | `x`             | `[action, slot]`           | `UIntVariable2`              | Allocation amount     |
| `CapacityOrderCompilation`     | `x`             | `[action, slot, order]`    | `UIntVariable3`              | Allocation amount     |
| `CapacityOrderCompilation`     | `b`             | `[action, slot, order]`    | `BinVariable3`               | Order occupation flag |
| `IterativeCapacityCompilation` | `x[executor]`   | `[iteration, columnIndex]` | `UIntVariable2`              | Allocation per column |
| All                            | `operationTime` | `[action, slot]`           | `LinearIntermediateSymbols2` | Operation time        |
| All                            | `capacity`      | `[executor, slot]`         | `LinearIntermediateSymbols2` | Total capacity        |

---

## Implementation Steps / 实施步骤

| Step | Task                                   | File                                    |
|------|----------------------------------------|-----------------------------------------|
| 1    | Create production action interface     | `model/ProductionAction.kt`             |
| 2    | Create capacity abstract interface     | `model/Capacity.kt`                     |
| 3    | Implement CapacityCompilation          | `model/CapacityCompilation.kt`          |
| 4    | Implement CapacityOrderCompilation     | `model/CapacityOrderCompilation.kt`     |
| 5    | Implement IterativeCapacityCompilation | `model/IterativeCapacityCompilation.kt` |
| 6    | Implement CapacityColumn               | `model/CapacityColumn.kt`               |
| 7    | Implement CapacityColumnAggregation    | `model/CapacityColumnAggregation.kt`    |
| 8    | Implement CapacitySchedulingSolution   | `model/CapacitySchedulingSolution.kt`   |
| 9    | Implement aggregation class            | `Aggregation.kt`                        |
| 10   | Implement context interface            | `CapacitySchedulingContext.kt`          |
| 11   | Implement order constraint             | `service/limits/OrderConstraint.kt`     |
| 12   | Implement other constraints            | `service/limits/*.kt`                   |

---

## Context Responsibilities / 上下文职责划分

| Context / 上下文                   | Responsibility / 职责                                                                              |
|---------------------------------|--------------------------------------------------------------------------------------------------|
| **capacity-scheduling-context** | Master problem modeling, adding columns, solving, extracting shadow prices / 主问题建模、添加列、求解、提取影子价格 |
| **capacity-generation-context** | Subproblem solving, generating new columns / 子问题求解、生成新列                                          |

---

## Relationship with Existing Modules / 与现有模块的关系

```
capacity-scheduling-context (new generic framework)
├── Defines Capacity interface with operationTime and capacity
├── Defines ProductionAction interface (discrete/continuous)
├── Three compilation classes for different scenarios
├── Supports column generation (master problem only)
└── Can be extended by specific business modules

capacity-generation-context (subproblem handling)
├── Generates new columns based on shadow prices
└── Uses capacity-scheduling-context's shadow prices

psp-domain-capacity-compilation-context (specific business implementation)
├── PulpingAssignment implements ProductionAction
├── PowerGenerationAssignment implements ProductionAction
└── Uses capacity-scheduling-context framework
```

---

## Class Inheritance Diagram / 类继承关系图

```
Capacity<A> (interface)
├── operationTime: [action, slot] -> duration
├── capacity: [executor, slot] -> duration
└── extractSolution(): CapacitySchedulingSolution<A>

    ├── CapacityCompilation<A> (无顺序 / No Order)
    │   └── x: [action, slot]
    │
    ├── CapacityOrderCompilation<A> (有顺序 / With Order)
    │   ├── x: [action, slot, order]
    │   └── b: [action, slot, order]
    │
    └── IterativeCapacityCompilation<E, A> (列生成 / Column Generation)
        └── x: Map<E, [iteration, columnIndex]>
```

---

## Integration with produce-context / 与 produce-context 的集成

### CapacityActionProduce Interface / 产能动作产量接口

```kotlin
/**
 * 支持 ProductionAction 的产量/消耗接口
 * Produce/Consumption interface that supports ProductionAction
 */
interface CapacityActionProduce<
    P : AbstractMaterial,
    C : AbstractMaterial
> {
    /**
     * 生产动作对应的产品产量（单位操作时间的产量）
     * Product produce per unit operation time
     */
    val produce: Map<P, Flt64>
    
    /**
     * 生产动作对应的原料消耗（单位操作时间的消耗）
     * Material consumption per unit operation time
     */
    val consumption: Map<C, Flt64>
}
```

### Extension Functions / 扩展函数

```kotlin
/**
 * 从 CapacityColumn 计算产量
 * Calculate produce from CapacityColumn
 */
fun <E : Executor, A : ProductionAction, P : AbstractMaterial> 
    CapacityColumn<E, A>.produce(product: P): Flt64

/**
 * 从 CapacityColumn 计算消耗
 * Calculate consumption from CapacityColumn
 */
fun <E : Executor, A : ProductionAction, C : AbstractMaterial> 
    CapacityColumn<E, A>.consumption(material: C): Flt64
```

### produce-context File Structure / 文件结构

```
domain/produce/model/
├── ProductionTask.kt                   # Material, Product, ProductionTask（已有）
├── CapacityActionProduce.kt            # CapacityActionProduce 接口（新增）
├── Produce.kt                          # Produce 接口
├── AbstractProduce.kt                  # 抽象基类
├── TaskSchedulingProduce.kt            # Task 调度实现
├── BunchSchedulingProduce.kt           # Bunch 调度实现
├── CapacitySchedulingProduce.kt        # Capacity 调度抽象基类（新增）
├── PlanCapacitySchedulingProduce.kt    # Plan 模式（新增）
└── BunchCapacitySchedulingProduce.kt   # Bunch 模式（新增）
```

### CapacitySchedulingProduce (Abstract Base) / 抽象基类

```kotlin
/**
 * 产能调度场景的产品产量管理抽象基类
 * Abstract base class for produce management in Capacity Scheduling
 */
abstract class CapacitySchedulingProduce<
    A : ProductionAction,
    P : AbstractMaterial,
    C : AbstractMaterial
>(
    products: List<Pair<P, MaterialDemand?>>,
    protected val actions: List<A>,
    protected val slots: List<TimeSlot>,
    protected val timeWindow: TimeWindow
) : AbstractProduce<ProductionTask<*, *, P, C>, *, *, P, C>(products) {
    
    override val overEnabled: Boolean = true
    override val lessEnabled: Boolean = true
    
    override lateinit var quantity: LinearExpressionSymbols1
    
    abstract fun register(model: LinearMetaModel): Try
}
```

### PlanCapacitySchedulingProduce / Plan 模式

```kotlin
/**
 * Plan 模式的产能调度产品产量管理
 * Plan-mode produce management for Capacity Scheduling
 */
class PlanCapacitySchedulingProduce<
    A : ProductionAction,
    P : AbstractMaterial,
    C : AbstractMaterial
>(
    products: List<Pair<P, MaterialDemand?>>,
    actions: List<A>,
    slots: List<TimeSlot>,
    timeWindow: TimeWindow
) : CapacitySchedulingProduce<A, P, C>(products, actions, slots, timeWindow) {
    
    override fun register(model: LinearMetaModel): Try {
        // 注册 quantity 变量
    }
    
    /**
     * 从 CapacityCompilation 绑定产量贡献
     * Bind produce contribution from CapacityCompilation
     */
    fun <Comp : Capacity<A>> bindCompilation(compilation: Comp): Try {
        for ((product, _) in products) {
            for (action in actions) {
                if (action is CapacityActionProduce<*, *>) {
                    val unitProduce = (action as CapacityActionProduce<P, *>).produce[product] ?: Flt64.zero
                    if (unitProduce neq Flt64.zero) {
                        for ((s, _) in slots.withIndex()) {
                            quantity[product].asMutable() += unitProduce * compilation.operationTime[action, s]
                        }
                    }
                }
            }
        }
        return ok
    }
}
```

### BunchCapacitySchedulingProduce / Bunch 模式（列生成）

```kotlin
/**
 * Bunch 模式的产能调度产品产量管理（支持列生成）
 * Bunch-mode produce management for Capacity Scheduling (with column generation)
 */
class BunchCapacitySchedulingProduce<
    E : Executor,
    A : ProductionAction,
    P : AbstractMaterial,
    C : AbstractMaterial
>(
    products: List<Pair<P, MaterialDemand?>>,
    actions: List<A>,
    slots: List<TimeSlot>,
    timeWindow: TimeWindow
) : CapacitySchedulingProduce<A, P, C>(products, actions, slots, timeWindow) {
    
    override fun register(model: LinearMetaModel): Try {
        // 注册 quantity 变量
    }
    
    /**
     * 从 IterativeCapacityCompilation 添加列贡献
     * Add column contribution from IterativeCapacityCompilation
     */
    suspend fun addColumns(
        iteration: UInt64,
        columns: List<CapacityColumn<E, A>>,
        compilation: IterativeCapacityCompilation<E, A>
    ): Try
}
```

---

## Integration with resource-context / 与 resource-context 的集成

### CapacityActionResource Interface / 产能动作资源接口

```kotlin
/**
 * 支持 ProductionAction 的资源接口
 * Resource interface that supports ProductionAction
 */
interface CapacityActionResource<out C : AbstractResourceCapacity> : Resource<C> {
    
    /**
     * 计算动作在指定时间范围内的资源消耗（单位操作时间的消耗）
     * Calculate resource consumption per unit operation time
     */
    fun <A : ProductionAction> usedBy(action: A, time: TimeRange): Flt64
}

/**
 * CapacityActionResource 的时隙
 * Time slot for CapacityActionResource
 */
data class CapacityActionResourceTimeSlot<
    out R : CapacityActionResource<C>,
    out C : AbstractResourceCapacity
>(
    override val origin: TimeSlot,
    override val resource: R,
    override val resourceCapacity: C,
    override val indexInRule: UInt64
) : ResourceTimeSlot<R, C>, AutoIndexed(CapacityActionResourceTimeSlot::class) {
    
    fun <A : ProductionAction> usedBy(action: A): Flt64 = resource.usedBy(action, time)
    
    override fun subOf(subTime: TimeRange): CapacityActionResourceTimeSlot<R, C>?
}
```

### resource-context File Structure / 文件结构

```
domain/resource/model/
├── Resource.kt                              # Resource 基类
├── CapacityActionResource.kt               # CapacityActionResource 接口（新增）
├── ExecutionResource.kt                    # ExecutionResource 基类
│   ├── TaskSchedulingExecutionResourceUsage.kt
│   └── BunchSchedulingExecutionResourceUsage.kt
├── CapacitySchedulingResourceUsage.kt      # 抽象基类（新增）
├── PlanCapacitySchedulingResourceUsage.kt  # Plan 模式（新增）
└── BunchCapacitySchedulingResourceUsage.kt # Bunch 模式（新增）
```

### CapacitySchedulingResourceUsage (Abstract Base) / 抽象基类

```kotlin
/**
 * 产能调度场景的资源使用量管理抽象基类
 * Abstract base class for resource usage in Capacity Scheduling
 */
abstract class CapacitySchedulingResourceUsage<
    A : ProductionAction,
    S : ResourceTimeSlot<R, C>,
    R : Resource<C>,
    C : AbstractResourceCapacity
>(
    protected val timeWindow: TimeWindow,
    resources: List<R>,
    protected val actions: List<A>
) : AbstractResourceUsage<S, R, C>() {
    
    override val overEnabled: Boolean = true
    override val lessEnabled: Boolean = true
    
    abstract override val name: String
    abstract override lateinit var quantity: LinearExpressionSymbols1
    
    abstract fun register(model: LinearMetaModel): Try
}
```

### PlanCapacitySchedulingResourceUsage / Plan 模式

```kotlin
/**
 * Plan 模式的产能调度资源使用量管理
 * Plan-mode resource usage for Capacity Scheduling
 */
class PlanCapacitySchedulingResourceUsage<
    A : ProductionAction,
    R : CapacityActionResource<C>,
    C : AbstractResourceCapacity
>(
    timeWindow: TimeWindow,
    resources: List<R>,
    times: List<TimeSlot>,
    actions: List<A>
) : CapacitySchedulingResourceUsage<A, CapacityActionResourceTimeSlot<R, C>, R, C>(
    timeWindow, resources, actions
) {
    
    override val name: String = "plan_capacity_scheduling_resource"
    
    override fun register(model: LinearMetaModel): Try {
        // 注册 quantity 变量
    }
    
    /**
     * 从 CapacityCompilation 绑定资源使用量
     * Bind resource usage from CapacityCompilation
     */
    fun <Comp : Capacity<A>> bindCompilation(compilation: Comp): Try {
        for (slot in timeSlots) {
            for (action in actions) {
                val unitUsage = slot.resource.usedBy(action, slot.time)
                if (unitUsage neq Flt64.zero) {
                    quantity[slot].asMutable() += unitUsage * compilation.operationTime[action, slot]
                }
            }
        }
        return ok
    }
}
```

### BunchCapacitySchedulingResourceUsage / Bunch 模式（列生成）

```kotlin
/**
 * Bunch 模式的产能调度资源使用量管理（支持列生成）
 * Bunch-mode resource usage for Capacity Scheduling (with column generation)
 */
class BunchCapacitySchedulingResourceUsage<
    E : Executor,
    A : ProductionAction,
    R : CapacityActionResource<C>,
    C : AbstractResourceCapacity
>(
    timeWindow: TimeWindow,
    resources: List<R>,
    times: List<TimeSlot>,
    actions: List<A>
) : CapacitySchedulingResourceUsage<A, CapacityActionResourceTimeSlot<R, C>, R, C>(
    timeWindow, resources, actions
) {
    
    override val name: String = "bunch_capacity_scheduling_resource"
    
    override fun register(model: LinearMetaModel): Try {
        // 注册 quantity 变量
    }
    
    /**
     * 从 IterativeCapacityCompilation 添加列贡献
     * Add column contribution from IterativeCapacityCompilation
     */
    suspend fun addColumns(
        iteration: UInt64,
        columns: List<CapacityColumn<E, A>>,
        compilation: IterativeCapacityCompilation<E, A>
    ): Try
}
```

---

## Quantity Calculation Formulas / 产量/资源消耗计算公式

### Produce Calculation / 产量计算

| Scenario   | Formula                                                                                     |
|------------|---------------------------------------------------------------------------------------------|
| Plan mode  | `quantity[product] = Σ_action Σ_slot operationTime[action, slot] × unitProduce`             |
| Bunch mode | `quantity[product] = Σ_column Σ_action columnAllocations[action] × unitProduce × x[column]` |

### Resource Usage Calculation / 资源消耗计算

| Scenario   | Formula                                                                                        |
|------------|------------------------------------------------------------------------------------------------|
| Plan mode  | `quantity[slot] = Σ_action operationTime[action, slot] × unitResourceUsage`                    |
| Bunch mode | `quantity[slot] = Σ_column Σ_action columnAllocations[action] × unitResourceUsage × x[column]` |

---

## Implementation Steps (Extended) / 实施步骤（扩展）

| Step | Task                                           | File                                                             |
|------|------------------------------------------------|------------------------------------------------------------------|
| 1    | Add CapacityActionProduce interface            | `produce-context/model/CapacityActionProduce.kt`                 |
| 2    | Add CapacityActionResource interface           | `resource-context/model/CapacityActionResource.kt`               |
| 3    | Implement CapacitySchedulingProduce            | `produce-context/model/CapacitySchedulingProduce.kt`             |
| 4    | Implement PlanCapacitySchedulingProduce        | `produce-context/model/PlanCapacitySchedulingProduce.kt`         |
| 5    | Implement BunchCapacitySchedulingProduce       | `produce-context/model/BunchCapacitySchedulingProduce.kt`        |
| 6    | Implement CapacitySchedulingResourceUsage      | `resource-context/model/CapacitySchedulingResourceUsage.kt`      |
| 7    | Implement PlanCapacitySchedulingResourceUsage  | `resource-context/model/PlanCapacitySchedulingResourceUsage.kt`  |
| 8    | Implement BunchCapacitySchedulingResourceUsage | `resource-context/model/BunchCapacitySchedulingResourceUsage.kt` |
| 9    | Update pom.xml dependencies                    | Both modules' pom.xml                                            |
| 10   | Compile and verify                             | -                                                                |

---

## Class Naming Convention / 类命名规范

| Scenario                             | produce-context                  | resource-context                        |
|--------------------------------------|----------------------------------|-----------------------------------------|
| Task scheduling                      | `TaskSchedulingProduce`          | `TaskSchedulingExecutionResourceUsage`  |
| Bunch scheduling (column generation) | `BunchSchedulingProduce`         | `BunchSchedulingExecutionResourceUsage` |
| Capacity scheduling - Plan mode      | `PlanCapacitySchedulingProduce`  | `PlanCapacitySchedulingResourceUsage`   |
| Capacity scheduling - Bunch mode     | `BunchCapacitySchedulingProduce` | `BunchCapacitySchedulingResourceUsage`  |
