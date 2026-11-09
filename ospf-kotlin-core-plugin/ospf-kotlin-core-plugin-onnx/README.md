# ospf-kotlin-core-plugin-onnx

:us: English | :cn: [简体中文](README_ch.md)

## Introduction

`ospf-kotlin-core-plugin-onnx` integrates ONNX models into OSPF through two **independent, non-fallback** paths:

1. **Inference / call-back path** — runs the model with ONNX Runtime and adapts outputs into objective and constraint extractors for call-back models.
2. **LP/QP lowering path** — parses `ModelProto` and lowers supported graphs into OSPF `IntermediateSymbol` expressions. Unsupported graphs fail at import time with a structured error; they never silently degrade into black-box inference.

All public entry points return `Ret`/`Try` values. Exceptions from ONNX Runtime and protobuf are captured at the boundary and converted into `OnnxError`.

## Installation

The module is part of the `ospf-kotlin-core-plugin` aggregator:

```xml
<dependency>
    <groupId>io.github.fuookami.ospf.kotlin.core.plugin</groupId>
    <artifactId>ospf-kotlin-core-plugin-onnx</artifactId>
    <version>1.1.2</version>
</dependency>
```

`ospf-kotlin-core`, `ospf-kotlin-math` and `ospf-kotlin-utils` carry no ONNX dependency.

## Supported subset

| Item | Supported |
|------|-----------|
| Opset | main domain `13..24` (configurable through `OnnxImportOptions.supportedOpset`) |
| Data types | `Float32`, `Float64`, `Float16`, `BFloat16`, `Int8`, `UInt8`, `Int16`, `UInt16`, `Int32`, `UInt32`, `Int64`, `UInt64`, `Bool` |
| Shapes | static, or dynamic resolved through `OnnxImportOptions.inputShapes` |
| Graph and constants | `Constant`, initializer, `Identity` |
| Linear/quadratic math | `Add`, `Sub`, `Mul`, `MatMul` (any rank, numpy batch broadcast), `Gemm` |
| Shape and indexing | `Reshape`, `Flatten`, `Transpose`, `Concat`, `Split`, `Gather`, `ReduceSum` |
| Activations | `Relu`, `Clip`, `LeakyRelu`, `HardSigmoid`, `Sigmoid`, `Tanh`, `Softplus`, `Elu` |
| Control flow | `If` (constant condition), `Loop` (constant trip count), `Scan` (static scan length) |
| Degree ceiling | `Linear` mode ≤ 1, `Quadratic` mode ≤ 2 |

`MatMul` follows the full ONNX shape rules: rank-1 operands act as vectors and rank >2 batch
dimensions broadcast under numpy rules. `Gemm` stays 2-D, as the standard requires.

A quadratic input to an activation is realized through an intermediate variable `aux == q(x)`
(quadratic mode only); under linear mode it fails with `DegreeOverflow` or `UnsupportedOperator`.
`Sigmoid`, `Tanh`, `Softplus` and `Elu` use a configurable piecewise-linear approximation and require
finite input bounds. Control-flow bodies are unrolled, so modelling size grows linearly with the
iteration count (the module caps unrolling at 256 iterations).

## Inference path

```kotlin
import java.nio.file.Paths
import fuookami.ospf.kotlin.core.plugin.onnx.graph.OnnxDataType
import fuookami.ospf.kotlin.core.plugin.onnx.inference.OnnxInference
import fuookami.ospf.kotlin.core.plugin.onnx.inference.OnnxTensorData

val inference = OnnxInference.load(Paths.get("model.onnx")).valueOrFail()
inference.use {
    println(it.inputInfos)   // name / dtype / shape
    println(it.outputInfos)

    val outputs = it.evaluate(
        mapOf(
            "x" to OnnxTensorData(
                dtype = OnnxDataType.Float32,
                shape = listOf(1, 2),
                data = doubleArrayOf(1.0, 2.0)   // row-major
            )
        )
    ).valueOrFail()
    println(outputs.getValue("y").data.toList())
}
```

`OnnxInference` implements `AutoCloseable`, keeps the JVM-wide `OrtEnvironment` open, and is safe for concurrent `evaluate` calls. Loading from `Path`, `ByteArray` and `InputStream` is supported; a caller-owned stream is read fully but never closed.

## Call-back path

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

// objective
val objective = adapter.objectiveExtractor()

// constraint: y >= bound
val constraint = adapter.constraintExtractor(Comparison.GE, Flt64(5.0))

