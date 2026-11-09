/**
 * ONNX 回调适配 / ONNX call-back adaptation
*/
package fuookami.ospf.kotlin.core.plugin.onnx.callback

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.math.symbol.operation.satisfiedBy
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.core.model.basic.Solution
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.token.AbstractTokenTable
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.core.plugin.onnx.error.OnnxError
import fuookami.ospf.kotlin.core.plugin.onnx.inference.OnnxInference
import fuookami.ospf.kotlin.core.plugin.onnx.inference.OnnxTensorData
import fuookami.ospf.kotlin.core.plugin.onnx.output.OnnxAggregation
import fuookami.ospf.kotlin.core.plugin.onnx.output.OnnxOutputSelection
import fuookami.ospf.kotlin.core.plugin.onnx.output.foldTensorData

/**
 * 一个 ONNX 输入的变量绑定 / Variable binding of one ONNX input
 *
 * 变量列表的顺序即 tensor 的行主序展开顺序，数量必须等于输入元素数。 / The variable order is the row-major flattening order of the tensor and its length must equal the
 * input element count.
 *
 * @property inputName ONNX 输入名称 / ONNX input name
 * @property variables 按行主序排列的变量 / variables in row-major order
*/
data class OnnxInputBinding<V>(
    val inputName: String,
    val variables: List<AbstractVariableItem<*, *>>
) where V : RealNumber<V>, V : NumberField<V>

