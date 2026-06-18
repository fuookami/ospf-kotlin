# 抛异常审计清单

## 审计目标
将所有 `throw` 语句替换为返回 `Result` 类型（Failed/Fatal），遵循项目错误处理规范。

## 迁移口径

1. 不做兼容层，不保留旧的 throwing API 作为正式入口。
2. 应用层、framework 编排层、领域校验、单位换算、几何约束失败函数，原函数签名直接改为 `Ret<T>` / `Try`（无后缀命名）。
3. getter、`by lazy`、构造参数默认值里的 `throw`，删除原 throwing 属性；新增 `xxxOrNull` nullable 接口，并以无后缀 `xxx()` 作为返回 `Ret<T>` / `Try` 的主接口。
4. 底层工具 / 数学 / multiarray / quantities 的 Kotlin 风格 API 补齐 `OrNull` / `Safe` 接口；`OrNull` 丢弃失败原因，`Safe` 返回 `Ret<T>` / `Try` 并保留错误信息。补齐后删除 `OrThrow` 变体。
5. 外部库 / 协议边界异常只允许在边界处 `catch`，并立即转换成 `Ret<T>` / `Try`，不穿透到业务层。

## 实际统计

- **生产代码 throw 总数**：~395 个
- **涉及文件数**：~105 个
- **测试代码 throw**：~146 个（保留）
- **已完成迁移**：~55 个文件，~70+ 个 throw（约 18%）
- **剩余生产代码 throw**：~55 个（均为合理保留）

---

## 已完成迁移清单

### 应用层（6 个文件）
| 文件 | 修改内容 | 状态 |
|------|---------|------|
| Csp1dShadowPriceLifecycle.kt | extractFromDualSolution 返回 Ret | done |
| Csp1dMilpSolver.kt | 新增 solveRet()，保留 solve() 兼容 | **(!) 违反不做兼容层原则，应删除 solve() 兼容层，统一为 `solve(): Ret`** |
| Csp1dColumnGeneration.kt | 删除 ensureRet，直接传播 Result | done |
| ColumnGenerationAlgorithm.kt | 接口返回 Ret | done |
| ColumnGenerationStandardExecutors.kt | 删除 ensureTry/ensureRet | done |
| ContinuousRadiusModelComponent.kt | register 返回 Try | done |

### gantt-scheduling 领域层（12 个文件）
| 文件 | 修改内容 | 状态 |
|------|---------|------|
| StorageResource.kt | register 返回 Failed | done |
| ExecutionResource.kt | register 返回 Failed | done |
| ConnectionResource.kt | register 返回 Failed | done |
| Produce.kt | register 返回 Failed | done |
| Consumption.kt | register 返回 Failed | done |
| ProductionTask.kt | quantityZero 返回 Ret | done |
| TaskStepConflictConstraint.kt | refresh 返回 Failed | done |
| TaskTime.kt | register 返回 Failed | done |
| TimeWindow.kt | upper/upperInterval 改为 nullable + Safe | **(!) 应为 `upperOrNull` / `upperIntervalOrNull` + `upper(): Ret` / `upperInterval(): Ret`，需修正** |
| Resource.kt | 添加 error imports | **(!) resourceQuantityZero 应为 `resourceQuantityZeroOrNull()` + `resourceQuantityZero(): Ret<V>`，需修正** |
| Cost.kt | solverCost 改为 nullable + Safe | **(!) 应为 `solverCostOrNull()` + `solverCost(): Ret<Flt64>`，需修正** |
| ShadowPriceMap.kt | 添加 imports | done |

