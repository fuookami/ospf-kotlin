# Slot-Based Bunch Compilation Design

# 分时隙任务束编译设计

## Overview / 概述

本文档描述了分时隙任务束编排模型的设计，该模型通过预先求解 capacity scheduling 问题获取时隙级的资源与产量中间值，从而简化 bunch compilation 主问题的计算复杂度。

This document describes the design of slot-based bunch compilation model, which pre-solves the capacity scheduling problem to obtain slot-level resource and produce intermediate
values, thereby simplifying the computational complexity of the bunch compilation master problem.

---

## Motivation / 动机

在传统的 bunch compilation 中，resource 和 produce 约束需要在主问题中动态计算，这导致了：

1. 主问题规模大，求解时间长
2. 约束复杂度高
3. 列生成迭代效率低

In traditional bunch compilation, resource and produce constraints need to be dynamically calculated in the master problem, which leads to:

1. Large master problem scale and long solving time
2. High constraint complexity
3. Low column generation iteration efficiency

通过分时隙预计算中间值，可以：

1. 减小主问题规模
2. 将 resource/produce 约束计算移至预求解阶段
3. 提供明确的时隙约束边界给 bunch 生成器

By pre-calculating intermediate values per time slot, we can:

1. Reduce master problem scale
2. Move resource/produce constraint calculation to pre-solving phase
3. Provide clear slot constraint boundaries to bunch generators

---

## Architecture / 架构

```
┌─────────────────────────────────────────────────────────────────┐
│                    Phase 1: Pre-Solving                          │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │           SlotBasedCapacityPreSolver                       │  │
│  │  ┌─────────────────────────────────────────────────────┐  │  │
│  │  │  CapacityCompilation / IterativeCapacityCompilation │  │  │
│  │  │  - Solves capacity scheduling problem               │  │  │
│  │  │  - Extracts operationTime per action per slot       │  │  │
│  │  │  - Calculates produce/consumption per slot          │  │  │
│  │  │  - Calculates resource usage per slot               │  │  │
│  │  └─────────────────────────────────────────────────────┘  │  │
│  │                          ↓                                 │  │
│  │           CapacityIntermediateValues<Action>               │  │
│  └───────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
                               ↓
┌─────────────────────────────────────────────────────────────────┐
│                  Phase 2: Bunch Generation                       │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │           SlotBasedBunchGenerator                          │  │
│  │  - Receives slot constraints from intermediate values     │  │
│  │  - Generates bunches that satisfy constraints             │  │
│  │  - Each bunch belongs to exactly one slot                 │  │
│  └───────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
                               ↓
┌─────────────────────────────────────────────────────────────────┐
│               Phase 3: Bunch Compilation (Master Problem)        │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │         SlotBasedBunchCompilation                          │  │
│  │  - Only handles bunch selection                            │  │
│  │  - No resource/produce constraint calculation              │  │
│  │  - Reduced problem complexity                              │  │
│  └───────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

---

## Core Components / 核心组件

### 1. SlotBasedBunch Interface / 接口

```kotlin
/**
 * 分时隙任务束接口
 * Slot-based task bunch interface
 *
 * 一个 SlotBasedBunch 只能属于一个时隙
 * A SlotBasedBunch can only belong to one time slot
 */
interface SlotBasedBunch<
    T : AbstractTask<E, A>,
    E : Executor,
    A : AssignmentPolicy<E>
> : AbstractTaskBunch<T, E, A> {
    
    /**
     * 所属时隙
     * The time slot this bunch belongs to
     */
    val slot: TimeSlot
    
    /**
     * 时隙索引
     * Slot index in the time window
     */
    val slotIndex: Int
}
```

---

### 2. SlotBasedCapacityResult / 时隙产能结果

```kotlin
/**
 * 分时隙产能结果
 * Slot-based capacity result
 *
 * 存储单个时隙的产能分配结果和中间值
 * Stores capacity allocation results and intermediate values for a single time slot
 */
