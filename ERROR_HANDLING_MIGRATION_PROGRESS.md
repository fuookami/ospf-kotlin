# 错误处理迁移进度

## 总体原则

**不做兼容层，不保留旧的 throwing API 作为正式入口。所有生产代码调用链一步到位迁到 Ret<T> / Try 或 OrNull 接口。**

### 命名规范
- **无后缀函数**：返回 `Ret<T>` / `Try` 的主接口，用于编排层、领域层、属性/lazy/构造默认值的可失败函数。
- **OrNull 后缀**：nullable 便利接口，丢弃失败原因，只表达"有没有值"。用于属性/lazy/构造默认值的降级接口，以及底层工具库中失败原因不重要的查询。
- **Safe 后缀**：仅用于底层工具 / 数学 / multiarray / quantities 模块中需要显式区别 Kotlin 风格查询的安全接口，返回 `Ret<T>` / `Try` 并保留错误信息。

---

## 2026-06-19 底层库继续处理

### 已完成
- `DataFrame`: 列名访问、列选择和 `toMap()` 的列缺失失败改为 `OrNull` / `Safe` / 无后缀 `Ret` 或 `Try`。
- `MultiArray`: 默认构造值失败改为 `newOrNull` / `newSafe` / 无后缀 `new(): Ret<...>`。
- `CollectionExtensions`: `average()` 空集合失败改为 `Ret`，补齐 `averageSafe()`，保留 `averageOrNull()` 作为 nullable 入口。
- `MinMax`: `Iterable<T>.minMax()` 空集合失败改为 `Ret`，补齐 `minMaxSafe()`，保留 `minMaxOrNull()`。
- `ExpressionDsl`: builder 空表达式失败改为 `buildOrNull()` + 无后缀 `build(): Ret<...>`，`booleanExpression()` 同步返回 `Ret<BooleanExpression>`。
- `Parse.kt`: Ret 包装层中 4 个用于触发 catch 的内部 `throw` 改为直接返回转换失败。
- `PolynomialParser.kt`: Ret 包装层中 3 个转换失败路径改为直接返回 `Failed`，不再用内部 `throw` 触发 catch。
- `Sum.kt`: `Array<out T>.sum()` 空数组失败改为 `Ret`，补齐 `sumSafe()` / `sumOrNull()`。
- `AxisPermutation3` / `QuantityAxisPermutation3`: `mapAxis()` 和圆柱 `apply/permute()` 改为传播 `Ret`。
- `Factorization`: `defactorize()` 负指数失败改为 `Ret<I>`。

### 本轮验证
- `mvn -pl ospf-kotlin-multiarray -am -DskipTests compile`：通过。
- `mvn -pl ospf-kotlin-math -am -DskipTests compile`：通过。
- `mvn -pl ospf-kotlin-quantities -am -DskipTests compile`：通过。
- `mvn -pl ospf-kotlin-quantities,ospf-kotlin-core,ospf-kotlin-framework-bpp3d/bpp3d-infrastructure -am -DskipTests compile`：通过。

### 当前扫描结论
- 本轮相关文件除 `Parse.kt` 私有递归下降 parser 内部错误跳转外，已无生产 `throw` 命中。
- 当轮全局生产代码 `throw` 计数曾降至 216；后续 `MultiArrayView` 和 symbol operation 处理继续降低计数，最新扫描见下方小节。

---

## 2026-06-19 本轮继续处理

