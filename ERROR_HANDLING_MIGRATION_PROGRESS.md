# 错误处理迁移进度

## 总体原则

**不做兼容层，不保留旧的 throwing API 作为正式入口。所有生产代码调用链一步到位迁到 Ret<T> / Try、OrNull 或 Safe 接口。**

### 1. 编排层与领域失败
直接修改原函数签名：
```kotlin
fun solve(...): Ret<SolveResult>
fun register(...): Try
fun convert(...): Ret<Quantity<V>>
```

### 2. Safe 命名规范
需要保留"显式安全调用"语义的接口统一使用 Safe 后缀：
```kotlin
fun upperSafe(): Ret<TimeWindow<V>>
fun solverCostSafe(default: Flt64? = null): Ret<Flt64>
fun resourceQuantityZeroSafe(...): Ret<V>
```

### 3. 属性 / lazy / 构造默认值
getter、by lazy、构造参数默认值中不能返回 Ret，原属性降级为 nullable，同时添加 Safe 接口：
```kotlin
val upper: TimeWindow<V>? get() = upperSafe().value
fun upperSafe(): Ret<TimeWindow<V>>

val solverCost: Flt64? get() = costSum?.value?.toFlt64()
fun solverCostSafe(default: Flt64? = null): Ret<Flt64>
```

### 4. 底层工具 / 数学 / multiarray / quantities
补齐两类安全入口：
```kotlin
fun getOrNull(...): T?
fun getSafe(...): Ret<T>

fun averageOrNull(...): V?
fun averageSafe(...): Ret<V>
```

### 5. 外部库 / 协议边界
异常只允许在边界处捕获，立即转换成 Ret / Try：
```kotlin
return try {
    ok(client.call(...))
} catch (e: Throwable) {
    Failed(ErrorCode.ApplicationFailed, "外部调用失败：${e.message}")
}
```

---

## 已完成的迁移（39 个文件）

### 第一阶段：应用层
| 文件 | 修改内容 | 状态 |
|------|---------|------|
| Csp1dShadowPriceLifecycle.kt | extractFromDualSolution 返回 Ret | ✅ |
| Csp1dMilpSolver.kt | 新增 solveRet()，保留 solve() 兼容 | ✅ |
| Csp1dColumnGeneration.kt | 删除 ensureRet，直接传播 Result | ✅ |
| TaskTime.kt | register 返回 Failed | ✅ |

### 第二阶段：领域层
| 文件 | 修改内容 | 状态 |
|------|---------|------|
| StorageResource.kt | register 返回 Failed | ✅ |
| ExecutionResource.kt | register 返回 Failed | ✅ |
| ConnectionResource.kt | register 返回 Failed | ✅ |
| Produce.kt | register 返回 Failed | ✅ |
| Consumption.kt | register 返回 Failed | ✅ |
| TaskStepConflictConstraint.kt | refresh 返回 Failed | ✅ |
| ProductionTask.kt | quantityZero 返回 Ret | ✅ |

### 第三阶段：框架层
| 文件 | 修改内容 | 状态 |
|------|---------|------|
| RemoteSolverHttpTransportPlugin.kt | 添加 imports | ✅ |
| Orientation.kt | require 返回 Ret | ✅ |
| ColumnGenerationAlgorithm.kt | 接口返回 Ret | ✅ |
| ColumnGenerationStandardExecutors.kt | 删除 ensureTry/ensureRet | ✅ |
| ContinuousRadiusModelComponent.kt | register 返回 Try | ✅ |
| TimeWindow.kt | upper/upperInterval 改为 nullable + Safe | ✅ |
| Resource.kt | 添加 error imports | ✅ |
| Cost.kt | solverCost 改为 nullable + Safe | ✅ |
| ShadowPriceMap.kt | 添加 imports | ✅ |

### 第四阶段：核心库
| 文件 | 修改内容 | 状态 |
|------|---------|------|
| SatisfiedAmount.kt | registerConstraints 返回 Failed | ✅ |
| MetaModelExportSupport.kt | throw → return Failed | ✅ |

