# ospf-kotlin-core-plugin-onnx

:us: [English](README.md) | :cn: 简体中文

## 简介

`ospf-kotlin-core-plugin-onnx` 通过两条**互相独立、互不回退**的路径把 ONNX 模型接入 OSPF：

1. **推理 / 回调路径** —— 用 ONNX Runtime 直接推理，把输出适配为回调模型的目标值与约束谓词。
2. **LP/QP 降阶路径** —— 解析 `ModelProto`，把支持的图降阶为 OSPF `IntermediateSymbol` 表达式。不支持的图在导入阶段直接返回结构化错误，绝不悄悄退化为黑盒推理。

所有公共入口都返回 `Ret`/`Try`；ONNX Runtime 与 protobuf 的异常只在边界捕获并转换为 `OnnxError`。

## 引入方式

模块属于 `ospf-kotlin-core-plugin` 聚合工程：

```xml
<dependency>
    <groupId>io.github.fuookami.ospf.kotlin.core.plugin</groupId>
    <artifactId>ospf-kotlin-core-plugin-onnx</artifactId>
    <version>1.1.2</version>
</dependency>
```

`ospf-kotlin-core`、`ospf-kotlin-math` 与 `ospf-kotlin-utils` 不引入任何 ONNX 依赖。

## 支持的子集

| 项目 | 支持范围 |
|------|----------|
| Opset | 主域 `13..24`（可用 `OnnxImportOptions.supportedOpset` 调整） |
| 数据类型 | `Float32`、`Float64`、`Float16`、`BFloat16`、`Int8`、`UInt8`、`Int16`、`UInt16`、`Int32`、`UInt32`、`Int64`、`UInt64`、`Bool` |
| 形状 | 静态，或通过 `OnnxImportOptions.inputShapes` 具体化的动态形状 |
| 图与常量 | `Constant`、initializer、`Identity` |
| 线性/二次计算 | `Add`、`Sub`、`Mul`、`MatMul`（任意秩，批量维按 numpy 广播）、`Gemm` |
| 形状与索引 | `Reshape`、`Flatten`、`Transpose`、`Concat`、`Split`、`Gather`、`ReduceSum` |
| 激活 | `Relu`、`Clip`、`LeakyRelu`、`HardSigmoid`、`Sigmoid`、`Tanh`、`Softplus`、`Elu` |
| 控制流 | `If`（条件为常量）、`Loop`（迭代次数为常量）、`Scan`（扫描长度静态） |
| 次数上限 | `Linear` 模式 ≤ 1，`Quadratic` 模式 ≤ 2 |

`MatMul` 遵循 ONNX 完整形状规则：秩 1 操作数按向量处理，秩 >2 的批量维按 numpy 规则广播。
`Gemm` 按标准要求保持二维。

二次输入送激活会先实现为中间变量 `aux == q(x)`（仅二次模式）；线性模式下会以
`DegreeOverflow` 或 `UnsupportedOperator` 失败。`Sigmoid`、`Tanh`、`Softplus`、`Elu`
使用可配置段数的分段线性近似，并要求输入范围有限。控制流循环体在导入期展开，建模规模随迭代次数
线性增长（`MAX_UNROLL` 上限 256）。

## 推理路径

```kotlin
import java.nio.file.Paths
import fuookami.ospf.kotlin.core.plugin.onnx.graph.OnnxDataType
import fuookami.ospf.kotlin.core.plugin.onnx.inference.OnnxInference
import fuookami.ospf.kotlin.core.plugin.onnx.inference.OnnxTensorData

val inference = OnnxInference.load(Paths.get("model.onnx")).valueOrFail()
inference.use {
    println(it.inputInfos)   // 名称 / 类型 / 形状
    println(it.outputInfos)

    val outputs = it.evaluate(
        mapOf(
            "x" to OnnxTensorData(
                dtype = OnnxDataType.Float32,
                shape = listOf(1, 2),
                data = doubleArrayOf(1.0, 2.0)   // 行主序
            )
        )
    ).valueOrFail()
    println(outputs.getValue("y").data.toList())
}
```

`OnnxInference` 实现 `AutoCloseable`，只关闭会话、不关闭 JVM 级共享的 `OrtEnvironment`，并支持并发 `evaluate`。可用 `Path`、`ByteArray`、`InputStream` 加载；调用方持有的流会被完整读取但不会被关闭。

## 回调路径

```kotlin
import fuookami.ospf.kotlin.core.plugin.onnx.callback.OnnxCallbackAdapter
import fuookami.ospf.kotlin.core.plugin.onnx.callback.OnnxInputBinding
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison

val adapter = OnnxCallbackAdapter.create(
    inference = inference,
    tokens = model.tokens,
    bindings = listOf(OnnxInputBinding(inputName = "x", variables = listOf(x0, x1))),
    outputName = "y",
    elementIndex = 0,
    converter = IntoValue.Identity
).valueOrFail()

// 目标函数
val objective = adapter.objectiveExtractor()

// 约束：y >= bound
val constraint = adapter.constraintExtractor(Comparison.GE, Flt64(5.0))

// 或显式取结构化错误
val value = adapter.evaluate(solution)   // Ret<Flt64>
```