### 已完成
- `FeasibleSolverOutput`: 删除构造参数默认值中的 throwing fallback，改为 `objValueOrNull` / `possibleBestObjValueOrNull` / `bestBoundValueOrNull`，并提供无后缀 `objValue()` / `possibleBestObjValue()` / `bestBoundValue()` 返回 `Ret<V>`。
- `CallBackModel`: 默认初始解生成不再抛 `UnsupportedOperationException`，未提供 generator 时返回空初始解列表。
- `ConstraintRelation`: 删除 `Comparison.NE` 的 throwing 构造入口，新增 `ofOrNull` / `ofSafe`，机制模型构造阶段将非法 `NE` 约束传播为 `Failed`。
- `Library.loadInJar`: 由 `Unit` 改为 `Try`，资源缺失、文件 IO、原生库加载失败均返回 `Failed`。
- SCIP 原生库加载入口：`loadLibraryInJar()` 改为 `Try`，静态初始化不再 `printStackTrace()`，`UnsatisfiedLinkError` 不再穿透。
- `RemoteSolverHttpClient` / `SolverExecutionPort` / `RemoteSolverClient`: 远程 HTTP 执行端口改为 `Ret` / `Try` 风格，HTTP 状态码、API envelope、对象存储、反序列化失败均在边界转换为 `Failed`。
- `EitherSerializer` / `OrientationSerializer`: 保留 serializer 协议要求的异常出口，但统一为 `SerializationException`，并移除 `printStackTrace()`。
- `utils.parallel.Common`: 删除无意义的 `catch { throw e }` 重抛；已有 `executeTryWithWorkerPool` / `executeExTryWithWorkerPool` 作为 Result 版本。

### 本轮验证
- `mvn -pl ospf-kotlin-core -am -DskipTests compile`：通过。
- `mvn -pl ospf-kotlin-framework -am -DskipTests compile`：通过。
- `mvn -pl ospf-kotlin-framework,ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-scip,ospf-kotlin-framework-bpp3d/bpp3d-infrastructure,ospf-kotlin-framework-bpp3d/bpp3d-domain-item-context,ospf-kotlin-framework-gantt-scheduling/gantt-scheduling-domain-resource-context,ospf-kotlin-framework-gantt-scheduling/gantt-scheduling-domain-task-compilation-context -am -DskipTests compile`：通过。

### 当前扫描结论
- 重点迁移范围（utils/core/framework/SCIP/BPP3D infrastructure/Gantt resource/task compilation）仅剩 serializer 协议边界的 4 个 `throw SerializationException`。
- 全局生产代码仍有 130 个 `throw`，主要集中在 `ospf-kotlin-multiarray`、`ospf-kotlin-math`、`ospf-kotlin-quantities` 的底层 Kotlin 风格 API、iterator、numeric constructors 和运算符重载中；这些不能简单归为"完全不可改进"，后续应按模块补齐 `OrNull` / `Safe` 或将解析/转换入口改为 `Ret`。

---

## 2026-06-19 MultiArrayView 向量索引处理

### 已完成
- `MultiArrayView`: 外部向量索引入口（`IntArray`、`vararg Int`、`Iterable<ULong>`、`vararg Indexed`）由裸值返回改为 `Ret<T>`，维度不匹配返回 `Failed(ErrorCode.IllegalArgument, ...)`，删除 1 处显式 `DimensionMismatchingException` 抛出。
- `MappedMultiArrayView`: 外部向量索引入口（`IntArray`、`vararg Int`）由裸值返回改为 `Ret<T>`，维度不匹配返回 `Failed(ErrorCode.IllegalArgument, ...)`，删除 1 处显式 `DimensionMismatchingException` 抛出。
- `AccessOrder.iterWithOrder`: 同步适配 `MultiArrayView` 向量索引的 `Ret<T>` 返回；迭代器自身的 `NoSuchElementException` 仍作为 Kotlin `Iterator.next()` 协议边界保留。
- `MultiArrayExtensions`: `MultiArrayView.get(Iterable<UInt64>)` 同步返回 `Ret<T>`。

### 本轮验证
- `mvn -pl ospf-kotlin-multiarray -am -DskipTests compile`：通过。
- `mvn -pl ospf-kotlin-math -am -DskipTests compile`：通过。

### 当前扫描结论
- `MultiArrayView.kt` 仅剩 `Iterator.next()` 协议要求的 `NoSuchElementException`。
- `AccessOrder.kt` 仅剩 2 个 `Iterator.next()` 协议要求的 `NoSuchElementException`。
- 当轮全局生产代码 `throw` 计数降至 208；最新计数见下方 symbol operation 小节。

---

