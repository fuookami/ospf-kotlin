# ONNX 双路径接入与 IntermediateSymbol 降阶

> 状态：**已实现**。本文件记录最终落地的设计与实现偏差，原始计划中标注"首版不支持"的限制均已补齐。

## Summary

新增独立模块 `ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-onnx`，隔离 ONNX Runtime 和 protobuf 依赖，提供两条互不回退的路径：

- **回调路径**：通过 ONNX Runtime 直接推理，向回调模型提供目标值和约束谓词。
- **LP/QP 路径**：解析 `ModelProto`，把支持的 ONNX 图转换为 OSPF 的线性/二次 `IntermediateSymbol`；不支持的图在导入阶段直接返回结构化 `OnnxError`，不会退回黑盒推理。

支持 opset `13..24`，13 种 ONNX 数据类型，静态 shape 以及通过 `inputShapes` 具体化的动态 shape，并在 OSPF 边界统一映射为 `Flt64`。

## Public API

### 推理与回调

```kotlin
OnnxInference.load(Path)
OnnxInference.load(ByteArray)
OnnxInference.load(InputStream): Ret<OnnxInference>
```

`OnnxInference`：

- 实现 `AutoCloseable`，持有 `OrtSession` 并共享 JVM 级 `OrtEnvironment`（共享环境不随句柄关闭）；
- 暴露输入输出名称、shape、dtype 等元数据，动态维度以 `null` 表示；
- 提供按名称传入 tensor 的 `evaluate(...)`，支持并发调用；
- 输出按名称返回，数据按 row-major 展平，同时保留 shape 和 dtype；
- 每次推理创建的 tensor/result 在调用结束后释放；
- 捕获 ONNX Runtime 异常并转换为 `OnnxError.InferenceFailed`；
- `InputStream` 由加载过程完整读取，但不关闭调用者持有的流；
- `ByteArray` 加载需要直接缓冲区，内部自行转换。

提供 `OnnxCallbackAdapter<V>`：

- 每个 ONNX 输入名称绑定一个显式的变量列表，列表数量必须等于输入 tensor 元素数量；
- 从 `Solution<V>` 按固定顺序提取变量值，按输入 dtype 构造 tensor（含 FP16/BF16 位模式）；
- 输出选择通过 `OnnxOutputSelection` 表达：取单个元素，或按 `sum`/`mean`/`first`/`last`/`max`/`min` 折叠整个张量；
- `evaluate(solution): Ret<V>` 提供可检查的结构化错误；
- `objectiveExtractor()` 适配现有 `Extractor<V?, Solution<V>>`；
- `constraintExtractor(comparison, bound, tolerance)` 适配现有 `Extractor<Boolean?, Solution<V>>`；
- 由于现有回调接口不能返回错误，Extractor 运行时推理失败返回 `null`，详细错误通过 `evaluate` 入口获得；
- 动态输入 shape 通过 `create(..., inputShapes)` 具体化。

### IntermediateSymbol 导入

```kotlin
OnnxFunctionalModel.load(
    source,
    inputBindings,
    options
): Ret<OnnxFunctionalModel>
```

其中：

- `inputBindings: Map<String, List<Symbol>>` 按 ONNX 输入名称绑定 OSPF 符号；
- 每个绑定列表必须匹配 tensor shape 的元素数量；
- `options` 包含导入模式、激活近似配置、外部输入范围覆盖和动态 shape 具体化；
- 模型句柄保存解析后的图、输入输出元数据、拓扑顺序和降阶报告。

模型句柄提供按选择获取标量输出：

```kotlin
outputs(outputName): Ret<List<IntermediateSymbol<Flt64>>>
linearOutput(outputName, elementIndex): Ret<LinearIntermediateSymbol<Flt64>>
quadraticOutput(outputName, elementIndex): Ret<QuadraticIntermediateSymbol<Flt64>>
aggregateOutput(selection): Ret<LinearIntermediateSymbol<Flt64>>
registerInto(tokens): Try
```

