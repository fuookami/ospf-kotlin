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
- **已完成迁移**：持续推进中，底层库已新增多批 `OrNull` / `Safe` / `Ret` 主接口
- **当前全局剩余生产代码 throw**：46 个（不含测试 / gurobi-test / target / benchmark）
- **当前全局剩余生产代码 require/check/error 扫描命中**：207 个（包含 logger/error 函数名误报）
- **重点迁移范围剩余 throw**：serializer / iterator 协议边界与 numeric/math 边界，需继续分批处理

---

## 2026-06-19 底层库继续处理结果

| 文件 | 修改内容 | 状态 |
|------|---------|------|
| DataFrame.kt | 列名访问、列选择、`toMap()` 的列缺失失败改为 `OrNull` / `Safe` / 无后缀 `Ret` 或 `Try` | done |
| MultiArray.kt | 默认构造值失败改为 `newOrNull` / `newSafe` / 无后缀 `new(): Ret<...>` | done |
| CollectionExtensions.kt | `average()` 空集合失败改为 `Ret`，补齐 `averageSafe()`，保留 `averageOrNull()` | done |
| MinMax.kt | `Iterable<T>.minMax()` 空集合失败改为 `Ret`，补齐 `minMaxSafe()` | done |
| ExpressionDsl.kt | builder 空表达式失败改为 `buildOrNull()` + 无后缀 `build(): Ret<...>`；`booleanExpression()` 返回 `Ret` | done |
| Parse.kt | Ret 包装层中的转换失败不再用内部 `throw` 触发 catch，直接返回 `Failed` | done |
| PolynomialParser.kt | 泛型解析 Ret 包装层中的转换失败直接返回 `Failed`，不再用内部 `throw` 触发 catch | done |
| Sum.kt | `Array<out T>.sum()` 空数组失败改为 `Ret`，补齐 `sumSafe()` / `sumOrNull()` | done |
| AxisPermutation3.kt | math / quantities 的 `mapAxis()` 和圆柱 `apply/permute()` 改为传播 `Ret` | done |
| Factorization.kt | `defactorize()` 负指数失败改为 `Ret<I>` | done |
| Parse.kt / PolynomialParser.kt / PolynomialLexer.kt | Flt64 与泛型多项式/不等式 parser/lexer 的内部控制流改为 `ParseResult`，删除 `DirectParseError`；无后缀解析入口返回 `ParseResult<T>`，`OrNull` 入口表达 nullable 转换语义 | done |
| EinsumParser.kt | 字符串表示法和 Einstein DSL 入口改为返回 `Ret`，表示法错误和操作数数量错误返回 `Failed` | done |
| Operations.kt (einsum) | `matmul` / `dot` / `trace` / `outer` / `transpose` / `contract` 改为返回 `Ret`，维度不匹配、形状不兼容、轴越界、默认零值不可推断均返回 `Failed` | done |

### 本轮验证
- `mvn -pl ospf-kotlin-multiarray -am -DskipTests compile`：通过。
- `mvn -pl ospf-kotlin-math -am -DskipTests compile`：通过。
- `mvn -pl ospf-kotlin-quantities -am -DskipTests compile`：通过。
- `mvn -pl ospf-kotlin-quantities,ospf-kotlin-core,ospf-kotlin-framework-bpp3d/bpp3d-infrastructure -am -DskipTests compile`：通过。

### 当前全局剩余

全局生产扫描剩余 130 个 `throw`，主要集中在：
- `ospf-kotlin-multiarray`: `Shape` / iterator 边界。
- `ospf-kotlin-math`: value range、symbol operation、numeric constructors/operators。
- `ospf-kotlin-quantities`: quantity 运算符、单位换算、维度校验。

---

## 2026-06-19 继续处理结果

