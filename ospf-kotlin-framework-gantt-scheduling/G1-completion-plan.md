# G1 完成计划 — 补齐 produce/resource/capacity 数量 Map、ShadowPriceMap、reduced cost 签名

## 约束

框架层 `AbstractShadowPriceMap`、`CGPipeline`、`ShadowPriceExtractor` 固定 Flt64。
领域层泛型化只能到"领域边界"：shadow price 从 solver 提取时是 Flt64，领域内部可以转为 V。

## 策略

1. 数量 Map 改为 `Map<*, V>` — 这些纯粹在领域层，无 solver 依赖
2. ShadowPriceMap 加 `<V>` — 内部 `Map<T, Flt64>` 保留给 solver 交互，新增 `Map<T, V>` 视图
3. reduced cost 签名改为 `(IT) -> V` — 调用方在领域层

## 修改清单

### Task 6: produce/resource/capacity 数量 Map 泛型化

#### produce-context

| 文件 | 当前 | 目标 |
|------|------|------|
| CapacityActionProduce.kt | `produce: Map<P, Flt64>`, `consumption: Map<C, Flt64>`, `overProduce: Map<P, Flt64>`, `lessProduce: Map<P, Flt64>`, `overConsumption: Map<C, Flt64>`, `lessConsumption: Map<C, Flt64>` | `Map<P, V>` / `Map<C, V>` |
| ProductionTask.kt | `produce: Map<P, Flt64>`, `consumption: Map<C, Flt64>` | `Map<P, V>` / `Map<C, V>` |
| ProduceQuantityConstraint.kt | `HashMap<P, Flt64>` | `HashMap<P, V>` |
| ConsumptionQuantityConstraint.kt | `HashMap<C, Flt64>` | `HashMap<C, Flt64>` | `HashMap<C, V>` |
| BunchCapacitySchedulingProduce.kt | `produce: Map<P, Flt64>` 等 | `Map<P, V>` |

#### resource-context

| 文件 | 当前 | 目标 |
|------|------|------|
| Resource.kt | `capacity: Flt64`, `usedCapacity: Flt64`, `lessCapacity: Flt64`, `overCapacity: Flt64` | `V` |
| ConnectionResource.kt | 同上 | `V` |
| ExecutionResource.kt | 同上 | `V` |
| StorageResource.kt | `HashMap<ResourceTimeSlot<R, C>, Flt64>` for capacity/usedCapacity/lessCapacity/overCapacity | `HashMap<ResourceTimeSlot<R, C>, V>` |
| ResourceCapacityConstraint.kt | `HashMap<ResourceTimeSlot<R, C>, Flt64>` | `HashMap<ResourceTimeSlot<R, C>, V>` |
| ResourceSlackMinimization.kt | 使用 resource 的 capacity/lessCapacity/overCapacity | 适配 V |
| ResourceUsageMinimization.kt | 同上 | 适配 V |

#### capacity-scheduling-context

| 文件 | 当前 | 目标 |
|------|------|------|
| CapacityColumn.kt | 已泛型 `CapacityColumn<E, A, V>` | 无需改 |
| CapacityColumnAggregation.kt | 已使用 `CapacityColumn<E, A, Flt64>` | 改为 `V` |
| IterativeCapacityCompilation.kt | 同上 | 改为 `V` |

### Task 7: ShadowPriceMap 泛型化

| 文件 | 修改 |
|------|------|
| ShadowPriceMap.kt | 加 `<V : RealNumber<V>>`，`shadowPrice: Map<T, Flt64>` 保留为 solver 交互，新增 `shadowPriceOf: (T) -> V` 视图；`reducedCost` 返回 `V` |
| DemandReducedCost.kt | 加 `<V : RealNumber<V>>`，`cost: Flt64` 改为 `cost: V` |

### Task 8: reduced cost/shadow price 签名泛型化

| 文件 | 修改 |
|------|------|
| Aggregation.kt (bunch-compilation) | `extractBunchReducedCosts` 返回 `Map<B, V>` |
| BunchCompilationContext.kt | `bunchReducedCosts: Map<B, V>` |
| SlotBasedBunchGenerator.kt | `shadowPrices: Map<T, V>` 参数 |
| Iteration.kt (application) | `bestReducedCost: V` |

## 执行顺序

1. 先改 ShadowPriceMap.kt + DemandReducedCost.kt（task-context 层，被所有上下文依赖）
2. 再改 produce-context 的数量 Map
3. 再改 resource-context 的数量 Map
4. 再改 capacity-scheduling-context 的引用
5. 再改 bunch-compilation 的 reduced cost 签名
6. 再改 bunch-generation 的 shadow price 参数
7. 最后改 application 的 Iteration
8. 每步后编译验证

## Flt64 typealias

每个泛型化的类型都加 Flt64 typealias：
- `typealias Flt64ShadowPriceMap<T> = ShadowPriceMap<T, Flt64>`
- `typealias Flt64DemandReducedCost<T> = DemandReducedCost<T, Flt64>`