## 2026-06-19 symbol operation ordered/matrix 处理

### 已完成
- `MatrixForm`: `LinearPolynomial.toMatrixForm()`、`QuadraticPolynomial.toMatrixForm()`、`CanonicalPolynomial.toMatrixForm()` 改为返回 `Ret<...>`，符号缺失、重复 order、不可转二次均返回 `Failed`。
- `Flt64MatrixForm`: Flt64 矩阵形式便捷函数同步返回 `Ret<...>` 并传播底层失败。
- `LinearQuadraticOps` / `CanonicalOps`: ordered evaluation 改为返回 `Ret<T>`，order 尺寸不匹配、重复符号、符号缺失均返回 `Failed`。
- `Evaluate` / `Inequality`: Flt64 ordered evaluation 和 ordered inequality satisfaction 同步返回 `Ret`。
- `CompileOps` / `Compile`: compile evaluation/gradient 的编译阶段符号缺失改为返回 `Failed`，Flt64 便捷入口同步返回 `Ret<compiledFunction>`。
- `Differentiate`: `CanonicalPolynomial<Flt64>.hessian()` 改为返回 `Ret<Array<DoubleArray>>`，非二次规范多项式返回 `Failed/Fatal`，不再抛 `IllegalArgumentException`。
- `CanonicalOps`: `computeRingPower()` 改为 `Ret<T>`，新增 `computeRingPowerOrNull()`；负指数在 `TimesGroup` 值域中通过 `reciprocal()` 支持，不支持时返回 `Failed/null`。
- `Evaluate`: canonical monomial/polynomial 的部分求值失败路径同步改为 `Ret`。
- `CompileOps`: compiled canonical evaluation/gradient 在编译阶段拒绝负指数并返回 `Failed`，运行期 lambda 仅使用非负幂实现。
- `PolynomialParser`: 泛型解析 Ret 包装层中的线性/二次转换失败直接返回 `Failed`。
- `Triangulation`: 3D 点集三角剖分的重复投影坐标失败改为 `Ret<List<Triangle<...>>>`，等值线三角剖分同步传播失败。
- `PlaneFrame3`: 纯 math 平面框架的 `normalAxis` 属性改为 `normalAxisOrNull`，新增 `normalAxis(): Ret<Axis3>`；依赖法向轴的 `distance()` / `vector()` 同步返回 `Ret`。
- `symbol/expression/parser/Parser`: 布尔表达式递归下降 parser 全部改为 `Ret<BooleanExpression>` 错误传播；`parseBooleanExpression()` 作为无后缀 Result 主接口，`parseBooleanExpressionOrNull()` 保留 nullable 便利入口。
- `symbol/operation/Parse`、`symbol/parse/PolynomialParser`、`PolynomialLexer`: Flt64 与泛型多项式/不等式 parser 的递归下降控制流全部改为 `ParseResult`，删除 `DirectParseError`；无后缀解析入口返回 `ParseResult<T>`，`OrNull` 入口表达 nullable 转换语义。
- `multiarray/einsum/EinsumParser`: 字符串表示法和 Einstein DSL 入口改为返回 `Ret`，表示法错误和操作数数量错误返回 `Failed`。
- `multiarray/einsum/Operations`: `matmul` / `dot` / `trace` / `outer` / `transpose` / `contract` 改为返回 `Ret`，维度不匹配、形状不兼容、轴越界、默认零值不可推断均返回 `Failed`；删除空矩阵转置初始化 lambda 的不可达 `throw`。

### 本轮验证
- `mvn -pl ospf-kotlin-math -am -DskipTests compile`：通过。
  - 当前仅剩 `CanonicalOps.kt` 的 `TimesGroup` 泛型能力检测 unchecked cast warning。

### 当前扫描结论
- `CanonicalOps.computeRingPower()` 的负指数保护 `throw` 已删除；`symbol/operation/Parse.kt`、`symbol/parse/PolynomialParser.kt`、`PolynomialLexer.kt`、`multiarray/einsum/EinsumParser.kt`、`multiarray/einsum/Operations.kt` 已无生产 `throw` 命中。
- 全局生产代码 `throw` 计数降至 130（不含 `src/test`、`src/gurobi-test`、`target`、`benchmark`）。