输出选择必须同时给出 `outputName` 与展平后的 `elementIndex`，不做任何隐式聚合。由于 `Extractor` 无法携带错误，运行时推理失败会让 Extractor 返回 `null`；需要详细错误时请用 `evaluate`。

## LP/QP 降阶路径

```kotlin
import fuookami.ospf.kotlin.core.plugin.onnx.model.OnnxFunctionalModel
import fuookami.ospf.kotlin.core.plugin.onnx.model.OnnxImportMode
import fuookami.ospf.kotlin.core.plugin.onnx.model.OnnxImportOptions

val options = OnnxImportOptions(mode = OnnxImportMode.Linear)
val model = OnnxFunctionalModel.load(
    source = Paths.get("model.onnx"),
    inputBindings = mapOf("x" to listOf(x0, x1)),
    options = options
).valueOrFail()

// 线性输出
val y = model.linearOutput("y", 0).valueOrFail()
println(y.polynomial)

// 把产生的全部符号（激活 + 输出）注册进令牌表
model.registerInto(metaModel.tokens).valueOrFail()
```

二次输出需要 `OnnxImportMode.Quadratic`：

```kotlin
val quadraticModel = OnnxFunctionalModel.load(
    source = Paths.get("qp.onnx"),
    inputBindings = mapOf("x" to listOf(x0, x1)),
    options = OnnxImportOptions(mode = OnnxImportMode.Quadratic)
).valueOrFail()
val y = quadraticModel.quadraticOutput("y", 0).valueOrFail()
```

`OnnxFunctionalModel.symbols` 按拓扑序排列，可重复注册；`registerInto` 会跳过已存在的符号。

### 输出聚合

两条路径都接受 `OnnxOutputSelection`：既可以取单个元素，也可以把整个张量折叠成一个标量。

```kotlin
import fuookami.ospf.kotlin.core.plugin.onnx.output.OnnxAggregation
import fuookami.ospf.kotlin.core.plugin.onnx.output.OnnxOutputSelection

// 单元素（默认）
val element = OnnxOutputSelection.element("y", 3)

// 折叠整个张量
val folded = OnnxOutputSelection.fold("y", OnnxAggregation.Mean)

// 回调路径
val adapter = OnnxCallbackAdapter.create(
    inference = inference,
    tokens = model.tokens,
    bindings = bindings,
    selection = folded,
    converter = IntoValue.Identity
).valueOrFail()

// 降阶路径
val symbol = model.aggregateOutput(folded).valueOrFail()
```

`Sum`、`Mean`、`First`、`Last` 完全在表达式代数内完成。`Max` 与 `Min` 使用核心的
`MaxFunction`/`MinFunction` 符号，因此会引入二值选择变量；全常量输出则直接数值折叠。
`Max`/`Min` 遇到二次输出会返回 `UnsupportedOperator`。

### 动态形状

`OnnxInference` 把动态维度暴露为 `null`，调用时给出实际形状即可。降阶路径上必须把动态输入具体化：

```kotlin
val options = OnnxImportOptions(
    inputShapes = mapOf("x" to listOf(3L, 2L))
)
```

覆盖值必须与声明秩一致，且不得与静态维度冲突；未知名称、秩不符与静态冲突分别以
`BindingMismatch`、`ShapeMismatch` 拒绝。`OnnxCallbackAdapter.create` 接受同样的 `inputShapes` 映射。

### 控制流

`If`、`Loop`、`Scan` 在导入期展开，因此每个分支与迭代次数都必须可静态确定：`If` 按常量条件选分支，
`Loop` 要求常量迭代次数，`Scan` 从扫描输入的首维推导迭代次数。非常量条件或迭代次数返回
`UnsupportedOperator`，模块把单次展开限制在 256 轮迭代以内。

### 激活与范围

`Relu` 与 `Clip` 复用核心的精确函数符号，`LeakyRelu` 与 `HardSigmoid` 展开为精确线性组合。其余激活使用分段线性近似，因此必须能证明输入范围有限：

```kotlin
val options = OnnxImportOptions(
    activationApproximation = OnnxActivationApproximation(segments = 32),
    inputBounds = mapOf("x" to OnnxBounds(lower = Flt64(-3.0), upper = Flt64(3.0)))
)
```

无法证明有限范围时导入直接返回 `MissingBound`。每增加一段都会多一个二值选择变量，段数越多 MILP 规模越大。

## 结构化错误

所有失败都是携带节点、张量与形状上下文的 `OnnxError`：

`LoadFailed`、`InvalidModel`、`UnsupportedOpset`、`UnsupportedOperator`、`UnsupportedDType`、`DynamicShape`、`ShapeMismatch`、`BindingMismatch`、`OutputSelection`、`UnsupportedAttribute`、`DegreeOverflow`、`MissingBound`、`InferenceFailed`、`ResourceClosed`。

结构化错误同时作为 `Ret` 的关联值保留，可以取回：

```kotlin
val structured = (result as? Failed)?.error?.value as? OnnxError
structured?.fullMessage()?.let(::println)
```

## 测试

测试模型全部用 `ModelProto` 构造器在内存中生成，仓库中不提交任何 `.onnx` 或 `.pb` 二进制。

```bash
mvn test -pl ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-onnx -am
```

## 许可

Apache License 2.0