| 文件 | 修改内容 | 状态 |
|------|---------|------|
| SolverOutput.kt | 删除 `castSolverFlt64FallbackToValueOrThrow`，改为 `xxxOrNull` nullable 字段 + 无后缀 `Ret` 访问方法 | done |
| CallBackModel.kt | 未提供初始解 generator 时返回空列表，不再抛 `UnsupportedOperationException` | done |
| ConstraintSign.kt / Relation.kt / Constraint.kt / MechanismModel.kt | `ConstraintRelation` 改为 `ofOrNull` / `ofSafe`，`Comparison.NE` 在机制模型构造阶段返回 `Failed` | done |
| MechanismModelFlt64Conversion.kt | Flt64 边界转换改为传播 `Ret` | done |
| Library.kt | `loadInJar` 改为 `Try`，资源/IO/原生库加载失败返回 `Failed` | done |
| SCIP solver | `loadLibraryInJar()` 改为 `Try`，加载失败不再 `printStackTrace()` 或漏出 `UnsatisfiedLinkError` | done |
| Common.kt | 删除 worker pool 中 `catch { throw e }` 的无效重抛 | done |
| Either.kt / Orientation.kt | serializer 边界统一抛 `SerializationException`，移除非协议异常和 `printStackTrace()` | done |
| SolverExecutionPort.kt / RemoteSolverClient.kt / RemoteSolverHttpClient.kt | 远程 HTTP 执行端口改为 `Ret` / `Try`，HTTP/API/storage/serde 失败在边界转换为 `Failed` | done |

### 本轮验证
- `mvn -pl ospf-kotlin-core -am -DskipTests compile`：通过。
- `mvn -pl ospf-kotlin-framework -am -DskipTests compile`：通过。
- `mvn -pl ospf-kotlin-framework,ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-scip,ospf-kotlin-framework-bpp3d/bpp3d-infrastructure,ospf-kotlin-framework-bpp3d/bpp3d-domain-item-context,ospf-kotlin-framework-gantt-scheduling/gantt-scheduling-domain-resource-context,ospf-kotlin-framework-gantt-scheduling/gantt-scheduling-domain-task-compilation-context -am -DskipTests compile`：通过。

### 当前重点范围剩余

| 文件 | 剩余 throw | 处理意见 |
|------|-----------|----------|
| Either.kt | `throw SerializationException` 2 处 | `KSerializer.deserialize` 协议要求异常出口，已限定为序列化边界 |
| Orientation.kt | `throw SerializationException` 2 处 | `KSerializer.deserialize` 协议要求异常出口，已限定为序列化边界 |

### 当前全局剩余

全局生产扫描仍有 130 个 `throw`，主要集中在：
- `ospf-kotlin-multiarray`: `Shape` / `MultiArray` / `DataFrame` / iterator 边界。
- `ospf-kotlin-math`: symbol operation、numeric/algebra、value range、iterator/empty collection 风格 API。
- `ospf-kotlin-quantities`: quantity 运算符、单位换算、维度校验。

这些不能再笼统标记为"均为设计选择保留"。后续应按模块逐步补齐 `OrNull` / `Safe` 或将解析/转换入口改为 `Ret`；确实受 Kotlin 协议限制的 iterator/serializer 可单独登记为协议边界。

---

## 2026-06-19 MultiArrayView 向量索引继续处理结果

| 文件 | 修改内容 | 状态 |
|------|---------|------|
| MultiArrayView.kt | 外部向量索引入口改为 `Ret<T>`，维度不匹配返回 `Failed`，删除 1 处显式 `DimensionMismatchingException` 抛出 | done |
| MultiArrayView.kt / MappedMultiArrayView | 映射视图外部向量索引入口改为 `Ret<T>`，维度不匹配返回 `Failed`，删除 1 处显式 `DimensionMismatchingException` 抛出 | done |
| AccessOrder.kt | `MultiArrayView.iterWithOrder()` 适配 `Ret<T>` 索引结果；`Iterator.next()` 协议异常保留 | done |
| MultiArrayExtensions.kt | `MultiArrayView.get(Iterable<UInt64>)` 同步返回 `Ret<T>` | done |