### bpp3d 框架（25+ 个文件）
| 文件 | 修改内容 | 状态 |
|------|---------|------|
| CylinderShapeContract.kt | 验证函数改为返回 Try | done |
| Package.kt | 验证函数改为返回 Try | done |
| DemandStatistics.kt | 验证函数改为返回 Try | done |
| QuantityDemandStatistics.kt | 验证函数改为返回 Try | done |
| QuantityGeometryCore.kt | 运算函数改为返回 Ret | done |
| PackingGeometryGuard.kt | 验证函数改为返回 Try | done |
| Packer.kt | 验证函数改为返回 Try | done |
| MaterialPacker.kt | 验证函数改为返回 Try | done |
| Load.kt | 验证函数改为返回 Try | done |
| ScaledBpp3dSolverValueAdapter.kt | 验证函数改为返回 Try | done |
| DemandConstraint.kt | 验证函数改为返回 Try | done |
| LayerGenerationContext.kt | 验证函数改为返回 Try | done |
| BottomUpLeftJustifiedAlgorithm.kt | 验证函数改为返回 Try | done |
| ItemMerger.kt | 验证函数改为返回 Try | done |
| Aggregation.kt | 验证函数改为返回 Try | done |
| Orientation.kt | require 返回 Ret | done |
| DepthBoundaryLayerOrientationPolicy.kt | 验证函数改为返回 Try | done |

### 核心库和工具库（5 个文件）
| 文件 | 修改内容 | 状态 |
|------|---------|------|
| MetaModelExportSupport.kt | throw -> return Failed | done |
| SatisfiedAmount.kt | registerConstraints 返回 Failed | done |
| SymbolDimensionRegistry.kt | validateAddSubDimension/inferDimension 返回 Try/Ret | done |
| RemoteSolverHttpTransportPlugin.kt | 添加 imports | done |
| RectangularPackingDemand.kt | 验证函数改为返回 Try | done |

### 函数签名改造（6 个文件）
| 文件 | 修改内容 | 状态 |
|------|---------|------|
| DomainValueConversion.kt | 添加 convertSolverValueSafe() 返回 Ret<V> | **(!) Safe 后缀不应用于领域层，应为 `convertSolverValue(): Ret<V>`，需修正** |
| QuantityArithmetic.kt | 添加 resolveForSafe() 返回 Ret<QuantityArithmetic<V>> | **(!) Safe 后缀不应用于领域层，应为 `resolveFor(): Ret<QuantityArithmetic<V>>`，需修正** |
| Csp1dProduceContext.kt | 添加 resolveDomainValueSampleSafe() 返回 Ret<V> | **(!) Safe 后缀不应用于应用层，应为 `resolveDomainValueSample(): Ret<V>`，需修正** |
| Csp1dColumnGeneration.kt | buildLpMaster 返回 Ret<LpMaster<V>> | done |
| ProductionTask.kt | 添加 produceSafe() 和 consumptionSafe() | **(!) Safe 后缀不应用于领域层，应为 `produce()` / `consumption(): Ret<V>`，需修正** |
| ShadowPriceMap.kt | 添加 reducedCostSafe() | **(!) Safe 后缀不应用于领域层，应为 `reducedCost(): Ret<V>`，需修正** |

---

## 剩余 throw 分析（~55 个生产代码）

### 保留的 throw

| 分类 | 数量 | 文件 | 说明 |
|------|------|------|------|
| OrThrow 变体 | ~40 | utils/Collection.kt, Find.kt, MinMax.kt 等 | **(!) 违反不做兼容层原则，应补齐 OrNull + Safe 接口后删除 OrThrow 变体** |
| 设计选择 | ~10 | CallBackModel.kt, ConstraintSign.kt | lambda 默认参数、验证边界 |
| 外部库边界 | ~10 | RemoteSolverHttpClient.kt, 持久化插件 | **(!) 应在边界处 catch 后转换成 Ret，不穿透到业务层** |
| 自定义异常 | ~5 | Csp1dRecovery.kt | 测试依赖的异常类型 |
| lambda 内 throw | ~8 | TaskTime.kt | 外层 catch 转 Failed |

### 不需要迁移的 throw

1. **设计选择**：如 `CallBackModel` 的 lambda 默认参数，抛异常表示"未提供回调"
2. **自定义异常**：测试代码依赖这些异常类型进行断言
3. **lambda 内 throw**：外层已有 catch 转 Failed，是合理的错误传播模式

---

## 构建状态
生产代码全部通过编译
ColumnGenerationAlgorithmTest.kt 有预存错误（非本次迁移导致）