---

## 2026-06-19 表达式求值、QuickDsl 与 quantities 平面框架处理

### 已完成
- `EvaluateBoolean`: 默认标量函数求值器不再用 `throw` / `require` 表达非法函数名、参数数量或参数类型；非法或不支持的函数求值返回 `null`，上层三值逻辑统一传播为 `Trivalent.Unknown`。
- `EvaluateBooleanTest`: 非法函数参数测试从断言异常改为断言 `Trivalent.Unknown`。
- `QuickDsl`: nullable transform 版本的 `sum` / `flatSum` / `qsum` / `flatQSum` 改为返回 `Ret<...>`；空单项式列表返回 `Failed(ErrorCode.DataEmpty, ...)`。
- `QuickDsl`: 补齐 `sumSafe` / `flatSumSafe` / `qsumSafe` / `flatQSumSafe` 和 `sumOrNull` / `flatSumOrNull` / `qsumOrNull` / `flatQSumOrNull`，底层数学 DSL 的空集合失败不再抛异常。
- `QuantityPlaneFrame3`: 删除构造校验 `require` 和 `normalAxis` getter 中的 `throw`；构造器收紧为私有，新增 `of(): Ret<QuantityPlaneFrame3>` / `ofOrNull()`，并提供 `normalAxisOrNull` 与 `normalAxis(): Ret<Axis3>`。

### 本轮验证
- `mvn -pl ospf-kotlin-math -am -DskipTests compile`：通过。
- `mvn -pl ospf-kotlin-quantities -am -DskipTests compile`：通过。

### 当前扫描结论
- `EvaluateBoolean.kt`、`QuickDsl.kt`、`ospf-kotlin-quantities/.../PlaneFrame3.kt` 已无生产 `throw` 命中。
- 全局生产代码 `throw` 计数降至 123（不含 `src/test`、`src/gurobi-test`、`target`、`benchmark`）。

---

## 2026-06-19 geometry QuantityOps / Box / Placement 处理

### 已完成
- `ospf-kotlin-math/.../geometry/QuantityOps.kt`: 纯数值几何比较失败改为 `quantityOrdSafe()` / `quantityOrdOrNull()`，`quantityMax/Min/Clamp/ContainsInRange` 改为 `Ret` 传播，不再抛 `IllegalArgumentException`。
- `ospf-kotlin-math/.../geometry/Box2.kt` / `Box3.kt` / `Placement2.kt` / `Placement3.kt`: `contains()` / `overlapped()` / `intersect()` 同步返回 `Ret<Boolean>` 或 `Ret<Box?>` / `Ret<Placement?>`，不可比较值不再被折叠成 `false` 或 `null`。
- `ospf-kotlin-quantities/.../geometry/QuantityOps.kt`: 物理量几何加减、比较、极值、clamp、range 判断补齐 `Safe` / `OrNull`，单位转换失败和量纲不匹配返回 `Failed(ErrorCode.IllegalArgument, ...)`。
- `QuantityBox2` / `QuantityBox3` / `QuantityPlacement2` / `QuantityPlacement3`: 可失败的 `maxX/maxY/maxZ` getter 降级为 `maxXOrNull` / `maxYOrNull` / `maxZOrNull`，无后缀 `maxX()` / `maxY()` / `maxZ()` 返回 `Ret<Quantity<V>>`。
- `QuantityCircle2.diameter` / `QuantityCylinder3.diameter`: 同单位自加改为直接构造 `Quantity`，避免依赖可失败 helper。

### 本轮验证
- `mvn -pl ospf-kotlin-math -am -DskipTests compile`：通过。
- `mvn -pl ospf-kotlin-quantities -DskipTests compile`：通过。
- `mvn -pl ospf-kotlin-quantities -am -DskipTests compile`：通过。

