package fuookami.ospf.kotlin.core.model.intermediate

import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.math.symbol.inequality.LinearInequality
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.symbol.function.hasUsableConditionFlattenedValues
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem

/**
 * 由现有二值 mask 门控的数值结果。 / Numeric result gated by an existing binary mask.
 * @property value 条件数值关系及原范围 / Conditional value and original bounds
 * @property mask 保留的输入 mask，可由多个结构共享 / Retained input mask, potentially shared
 * @property converter 数值转换器 / Value converter
 * @property usage 使用语境 / Usage context
 * @property maskDefinition 可选的 mask 定义，强制 mask 等于该表达式 / Optional definition enforcing mask equality to this expression
 */
data class MaskingStructure<V>(
    val value: ConditionalValue<V>,
    val mask: AbstractVariableItem<*, *>,
    val converter: IntoValue<V>,
    val usage: FunctionUsageSummary = FunctionUsageSummary(),
    val maskDefinition: LinearPolynomial<V>? = null
) : DeferredFunctionStructure where V : RealNumber<V>, V : NumberField<V> {
    val resultVariable: AbstractVariableItem<*, *> get() = value.resultVariable
    /** 本结构定义并必须保留的公开列。 / Public columns defined and retained by this structure. */
    val retainedResultVariables: List<AbstractVariableItem<*, *>>
        get() = listOf(resultVariable) + if (maskDefinition != null) listOf(mask) else emptyList()
    val maskPolynomial: LinearPolynomial<V>
        get() = LinearPolynomial(listOf(LinearMonomial(converter.one, mask)), converter.zero)

    internal fun generateConstraints(): Ret<List<LinearInequality<V>>> {
        return try {
            val constraints = when (val generated = value.generateConstraints(mask, converter)) {
                is Ok -> generated.value
                is Failed -> return Failed(generated.error)
                is Fatal -> return Fatal(generated.errors)
            }
            val definition = maskDefinition ?: return Ok(constraints)
            val difference = LinearPolynomial(
                monomials = definition.monomials.map { LinearMonomial(-it.coefficient, it.symbol) } + LinearMonomial(converter.one, mask),
                constant = -definition.constant
            )
            if (!hasUsableConditionFlattenedValues(difference, converter)) {
                return Failed(ErrorCode.IllegalArgument, "mask 定义数值无效。 / Invalid mask definition values.")
            }
            Ok(listOf(LinearInequality(
                lhs = difference,
                rhs = LinearPolynomial(emptyList(), converter.zero),
                comparison = Comparison.EQ,
                name = "${value.name}_mask_eq"
            )) + constraints)
        } catch (_: RuntimeException) {
            Failed(ErrorCode.IllegalArgument, "生成 mask 定义失败。 / Failed to generate mask definition.")
        }
    }

    internal fun materializeFallbackConstraints(): Ret<DeferredFunctionFallbackConstraints> {
        return when (val constraints = generateConstraints()) {
            is Ok -> convertPwlConstraintsToFlt64(constraints.value, converter)
            is Failed -> Failed(constraints.error)
            is Fatal -> Fatal(constraints.errors)
        }
    }
}