/**
 * 把 ONNX 推理结果适配为回调模型的目标值与约束谓词 / Adapt ONNX inference results into call-back model objectives and constraint predicates
 *
 * 输出选择必须同时给出 [outputName] 与展平后的 [elementIndex]，不做任何隐式聚合。 / Output selection requires both [outputName] and the flattened [elementIndex]; no implicit aggregation
 * is performed.
 *
 * 输出选择通过 [selection] 指定：取单个元素，或按 sum/mean/first/last/max/min 折叠整个张量。
 * / Output selection goes through [selection]: either one element, or a sum/mean/first/last/max/min fold
 * over the whole tensor.
 *
 * 现有回调接口 [Extractor] 无法携带错误，因此运行时推理失败时 Extractor 返回 null；
 * 需要详细错误请改用 [evaluate]。 / The existing [Extractor] interface cannot carry errors, so a runtime inference failure makes the
 * extractor return null; use [evaluate] when the detailed error matters.
 *
 * @property inference 推理句柄 / inference handle
 * @property tokens 令牌表，用于把变量解析为解索引 / token table used to resolve variables into solution indices
 * @property bindings 输入绑定 / input bindings
 * @property outputName 输出名称 / output name
 * @property elementIndex 输出展平索引 / flattened output index
 * @property selection 输出选择（含聚合方式）/ output selection including the aggregation mode
 * @property converter 值类型转换器 / value type converter
*/
class OnnxCallbackAdapter<V> private constructor(
    private val inference: OnnxInference,
    private val tokens: AbstractTokenTable<V>,
    private val bindings: List<OnnxInputBinding<V>>,
    val outputName: String,
    val elementIndex: Int,
    val selection: OnnxOutputSelection,
    private val inputShapes: Map<String, List<Long>>,
    private val converter: IntoValue<V>
) where V : RealNumber<V>, V : NumberField<V> {

    /** 绑定的输入名称集合 / Bound input names */
    val inputNames: List<String>
        get() = bindings.map { it.inputName }

    /**
     * 解析输入形状，动态维度用调用方覆盖补齐 / Resolve an input shape, filling dynamic dimensions from the caller override
     *
     * @param inputName 输入名 / input name
     * @param declared 模型声明形状 / declared model shape
     * @return 全静态形状，无法补齐时返回 null / fully static shape, or null when it cannot be completed
     */
    private fun resolvedShape(inputName: String, declared: List<Long?>): List<Long>? {
        return resolveShape(inputName, declared, inputShapes)
    }

    /**
     * 推理并取出选定输出标量 / Run inference and read the selected output scalar
     *
     * @param solution 解向量 / solution vector
     * @return 输出标量 / selected output scalar
     */
    fun evaluate(solution: Solution<V>): Ret<V> {
        val inputs = LinkedHashMap<String, OnnxTensorData>()
        for (binding in bindings) {
            val info = inference.inputInfos.firstOrNull { it.name == binding.inputName }
                ?: return failed(
                    OnnxError.BindingMismatch(
                        detail = "模型缺少输入 '${binding.inputName}' / model has no input '${binding.inputName}'",
                        tensorName = binding.inputName
                    ).toError()
                )
            val shape = resolvedShape(binding.inputName, info.shape)
                ?: return failed(
                    OnnxError.DynamicShape(
                        detail = "输入 '${binding.inputName}' 含动态维度且未提供具体形状 / " +
                            "input '${binding.inputName}' has dynamic dimensions and no concrete shape was provided",
                        tensorName = binding.inputName,
                        shape = info.shape
                    ).toError()
                )
            val elementCount = shape.fold(1L) { acc, dim -> acc * dim }.toInt()
            if (elementCount != binding.variables.size) {
                return failed(
                    OnnxError.BindingMismatch(
                        detail = "输入 '${binding.inputName}' 元素数 $elementCount 与绑定变量数 ${binding.variables.size} 不符 / " +
                            "input '${binding.inputName}' has $elementCount elements but ${binding.variables.size} variables are bound",
                        tensorName = binding.inputName
                    ).toError()
                )
            }
            val data = DoubleArray(elementCount)
            for ((position, variable) in binding.variables.withIndex()) {
                val index = tokens.indexOf(variable)
                    ?: return failed(
                        OnnxError.BindingMismatch(
                            detail = "变量 '${variable.name}' 不在令牌表中 / variable '${variable.name}' is not registered in the token table",
                            tensorName = binding.inputName
                        ).toError()
                    )
                val value = solution.getOrNull(index)
                    ?: return failed(
                        OnnxError.BindingMismatch(
                            detail = "解向量长度 ${solution.size} 不含索引 $index（变量 '${variable.name}'）/ " +
                                "solution length ${solution.size} has no index $index for variable '${variable.name}'",
                            tensorName = binding.inputName
                        ).toError()
                    )
                data[position] = converter.fromValue(value).toDouble()
            }
            inputs[binding.inputName] = OnnxTensorData(
                dtype = info.dtype,
                shape = shape.map { it.toInt() },
                data = data
            )
        }

        val outputs = when (val result = inference.evaluate(inputs)) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        val output = outputs[outputName]
            ?: return failed(
                OnnxError.OutputSelection(
                    detail = "模型未返回输出 '$outputName' / model did not return output '$outputName'",
                    tensorName = outputName
                ).toError()
            )
        val folded = when (val result = foldTensorData(output, selection, outputName)) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        return ok(converter.intoValue(Flt64(folded)))
    }

    /**
     * 适配为现有目标函数提取器 / Adapt into an existing objective extractor
     *
     * @return 目标值提取器，推理失败时返回 null / objective extractor returning null on inference failure
     */
    fun objectiveExtractor(): Extractor<V?, Solution<V>> {
        return Extractor { solution -> evaluate(solution).value }
    }

    /**
     * 适配为现有约束谓词提取器 / Adapt into an existing constraint predicate extractor
     *
     * @param comparison 比较关系 / comparison relation
     * @param bound 右端常数 / right-hand constant
     * @param tolerance 可选容差；为 null 时使用精确比较 / optional tolerance; exact comparison when null
     * @return 约束谓词提取器，推理失败时返回 null / constraint predicate extractor returning null on inference failure
     */
    fun constraintExtractor(
        comparison: Comparison,
        bound: V,
        tolerance: V? = null
    ): Extractor<Boolean?, Solution<V>> {
        return Extractor { solution ->
            val value = evaluate(solution).value ?: return@Extractor null
            if (tolerance == null) {
                comparison.satisfiedBy(value.toFlt64(), bound.toFlt64())
            } else {
                // 容差只在紧侧放宽，避免把明显违反的解判为可行。
                // Tolerance only loosens the tight side so clearly violated solutions stay infeasible.
                val lower = converter.fromValue(bound) - converter.fromValue(tolerance)
                val upper = converter.fromValue(bound) + converter.fromValue(tolerance)
                val actual = value.toFlt64()
                when (comparison) {
                    Comparison.LT -> actual < upper
                    Comparison.LE -> actual <= upper
                    Comparison.GT -> actual > lower
                    Comparison.GE -> actual >= lower
                    Comparison.EQ -> actual >= lower && actual <= upper
                    Comparison.NE -> actual < lower || actual > upper
                }
            }
        }
    }

    companion object {
        /**
         * 解析输入形状，动态维度用调用方覆盖补齐 / Resolve an input shape, filling dynamic dimensions from the caller override
         *
         * @param inputName 输入名 / input name
         * @param declared 模型声明形状 / declared model shape
         * @param overrides 覆盖表 / override table
         * @return 全静态形状，无法补齐时返回 null / fully static shape, or null when it cannot be completed
         */
        private fun resolveShape(
            inputName: String,
            declared: List<Long?>,
            overrides: Map<String, List<Long>>
        ): List<Long>? {
            val override = overrides[inputName]
            if (override != null) {
                if (override.size != declared.size) {
                    return null
                }
                for (position in declared.indices) {
                    val value = declared[position]
                    if (value != null && value != override[position]) {
                        return null
                    }
                }
                return override
            }
            if (declared.any { it == null }) {
                return null
            }
            val static = declared.map { it!! }
            return static.takeIf { it.all { dim -> dim > 0L } }
        }
        /**
         * 创建回调适配器 / Create a call-back adapter
         *
         * 构造期完成全部校验：绑定覆盖、元素数一致、变量可解析、输出名与索引有效。 / All validation happens at construction: binding coverage, element counts, resolvable variables,
         * and a valid output name and index.
         *
         * @param inference 推理句柄 / inference handle
         * @param tokens 令牌表 / token table
         * @param bindings 输入绑定 / input bindings
         * @param outputName 输出名称 / output name
         * @param elementIndex 输出展平索引 / flattened output index
         * @param converter 值类型转换器 / value type converter
         * @return 适配器 / adapter
         */
        fun <V> create(
            inference: OnnxInference,
            tokens: AbstractTokenTable<V>,
            bindings: List<OnnxInputBinding<V>>,
            outputName: String,
            elementIndex: Int,
            converter: IntoValue<V>,
            inputShapes: Map<String, List<Long>> = emptyMap()
        ): Ret<OnnxCallbackAdapter<V>> where V : RealNumber<V>, V : NumberField<V> {
            return create(
                inference = inference,
                tokens = tokens,
                bindings = bindings,
                selection = OnnxOutputSelection.element(outputName, elementIndex),
                converter = converter,
                inputShapes = inputShapes
            )
        }

        /**
         * 按输出选择创建回调适配器 / Create a call-back adapter from an output selection
         *
         * @param inference 推理句柄 / inference handle
         * @param tokens 令牌表 / token table
         * @param bindings 输入绑定 / input bindings
         * @param selection 输出选择 / output selection
         * @param converter 值类型转换器 / value type converter
         * @return 适配器 / adapter
         */
        fun <V> create(
            inference: OnnxInference,
            tokens: AbstractTokenTable<V>,
            bindings: List<OnnxInputBinding<V>>,
            selection: OnnxOutputSelection,
            converter: IntoValue<V>,
            inputShapes: Map<String, List<Long>> = emptyMap()
        ): Ret<OnnxCallbackAdapter<V>> where V : RealNumber<V>, V : NumberField<V> {
            val outputName = selection.outputName
            val elementIndex = selection.elementIndex
            if (inference.isClosed) {
                return failed(OnnxError.ResourceClosed("inference handle is closed").toError())
            }
            val declared = inference.inputInfos.map { it.name }.toSet()
            val bound = bindings.map { it.inputName }.toSet()
            if (bound.size != bindings.size) {
                return failed(
                    OnnxError.BindingMismatch("输入绑定存在重复名称 / duplicated input names in bindings").toError()
                )
            }
            val missing = declared - bound
            if (missing.isNotEmpty()) {
                return failed(
                    OnnxError.BindingMismatch(
                        detail = "缺少输入绑定 ${missing.sorted()} / missing bindings for ${missing.sorted()}"
                    ).toError()
                )
            }
            val unexpected = bound - declared
            if (unexpected.isNotEmpty()) {
                return failed(
                    OnnxError.BindingMismatch(
                        detail = "绑定到不存在的输入 ${unexpected.sorted()} / bindings for unknown inputs ${unexpected.sorted()}"
                    ).toError()
                )
            }
            for (binding in bindings) {
                val info = inference.inputInfos.first { it.name == binding.inputName }
                val shape = resolveShape(binding.inputName, info.shape, inputShapes)
                if (shape == null) {
                    return failed(
                        OnnxError.DynamicShape(
                            detail = "输入 '${binding.inputName}' 含动态维度且未提供具体形状 / " +
                                "input '${binding.inputName}' has dynamic dimensions and no concrete shape was provided",
                            tensorName = binding.inputName,
                            shape = info.shape
                        ).toError()
                    )
                }
                val elementCount = shape.fold(1L) { acc, dim -> acc * dim }.toInt()
                if (elementCount != binding.variables.size) {
                    return failed(
                        OnnxError.BindingMismatch(
                            detail = "输入 '${binding.inputName}' 元素数 $elementCount 与绑定变量数 ${binding.variables.size} 不符 / " +
                                "input '${binding.inputName}' has $elementCount elements but ${binding.variables.size} variables are bound",
                            tensorName = binding.inputName
                        ).toError()
                    )
                }
                for (variable in binding.variables) {
                    if (tokens.indexOf(variable) == null) {
                        return failed(
                            OnnxError.BindingMismatch(
                                detail = "变量 '${variable.name}' 不在令牌表中 / variable '${variable.name}' is not registered in the token table",
                                tensorName = binding.inputName
                            ).toError()
                        )
                    }
                }
            }

            val outputInfo = inference.outputInfos.firstOrNull { it.name == outputName }
                ?: return failed(
                    OnnxError.OutputSelection(
                        detail = "模型缺少输出 '$outputName' / model has no output '$outputName'",
                        tensorName = outputName
                    ).toError()
                )
            val outputCount = outputInfo.elementCount
            // 只有按元素选择时才校验索引；折叠方式不依赖索引。
            // The index is validated only for element selection; folding modes do not use it.
            if (selection.aggregation == OnnxAggregation.Element && outputCount != null && elementIndex !in 0 until outputCount) {
                return failed(
                    OnnxError.OutputSelection(
                        detail = "输出 '$outputName' 元素数 $outputCount 不含索引 $elementIndex / " +
                            "output '$outputName' has $outputCount elements, index $elementIndex is out of range",
                        tensorName = outputName
                    ).toError()
                )
            }

            return ok(
                OnnxCallbackAdapter(
                    inference = inference,
                    tokens = tokens,
                    bindings = bindings,
                    outputName = outputName,
                    elementIndex = elementIndex,
                    selection = selection,
                    inputShapes = inputShapes,
                    converter = converter
                )
            )
        }
    }
}