### 本轮验证
- `mvn -pl ospf-kotlin-multiarray -am -DskipTests compile`：通过。
- `mvn -pl ospf-kotlin-math -am -DskipTests compile`：通过。

### 当前全局剩余

当轮全局生产扫描剩余 208 个 `throw`。`MultiArrayView.kt` 仅剩 Kotlin `Iterator.next()` 协议边界异常；`AccessOrder.kt` 仅剩 2 个 Kotlin `Iterator.next()` 协议边界异常。最新计数见下方 symbol operation 小节。

---

## 2026-06-19 symbol operation ordered/matrix 处理结果

| 文件 | 修改内容 | 状态 |
|------|---------|------|
| MatrixForm.kt | `toMatrixForm()` 系列改为 `Ret<...>`，符号缺失、重复 order、不可转二次返回 `Failed` | done |
| Flt64MatrixForm.kt | Flt64 矩阵形式便捷函数同步返回 `Ret<...>` | done |
| LinearQuadraticOps.kt / CanonicalOps.kt | ordered evaluation 改为 `Ret<T>`，order 尺寸不匹配、重复符号、符号缺失返回 `Failed` | done |
| Evaluate.kt / Inequality.kt | Flt64 ordered evaluation 和 ordered inequality satisfaction 同步返回 `Ret` | done |
| CompileOps.kt / Compile.kt | compile evaluation/gradient 编译阶段符号缺失返回 `Failed`，Flt64 便捷入口返回 `Ret<compiledFunction>` | done |
| Differentiate.kt | `CanonicalPolynomial<Flt64>.hessian()` 改为 `Ret<Array<DoubleArray>>`，非二次规范多项式返回 `Failed/Fatal` | done |
| CanonicalOps.kt / Evaluate.kt / CompileOps.kt | canonical 幂运算改为 `Ret` / `OrNull`；负指数在 `TimesGroup` 值域中支持，不支持时返回 `Failed/null`；compiled canonical 在编译阶段拒绝负指数 | done |
| PolynomialParser.kt | 泛型解析 Ret 包装层转换失败直接返回 `Failed` | done |
| Triangulation.kt | 3D 点集重复投影坐标失败改为 `Ret`；等值线三角剖分同步传播失败 | done |
| PlaneFrame3.kt | `normalAxis` 属性改为 `normalAxisOrNull` + `normalAxis(): Ret<Axis3>`；`distance()` / `vector()` 同步返回 `Ret` | done |
| symbol/expression/parser/Parser.kt | 布尔表达式递归下降 parser 改为 `Ret<BooleanExpression>`；`parseBooleanExpression()` 无后缀 Result 主接口，`parseBooleanExpressionOrNull()` nullable 入口 | done |
| symbol/operation/Parse.kt / symbol/parse/PolynomialParser.kt / PolynomialLexer.kt | Flt64 与泛型多项式/不等式 parser/lexer 改为 `ParseResult` 错误传播，删除 `DirectParseError` 控制流 | done |
| multiarray/einsum/EinsumParser.kt | 字符串表示法和 Einstein DSL 入口改为 `Ret`，表示法错误和操作数数量错误返回 `Failed` | done |
| multiarray/einsum/Operations.kt | 常见 einsum 运算改为 `Ret`，维度/形状/轴/默认零值失败返回 `Failed` | done |

### 本轮验证
- `mvn -pl ospf-kotlin-math -am -DskipTests compile`：通过。

### 当前全局剩余

全局生产扫描剩余 130 个 `throw`。`CanonicalOps.computeRingPower()` 的负指数保护已改为 `Ret` / `OrNull`；`symbol/operation/Parse.kt`、`symbol/parse/PolynomialParser.kt`、`PolynomialLexer.kt`、`multiarray/einsum/EinsumParser.kt`、`multiarray/einsum/Operations.kt` 已无生产 `throw` 命中。

---

## 2026-06-19 表达式求值、QuickDsl 与 quantities 平面框架处理结果

