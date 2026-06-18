# 错误处理迁移进度

## 总体原则

**不做兼容层，不保留旧的 throwing API 作为正式入口。所有生产代码调用链一步到位迁到 Ret<T> / Try 或 OrNull 接口。**

### 命名规范
- **无后缀函数**：返回 `Ret<T>` / `Try` 的主接口，用于编排层、领域层、属性/lazy/构造默认值的可失败函数。
- **OrNull 后缀**：nullable 便利接口，丢弃失败原因，只表达"有没有值"。用于属性/lazy/构造默认值的降级接口，以及底层工具库中失败原因不重要的查询。
- **Safe 后缀**：仅用于底层工具 / 数学 / multiarray / quantities 模块中需要显式区别 Kotlin 风格查询的安全接口，返回 `Ret<T>` / `Try` 并保留错误信息。

---

## 已完成迁移

### 第一阶段：应用层（6 个文件）
- Csp1dShadowPriceLifecycle.kt: extractFromDualSolution 返回 Ret
- Csp1dMilpSolver.kt: 删除 solve() 兼容层，只保留返回 Ret 的版本
  - **(!) 实际代码仍保留 solve() 兼容层，需删除并统一为 `solve(): Ret`**
- Csp1dColumnGeneration.kt: 删除 ensureRet，直接传播 Result
- Csp1dMilp.kt: 更新 solveMilp() 处理 Ret 返回值
- ColumnGenerationAlgorithm.kt: 接口返回 Ret
- ColumnGenerationStandardExecutors.kt: 删除 ensureTry/ensureRet
- ContinuousRadiusModelComponent.kt: register 返回 Try

### 第二阶段：gantt-scheduling 领域层（12 个文件）
- StorageResource.kt, ExecutionResource.kt, ConnectionResource.kt: register 返回 Failed
- Produce.kt, Consumption.kt, ProductionTask.kt: register 返回 Failed / quantityZero 返回 Ret
- TaskStepConflictConstraint.kt: refresh 返回 Failed
- TaskTime.kt: register 返回 Failed
- TimeWindow.kt: upper/upperInterval 改为 nullable + Safe
  - **(!) 应为 `upperOrNull` / `upperIntervalOrNull` + `upper(): Ret` / `upperInterval(): Ret`，需修正**
- Resource.kt, Cost.kt, ShadowPriceMap.kt, TaskBunch.kt: 添加 imports / nullable + Safe
  - **(!) 应为 OrNull + 无后缀 Result 主接口，需修正**

### 第三阶段：bpp3d 框架（25+ 个文件）
- ColumnGenerationAlgorithm.kt: solve() 返回 Ret，删除 throw
- ColumnGenerationApplicationService.kt: solve() 返回 Ret，删除 throw
- CylinderShapeContract.kt: 验证函数改为返回 Try
- Package.kt: 验证函数改为返回 Try
- DemandStatistics.kt: 验证函数改为返回 Try
- QuantityDemandStatistics.kt: 验证函数改为返回 Try
- QuantityGeometryCore.kt: 运算函数改为返回 Ret
- PackingGeometryGuard.kt: 验证函数改为返回 Try
- Packer.kt: 验证函数改为返回 Try
- MaterialPacker.kt: 验证函数改为返回 Try
- Load.kt: 验证函数改为返回 Try
- ScaledBpp3dSolverValueAdapter.kt: 验证函数改为返回 Try
- DemandConstraint.kt: 验证函数改为返回 Try
- LayerGenerationContext.kt: 验证函数改为返回 Try
- BottomUpLeftJustifiedAlgorithm.kt: 验证函数改为返回 Try
- ItemMerger.kt: 验证函数改为返回 Try
- Aggregation.kt: 验证函数改为返回 Try
- Orientation.kt: require 返回 Ret
- DepthBoundaryLayerOrientationPolicy.kt: 验证函数改为返回 Try
- 其他: 添加 imports、minor fixes

### 第四阶段：核心库和工具库（5 个文件）
- MetaModelExportSupport.kt: throw -> return Failed
- SatisfiedAmount.kt: registerConstraints 返回 Failed
- SymbolDimensionRegistry.kt: validateAddSubDimension/inferDimension 返回 Try/Ret
- RemoteSolverHttpTransportPlugin.kt: 添加 imports
- RectangularPackingDemand.kt: 验证函数改为返回 Try