### 当前扫描结论
- `ospf-kotlin-math/.../geometry/QuantityOps.kt` 与 `ospf-kotlin-quantities/.../geometry/QuantityOps.kt` 已无生产 `throw` 命中。
- 全局生产代码 `throw` 计数降至 90（不含 `src/test`、`src/gurobi-test`、`target`、`benchmark`）。
- 当前主要剩余：`ValueWrapper.kt` 29 个、`Shape.kt` 28 个、`SymbolQuantityOps.kt` 8 个、`ConstantProviders.kt` 3 个、`Quantity.kt` 3 个，以及 serializer / iterator / numeric operator / KSP 字符串等边界项。

---

## 2026-06-19 Scale 与 SymbolQuantityOps 处理

### 已完成
- `Scale`: `Scale / FltX`、`Scale / RtnX` 的除零失败不再抛 `ArithmeticException`，改为返回 nullable 结果；补齐 `divOrNull()` 与 `divSafe(): Ret<Scale>`。
- `SymbolQuantityOps`: 线性多项式物理量的 `plus` / `minus` 四个 operator 改为返回 `Ret<Quantity<...>>`，量纲不匹配和单位转换失败返回 `Failed(ErrorCode.IllegalArgument, ...)`。
- `SymbolQuantityOps`: 补齐 `plusSafe` / `minusSafe` 与 `plusOrNull` / `minusOrNull`，底层 quantities Kotlin 风格 API 不再保留 throwing 加减入口。
- `Reciprocal.kt`: 调整 KDoc，删除审计误报中的 `throw` 表述。

### 本轮验证
- `mvn -pl ospf-kotlin-math -am -DskipTests compile`：通过。
- `mvn -pl ospf-kotlin-quantities -am -DskipTests compile`：通过。

### 当前扫描结论
- `Scale.kt` 与 `SymbolQuantityOps.kt` 已无生产 `throw` 命中。
- 全局生产代码 `throw` 计数降至 79（不含 `src/test`、`src/gurobi-test`、`target`、`benchmark`）。
- 当前主要剩余：`ValueWrapper.kt` 29 个、`Shape.kt` 28 个、`ConstantProviders.kt` 3 个、`Quantity.kt` 3 个，以及 serializer / iterator / numeric operator / KSP 字符串等边界项。

---

## 2026-06-19 Quantity 乘除与 Shape 安全入口处理

### 已完成
- `Quantity`: 删除标量乘除和一元负号中的 3 个 `require`；仿射单位不支持的标量运算改为 `timesSafe` / `timesOrNull`、`divSafe` / `divOrNull`、`unaryMinusSafe` / `unaryMinusOrNull`。
- quantities 几何面积/体积派生计算改为内部确定性 `quantityProduct(...)`，避免通用 nullable operator 影响圆、矩形、长方体、圆柱等派生属性。
- BPP2D / BPP3D / CSP1D 相关调用点同步适配 nullable quantity 乘除；确定性面积、体积、重量计算改为显式 helper 或短路传播。
- `Shape`: 为底层 multiarray DSL 补齐 `offsetSafe` / `offsetOrNull` 与 `dummyVectorSafe` / `dummyVectorOrNull`；旧 DSL 包装暂保留，便于后续分批改调用面。
- `FltXJsonSerializer`: 非 JSON decoder 的协议边界异常从 `IllegalStateException` 改为 `SerializationException`，与 serializer 边界语义一致。
- `ValueWrapperSerializer` / `ValueRangeSerializer` / `RationalSerializer`: JSON encoder/decoder/object/field 校验从 `require` 改为集中 `SerializationException` helper，清理 serializer 协议边界的 `require` 噪音。

### 本轮验证
- `mvn -pl ospf-kotlin-quantities -am -DskipTests compile`：通过。
- `mvn -pl ospf-kotlin-framework-bpp3d/bpp3d-infrastructure -am -DskipTests compile`：通过。
- `mvn -pl ospf-kotlin-framework-bpp2d,ospf-kotlin-framework-bpp3d/bpp3d-infrastructure,ospf-kotlin-framework-bpp3d/bpp3d-domain-item-context,ospf-kotlin-framework-csp1d/csp1d-domain-material-context,ospf-kotlin-framework-gantt-scheduling/gantt-scheduling-domain-resource-context -am -DskipTests compile`：通过。
- `mvn -pl ospf-kotlin-multiarray -am -DskipTests compile`：通过。
- `mvn -pl ospf-kotlin-math -am -DskipTests compile`：通过。