// or read the structured error explicitly
val value = adapter.evaluate(solution)   // Ret<Flt64>
```

Output selection always requires both `outputName` and a flattened `elementIndex`; no implicit aggregation is performed. Because `Extractor` cannot carry an error, a runtime inference failure makes the extractor return `null`; use `evaluate` when the error matters.

## LP/QP lowering path

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

// linear output
val y = model.linearOutput("y", 0).valueOrFail()
println(y.polynomial)

// register every produced symbol (activations + outputs) into the token table
model.registerInto(metaModel.tokens).valueOrFail()
```

Quadratic outputs require `OnnxImportMode.Quadratic`:

```kotlin
val quadraticModel = OnnxFunctionalModel.load(
    source = Paths.get("qp.onnx"),
    inputBindings = mapOf("x" to listOf(x0, x1)),
    options = OnnxImportOptions(mode = OnnxImportMode.Quadratic)
).valueOrFail()
val y = quadraticModel.quadraticOutput("y", 0).valueOrFail()
```

`OnnxFunctionalModel.symbols` is topologically ordered and safe to register repeatedly; `registerInto` skips symbols that are already present.

### Output aggregation

Both paths accept `OnnxOutputSelection`, so an output can either pick one element or fold the whole
tensor:

```kotlin
import fuookami.ospf.kotlin.core.plugin.onnx.output.OnnxAggregation
import fuookami.ospf.kotlin.core.plugin.onnx.output.OnnxOutputSelection

// single element (default)
val element = OnnxOutputSelection.element("y", 3)

// fold the whole tensor
val folded = OnnxOutputSelection.fold("y", OnnxAggregation.Mean)

// call-back path
val adapter = OnnxCallbackAdapter.create(
    inference = inference,
    tokens = model.tokens,
    bindings = bindings,
    selection = folded,
    converter = IntoValue.Identity
).valueOrFail()

// lowering path
val symbol = model.aggregateOutput(folded).valueOrFail()
```

`Sum`, `Mean`, `First` and `Last` stay inside the expression algebra. `Max` and `Min` use the core
`MaxFunction`/`MinFunction` symbols and therefore introduce binary selector variables; a fully
constant output folds numerically instead. A quadratic output under `Max`/`Min` fails with
`UnsupportedOperator`.

### Dynamic shapes

`OnnxInference` exposes dynamic dimensions as `null` and accepts the actual shape at call time.
For the lowering path a dynamic input must be made concrete:

```kotlin
val options = OnnxImportOptions(
    inputShapes = mapOf("x" to listOf(3L, 2L))
)
```

An override must match the declared rank and must not contradict a static dimension; unknown names,
rank mismatches and static conflicts are rejected with `BindingMismatch` or `ShapeMismatch`.
`OnnxCallbackAdapter.create` accepts the same `inputShapes` map.

### Control flow

`If`, `Loop` and `Scan` are unrolled at import time, so every branch and iteration count must be
constant. `If` picks the branch selected by a constant condition; `Loop` requires a constant trip
count; `Scan` derives the trip count from the leading dimension of the scan input. Non-constant
conditions and trip counts fail with `UnsupportedOperator`, and the total iteration count is capped
at `OnnxSubgraph.MAX_UNROLL`.

### Activations and bounds

`Relu` and `Clip` reuse the exact core function symbols, `LeakyRelu` and `HardSigmoid` expand into exact linear combinations. The remaining activations are approximated piecewise-linearly and therefore need finite input bounds:

```kotlin
val options = OnnxImportOptions(
    activationApproximation = OnnxActivationApproximation(segments = 32),
    inputBounds = mapOf("x" to OnnxBounds(lower = Flt64(-3.0), upper = Flt64(3.0)))
)
```

Without a provable finite range the import fails with `MissingBound`. Each segment adds one binary selector variable, so more segments mean a larger MILP.

## Structured errors

Every failure is an `OnnxError` carrying node, tensor and shape context:

`LoadFailed`, `InvalidModel`, `UnsupportedOpset`, `UnsupportedOperator`, `UnsupportedDType`, `DynamicShape`, `ShapeMismatch`, `BindingMismatch`, `OutputSelection`, `UnsupportedAttribute`, `DegreeOverflow`, `MissingBound`, `InferenceFailed`, `ResourceClosed`.

The structured error is also attached as the `Ret` error value, so it can be recovered:

```kotlin
val structured = (result as? Failed)?.error?.value as? OnnxError
structured?.fullMessage()?.let(::println)
```

## Tests

Models used in tests are built in memory with `ModelProto` builders; no `.onnx` or `.pb` binary is committed.

```bash
mvn test -pl ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-onnx -am
```

## License

Apache License 2.0
