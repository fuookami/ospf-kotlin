/**
 * ONNX 降阶模型 / ONNX lowered model
*/
package fuookami.ospf.kotlin.core.plugin.onnx.model

import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.core.model.basic.ExpressionRange
import fuookami.ospf.kotlin.core.symbol.IntermediateSymbol
import fuookami.ospf.kotlin.core.symbol.LinearIntermediateSymbol
import fuookami.ospf.kotlin.core.symbol.QuadraticIntermediateSymbol
import fuookami.ospf.kotlin.core.token.AbstractMutableTokenTable
import fuookami.ospf.kotlin.core.token.MutableTokenTable
import fuookami.ospf.kotlin.core.token.register
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.core.plugin.onnx.error.OnnxError
import fuookami.ospf.kotlin.core.plugin.onnx.graph.OnnxParsedGraph
import fuookami.ospf.kotlin.core.plugin.onnx.graph.OnnxParseResult
import fuookami.ospf.kotlin.core.plugin.onnx.graph.parseOnnxGraph
import fuookami.ospf.kotlin.core.plugin.onnx.inference.OnnxTensorInfo
import fuookami.ospf.kotlin.core.plugin.onnx.lowering.*
import fuookami.ospf.kotlin.core.plugin.onnx.output.OnnxAggregation
import fuookami.ospf.kotlin.core.plugin.onnx.output.OnnxOutputSelection
import fuookami.ospf.kotlin.core.plugin.onnx.output.foldTensor

/**
 * 降阶报告 / Lowering report
 *
 * @property mode 导入模式 / import mode
 * @property nodes 降阶的节点数 / number of lowered nodes
 * @property approximatedActivations 采用分段线性近似的激活节点 / activation nodes expressed with a piecewise-linear approximation
 * @property segments 分段数 / segment count
*/
data class OnnxLoweringReport(
    val mode: OnnxImportMode,
    val nodes: Int,
    val approximatedActivations: List<String>,
    val segments: Int
)