### 第五阶段：函数签名改造（本轮完成）
- DomainValueConversion.kt: 添加 convertSolverValueSafe() 返回 Ret<V>
  - **(!) Safe 后缀不应用于领域层，应为 `convertSolverValue(): Ret<V>`，需修正**
- QuantityArithmetic.kt: 添加 resolveForSafe() 返回 Ret<QuantityArithmetic<V>>
  - **(!) Safe 后缀不应用于领域层，应为 `resolveFor(): Ret<QuantityArithmetic<V>>`，需修正**
- Csp1dProduceContext.kt: 添加 resolveDomainValueSampleSafe() 返回 Ret<V>
  - **(!) Safe 后缀不应用于应用层，应为 `resolveDomainValueSample(): Ret<V>`，需修正**
- Csp1dColumnGeneration.kt: buildLpMaster 返回 Ret<LpMaster<V>>，消除内联 throw
- ProductionTask.kt: 添加 produceSafe() 和 consumptionSafe() 返回 Ret<V>
  - **(!) Safe 后缀不应用于领域层，应为 `produce()` / `consumption(): Ret<V>`，需修正**
- ShadowPriceMap.kt: 添加 reducedCostSafe() 返回 Ret<V>
  - **(!) Safe 后缀不应用于领域层，应为 `reducedCost(): Ret<V>`，需修正**

### 测试代码修复
- ColumnGenerationAlgorithmTest.kt: 添加 unwrap() 辅助函数，solve() 调用添加 .unwrap()
- Csp1dApplicationAcceptanceTest.kt: 添加 unwrapMilpResult() 辅助函数，solve() 调用使用 unwrapMilpResult()
- ContinuousRadiusModelComponentTest.kt: register() 调用更新
- ResourceQuantityFltXTest.kt: resourceQuantityZero 添加 !!

### 辅助修改
- Csp1dProduceContext.kt, WasteObjectivePipeline.kt, BatchMinimizationObjective.kt: produce[index] 添加 !!
- Compilation.kt (task/bunch), TaskBunch.kt: solverCost() 添加 !!

### 构建状态
生产代码全部通过编译
ColumnGenerationAlgorithmTest.kt 有预存错误（非本次迁移导致）

---

## 总体统计

| 模块 | throw 总数 | 已完成 | 剩余 | 说明 |
|------|-----------|--------|------|------|
| ospf-kotlin-utils | ~42 | 0 | ~42 | OrThrow 变体（(!) 违反不做兼容层原则，应补齐 OrNull + Safe 后删除 OrThrow） |
| ospf-kotlin-core | ~12 | 2 | ~10 | 含设计选择保留 |
| ospf-kotlin-multiarray | ~39 | 0 | ~39 | 含设计选择保留（(!) 同上，应补齐 OrNull + Safe 后删除 OrThrow） |
| ospf-kotlin-quantities | ~22 | 1 | ~21 | 含设计选择保留（(!) 同上，应补齐 OrNull + Safe 后删除 OrThrow） |
| ospf-kotlin-math | ~120+ | 0 | ~120+ | 含设计选择保留（(!) 同上，应补齐 OrNull + Safe 后删除 OrThrow） |
| ospf-kotlin-framework | ~8 | 1 | ~7 | 外部库边界（(!) 应在边界处 catch 后转换成 Ret） |
| ospf-kotlin-framework-gantt-scheduling | ~25 | 14 | ~11 | 含设计选择保留 |
| ospf-kotlin-framework-csp1d | ~15 | 12 | ~3 | 含自定义异常保留 |
| ospf-kotlin-framework-bpp2d | 4 | 1 | 3 | |
| ospf-kotlin-framework-bpp3d | ~50+ | 30+ | ~20 | 含设计选择保留 |
| ospf-kotlin-framework-plugin | ~10 | 0 | ~10 | 外部库边界（(!) 应在边界处 catch 后转换成 Ret） |
| 测试代码 | ~146 | 0 | ~146（保留） | |
| **总计** | **~395** | **~60+** | **~55（生产代码）** | |

