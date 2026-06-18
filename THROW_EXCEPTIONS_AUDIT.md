# 抛异常审计清单

## 审计目标
将所有 `throw` 语句替换为返回 `Result` 类型（Failed/Fatal），遵循项目错误处理规范。

## 实际统计

- **生产代码 throw 总数**：~395 个
- **涉及文件数**：~105 个
- **测试代码 throw**：~146 个（保留）
- **已完成迁移**：39 个文件，~56 个 throw（约 14%）

---

## 已完成迁移清单

| 文件 | 原 throw 数 | 修改内容 | 状态 |
|------|------------|---------|------|
| Csp1dShadowPriceLifecycle.kt | 2 | extractFromDualSolution 返回 Ret | ✅ |
| Csp1dMilpSolver.kt | 8 | 新增 solveRet()，保留 solve() 兼容 | ✅ |
| Csp1dColumnGeneration.kt | 5 | 删除 ensureRet，直接传播 Result | ✅ |
| TaskTime.kt | 1 | register 返回 Failed | ✅ |
| StorageResource.kt | 1 | register 返回 Failed | ✅ |
| ExecutionResource.kt | 1 | register 返回 Failed | ✅ |
| ConnectionResource.kt | 1 | register 返回 Failed | ✅ |
| Produce.kt | 1 | register 返回 Failed | ✅ |
| Consumption.kt | 1 | register 返回 Failed | ✅ |
| TaskStepConflictConstraint.kt | 1 | refresh 返回 Failed | ✅ |
| ProductionTask.kt | 4 | quantityZero 返回 Ret | ✅ |
| RemoteSolverHttpTransportPlugin.kt | 1 | 添加 imports | ✅ |
| Orientation.kt | 1 | require 返回 Ret | ✅ |
| SatisfiedAmount.kt | 1 | registerConstraints 返回 Failed | ✅ |
| MetaModelExportSupport.kt | 1 | throw → return Failed | ✅ |
| ColumnGenerationAlgorithm.kt | 0 | 接口返回 Ret | ✅ |
| ColumnGenerationStandardExecutors.kt | 6 | 删除 ensureTry/ensureRet | ✅ |
| ContinuousRadiusModelComponent.kt | 0 | register 返回 Try | ✅ |
| TimeWindow.kt | 1 | upper/upperInterval 改为 nullable + Safe | ✅ |
| Resource.kt | 1 | 添加 error imports | ✅ |
| Cost.kt | 1 | solverCost 改为 nullable + Safe | ✅ |
| ShadowPriceMap.kt | 0 | 添加 imports | ✅ |
| Csp1dProduceContext.kt | 0 | produce[index] 添加 !! | ✅ |
| WasteObjectivePipeline.kt | 0 | produce[index] 添加 !! | ✅ |
| BatchMinimizationObjective.kt | 0 | produce[index] 添加 !! | ✅ |
| Compilation.kt (task) | 0 | solverCost() 添加 !! | ✅ |
| Compilation.kt (bunch) | 0 | solverCost() 添加 !! | ✅ |
| TaskBunch.kt | 0 | solverCost() 添加 !! | ✅ |
| CylinderShapeContract.kt | 12 | 8 个 require 验证函数返回 Try | ✅ |
| PackingGeometryGuard.kt | 3 | requireHorizontalCylinderSupport + requirePackedBinShapeGeometry 返回 Try | ✅ |
| Packer.kt | 1 | requireSingleCylinderAxisPerLayer 返回 Try | ✅ |
| SymbolDimensionRegistry.kt | 6 | validateAddSubDimension 返回 Try，inferDimension 返回 Ret | ✅ |
| LayerPlacementAdapter.kt | 0 | requireVerifiedGeneratedCylinderCandidate 添加 !! | ✅ |
| LayerGenerationContext.kt | 0 | 4 个验证函数调用添加 !! | ✅ |
| Package.kt | 0 | requireConcreteCylinderRadiusProductionMetadata 添加 !! | ✅ |
| PackageAttribute.kt | 0 | requireUprightVerticalCylinderSupport 添加 !! | ✅ |
| ItemMerger.kt | 0 | 7 个 requireNoCylinderItemsForCuboidOnlyPath 调用添加 !! | ✅ |
| SimpleBlockGenerator.kt | 0 | requireSupportedCylinderItemForSimpleBlock 添加 !! | ✅ |
| CylinderUnsupportedGuard.kt | 0 | requireNoCylinderItemsForCuboidOnlyPath 添加 !! | ✅ |
| PackingRendererAdapter.kt | 0 | requirePackedBinShapeGeometry 添加 !! | ✅ |

**总计**：39 个文件，~56 个 throw

---

## 剩余 throw 分析

### 需要修改函数签名的 throw（~300 个）

这些 throw 在返回具体类型的函数中，需要修改函数签名才能改为返回 Result。

**主要分布**：
- ospf-kotlin-math: ~120 个（ValueWrapper.kt、Operations.kt 等）
- ospf-kotlin-multiarray: ~39 个（Shape.kt、DataFrame.kt 等）
- ospf-kotlin-utils: ~42 个（Collection.kt、Find.kt 等）
- ospf-kotlin-quantities: ~16 个（Quantity.kt、SymbolQuantityOps.kt 等）
- ospf-kotlin-framework-bpp3d: ~30 个（Package.kt 等，CylinderShapeContract.kt、PackingGeometryGuard.kt、Packer.kt 已迁移）

### 私有辅助函数中的 throw（~50 个）

被内部调用，修改影响可控。

### 验证函数中的 throw（~8 个）

部分已在阶段 1 迁移完成，剩余为未涵盖的验证函数。

返回 Unit 的验证函数，可改为返回 Try。

### 保留的 throw

- **数学运算边界**（~30 个）：ValueWrapper.kt 中的无穷大运算，保留异常合理
- **序列化/反序列化**（~10 个）：外部库交互边界
- **测试代码**（~146 个）：保留

---

## 下一步执行计划

### 阶段 1：验证函数改为返回 Try（~30 个）✅ 已完成
- CylinderShapeContract.kt 中的验证函数 → 8 个函数返回 Try
- PackingGeometryGuard.kt 中的验证函数 → 2 个函数返回 Try
- Packer.kt 中的验证函数 → 1 个函数返回 Try

### 阶段 2：私有辅助函数改为返回 Result（~50 个）
- 各模块中的私有辅助函数

### 阶段 3：核心库提供 safe API（~200 个）
- Collection.kt: minOrNull() / minSafe()
- Find.kt: firstOrNull() / firstSafe()
- Shape.kt: getOrNull() / getSafe()
- Quantity.kt: convertOrNull() / convertSafe()

### 阶段 4：外部库边界异常捕获（~10 个）
- RemoteSolverHttpClient.kt
- 持久化插件