- 输出 API 只暴露核心的 `LinearIntermediateSymbol<Flt64>` 或 `QuadraticIntermediateSymbol<Flt64>`，不增加插件专用的 `LinearFunctionalSymbol`/`QuadraticFunctionalSymbol` 类型；
- `aggregateOutput` 在 `Max`/`Min` 时复用核心 `MaxFunction`/`MinFunction`，因此会注册二值选择变量；全常量输出直接数值折叠；
- 输出 tensor 也可通过名称查询为有序标量符号列表；
- 每个符号的结果多项式必须是真实表达式或结果变量，禁止使用零多项式掩盖黑盒；
- `IntermediateSymbol.evaluate(...)` 只使用解析后的 OSPF 表达式，不调用 ONNX Runtime；
- 辅助变量和约束按拓扑顺序注册，`registerInto` 跳过已存在符号，重复注册安全。

## Graph Parsing and Lowering

解析器使用 `ai.onnx.proto.OnnxMl.ModelProto`，合并 graph initializer 和 `Constant` 节点，先完成模型校验，再构建内部 tensor expression DAG。子图通过 `parseOnnxSubgraph` 递归解析，字符串/子图属性 `GRAPH`/`GRAPHS` 在属性归一化阶段保留。

### 算子白名单

- 图和常量：`Constant`、initializer、`Identity`；
- 线性/二次计算：`Add`、`Sub`、`Mul`、`MatMul`、`Gemm`；
- 张量形状和索引：`Reshape`、`Flatten`、`Transpose`、`Concat`、`Split`、`Gather`、`ReduceSum`；
- 激活和裁剪：`Relu`、`Clip`、`LeakyRelu`、`HardSigmoid`、`Sigmoid`、`Tanh`、`Softplus`、`Elu`；
- 控制流：`If`、`Loop`、`Scan`。

### Shape、广播和阶数

- `Add`、`Sub`、`Mul` 实现 ONNX 多向广播；每个维度必须静态已知，并遵守维度相等或为 `1` 的规则；
- `MatMul` 实现 **ONNX 完整** shape 规则：秩 1 操作数按向量处理，秩 >2 的批量维按 numpy 规则广播，输出按规则压掉秩 1 引入的维度；
- `Gemm` 按标准保持二维输入，支持转置、`alpha`、`beta` 和偏置，内部复用 `MatMul` 降阶；
- QP 模式允许两个符号表达式相乘，包括符合 shape 的符号矩阵乘法；
- 表达式次数必须始终不超过二次；二次表达式再次与非常量相乘时返回 `DegreeOverflow`；
- `Reshape` 的 shape 输入必须是静态常量，并处理固定 shape 下允许的 `0`/`-1` 规则；
- `Flatten`、`Transpose`、`Concat`、`Split`、`Gather`、`ReduceSum` 的 axis、perm、split、indices 等参数必须可静态解析；
- 动态 indices、动态 reshape 和无法证明的 shape 直接失败；
- 动态**输入维度**通过 `OnnxImportOptions.inputShapes` 具体化；覆盖值必须与声明秩一致且不与静态维度冲突，否则返回 `ShapeMismatch` 或 `BindingMismatch`；
- LP 模式拒绝无法准确线性表达的乘法或二次结果；
- QP 模式允许线性与二次表达式，但拒绝三次及以上表达式。

### 激活与范围传播

- `Relu` 使用现有 `PositivePartFunction`；
- `Clip` 使用现有 `ClampFunction`；
- `LeakyRelu`、`HardSigmoid` 采用精确的线性表达；
- `Sigmoid`、`Tanh`、`Softplus`、`Elu` 使用可配置分段线性近似（默认 8 段，允许 `2..1024`）；
- 分段函数要求输入范围有限；超过 `MAX_PIECEWISE_BOUND = 1e6` 的形式有限区间视为缺少有效范围；
- 优先使用输入变量范围和区间传播结果；可通过输入名称提供显式 finite bounds 覆盖传播结果；
- 仍无法得到有限范围时返回 `MissingBound`；
- **二次输入送激活**：先通过核心 `QuadraticExpressionSymbol.useIntermediateVariable` 实现为中间变量 `aux == q(x)`，再对 `aux` 施加激活，使约束规模随元素数线性增长；线性模式下返回 `DegreeOverflow` 或 `UnsupportedOperator`。