### 剩余 throw 分类

1. **OrThrow 变体**（~40 个）：utils/multiarray/quantities/math 模块的 `minOrThrow`、`maxOrThrow`、`firstOrThrow` 等，已有 `OrNull` 对应版本
   - (!) 违反不做兼容层原则，应补齐 Safe 接口后删除 OrThrow 变体
2. **设计选择**（~10 个）：CallBackModel lambda 默认参数、ConstraintSign 验证等，保留
3. **外部库边界**（~10 个）：RemoteSolverHttpClient、持久化插件、序列化边界
   - (!) 外部库边界异常应在边界处 catch 后转换成 Ret，不应保留 throw 穿透到业务层
4. **自定义异常**（~5 个）：Csp1dRecoveryFallbackDisabledException、Csp1dRecoverySolveException，测试依赖，保留
5. **lambda 内 throw**（~8 个）：TaskTime.kt 等，外层 catch 转 Failed，保留

---

## 构建状态
生产代码全部通过编译
ColumnGenerationAlgorithmTest.kt 有预存错误（非本次迁移导致）

---

## 迁移完成总结

### 已完成的工作

1. **应用层**：Csp1d 和 BPP3D 的求解器接口、列生成算法全部迁移到 Ret
2. **领域层**：gantt-scheduling 和 BPP3D 的验证函数、运算函数迁移到 Try/Ret
3. **函数签名改造**：为返回特定类型的函数添加 Safe 版本（如 `produceSafe()`、`reducedCostSafe()`）
   - (!) Safe 后缀不应用于编排层/领域层，应为无后缀 Result 主接口
4. **内联 throw 消除**：`Csp1dColumnGeneration.buildLpMaster` 返回 `Ret<LpMaster<V>>`

### 保留的 throw（合理设计）

| 分类 | 数量 | 原因 |
|------|------|------|
| OrThrow 变体 | ~40 | 已有 OrNull 对应版本，用户可选择（(!) 违反不做兼容层原则，应补齐 Safe 接口后删除 OrThrow 变体） |
| 设计选择 | ~10 | lambda 默认参数、验证边界 |
| 外部库边界 | ~10 | 序列化、HTTP 客户端（(!) 应在边界处 catch 后转换成 Ret，不应保留 throw 穿透到业务层） |
| 自定义异常 | ~5 | 测试依赖的异常类型 |
| lambda 内 throw | ~8 | 外层 catch 转 Failed |

### 后续可选工作

1. **测试代码迁移**：将测试中的 `unwrap()` 辅助函数统一
2. **文档更新**：更新 API 文档，标注无后缀 Result 主接口和 OrNull 接口
3. **代码审查**：确认所有保留的 throw 确实合理

---

## 待修正项（与约定不一致）

以下为另一个会话已完成但与本次确认的迁移口径不一致的代码，需要后续修正：

| 问题 | 涉及文件 | 修正方向 | 状态 |
|------|---------|---------|------|
| 保留 solve() 兼容层 | Csp1dMilpSolver.kt | 删除 solve() 兼容层，统一为 `solve(): Ret` | ✅ 已确认无兼容层 |
| Safe 后缀用于编排层/领域层 | DomainValueConversion.kt, QuantityArithmetic.kt, Csp1dProduceContext.kt, ProductionTask.kt, ShadowPriceMap.kt | 改为无后缀 Result 主接口（如 `convertSolverValue()`、`produce()`、`reducedCost()`） | ✅ 已修正 |
| 属性用 nullable + Safe | TimeWindow.kt, Cost.kt, Resource.kt | 改为 OrNull + 无后缀 Result 主接口（如 `upperOrNull` + `upper(): Ret`） | ✅ 已修正 |
| OrThrow 变体保留 | utils/Collection.kt, Find.kt, MinMax.kt, multiarray/Shape.kt 等 | 删除 OrThrow 变体，保留 OrNull 版本 | ✅ 已修正 |
| 外部库边界 throw 保留 | RemoteSolverClient.kt, RemoteLinearSolver.kt, RemoteQuadraticSolver.kt | 边界处 catch 后转换成 Ret，不穿透到业务层 | ✅ 已修正 |