data class SlotBasedCapacityResult<A : ProductionAction>(
    /**
     * 所属时隙
     * The time slot
     */
    val slot: TimeSlot,
    
    /**
     * 时隙索引
     * Slot index
     */
    val slotIndex: Int,
    
    /**
     * 该时隙内的动作分配
     * Action allocations in this slot
     */
    val actionAllocations: List<ActionAllocation<A>>,
    
    /**
     * 该时隙的总成本
     * Total cost for this slot
     */
    val totalCost: Flt64,
    
    /**
     * 该时隙的产品产量（按产品）
     * Product production by product in this slot
     */
    val produceByProduct: Map<AbstractMaterial, Flt64>,
    
    /**
     * 该时隙的原料消耗（按原料）
     * Material consumption by material in this slot
     */
    val consumptionByMaterial: Map<AbstractMaterial, Flt64>,
    
    /**
     * 该时隙的资源使用量（按资源）
     * Resource usage by resource in this slot
     */
    val resourceUsageByResource: Map<AbstractResourceCapacity, Flt64>
)
```

---

### 3. CapacityIntermediateValues / 产能中间值集合

```kotlin
/**
 * 产能中间值集合
 * Capacity intermediate values collection
 *
 * 聚合所有时隙的产能结果，提供查询接口
 * Aggregates capacity results for all slots, provides query interface
 */
class CapacityIntermediateValues<A : ProductionAction>(
    /**
     * 时隙列表
     * List of time slots
     */
    val slots: List<TimeSlot>,
    
    /**
     * 各时隙的产能结果
     * Capacity results by slot
     */
    val results: Map<TimeSlot, SlotBasedCapacityResult<A>>
) {
    /**
     * 获取指定时隙的产品产量
     * Get product production for specified slot
     */
    fun produce(slot: TimeSlot, product: AbstractMaterial): Flt64 {
        return results[slot]?.produceByProduct?.get(product) ?: Flt64.zero
    }
    
    /**
     * 获取指定时隙的原料消耗
     * Get material consumption for specified slot
     */
    fun consumption(slot: TimeSlot, material: AbstractMaterial): Flt64 {
        return results[slot]?.consumptionByMaterial?.get(material) ?: Flt64.zero
    }
    
    /**
     * 获取指定时隙的资源使用量
     * Get resource usage for specified slot
     */
    fun resourceUsage(slot: TimeSlot, resource: AbstractResourceCapacity): Flt64 {
        return results[slot]?.resourceUsageByResource?.get(resource) ?: Flt64.zero
    }
    
    /**
     * 获取指定时隙的所有约束
     * Get all constraints for specified slot
     */
    fun slotConstraints(slot: TimeSlot): SlotConstraints?
}
```

---

### 4. SlotConstraints / 时隙约束

```kotlin
/**
 * 时隙约束
 * Slot constraints
 *
 * 描述单个时隙的资源、产量、消耗约束边界
 * Describes resource, produce, consumption constraint boundaries for a single slot
 */
data class SlotConstraints(
    /**
     * 所属时隙
     * The time slot
     */
    val slot: TimeSlot,
    
    /**
     * 时隙索引
     * Slot index
     */
    val slotIndex: Int,
    
    /**
     * 产品产量上限
     * Maximum production by product
     */
    val maxProduce: Map<AbstractMaterial, Flt64>,
    
    /**
     * 产品产量下限
     * Minimum production by product
     */
    val minProduce: Map<AbstractMaterial, Flt64>,
    
    /**
     * 原料消耗上限
     * Maximum consumption by material
     */
    val maxConsumption: Map<AbstractMaterial, Flt64>,
    
    /**
     * 原料消耗下限
     * Minimum consumption by material
     */
    val minConsumption: Map<AbstractMaterial, Flt64>,
    
    /**
     * 资源使用量上限
     * Maximum resource usage by resource
     */
    val maxResourceUsage: Map<AbstractResourceCapacity, Flt64>,
    
    /**
     * 资源使用量下限
     * Minimum resource usage by resource
     */
    val minResourceUsage: Map<AbstractResourceCapacity, Flt64>
) {
    companion object {
        /**
         * 从产能结果创建约束
         * Create constraints from capacity result
         */
        fun <A : ProductionAction> from(
            result: SlotBasedCapacityResult<A>,
            tolerance: Flt64 = Flt64.zero
        ): SlotConstraints
    }
}
```

---

### 5. SlotBasedCapacityPreSolver / 预求解服务

```kotlin
/**
 * 分时隙产能预求解服务
 * Slot-based capacity pre-solving service
 *
 * 负责求解 capacity scheduling 问题并提取中间值
 * Responsible for solving capacity scheduling problem and extracting intermediate values
 */