/**
 * 降阶后的 ONNX 函数模型 / Lowered ONNX functional model
 *
 * 输出只暴露核心的 [LinearIntermediateSymbol] 或 [QuadraticIntermediateSymbol]，不新增插件专用符号类型。 / Outputs expose only the core [LinearIntermediateSymbol] or [QuadraticIntermediateSymbol]; no
 * plugin-specific symbol type is introduced.
 *
 * 每个输出符号的结果多项式都是真实表达式；[IntermediateSymbol.evaluate] 只走 OSPF 表达式，
 * 不调用 ONNX Runtime。 / Every output symbol carries a real polynomial, and [IntermediateSymbol.evaluate] runs the OSPF
 * expression only, never ONNX Runtime.
 *
 * @property inputInfos 输入元信息 / input metadata
 * @property outputInfos 输出元信息 / output metadata
 * @property report 降阶报告 / lowering report
 * @property symbols 拓扑序的中间符号，可直接注册到令牌表 / topologically ordered intermediate symbols ready for token-table registration
*/
class OnnxFunctionalModel private constructor(
    val inputInfos: List<OnnxTensorInfo>,
    val outputInfos: List<OnnxTensorInfo>,
    val report: OnnxLoweringReport,
    symbols: List<IntermediateSymbol<Flt64>>,
    private val outputs: Map<String, List<IntermediateSymbol<Flt64>>>,
    private val outputTensors: Map<String, OnnxTensorExpr>,
    private val mode: OnnxImportMode
) {

    /** 符号列表的可变后备存储，聚合输出会追加新符号。 / Mutable backing store; aggregated outputs append new symbols. */
    private val _symbols: MutableList<IntermediateSymbol<Flt64>> = symbols.toMutableList()

    /** 拓扑序的中间符号，可直接注册到令牌表 / Topologically ordered intermediate symbols ready for token-table registration */
    val symbols: List<IntermediateSymbol<Flt64>>
        get() = _symbols

    /**
     * 取一个按选择聚合后的标量输出 / Get one scalar output aggregated according to the selection
     *
     * `Max`/`Min` 会注册核心极值函数符号，因此生成的符号已在 [symbols] 中；
     * `Sum`/`Mean`/`First`/`Last` 只产生表达式符号。 / `Max`/`Min` register core extremum function symbols, so the produced symbol is already in
     * [symbols]; `Sum`/`Mean`/`First`/`Last` produce an expression symbol only.
     *
     * @param selection 输出选择 / output selection
     * @return 线性中间符号 / linear intermediate symbol
     */
    fun aggregateOutput(selection: OnnxOutputSelection): Ret<LinearIntermediateSymbol<Flt64>> {
        val list = outputs[selection.outputName]
            ?: return failed(
                OnnxError.OutputSelection(
                    detail = "模型缺少输出 '${selection.outputName}' / model has no output '${selection.outputName}'",
                    tensorName = selection.outputName
                ).toError()
            )
        // First/Last/Element 直接复用已有输出符号，不再包一层表达式。
        // First/Last/Element reuse the existing output symbol instead of wrapping it in another expression.
        when (selection.aggregation) {
            OnnxAggregation.First -> return asLinear(list[0], selection)
            OnnxAggregation.Last -> return asLinear(list[list.size - 1], selection)
            OnnxAggregation.Element -> {
                if (selection.elementIndex !in list.indices) {
                    return failed(
                        OnnxError.OutputSelection(
                            detail = "输出 '${selection.outputName}' 元素数 ${list.size} 不含索引 ${selection.elementIndex} / " +
                                "output '${selection.outputName}' has ${list.size} elements, index ${selection.elementIndex} is out of range",
                            tensorName = selection.outputName
                        ).toError()
                    )
                }
                return asLinear(list[selection.elementIndex], selection)
            }

            else -> Unit
        }

        // 聚合使用降阶期的原始张量表达式，保留常量与次数信息。
        // Aggregation uses the raw lowered tensor expression, preserving constant and degree information.
        val tensor = outputTensors[selection.outputName]
            ?: OnnxTensorExpr(
                shape = listOf(list.size),
                elements = list.map { symbol ->
                    OnnxExpr.Linear(
                        LinearPolynomial(
                            monomials = listOf(LinearMonomial(Flt64.one, symbol)),
                            constant = Flt64.zero
                        )
                    )
                }
            )
        val mode = when (this.mode) {
            OnnxImportMode.Linear -> OnnxDegreeMode.Linear
            OnnxImportMode.Quadratic -> OnnxDegreeMode.Quadratic
        }
        val folded = when (val result = foldTensor(tensor, selection, mode, selection.outputName)) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        for (symbol in folded.symbols) {
            registerSymbol(symbol)
        }
        val built = when (val result = buildOutputSymbol(selectionName(selection), folded.expr, mode)) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        registerSymbol(built)
        return asLinear(built, selection)
    }

    /** 把符号按线性输出返回 / Return a symbol as a linear output */
    private fun asLinear(
        symbol: IntermediateSymbol<Flt64>,
        selection: OnnxOutputSelection
    ): Ret<LinearIntermediateSymbol<Flt64>> {
        return when (symbol) {
            is LinearIntermediateSymbol<*> -> {
                @Suppress("UNCHECKED_CAST")
                ok(symbol as LinearIntermediateSymbol<Flt64>)
            }

            else -> failed(
                OnnxError.OutputSelection(
                    detail = "输出 '${selection.outputName}' 不是线性符号 / " +
                        "output '${selection.outputName}' is not a linear symbol",
                    tensorName = selection.outputName
                ).toError()
            )
        }
    }

    /** 生成聚合输出的符号名 / Build the symbol name of an aggregated output */
    private fun selectionName(selection: OnnxOutputSelection): String {
        return if (selection.aggregation == OnnxAggregation.Element) {
            "${selection.outputName}_${selection.elementIndex}"
        } else {
            "${selection.outputName}_${selection.aggregation.name.lowercase()}"
        }
    }

    /** 把符号登记到可变符号列表 / Register a symbol into the mutable symbol list */
    private fun registerSymbol(symbol: IntermediateSymbol<Flt64>) {
        val mutable = _symbols
        if (mutable.none { it === symbol }) {
            mutable.add(symbol)
        }
    }

    /**
     * 按名称取输出的有序标量符号 / Get the ordered scalar symbols of an output by name
     *
     * @param outputName 输出名称 / output name
     * @return 标量符号列表 / scalar symbol list
     */
    fun outputs(outputName: String): Ret<List<IntermediateSymbol<Flt64>>> {
        val list = outputs[outputName]
            ?: return failed(
                OnnxError.OutputSelection(
                    detail = "模型缺少输出 '$outputName' / model has no output '$outputName'",
                    tensorName = outputName
                ).toError()
            )
        return ok(list)
    }

    /**
     * 取一个标量输出的线性符号 / Get the linear symbol of one scalar output
     *
     * @param outputName 输出名称 / output name
     * @param elementIndex 展平索引 / flattened index
     * @return 线性中间符号 / linear intermediate symbol
     */
    fun linearOutput(outputName: String, elementIndex: Int): Ret<LinearIntermediateSymbol<Flt64>> {
        val symbol = when (val result = scalarOutput(outputName, elementIndex)) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        return when (symbol) {
            is LinearIntermediateSymbol<*> -> {
                @Suppress("UNCHECKED_CAST")
                ok(symbol as LinearIntermediateSymbol<Flt64>)
            }

            else -> failed(
                OnnxError.OutputSelection(
                    detail = "输出 '$outputName'[$elementIndex] 不是线性符号 / " +
                        "output '$outputName'[$elementIndex] is not a linear symbol",
                    tensorName = outputName
                ).toError()
            )
        }
    }

    /**
     * 取一个标量输出的二次符号 / Get the quadratic symbol of one scalar output
     *
     * @param outputName 输出名称 / output name
     * @param elementIndex 展平索引 / flattened index
     * @return 二次中间符号 / quadratic intermediate symbol
     */
    fun quadraticOutput(outputName: String, elementIndex: Int): Ret<QuadraticIntermediateSymbol<Flt64>> {
        if (mode != OnnxImportMode.Quadratic) {
            return failed(
                OnnxError.OutputSelection(
                    detail = "线性导入模式下不提供二次输出，请使用 OnnxImportMode.Quadratic / " +
                        "quadratic outputs require OnnxImportMode.Quadratic",
                    tensorName = outputName
                ).toError()
            )
        }
        val symbol = when (val result = scalarOutput(outputName, elementIndex)) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        return when (symbol) {
            is QuadraticIntermediateSymbol<*> -> {
                @Suppress("UNCHECKED_CAST")
                ok(symbol as QuadraticIntermediateSymbol<Flt64>)
            }

            else -> failed(
                OnnxError.OutputSelection(
                    detail = "输出 '$outputName'[$elementIndex] 不是二次符号 / " +
                        "output '$outputName'[$elementIndex] is not a quadratic symbol",
                    tensorName = outputName
                ).toError()
            )
        }
    }

    /**
     * 把所有符号按拓扑序注册到令牌表 / Register every symbol into a token table in topological order
     *
     * 重复调用是安全的：已存在的变量会被跳过，符号按身份去重。 / Repeated calls are safe: existing variables are skipped and symbols are de-duplicated by identity.
     *
     * @param tokens 目标可变令牌表 / target mutable token table
     * @return 注册结果 / registration result
     */
    fun registerInto(tokens: AbstractMutableTokenTable<Flt64>): Try {
        val mutable = tokens as? MutableTokenTable<Flt64>
            ?: return Failed(
                ErrorCode.IllegalArgument,
                "注册需要 MutableTokenTable / registration requires a MutableTokenTable"
            )
        // 先按身份把符号放进符号表（跳过已存在者），再由公共注册流程补齐辅助 token 与依赖层。
        // First insert the symbols by identity (skipping existing ones), then let the shared registration path fill in auxiliary tokens and dependency layers.
        val existing = mutable.symbols.toList()
        for (symbol in symbols) {
            if (existing.any { it === symbol }) {
                continue
            }
            when (val result = mutable.add(symbol)) {
                is Ok -> {}
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
        }
        return symbols.register(tokenTable = mutable, skipExistingVariables = true)
    }

    /** 取单个标量输出符号 / Get one scalar output symbol */
    private fun scalarOutput(outputName: String, elementIndex: Int): Ret<IntermediateSymbol<Flt64>> {
        val list = outputs[outputName]
            ?: return failed(
                OnnxError.OutputSelection(
                    detail = "模型缺少输出 '$outputName' / model has no output '$outputName'",
                    tensorName = outputName
                ).toError()
            )
        if (elementIndex !in list.indices) {
            return failed(
                OnnxError.OutputSelection(
                    detail = "输出 '$outputName' 元素数 ${list.size} 不含索引 $elementIndex / " +
                        "output '$outputName' has ${list.size} elements, index $elementIndex is out of range",
                    tensorName = outputName
                ).toError()
            )
        }
        return ok(list[elementIndex])
    }

    companion object {

        /**
         * 从文件加载 / Load from a file
         *
         * @param source 模型路径 / model path
         * @param inputBindings 输入名到符号列表的绑定 / bindings from input name to symbol list
         * @param options 导入选项 / import options
         * @return 降阶模型 / lowered model
         */
        fun load(
            source: Path,
            inputBindings: Map<String, List<Symbol>>,
            options: OnnxImportOptions = OnnxImportOptions()
        ): Ret<OnnxFunctionalModel> {
            if (!Files.isRegularFile(source)) {
                return failed(OnnxError.LoadFailed("not a regular file: $source").toError())
            }
            val bytes = try {
                Files.readAllBytes(source)
            } catch (e: Exception) {
                return failed(
                    OnnxError.LoadFailed("failed to read $source: ${e.message ?: e::class.simpleName}").toError()
                )
            }
            return load(bytes, inputBindings, options)
        }

        /**
         * 从字节数组加载 / Load from a byte array
         *
         * @param source 模型字节 / model bytes
         * @param inputBindings 输入名到符号列表的绑定 / bindings from input name to symbol list
         * @param options 导入选项 / import options
         * @return 降阶模型 / lowered model
         */
        fun load(
            source: ByteArray,
            inputBindings: Map<String, List<Symbol>>,
            options: OnnxImportOptions = OnnxImportOptions()
        ): Ret<OnnxFunctionalModel> {
            if (source.isEmpty()) {
                return failed(OnnxError.LoadFailed("empty model bytes").toError())
            }
            val graph = when (val parsed = parseOnnxGraph(source)) {
                is OnnxParseResult.Success -> parsed.graph
                is OnnxParseResult.Failure -> return failed(parsed.error.toError())
            }
            return lower(graph, inputBindings, options)
        }

        /**
         * 从输入流加载 / Load from an input stream
         *
         * 流会被完整读取，但不会关闭。 / The stream is fully read but never closed.
         *
         * @param source 输入流 / input stream
         * @param inputBindings 输入名到符号列表的绑定 / bindings from input name to symbol list
         * @param options 导入选项 / import options
         * @return 降阶模型 / lowered model
         */
        fun load(
            source: InputStream,
            inputBindings: Map<String, List<Symbol>>,
            options: OnnxImportOptions = OnnxImportOptions()
        ): Ret<OnnxFunctionalModel> {
            val bytes = try {
                source.readBytes()
            } catch (e: Exception) {
                return failed(
                    OnnxError.LoadFailed("failed to read stream: ${e.message ?: e::class.simpleName}").toError()
                )
            }
            return load(bytes, inputBindings, options)
        }

        /** 执行降阶 / Perform lowering */
        private fun lower(
            graph: OnnxParsedGraph,
            inputBindings: Map<String, List<Symbol>>,
            options: OnnxImportOptions
        ): Ret<OnnxFunctionalModel> {
            if (graph.opsetVersion !in options.supportedOpset.map { it.toLong() }) {
                return failed(
                    OnnxError.UnsupportedOpset(
                        domain = "",
                        version = graph.opsetVersion,
                        supported = options.supportedOpset
                    ).toError()
                )
            }
            val mode = when (options.mode) {
                OnnxImportMode.Linear -> OnnxDegreeMode.Linear
                OnnxImportMode.Quadratic -> OnnxDegreeMode.Quadratic
            }

            // 把动态维度具体化：覆盖值必须与模型声明兼容（动态维或同值静态维）。
            // Materialize dynamic dimensions: an override must be compatible with the declared shape (dynamic or equal static).
            val resolvedGraph = when (val result = materializeShapes(graph, options.inputShapes)) {
                is Ok -> result.value
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
            val graph = resolvedGraph

            // 显式范围覆盖写回绑定变量的取值域，区间传播与 Big-M 取证都能看到。
            // Explicit bound overrides are written back onto the bound variables' ranges so interval propagation and Big-M derivation both observe them.
            for ((inputName, bounds) in options.inputBounds) {
                val boundSymbols = inputBindings[inputName]
                    ?: return failed(
                        OnnxError.BindingMismatch(
                            detail = "范围覆盖指向未知输入 '$inputName' / bound override targets unknown input '$inputName'",
                            tensorName = inputName
                        ).toError()
                    )
                for (symbol in boundSymbols) {
                    val variable = symbol as? AbstractVariableItem<*, *>
                        ?: return failed(
                            OnnxError.BindingMismatch(
                                detail = "范围覆盖只支持变量，'${symbol.name}' 不是变量 / " +
                                    "bound overrides support variables only, '${symbol.name}' is not a variable",
                                tensorName = inputName
                            ).toError()
                        )
                    @Suppress("UNCHECKED_CAST")
                    val range = variable.range as ExpressionRange<Flt64>
                    if (!range.geq(bounds.lower) || !range.leq(bounds.upper)) {
                        return failed(
                            OnnxError.MissingBound(
                                detail = "输入 '$inputName' 的范围覆盖与已有取值域不相交 / " +
                                    "the bound override of input '$inputName' does not intersect the current range",
                                tensorName = inputName
                            ).toError()
                        )
                    }
                }
            }

            val bindings = inputBindings.mapValues { (_, symbols) ->
                symbols.map { symbol ->
                    OnnxInputElement(
                        symbol = symbol,
                        variable = OnnxExpr.Linear(
                            fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial(
                                monomials = listOf(
                                    fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial(Flt64.one, symbol)
                                ),
                                constant = Flt64.zero
                            )
                        )
                    )
                }
            }
            val context = OnnxLoweringContext(
                mode = mode,
                bindings = bindings,
                activationSegments = options.activationApproximation.segments
            )
            when (val lowered = lowerGraph(graph, context)) {
                is OnnxLoweringResult.Failure -> return failed(lowered.error.toError())
                is OnnxLoweringResult.Success -> Unit
            }

            val outputSymbols = LinkedHashMap<String, List<IntermediateSymbol<Flt64>>>()
            val outputTensors = LinkedHashMap<String, OnnxTensorExpr>()
            for (output in graph.outputs) {
                val tensor = context.values[output.name]
                    ?: return failed(
                        OnnxError.OutputSelection(
                            detail = "图输出 '${output.name}' 未被降阶 / graph output '${output.name}' was not lowered",
                            tensorName = output.name
                        ).toError()
                    )
                outputTensors[output.name] = tensor
                val scalars = ArrayList<IntermediateSymbol<Flt64>>(tensor.elementCount)
                for ((position, element) in tensor.elements.withIndex()) {
                    val name = "${output.name}_$position"
                    val symbol = when (val built = buildOutputSymbol(name, element, mode)) {
                        is Ok -> built.value
                        is Failed -> return Failed(built.error)
                        is Fatal -> return Fatal(built.errors)
                    }
                    if (symbol !in context.symbols) {
                        context.symbols.add(symbol)
                    }
                    scalars.add(symbol)
                }
                outputSymbols[output.name] = scalars
            }

            val inputInfos = graph.inputs.map { OnnxTensorInfo(name = it.name, dtype = it.dtype, shape = it.shape) }
            val outputInfos = graph.outputs.map { OnnxTensorInfo(name = it.name, dtype = it.dtype, shape = it.shape) }

            return ok(
                OnnxFunctionalModel(
                    inputInfos = inputInfos,
                    outputInfos = outputInfos,
                    report = OnnxLoweringReport(
                        mode = options.mode,
                        nodes = graph.nodes.size,
                        approximatedActivations = context.approximatedActivations.toList(),
                        segments = options.activationApproximation.segments
                    ),
                    symbols = context.symbols.toList(),
                    outputs = outputSymbols,
                    outputTensors = outputTensors,
                    mode = options.mode
                )
            )
        }

        /**
         * 用调用方给出的具体形状替换动态维度 / Replace dynamic dimensions with caller-supplied concrete shapes
         *
         * 覆盖只允许出现在图输入上；未覆盖的输入若仍含动态维度，由后续降阶以 `DynamicShape` 失败。 / Overrides are allowed on graph inputs only; inputs left with dynamic dimensions fail later with
         * `DynamicShape` during lowering.
         *
         * @param graph 解析后的图 / parsed graph
         * @param overrides 输入名到具体形状的映射 / map from input name to concrete shape
         * @return 具体化后的图 / materialized graph
         */
        private fun materializeShapes(
            graph: OnnxParsedGraph,
            overrides: Map<String, List<Long>>
        ): Ret<OnnxParsedGraph> {
            if (overrides.isEmpty()) {
                return ok(graph)
            }
            val known = graph.inputs.map { it.name }.toSet()
            val unknown = overrides.keys - known
            if (unknown.isNotEmpty()) {
                return failed(
                    OnnxError.BindingMismatch(
                        detail = "形状覆盖指向未知输入 ${unknown.sorted()} / shape overrides target unknown inputs ${unknown.sorted()}"
                    ).toError()
                )
            }
            val inputs = graph.inputs.map { input ->
                val override = overrides[input.name] ?: return@map input
                if (override.size != input.shape.size) {
                    return failed(
                        OnnxError.ShapeMismatch(
                            detail = "输入 '${input.name}' 的具体形状秩 ${override.size} 与声明秩 ${input.shape.size} 不符 / " +
                                "concrete rank ${override.size} of input '${input.name}' does not match declared rank ${input.shape.size}",
                            tensorName = input.name
                        ).toError()
                    )
                }
                for (position in input.shape.indices) {
                    val declared = input.shape[position]
                    if (declared != null && declared != override[position]) {
                        return failed(
                            OnnxError.ShapeMismatch(
                                detail = "输入 '${input.name}' 第 $position 维声明为 $declared，与覆盖值 ${override[position]} 冲突 / " +
                                    "dimension $position of input '${input.name}' is declared $declared but overridden to ${override[position]}",
                                tensorName = input.name
                            ).toError()
                        )
                    }
                }
                input.copy(shape = override.map { it as Long? })
            }
            // 输出形状无法从覆盖直接推导，保留声明；由降阶层按实际表达式给出形状。
            // Output shapes cannot be derived from input overrides, so declarations are kept; the lowering stage derives the actual shape from the expressions.
            return ok(graph.copy(inputs = inputs))
        }

        /** 把标量表达式构造成输出符号 / Build an output symbol from a scalar expression */        private fun buildOutputSymbol(
            name: String,
            element: OnnxExpr,
            mode: OnnxDegreeMode
        ): Ret<IntermediateSymbol<Flt64>> {
            return when (element) {
                is OnnxExpr.Constant -> {
                    val polynomial = fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial(
                        monomials = emptyList(),
                        constant = element.value
                    )
                    val symbol: LinearIntermediateSymbol<Flt64> = LinearIntermediateSymbol.empty(
                        constants = Flt64,
                        name = name
                    )
                    symbol.asMutable().setConstant(polynomial.constant)
                    ok(symbol)
                }

                is OnnxExpr.Linear -> {
                    val symbol: LinearIntermediateSymbol<Flt64> = LinearIntermediateSymbol.empty(
                        constants = Flt64,
                        name = name
                    )
                    val mutable = symbol.asMutable()
                    for (monomial in element.polynomial.monomials) {
                        mutable.addMonomial(monomial)
                    }
                    mutable.setConstant(element.polynomial.constant)
                    ok(symbol)
                }

                is OnnxExpr.Quadratic -> {
                    if (mode != OnnxDegreeMode.Quadratic) {
                        return failed(
                            OnnxError.DegreeOverflow(
                                detail = "线性模式下输出 '$name' 为二次表达式 / output '$name' is quadratic under linear mode",
                                nodeName = name
                            ).toError()
                        )
                    }
                    val symbol: QuadraticIntermediateSymbol<Flt64> = QuadraticIntermediateSymbol.empty(
                        constants = Flt64,
                        name = name
                    )
                    val mutable = symbol.asMutable()
                    for (monomial in element.polynomial.monomials) {
                        mutable.addMonomial(monomial)
                    }
                    mutable.setConstant(element.polynomial.constant)
                    ok(symbol)
                }
            }
        }
    }
}
