package fuookami.ospf.kotlin.core.model.intermediate

import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.*

/** 纯二值逻辑运算。 / Pure binary logic operation. */
enum class BinaryLogicOperation { And, Or, Not, Xor }

/** 纯二值逻辑函数的延迟快照。 / Deferred snapshot for a pure binary logic function. */
data class BinaryLogicStructure<V>(
    val operation: BinaryLogicOperation,
    val inputs: List<AbstractVariableItem<*, *>>,
    val resultVariable: AbstractVariableItem<*, *>,
    val converter: IntoValue<V>,
    val name: String,
    val usage: FunctionUsageSummary = FunctionUsageSummary()
) : DeferredFunctionStructure where V : RealNumber<V>, V : NumberField<V> {
    internal fun materializeFallbackConstraints(): Ret<DeferredFunctionFallbackConstraints> {
        if (inputs.isEmpty() || inputs.any { !it.type.isBinaryType } || !resultVariable.type.isBinaryType ||
            operation == BinaryLogicOperation.Not && inputs.size != 1
        ) return Failed(ErrorCode.IllegalArgument, "二值逻辑结构输入无效。 / Invalid binary logic structure inputs.")
        val zero = converter.zero
        val one = converter.one
        val count = inputs.drop(1).fold(one) { sum, _ -> sum + one }
        val rows = mutableListOf<LinearInequality<V>>()
        fun polynomial(terms: List<LinearMonomial<V>>, constant: V = zero) = LinearPolynomial(terms, constant)
        when (operation) {
            BinaryLogicOperation.And -> {
                inputs.forEachIndexed { index, input -> rows += LinearInequality(
                    polynomial(listOf(LinearMonomial(one, resultVariable), LinearMonomial(-one, input))),
                    polynomial(emptyList()), Comparison.LE, "${name}_and_le_$index"
                ) }
                rows += LinearInequality(
                    polynomial(inputs.map { LinearMonomial(one, it) } + LinearMonomial(-count, resultVariable)),
                    polynomial(emptyList(), one - count), Comparison.GE, "${name}_and_sum"
                )
            }
            BinaryLogicOperation.Or -> {
                inputs.forEachIndexed { index, input -> rows += LinearInequality(
                    polynomial(listOf(LinearMonomial(one, resultVariable), LinearMonomial(-one, input))),
                    polynomial(emptyList()), Comparison.GE, "${name}_or_ge_$index"
                ) }
                rows += LinearInequality(
                    polynomial(inputs.map { LinearMonomial(one, it) } + LinearMonomial(-one, resultVariable)),
                    polynomial(emptyList()), Comparison.LE, "${name}_or_sum"
                )
            }
            BinaryLogicOperation.Not -> rows += LinearInequality(
                polynomial(listOf(LinearMonomial(one, resultVariable), LinearMonomial(one, inputs.single()))),
                polynomial(emptyList(), one), Comparison.EQ, "${name}_not"
            )
            BinaryLogicOperation.Xor -> {
                rows += LinearInequality(
                    polynomial(listOf(LinearMonomial(one, resultVariable)) + inputs.map { LinearMonomial(-one, it) }),
                    polynomial(emptyList()), Comparison.LE, "${name}_xor_sum_ub"
                )
                inputs.forEachIndexed { index, input ->
                    rows += LinearInequality(
                        polynomial(listOf(LinearMonomial(one, resultVariable), LinearMonomial(-one, input)) +
                            inputs.filterIndexed { other, _ -> other != index }.map { LinearMonomial(one, it) }),
                        polynomial(emptyList()), Comparison.GE, "${name}_xor_single_$index"
                    )
                }
                for (first in inputs.indices) for (second in first + 1 until inputs.size) rows += LinearInequality(
                    polynomial(listOf(LinearMonomial(one, resultVariable), LinearMonomial(one, inputs[first]), LinearMonomial(one, inputs[second]))),
                    polynomial(emptyList(), one + one), Comparison.LE, "${name}_xor_pair_${first}_$second"
                )
            }
        }
        return convertPwlConstraintsToFlt64(rows, converter)
    }
}