class SlotBasedCapacityPreSolver<A : ProductionAction>(
    /**
     * 生产动作列表
     * List of production actions
     */
    private val actions: List<A>,
    
    /**
     * 时隙列表
     * List of time slots
     */
    private val slots: List<TimeSlot>,
    
    /**
     * 时间窗口
     * Time window
     */
    private val timeWindow: TimeWindow,
    
    /**
     * 是否使用列生成模式
     * Whether to use column generation mode
     */
    private val useColumnGeneration: Boolean = false
) {
    /**
     * 产能编译对象
     * Capacity compilation object
     */
    private val compilation: Capacity<A> = if (useColumnGeneration) {
        IterativeCapacityCompilation(executors, actions, slots, timeWindow)
    } else {
        CapacityCompilation(actions, slots, timeWindow)
    }
    
    /**
     * 注册到模型
     * Register to model
     */
    fun register(model: LinearMetaModel): Try
    
    /**
     * 执行预求解
     * Execute pre-solving
     */
    suspend fun solve(
        model: AbstractLinearMetaModel,
        solver: Solver
    ): Ret<CapacityIntermediateValues<A>>
    
    /**
     * 提取中间值
     * Extract intermediate values
     */
    fun extractIntermediateValues(
        model: AbstractLinearMetaModel
    ): Ret<CapacityIntermediateValues<A>>
}
```

---

### 6. SlotBasedBunchCompilation / 编译类

```kotlin
/**
 * 分时隙任务束编译类
 * Slot-based bunch compilation class
 *
 * 继承 BunchCompilation，增加时隙相关功能
 * Extends BunchCompilation with slot-related functionality
 */
class SlotBasedBunchCompilation<
    B : SlotBasedBunch<T, E, A>,
    T : AbstractTask<E, A>,
    E : Executor,
    A : AssignmentPolicy<E>
>(
    private val tasks: List<T>,
    private val executors: List<E>,
    private val slots: List<TimeSlot>,
    private val lockCancelTasks: Set<T> = emptySet(),
    override val withExecutorLeisure: Boolean = true
) : BunchCompilation<B, T, E, A>(tasks, executors, lockCancelTasks, withExecutorLeisure) {
    
    /**
     * 按时隙分组的 bunches
     * Bunches grouped by slot
     */
    val bunchesBySlot: Map<TimeSlot, List<B>>
    
    /**
     * 按时隙添加列
     * Add columns by slot
     */
    suspend fun addColumnsBySlot(
        iteration: UInt64,
        newBunches: List<B>,
        model: AbstractLinearMetaModel
    ): Ret<Map<TimeSlot, List<B>>>
}
```

---

### 7. SlotBasedBunchCompilationContext / 上下文接口

```kotlin
/**
 * 分时隙任务束编译上下文接口
 * Slot-based bunch compilation context interface
 */
interface SlotBasedBunchCompilationContext<
    Args : AbstractGanttSchedulingShadowPriceArguments<E, A>,
    B : SlotBasedBunch<T, E, A>,
    T : AbstractTask<E, A>,
    E : Executor,
    A : AssignmentPolicy<E>,
    Action : ProductionAction