| 文件 | 修改内容 | 状态 |
|------|---------|------|
| EvaluateBoolean.kt | 默认标量函数求值器非法函数名、参数数量、参数类型不再抛异常，统一返回 `null` 并由三值逻辑传播为 `Unknown` | done |
| EvaluateBooleanTest.kt | 非法函数参数测试改为断言 `Trivalent.Unknown` | done |
| QuickDsl.kt | nullable transform 版本 `sum` / `flatSum` / `qsum` / `flatQSum` 改为返回 `Ret<...>`，空单项式列表返回 `Failed(ErrorCode.DataEmpty, ...)` | done |
| QuickDsl.kt | 补齐 `Safe` 和 `OrNull` 入口：`sumSafe` / `flatSumSafe` / `qsumSafe` / `flatQSumSafe` 与 `sumOrNull` / `flatSumOrNull` / `qsumOrNull` / `flatQSumOrNull` | done |
| Quantity PlaneFrame3.kt | `QuantityPlaneFrame3` 构造器收紧为私有，新增 `of(): Ret<...>` / `ofOrNull()`，删除构造校验 `require` 和 `normalAxis` getter 中的 `throw` | done |

### 本轮验证
- `mvn -pl ospf-kotlin-math -am -DskipTests compile`：通过。
- `mvn -pl ospf-kotlin-quantities -am -DskipTests compile`：通过。

### 当前全局剩余

全局生产扫描剩余 123 个 `throw`。`EvaluateBoolean.kt`、`QuickDsl.kt`、`ospf-kotlin-quantities/.../PlaneFrame3.kt` 已无生产 `throw` 命中。

---

## 2026-06-19 geometry QuantityOps / Box / Placement 处理结果

| 文件 | 修改内容 | 状态 |
|------|---------|------|
| math/geometry/QuantityOps.kt | 几何比较 helper 改为 `quantityOrdSafe()` / `quantityOrdOrNull()`，max/min/clamp/range 判断改为 `Ret` 传播 | done |
| math/geometry/Box2.kt / Box3.kt | `contains()` / `overlapped()` / `intersect()` 改为 `Ret<Boolean>` 或 `Ret<Box?>` | done |
| math/geometry/Placement2.kt / Placement3.kt | 同步传播底层 box 的 `Ret` 结果 | done |
| quantities/geometry/QuantityOps.kt | 物理量几何加减、比较、极值、clamp、range 判断补齐 `Safe` / `OrNull`，单位转换和量纲失败返回 `Failed` | done |
| quantities/geometry/Box2.kt / Box3.kt | 可失败 `maxX/maxY/maxZ` getter 改为 `maxXOrNull` / `maxYOrNull` / `maxZOrNull`，无后缀函数返回 `Ret`；几何谓词/求交返回 `Ret` | done |
| quantities/geometry/Placement2.kt / Placement3.kt | 同步提供 `max*OrNull` 与 `max*(): Ret`，并传播 `contains/overlapped/intersect` 的 `Ret` | done |
| quantities/geometry/Projection2.kt / Cylinder3.kt | `diameter` 同单位自加改为直接构造 `Quantity`，不再依赖可失败 helper | done |

### 本轮验证
- `mvn -pl ospf-kotlin-math -am -DskipTests compile`：通过。
- `mvn -pl ospf-kotlin-quantities -DskipTests compile`：通过。
- `mvn -pl ospf-kotlin-quantities -am -DskipTests compile`：通过。

### 当前全局剩余

全局生产扫描剩余 90 个 `throw`。本轮已清除 math / quantities 两套 geometry `QuantityOps.kt` 中的 4 个实际 `throw`。

主要剩余分布：
- `ValueWrapper.kt`: 29 个。
- `Shape.kt`: 28 个。
- `SymbolQuantityOps.kt`: 8 个。
- `ConstantProviders.kt`: 3 个。
- `Quantity.kt`: 3 个。
- 其余为 serializer / iterator 协议边界、numeric operator、KSP 字符串与少量数学构造边界。