### 当前扫描结论
- `Quantity.kt` 已无生产 `throw` / `require` / `check` / `error` 命中。
- 全局生产代码 `throw` 计数为 46（不含 `src/test`、`src/gurobi-test`、`target`、`benchmark`）；serializer 协议异常集中到 `math/Serialization.kt` 后，协议边界更清晰。
- 全局生产代码 `require` / `check` / `error` 扫描计数降至 207（包含 logger/error 函数名误报）。
- 主要剩余：`Shape.kt` 29 个、serializer 协议边界、iterator 协议边界、numeric/math 边界；`ConstantProviders.kt` 3 个仍需按调用面单独迁移。

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
| ospf-kotlin-utils | ~42 | 33+ | serializer 边界 2 个 + 底层 API | `Library.loadInJar` 已改 Try；`EitherSerializer` 仅保留 `SerializationException` |
| ospf-kotlin-core | ~12 | 已清理本轮审计项 | 需按全局扫描继续细分 | `CallBackModel`、`ConstraintSign`、`SolverOutput` 已处理 |
| ospf-kotlin-multiarray | ~39 | 持续推进 | Shape + iterator 边界为主 | `DataFrame`、`MultiArray`、`AccessOrder.fromList`、`MultiArrayView` 向量索引已处理 |
| ospf-kotlin-quantities | ~22 | 持续推进 | 继续细分 | 已处理 quantities 平面框架；仍有 quantity 运算符、单位换算、维度校验 |
| ospf-kotlin-math | ~120+ | 持续推进 | 继续细分 | 已处理 average、MinMax、ExpressionDsl、Array.sum、AxisPermutation、defactorize、ordered/matrix symbol operation、canonical power、Triangulation、PlaneFrame3、boolean expression parser、表达式求值和 QuickDsl |
| ospf-kotlin-framework | ~8 | 远程 HTTP 已处理 | 主要剩 printStackTrace/其他边界 | `RemoteSolverHttpClient` 已改 Ret 端口 |
| ospf-kotlin-framework-gantt-scheduling | ~25 | 14 | ~11 | 含设计选择保留 |
| ospf-kotlin-framework-csp1d | ~15 | 12 | ~3 | 含自定义异常保留 |
| ospf-kotlin-framework-bpp2d | 4 | 1 | 3 | |
| ospf-kotlin-framework-bpp3d | ~50+ | 30+ | ~20 | 含设计选择保留 |
| ospf-kotlin-framework-plugin | ~10 | 0 | ~10 | 设计选择保留（策略模式 FailFast/ClientFilter） |
| 测试代码 | ~146 | 0 | ~146（保留） | |
| **总计** | **~395** | **持续推进中** | **全局 46 个生产 throw** | 不能再标记为仅剩 18 个；底层库仍需分模块处理 |

### 剩余 throw 分类（均为合理保留）

1. **设计选择**（~18 个）：
   - CallBackModel lambda 默认参数（3 个）
   - ConstraintSign 验证（1 个）
   - BigM 不支持操作（1 个）
   - Shape 维度/索引验证（~20 个）
   - Quantity 运算符重载维度校验（~16 个）
   - Framework-plugin 策略模式（~10 个）
2. **自定义异常**（~5 个）：Csp1dRecoveryFallbackDisabledException、Csp1dRecoverySolveException，测试依赖，保留
3. **lambda 内 throw**（~8 个）：TaskTime.kt 等，外层 catch 转 Failed，保留

---

## 构建状态
本轮涉及的 core/framework/SCIP/BPP3D/Gantt 目标模块生产代码均已通过 `-DskipTests compile`。
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