> : BunchCompilationContext<Args, B, T, E, A> {
    
    /**
     * 时隙列表
     * List of time slots
     */
    val slots: List<TimeSlot>
    
    /**
     * 产能预求解器
     * Capacity pre-solver
     */
    val capacityPreSolver: SlotBasedCapacityPreSolver<Action>
    
    /**
     * 产能中间值（预求解后填充）
     * Capacity intermediate values (populated after pre-solving)
     */
    val intermediateValues: CapacityIntermediateValues<Action>?
    
    /**
     * 执行产能预求解
     * Execute capacity pre-solving
     */
    suspend fun preSolveCapacity(
        model: AbstractLinearMetaModel,
        solver: Solver
    ): Ret<CapacityIntermediateValues<Action>>
    
    /**
     * 获取指定时隙的约束
     * Get constraints for specified slot
     */
    fun slotConstraints(slot: TimeSlot): SlotConstraints?
}
```

---

## bunch-generation-context Components / bunch-generation-context 组件

### SlotBasedBunchGenerator Interface / 生成器接口

```kotlin
/**
 * 分时隙任务束生成器接口
 * Slot-based bunch generator interface
 *
 * 生成满足时隙约束的 bunch
 * Generates bunches that satisfy slot constraints
 */
interface SlotBasedBunchGenerator<
    B : SlotBasedBunch<T, E, A>,
    T : AbstractTask<E, A>,
    E : Executor,
    A : AssignmentPolicy<E>,
    Action : ProductionAction
> {
    /**
     * 支持的执行器
     * Supported executors
     */
    val executors: List<E>
    
    /**
     * 为指定时隙生成 bunch
     * Generate bunches for specified slot
     *
     * @param iteration Current iteration number / 当前迭代数
     * @param slot Target time slot / 目标时隙
     * @param constraints Slot constraints / 时隙约束
     * @param shadowPrices Task shadow prices / 任务影子价格
     * @return Generated bunches / 生成的 bunch 列表
     */
    suspend fun generate(
        iteration: UInt64,
        slot: TimeSlot,
        constraints: SlotConstraints,
        shadowPrices: Map<T, Flt64>
    ): Ret<List<B>>
    
    /**
     * 批量生成所有时隙的 bunch
     * Generate bunches for all slots in batch
     */
    suspend fun generateAll(
        iteration: UInt64,
        intermediateValues: CapacityIntermediateValues<Action>,
        shadowPrices: Map<T, Flt64>
    ): Ret<List<B>>
}
```

---

## Workflow / 工作流程

### Complete Workflow / 完整流程

```
1. 初始化阶段 / Initialization Phase:
   ┌─────────────────────────────────────────────────────────────┐
   │  val context = SlotBasedBunchCompilationContextImpl(...)    │
   │  context.register(model)                                     │
   └─────────────────────────────────────────────────────────────┘

2. 预求解阶段 / Pre-Solving Phase:
   ┌─────────────────────────────────────────────────────────────┐
   │  val intermediateValues = context.preSolveCapacity(model,   │
   │                                                      solver) │
   │  // intermediateValues 包含每个时隙的中间值                   │
   └─────────────────────────────────────────────────────────────┘

3. Bunch 生成阶段 / Bunch Generation Phase:
   ┌─────────────────────────────────────────────────────────────┐
   │  val generator = SlotBasedBunchGeneratorImpl(...)           │
   │  for (slot in slots) {                                       │
   │      val constraints = context.slotConstraints(slot)         │
   │      val newBunches = generator.generate(iteration, slot,    │
   │                                        constraints, ...)     │
   │      context.addColumns(iteration, newBunches, model)        │
   │  }                                                           │
   └─────────────────────────────────────────────────────────────┘

4. 主问题求解阶段 / Master Problem Solving Phase:
   ┌─────────────────────────────────────────────────────────────┐
   │  solver(model)                                               │
   │  // 只处理 bunch 选择，无需 resource/produce 约束计算        │
   └─────────────────────────────────────────────────────────────┘

5. 迭代阶段 / Iteration Phase:
   ┌─────────────────────────────────────────────────────────────┐
   │  while (!converged) {                                        │
   │      extractShadowPrices(model)                              │
   │      generateNewBunches(...)                                 │
   │      addColumns(...)                                         │
   │      solve(model)                                            │
   │  }                                                           │
   └─────────────────────────────────────────────────────────────┘