---

## 2026-06-19 Scale 与 SymbolQuantityOps 处理结果

| 文件 | 修改内容 | 状态 |
|------|---------|------|
| math/Scale.kt | `Scale / FltX`、`Scale / RtnX` 除零失败改为 nullable 返回；新增 `divOrNull()` / `divSafe(): Ret<Scale>` | done |
| quantities/math/symbol/SymbolQuantityOps.kt | 线性多项式物理量 `plus` / `minus` operator 改为返回 `Ret<Quantity<...>>`，量纲不匹配和单位转换失败返回 `Failed` | done |
| quantities/math/symbol/SymbolQuantityOps.kt | 补齐 `plusSafe` / `minusSafe` 与 `plusOrNull` / `minusOrNull`，删除 8 个实际 `throw` | done |
| math/operator/Reciprocal.kt | 调整 KDoc，删除 `throw` 审计误报 | done |

### 本轮验证
- `mvn -pl ospf-kotlin-math -am -DskipTests compile`：通过。
- `mvn -pl ospf-kotlin-quantities -am -DskipTests compile`：通过。

### 当前全局剩余

当轮全局生产扫描剩余 79 个 `throw`。`Scale.kt` 与 `SymbolQuantityOps.kt` 已无生产 `throw` 命中；最新计数见下方 Quantity 乘除与 Shape 安全入口处理小节。

主要剩余分布：
- `ValueWrapper.kt`: 29 个。
- `Shape.kt`: 28 个。
- `ConstantProviders.kt`: 3 个。
- `Quantity.kt`: 3 个。
- 其余为 serializer / iterator 协议边界、numeric operator、KSP 字符串与少量数学构造边界。

---

## 2026-06-19 Quantity 乘除与 Shape 安全入口处理结果

| 文件 | 修改内容 | 状态 |
|------|---------|------|
| quantities/quantity/Quantity.kt | 标量乘除、一元负号的仿射单位失败改为 `Safe` / `OrNull`，删除 3 个 `require` | done |
| quantities/geometry/QuantityOps.kt | 增加确定性 `quantityProduct(...)`，供内部几何派生计算使用 | done |
| quantities/geometry/Box2.kt / Projection2.kt / Cuboid3.kt / Cylinder3.kt | 面积、体积、平方距离等确定性计算改用内部 helper | done |
| BPP2D / BPP3D / CSP1D 调用点 | 面积、体积、重量等 quantity 乘除调用同步适配 nullable operator | done |
| multiarray/Shape.kt | 新增 `offsetSafe` / `offsetOrNull` 与 `dummyVectorSafe` / `dummyVectorOrNull`，为后续移除 DSL throwing 包装铺底 | done |
| math/algebra/number/Floating.kt | `FltXJsonSerializer` 非 JSON decoder 边界改抛 `SerializationException` | done |
| math/Serialization.kt | 新增 serializer 协议失败集中 helper，供 math serializer 复用 | done |
| math/algebra/value_range/ValueWrapper.kt / ValueRange.kt | JSON encoder/decoder/object/field 校验从 `require` 改为 serializer 协议 helper | done |
| math/algebra/number/Rational.kt | serializer JSON 校验从 `require` 改为 serializer 协议 helper；分母为零构造边界仍保留待单独迁移 | done |

### 本轮验证
- `mvn -pl ospf-kotlin-quantities -am -DskipTests compile`：通过。
- `mvn -pl ospf-kotlin-framework-bpp3d/bpp3d-infrastructure -am -DskipTests compile`：通过。
- `mvn -pl ospf-kotlin-framework-bpp2d,ospf-kotlin-framework-bpp3d/bpp3d-infrastructure,ospf-kotlin-framework-bpp3d/bpp3d-domain-item-context,ospf-kotlin-framework-csp1d/csp1d-domain-material-context,ospf-kotlin-framework-gantt-scheduling/gantt-scheduling-domain-resource-context -am -DskipTests compile`：通过。
- `mvn -pl ospf-kotlin-multiarray -am -DskipTests compile`：通过。
- `mvn -pl ospf-kotlin-math -am -DskipTests compile`：通过。

