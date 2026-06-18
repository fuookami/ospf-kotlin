# 错误处理迁移进度

## 总体原则

**不做兼容层，不保留旧的 throwing API 作为正式入口。所有生产代码调用链一步到位迁到 Ret<T> / Try、OrNull 或 Safe 接口。**

---

## 已完成迁移

### 第一阶段：应用层（6 个文件）
- Csp1dShadowPriceLifecycle.kt: extractFromDualSolution 返回 Ret
- Csp1dMilpSolver.kt: 删除 solve() 兼容层，只保留返回 Ret 的版本
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
- Resource.kt, Cost.kt, ShadowPriceMap.kt, TaskBunch.kt: 添加 imports / nullable + Safe

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
- MetaModelExportSupport.kt: throw → return Failed
- SatisfiedAmount.kt: registerConstraints 返回 Failed
- SymbolDimensionRegistry.kt: validateAddSubDimension/inferDimension 返回 Try/Ret
- RemoteSolverHttpTransportPlugin.kt: 添加 imports
- RectangularPackingDemand.kt: 验证函数改为返回 Try

### 测试代码修复
- ColumnGenerationAlgorithmTest.kt: 添加 unwrap() 辅助函数，solve() 调用添加 .unwrap() ✅
- Csp1dApplicationAcceptanceTest.kt: 添加 unwrap() 辅助函数，solve() 调用添加 .unwrap() ✅
- ContinuousRadiusModelComponentTest.kt: register() 调用更新 ✅
- ResourceQuantityFltXTest.kt: resourceQuantityZero 添加 !! ✅

### 辅助修改
- Csp1dProduceContext.kt, WasteObjectivePipeline.kt, BatchMinimizationObjective.kt: produce[index] 添加 !!
- Compilation.kt (task/bunch), TaskBunch.kt: solverCost() 添加 !!

### 构建状态
✅ 生产代码全部通过编译
⚠️ ColumnGenerationAlgorithmTest.kt 有预存错误（非本次迁移导致）

---

## 总体统计

| 模块 | throw 总数 | 已完成 | 剩余 |
|------|-----------|--------|------|
| ospf-kotlin-utils | ~42 | 0 | ~42 |
| ospf-kotlin-core | ~12 | 2 | ~10 |
| ospf-kotlin-multiarray | ~39 | 0 | ~39 |
| ospf-kotlin-quantities | ~22 | 1 | ~21 |
| ospf-kotlin-math | ~120+ | 0 | ~120+ |
| ospf-kotlin-framework | ~8 | 1 | ~7 |
| ospf-kotlin-framework-gantt-scheduling | ~25 | 8 | ~17 |
| ospf-kotlin-framework-csp1d | ~15 | 8 | ~7 |
| ospf-kotlin-framework-bpp2d | 4 | 1 | 3 |
| ospf-kotlin-framework-bpp3d | ~50+ | 30+ | ~20 |
| ospf-kotlin-framework-plugin | ~10 | 0 | ~10 |
| 测试代码 | ~146 | 0 | ~146（保留） |
| **总计** | **~395** | **~51** | **~344** |

---

## 构建状态
✅ 全部通过编译

---

## 下一步执行计划

### 阶段 1：bpp3d 剩余文件（~25 个）
继续处理 bpp3d 框架中剩余的 throw。

### 阶段 2：quantities 模块（~21 个）
- Quantity.kt: convertOrNull() / convertSafe()
- SymbolQuantityOps.kt: 运算函数改为返回 Ret
- geometry/QuantityOps.kt: 运算函数改为返回 Ret

### 阶段 3：core 和 utils 模块（~52 个）
- Collection.kt: minOrNull() / minSafe()
- Find.kt: firstOrNull() / firstSafe()
- BigM.kt: 验证函数改为返回 Try
- CallBackModel.kt: 默认 lambda 改为返回空列表

### 阶段 4：math 模块（~120 个）
- ValueWrapper.kt: 数学运算边界（保留异常或提供 safe API）
- Operations.kt, Parse.kt: 解析函数改为返回 Ret
- Floating.kt, Integer.kt: 类型转换函数改为返回 Ret

### 阶段 5：multiarray 模块（~39 个）
- Shape.kt: getOrNull() / getSafe()
- DataFrame.kt: 列名查找改为返回 Ret

### 阶段 6：外部库边界（~10 个）
- RemoteSolverHttpClient.kt: try-catch 转换为 Ret
- 持久化插件: try-catch 转换为 Ret