### 子图控制流

- `If` 要求条件为常量，按条件内联 `then_branch` 或 `else_branch`；
- `Loop` 要求迭代次数为常量，展开后沿携带依赖逐轮传递，扫描输出按轮拼接；
- `Scan` 从扫描输入首维推导迭代次数，逐轮按首维切片并拼接扫描输出；
- 非常量条件、非常量迭代次数与超限迭代次数返回 `UnsupportedOperator`；
- 单次展开上限 `MAX_UNROLL = 256` 轮，展开式建模规模随迭代次数线性增长。

### 数据类型

`Float32`、`Float64`、`Float16`、`BFloat16`、`Int8`、`UInt8`、`Int16`、`UInt16`、`Int32`、`UInt32`、`Int64`、`UInt64`、`Bool`。

- 常量解码同时支持 `raw_data`（按 dtype 位宽小端解析）与 typed data 列表；
- FP16/BF16 通过 `OnnxHalfPrecision` 做位模式与 `Double` 的双向换算，常量侧从 `int32_data` 读取位模式；
- 推理张量按 dtype 分别构造 `FloatBuffer`/`DoubleBuffer`/`ShortBuffer`/`IntBuffer`/`LongBuffer`/`ByteBuffer`，半精度使用显式 `OnnxJavaType.FLOAT16`/`BFLOAT16` 重载；
- 未知或字符串类型返回 `UnsupportedDType`。

## Structured Errors

插件领域错误 `OnnxError` 均保留节点、输入输出名称和 shape 等上下文，并转换为项目统一的 `Ret/Failed`；结构化错误同时作为 `Ret` 的关联值保留以便取回：

- `LoadFailed`
- `InvalidModel`
- `UnsupportedOpset`
- `UnsupportedOperator`
- `UnsupportedDType`
- `DynamicShape`
- `ShapeMismatch`
- `BindingMismatch`
- `OutputSelection`
- `UnsupportedAttribute`
- `DegreeOverflow`
- `MissingBound`
- `InferenceFailed`
- `ResourceClosed`

公共加载、解析、降阶和类型化推理入口均不向业务层抛出异常。外部 ONNX Runtime 和 protobuf 异常只在边界捕获并转换为上述错误。

## Maven、文档与模块

- 根 Maven 属性包含 ONNX Runtime `1.31.1` 和 `onnx-protobuf_2.13` `0.9.24` 版本；
- `ospf-kotlin-dependencies/pom.xml` 收录两者依赖管理；
- `ospf-kotlin-core-plugin/pom.xml` 已注册新模块；
- 新模块只依赖 `ospf-kotlin-core`、`ospf-kotlin-utils`、ONNX Runtime 和 protobuf，核心模块不引入 ONNX 原生依赖；
- 新模块提供中英文 README，覆盖推理、回调目标/约束、输出聚合、动态 shape、控制流、线性输出与二次输出示例，并附错误码表；
- `ospf-kotlin-core-plugin` README 中英文均补充 ONNX 插件说明与链接；
- `changelog/1.1.2.md` 与 `changelog/1.1.2_ch.md` 补充 ONNX 接入章节与测试说明。

## Tests and Acceptance

测试使用内存构造的 `ModelProto`，避免把模型二进制和其他生成物提交到仓库。共 **113** 个测试：