### 当前全局剩余

全局生产扫描剩余 46 个 `throw`。`Quantity.kt` 已无生产 `throw` / `require` 命中；`Shape.kt` 已补安全入口，但旧 DSL operator 包装仍保留。serializer 的 JSON 校验 `require` 已集中迁到协议 helper。

主要剩余分布：
- `Shape.kt`: 29 个。
- Serializer 协议边界：`Either.kt`、`Orientation.kt`、`Floating.kt`、`math/Serialization.kt`。
- Iterator 协议边界：`AccessOrder.kt` 2 个、`MultiArrayView.kt` 1 个、`IntegerRange.kt` 1 个。
- Numeric/math 边界：`FltXPowerStrategy.kt`、`Integer.kt`、`UInteger.kt`、`Rational.kt`、`ConstantProviders.kt`。

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

### OrThrow 变体清理（5 个文件，本轮完成）
| 文件 | 修改内容 | 状态 |
|------|---------|------|
| Collection.kt | 删除 24 个 throwing 变体，添加 24 个 Safe 变体 | done |
| Find.kt (parallel) | 删除 4 个 throwing 变体 | done |
| MinMax.kt (parallel) | 删除 1 个 throwing 变体 | done |
| MaxMin.kt (parallel) | 删除 2 个 throwing 变体 | done |
| CollectionExtensions.kt | 添加 6 个 Safe average 变体 | done |

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

## 剩余 throw 分析（旧记录，待按当前扫描继续修正）

### 已完成的迁移（本轮）

| 文件 | 修改内容 | 状态 |
|------|---------|------|
| Collection.kt | 删除 24 个 throwing 变体，添加 24 个 Safe 变体 | done |
| Find.kt (parallel) | 删除 4 个 throwing 变体（firstParallelly, lastParallelly, firstNotNullOfParallelly, lastNotNullOfParallelly） | done |
| MinMax.kt (parallel) | 删除 1 个 throwing 变体（minMaxByParallelly） | done |
| MaxMin.kt (parallel) | 删除 2 个 throwing 变体（maxByParallelly, minByParallelly） | done |
| CollectionExtensions.kt | 添加 6 个 Safe average 变体（safeAverage, safeAverageAsFlt64） | done |

### 旧版保留判断（不再完全准确）

| 分类 | 数量 | 文件 | 说明 |
|------|------|------|------|
| 设计选择 | ~18 | CallBackModel.kt, ConstraintSign.kt, BigM.kt, Shape.kt, Quantity.kt, Framework-plugin | lambda 默认参数、验证边界、运算符重载维度校验、策略模式 |
| 自定义异常 | ~5 | Csp1dRecovery.kt | 测试依赖的异常类型 |
| lambda 内 throw | ~8 | TaskTime.kt | 外层 catch 转 Failed |

### 不需要迁移的 throw（需逐项复核）

1. **设计选择**：如 `CallBackModel` 的 lambda 默认参数，抛异常表示"未提供回调"
2. **运算符重载**：Quantity 的 `+`/`-` 运算符无法返回 Ret，维度校验必须抛异常
3. **Shape 验证**：维度/索引验证是编程错误检测，应快速失败
4. **自定义异常**：测试代码依赖这些异常类型进行断言
5. **lambda 内 throw**：外层已有 catch 转 Failed，是合理的错误传播模式
6. **Framework-plugin 策略模式**：FailFast 策略故意抛异常，AlwaysFalse 返回 false

---

## 构建状态
本轮涉及的 core/framework/SCIP/BPP3D/Gantt 目标模块生产代码均已通过 `-DskipTests compile`。
ColumnGenerationAlgorithmTest.kt 有预存错误（非本次迁移导致）