### 第五阶段：辅助修改
| 文件 | 修改内容 | 状态 |
|------|---------|------|
| Csp1dProduceContext.kt | produce[index] 添加 !! | ✅ |
| WasteObjectivePipeline.kt | produce[index] 添加 !! | ✅ |
| BatchMinimizationObjective.kt | produce[index] 添加 !! | ✅ |
| Compilation.kt (task) | solverCost() 添加 !! | ✅ |
| Compilation.kt (bunch) | solverCost() 添加 !! | ✅ |
| TaskBunch.kt | solverCost() 添加 !! | ✅ |

### 第六阶段：验证函数改为返回 Try（20 个 throw，20 个调用点）
| 文件 | 修改内容 | 状态 |
|------|---------|------|
| CylinderShapeContract.kt | 8 个 require 验证函数返回 Try（12 个 throw → return Failed） | ✅ |
| PackingGeometryGuard.kt | requireHorizontalCylinderSupport + requirePackedBinShapeGeometry 返回 Try（3 个 throw） | ✅ |
| Packer.kt | requireSingleCylinderAxisPerLayer 返回 Try（1 个 throw），invoke 添加 !! | ✅ |
| SymbolDimensionRegistry.kt | validateAddSubDimension 返回 Try，inferDimension 返回 Ret（6 个 throw） | ✅ |
| LayerPlacementAdapter.kt | requireVerifiedGeneratedCylinderCandidate 添加 !! | ✅ |
| LayerGenerationContext.kt | 4 个验证函数调用添加 !! | ✅ |
| Package.kt | requireConcreteCylinderRadiusProductionMetadata 添加 !! | ✅ |
| PackageAttribute.kt | requireUprightVerticalCylinderSupport 添加 !! | ✅ |
| ItemMerger.kt | 7 个 requireNoCylinderItemsForCuboidOnlyPath 调用添加 !! | ✅ |
| SimpleBlockGenerator.kt | requireSupportedCylinderItemForSimpleBlock 添加 !! | ✅ |
| CylinderUnsupportedGuard.kt | requireNoCylinderItemsForCuboidOnlyPath 添加 !! | ✅ |
| PackingRendererAdapter.kt | requirePackedBinShapeGeometry 添加 !! | ✅ |

---

## 总体统计

| 模块 | throw 总数 | 已完成 | 剩余 |
|------|-----------|--------|------|
| ospf-kotlin-utils | ~42 | 0 | ~42 |
| ospf-kotlin-core | ~12 | 2 | ~10 |
| ospf-kotlin-multiarray | ~39 | 0 | ~39 |
| ospf-kotlin-quantities | ~22 | 6 | ~16 |
| ospf-kotlin-math | ~120+ | 0 | ~120+ |
| ospf-kotlin-framework | ~8 | 1 | ~7 |
| ospf-kotlin-framework-gantt-scheduling | ~25 | 8 | ~17 |
| ospf-kotlin-framework-csp1d | ~15 | 6 | ~9 |
| ospf-kotlin-framework-bpp2d | 4 | 0 | 4 |
| ospf-kotlin-framework-bpp3d | ~50+ | 20 | ~30+ |
| ospf-kotlin-framework-plugin | ~10 | 0 | ~10 |
| 测试代码 | ~146 | 0 | ~146（保留） |
| **总计** | **~395** | **~43** | **~352** |

---

## 下一步执行计划

### 阶段 1：验证函数改为返回 Try（~30 个）✅ 已完成
按原则 1 处理返回 Unit 的验证函数：
- CylinderShapeContract.kt 中的验证函数 → 8 个函数返回 Try（12 个 throw）
- PackingGeometryGuard.kt 中的验证函数 → 2 个函数返回 Try（3 个 throw）
- Packer.kt 中的验证函数 → 1 个函数返回 Try（1 个 throw）
- SymbolDimensionRegistry.kt → validateAddSubDimension 返回 Try，inferDimension 返回 Ret（6 个 throw）

### 阶段 2：私有辅助函数改为返回 Result（~50 个）
逐个处理各模块中的私有辅助函数。

### 阶段 3：核心库提供 safe API（~200 个）
按原则 4 补齐安全入口：
- Collection.kt: `minOrNull()` / `minSafe()`
- Find.kt: `firstOrNull()` / `firstSafe()`
- Shape.kt: `getOrNull()` / `getSafe()`
- Quantity.kt: `convertOrNull()` / `convertSafe()`

### 阶段 4：外部库边界异常捕获（~10 个）
按原则 5 处理：
- RemoteSolverHttpClient.kt
- 持久化插件

---

## 构建状态
✅ 全部通过编译