- 图解析（12）：opset 校验、拓扑序、重复输出、`Constant` 折叠、不受支持 dtype；
- 推理（10）：Float32/Float64 推理、tensor flatten、输出索引选择、Path/ByteArray/InputStream 加载、资源关闭、并发；
- 回调（11）：目标值、约束比较、缺失变量、运行时推理失败、构造期校验；
- 降阶（18）：Add/Sub/Mul 广播、MatMul（含批量广播）、Gemm、Reshape、Transpose、Concat、Split、Gather、ReduceSum；
- 激活（13）：精确与分段近似、范围传播、显式范围覆盖、缺少范围失败、分段参数校验、二次输入桥接；
- 注册（8）：拓扑序注册、幂等重注册、输出多项式真实性、Path/Stream 加载；
- 数据类型（11）：dtype 映射、半精度与 bf16 往返、FP16 常量与 `raw_data`、Int8/UInt8/Bool、FP16 推理、不受支持 dtype；
- 聚合（10）：选择校验、两条路径的 sum/mean/first/last/max/min、极值符号注册、常量折叠、越界与未知输出；
- 动态形状（10）：动态维暴露为 `null`、具体化后推理与降阶、缺失覆盖、秩与静态维冲突、未知输入、`OnnxBounds` 校验；
- 子图控制流（10）：`If` 真/假分支与非常量条件、`Loop` 展开与零迭代、超限与非常量迭代次数、`Scan` 状态累加、扫描输出拼接。

已验证：

```text
mvn compile -pl ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-onnx -am
mvn test -pl ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-onnx   → 113 passed
mvn clean compile test-compile -T 0.75C                              → BUILD SUCCESS, 0 error
```

全量 `mvn test -T 0.75C` 中本模块通过；`ospf-kotlin-core-plugin-heuristic` 的
`HyperHeuristicTest.asynchronousStopAndCheckpointShouldDrainOutOfOrderFeedbackExactlyOnce`
在并行全量运行下偶发失败（异步乱序反馈的时序断言），单模块运行通过，与本模块无关。

所有构建和测试日志写入 `%TEMP%`，不把报告、模型临时文件或其他生成物放入仓库。

## Assumptions

- OSPF 求解器边界统一使用 `Flt64`；`OnnxBounds` 使用 `Flt64`，直接与变量的 `ExpressionRange<Flt64>` 求交，省掉一次转换；
- 结构性字段保持原生类型：`elementIndex` 与 `shape` 是索引与结构性元数据，`segments`/`supportedOpset` 是配置与协议区间，`OnnxTensorData.data` 是供 ORT/protobuf 使用的原始载荷缓冲区；
- 维度沿用仓库既有约定（`ospf-kotlin-multiarray` 的 `Shape` 同样存 `Int`），不引入代数整数类型；
- 广播仅针对白名单算子，并要求所有维度静态可知或已具体化；
- QP 路径允许符号 `Mul`/`MatMul`，但最终表达式次数不能超过二次；
- 多输出模型默认按 `outputName + flatten index` 选择标量；`OnnxOutputSelection` 的折叠模式是显式选择，不是隐式聚合；
- 回调和 LP/QP 是完全独立的执行路径；LP/QP 失败时不自动调用 ONNX Runtime；
- `OnnxFunctionalModel` 是插件侧的模型封装，其输出按 LP/QP 模式分别返回核心 `LinearIntermediateSymbol<Flt64>` 或 `QuadraticIntermediateSymbol<Flt64>`，不暴露插件专用 FunctionalSymbol 类型。

## 实现偏差记录

相对最初计划，以下限制在实现阶段被补齐（均已带测试）：

| 原计划限制 | 最终实现 |
| --- | --- |
| 仅 `Float32`/`Float64` | 13 种 dtype，含 FP16/BF16/整数/Bool |
| 仅静态 shape | 动态维可经 `inputShapes` 具体化 |
| 不支持隐式输出聚合 | `OnnxOutputSelection` 显式支持 sum/mean/first/last/max/min |
| `MatMul` 首版秩 ≤ 2 | 任意秩 + numpy 批量广播 |
| 二次输入送激活拒绝 | 经核心中间变量桥接为 `aux == q(x)` |
| 无子图支持 | `If`/`Loop`/`Scan` 静态展开 |
| `OnnxBounds` 用 `Double` | 改用 `Flt64` |

保留的上限（带 `ponytail:` 注释）：循环展开上限 256 轮；线性模式不做二次输入可行化。
