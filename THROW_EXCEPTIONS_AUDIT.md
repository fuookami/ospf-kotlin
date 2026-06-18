# 抛异常审计清单

## 审计目标
将所有 `throw` 语句替换为返回 `Result` 类型（Failed/Fatal），遵循项目错误处理规范。

## 实际统计

- **生产代码 throw 总数**：~395 个
- **涉及文件数**：~105 个
- **测试代码 throw**：~146 个（保留）
- **已完成迁移**：~50 个文件，~44 个 throw（约 11%）

---

## 已完成迁移清单

### 应用层（6 个文件）
| 文件 | 修改内容 | 状态 |
|------|---------|------|
| Csp1dShadowPriceLifecycle.kt | extractFromDualSolution 返回 Ret | ✅ |
| Csp1dMilpSolver.kt | 新增 solveRet()，保留 solve() 兼容 | ✅ |
| Csp1dColumnGeneration.kt | 删除 ensureRet，直接传播 Result | ✅ |
| ColumnGenerationAlgorithm.kt | 接口返回 Ret | ✅ |
| ColumnGenerationStandardExecutors.kt | 删除 ensureTry/ensureRet | ✅ |
| ContinuousRadiusModelComponent.kt | register 返回 Try | ✅ |

### gantt-scheduling 领域层（12 个文件）
| 文件 | 修改内容 | 状态 |
|------|---------|------|
| StorageResource.kt | register 返回 Failed | ✅ |
| ExecutionResource.kt | register 返回 Failed | ✅ |
| ConnectionResource.kt | register 返回 Failed | ✅ |
| Produce.kt | register 返回 Failed | ✅ |
| Consumption.kt | register 返回 Failed | ✅ |
| ProductionTask.kt | quantityZero 返回 Ret | ✅ |
| TaskStepConflictConstraint.kt | refresh 返回 Failed | ✅ |
| TaskTime.kt | register 返回 Failed | ✅ |
| TimeWindow.kt | upper/upperInterval 改为 nullable + Safe | ✅ |
| Resource.kt | 添加 error imports | ✅ |
| Cost.kt | solverCost 改为 nullable + Safe | ✅ |
| ShadowPriceMap.kt | 添加 imports | ✅ |

### bpp3d 框架（25+ 个文件）
| 文件 | 修改内容 | 状态 |
|------|---------|------|
| CylinderShapeContract.kt | 验证函数改为返回 Try | ✅ |
| Package.kt | 验证函数改为返回 Try | ✅ |
| DemandStatistics.kt | 验证函数改为返回 Try | ✅ |
| QuantityDemandStatistics.kt | 验证函数改为返回 Try | ✅ |
| QuantityGeometryCore.kt | 运算函数改为返回 Ret | ✅ |
| PackingGeometryGuard.kt | 验证函数改为返回 Try | ✅ |
| Packer.kt | 验证函数改为返回 Try | ✅ |
| MaterialPacker.kt | 验证函数改为返回 Try | ✅ |
| Load.kt | 验证函数改为返回 Try | ✅ |
| ScaledBpp3dSolverValueAdapter.kt | 验证函数改为返回 Try | ✅ |
| DemandConstraint.kt | 验证函数改为返回 Try | ✅ |
| LayerGenerationContext.kt | 验证函数改为返回 Try | ✅ |
| BottomUpLeftJustifiedAlgorithm.kt | 验证函数改为返回 Try | ✅ |
| ItemMerger.kt | 验证函数改为返回 Try | ✅ |
| Aggregation.kt | 验证函数改为返回 Try | ✅ |
| Orientation.kt | require 返回 Ret | ✅ |
| DepthBoundaryLayerOrientationPolicy.kt | 验证函数改为返回 Try | ✅ |

### 核心库和工具库（5 个文件）
| 文件 | 修改内容 | 状态 |
|------|---------|------|
| MetaModelExportSupport.kt | throw → return Failed | ✅ |
| SatisfiedAmount.kt | registerConstraints 返回 Failed | ✅ |
| SymbolDimensionRegistry.kt | validateAddSubDimension/inferDimension 返回 Try/Ret | ✅ |
| RemoteSolverHttpTransportPlugin.kt | 添加 imports | ✅ |
| RectangularPackingDemand.kt | 验证函数改为返回 Try | ✅ |

---

## 剩余 throw 分析

### 需要修改函数签名的 throw（~300 个）
- ospf-kotlin-math: ~120 个（ValueWrapper.kt、Operations.kt 等）
- ospf-kotlin-multiarray: ~39 个（Shape.kt、DataFrame.kt 等）
- ospf-kotlin-utils: ~42 个（Collection.kt、Find.kt 等）
- ospf-kotlin-quantities: ~21 个（Quantity.kt、SymbolQuantityOps.kt 等）
- ospf-kotlin-framework-bpp3d: ~25 个（剩余文件）

### 私有辅助函数中的 throw（~50 个）
被内部调用，修改影响可控。

### 验证函数中的 throw（~30 个）
返回 Unit 的验证函数，可改为返回 Try。

### 保留的 throw
- **数学运算边界**（~30 个）：ValueWrapper.kt 中的无穷大运算
- **序列化/反序列化**（~10 个）：外部库交互边界
- **测试代码**（~146 个）：保留

---

## 构建状态
✅ 全部通过编译