```

---

## File Structure / 文件结构

### bunch-compilation-context

```
gantt-scheduling-domain-bunch-compilation-context/
├── seg-design.md                    # This design document / 本设计文档
├── pom.xml                          # Updated with dependencies
└── src/main/fuookami/ospf/kotlin/framework/gantt_scheduling/domain/bunch_compilation/
    ├── Aggregation.kt               # Extended with slot-based aggregation
    ├── BunchCompilationContext.kt   # Base interface
    ├── model/
    │   ├── Compilation.kt           # Base compilation class
    │   ├── SlotBasedBunch.kt        # NEW: Slot-based bunch interface
    │   ├── SlotBasedCapacityResult.kt  # NEW: Slot capacity result
    │   └── SlotBasedBunchCompilation.kt # NEW: Slot-based compilation
    └── service/
        ├── SlotBasedCapacityPreSolver.kt  # NEW: Pre-solver service
        ├── SlotBasedBunchCompilationContext.kt  # NEW: Context interface
        └── limits/
            └── BunchCostMinimization.kt
```

### bunch-generation-context

```
gantt-scheduling-domain-bunch-generation-context/
├── pom.xml                          # Updated with dependencies
└── src/main/fuookami/ospf/kotlin/framework/gantt_scheduling/domain/bunch_generation/
    ├── model/
    │   ├── Graph.kt
    │   └── Label.kt
    └── service/
        ├── PlannedTaskBunchGenerator.kt
        ├── UnplannedTaskBunchGenerator.kt
        └── SlotBasedBunchGenerator.kt  # NEW: Slot-based generator interface
```

---

## Dependencies / 依赖关系

```xml
<!-- bunch-compilation-context pom.xml -->
<dependency>
    <groupId>fuookami.ospf.kotlin.framework.gantt-scheduling</groupId>
    <artifactId>gantt-scheduling-domain-capacity-scheduling-context</artifactId>
</dependency>

<!-- bunch-generation-context pom.xml -->
<dependency>
    <groupId>fuookami.ospf.kotlin.framework.gantt-scheduling</groupId>
    <artifactId>gantt-scheduling-domain-capacity-scheduling-context</artifactId>
</dependency>
```

---

## Implementation Steps / 实施步骤

| Step | Task                                     | Module                    | File                                          |
|------|------------------------------------------|---------------------------|-----------------------------------------------|
| 1    | Create SlotBasedBunch interface          | bunch-compilation-context | `model/SlotBasedBunch.kt`                     |
| 2    | Create SlotBasedCapacityResult           | bunch-compilation-context | `model/SlotBasedCapacityResult.kt`            |
| 3    | Create CapacityIntermediateValues        | bunch-compilation-context | `model/SlotBasedCapacityResult.kt`            |
| 4    | Create SlotConstraints                   | bunch-compilation-context | `model/SlotBasedCapacityResult.kt`            |
| 5    | Create SlotBasedBunchCompilation         | bunch-compilation-context | `model/SlotBasedBunchCompilation.kt`          |
| 6    | Create SlotBasedCapacityPreSolver        | bunch-compilation-context | `service/SlotBasedCapacityPreSolver.kt`       |
| 7    | Create SlotBasedBunchCompilationContext  | bunch-compilation-context | `service/SlotBasedBunchCompilationContext.kt` |
| 8    | Create SlotBasedBunchGenerator interface | bunch-generation-context  | `service/SlotBasedBunchGenerator.kt`          |
| 9    | Update pom.xml dependencies              | both modules              | `pom.xml`                                     |

---

## Notes / 注意事项

1. **Slot Correspondence**: The correspondence between bunch and slot is ensured by the bunch generator, not by the compilation context.

   **时隙对应**：bunch 与时隙的对应关系由 bunch 生成器保证，而非编译上下文。

2. **Intermediate Values**: Intermediate values are calculated once during pre-solving and used as constraints for bunch generation, not re-calculated in the master problem.

   **中间值**：中间值在预求解阶段一次性计算，作为 bunch 生成的约束使用，不在主问题中重新计算。

3. **Single Slot Constraint**: Each bunch can only belong to one time slot. This is enforced by the `SlotBasedBunch` interface.

   **单时隙约束**：每个 bunch 只能属于一个时隙，这由 `SlotBasedBunch` 接口强制保证。

4. **Column Generation Support**: The design supports both direct solving mode and column generation mode.

   **列生成支持**：设计同时支持直接求解模式和列生成模式。