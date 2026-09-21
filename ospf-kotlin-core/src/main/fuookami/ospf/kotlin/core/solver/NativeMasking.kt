package fuookami.ospf.kotlin.core.solver

import kotlin.math.abs
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.core.model.intermediate.MaskingStructure
import fuookami.ospf.kotlin.core.solver.value.toSolverDouble
import fuookami.ospf.kotlin.core.variable.VariableItemKey
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem

/**
 * 已验证的二值门控。 / Validated binary masking.
 *
 * @property maskKey 输入 mask 键 / Input mask key
 * @property value 数值结果及范围证明 / Numeric result and bounds proof
 * @property definition 可选的 mask 定义等式 / Optional mask definition equality
 */
data class NativeMaskingData(
    val maskKey: VariableItemKey,
    val value: NativeConditionalValueData,
    val definition: NativeMaskDefinitionData? = null
)

/**
 * mask 的仿射定义，不要求表达式整个域都在 [0,1] 内。 / Affine mask definition without requiring its entire domain inside [0,1].
 *
 * @property terms 仿射系数 / Affine coefficients
 * @property constant 常数项 / Constant term
 */
data class NativeMaskDefinitionData(val terms: Map<VariableItemKey, Double>, val constant: Double)

/**
 * 验证门控关系及原 fallback 输入域。 / Validate masking and the original fallback input domain.
 *
 * @param structure 门控快照 / Masking snapshot
 * @param maximumMagnitude 求解器数值幅值上限 / Solver magnitude limit
 * @return 已验证数据或错误 / Validated data or failure
 */
fun prepareNativeMasking(
    structure: MaskingStructure<*>,
    maximumMagnitude: Double = Double.POSITIVE_INFINITY
): Ret<NativeMaskingData> = prepareMasking(structure, maximumMagnitude)

private fun <V> prepareMasking(
    structure: MaskingStructure<V>,
    maximumMagnitude: Double
): Ret<NativeMaskingData> where V : RealNumber<V>, V : NumberField<V> {
    return try {
        when (val constraints = structure.generateConstraints()) {
            is Ok -> Unit
            is Failed -> return Failed(constraints.error)
            is Fatal -> return Fatal(constraints.errors)
        }
        val definition = structure.maskDefinition?.let { polynomial ->
            fun usable(value: Double) = value.isFinite() && abs(value) < minOf(maximumMagnitude, Double.MAX_VALUE)
            val constant = structure.converter.fromValue(polynomial.constant).toSolverDouble(fieldName = "maskDefinition.constant")
            if (!usable(constant)) return Failed(ErrorCode.IllegalArgument, "mask 定义常数无效。 / Invalid mask definition constant.")
            val terms = linkedMapOf<VariableItemKey, Double>()
            for (term in polynomial.monomials) {
                val variable = term.symbol as? AbstractVariableItem<*, *>
                    ?: return Failed(ErrorCode.IllegalArgument, "mask 定义未展平。 / Mask definition is not flattened.")
                if (variable.key == structure.mask.key || variable.key == structure.resultVariable.key) {
                    return Failed(ErrorCode.IllegalArgument, "mask 定义包含自身结果。 / Mask definition references its own result.")
                }
                val coefficient = structure.converter.fromValue(term.coefficient).toSolverDouble(fieldName = "maskDefinition.coefficient")
                val combined = (terms[variable.key] ?: 0.0) + coefficient
                if (!usable(coefficient) || !usable(combined)) return Failed(ErrorCode.IllegalArgument, "mask 定义系数无效。 / Invalid mask definition coefficient.")
                terms[variable.key] = combined
            }
            NativeMaskDefinitionData(terms.filterValues { it != 0.0 }, constant)
        }
        when (val value = prepareConditionalValue(structure.value, structure.converter, maximumMagnitude)) {
            is Ok -> Ok(NativeMaskingData(
                maskKey = structure.mask.key,
                value = value.value,
                definition = definition
            ))
            is Failed -> Failed(value.error)
            is Fatal -> Fatal(value.errors)
        }
    } catch (_: RuntimeException) {
        Failed(ErrorCode.IllegalArgument, "门控原生范围证明失败。 / Native masking bounds proof failed.")
    }
}
