@file:Suppress("unused")

/** 相等函数符号 / Same-as equality function symbol */
package fuookami.ospf.kotlin.core.symbol.function

import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMechanismModel
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.token.AddableTokenCollection
import fuookami.ospf.kotlin.core.variable.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.utils.functional.*

/**
 * 相同满足函数符号 / Same-as function symbol
 *
 * 提供 [SameAsFunction]，当所有不等式满足状态一致时返回 1。
 *
 * Provides [SameAsFunction] that returns 1 when all inequalities have the same satisfaction status.
 */

/**
 * 相等函数符号：当所有不等式具有相同的满足状态时返回 1（全满足或全不满足），否则返回 0。
 * SameAs function symbol: returns 1 if all inequalities have the same satisfaction status
 * (all true or all false), returns 0 otherwise.
 *
 * 约束模式：
 * Constraint pattern:
 * - 每个输入不等式获得二值标志 `u[i]`（满足时为 1，不满足时为 0）
 * - Each input inequality gets a binary flag `u[i]` (1 if satisfied, 0 if not)
 * - BigM 约束将每个标志链接到其不等式
 * - BigM constraints link each flag to its inequality
 * - 约束模式下：所有标志被强制相等（全满足或全不满足）
 * - In constraint mode: all flags are forced equal (all satisfied or all unsatisfied)
 * - 度量模式下：y 度量是否全部相同
 * - In measurement mode: y measures whether all are the same
 *
 * @property inequalities 要比较的线性不等式列表 / list of linear inequalities to compare
 * @property constraint 若为 true，强制所有不等式具有相同满足状态；若为 false，仅度量 / if true, force all inequalities to have same satisfaction; if false, measure only
 * @property epsilon 松弛不等式的最小间隙（默认 1e-6）/ minimum gap for relaxed inequality (default 1e-6)
 * @property m 指示约束的 Big-M 常量（默认 1e6）/ Big-M constant for indicator constraints (default 1e6)
 * @property converter 值类型转换器 / value type converter
 * @property name 此函数的唯一名称 / unique name for this function
 * @property displayName 可选的人类可读显示名称 / optional human-readable display name
 */
class SameAsFunction<V>(
    val inequalities: List<LinearInequality<V>>,
    val constraint: Boolean = true,
    val epsilon: V,
    val m: V,
    private val converter: IntoValue<V>,
    override var name: String,
    override var displayName: String? = null
) : MathFunctionSymbol<V> where V : RealNumber<V>, V : NumberField<V> {

    init {
        require(inequalities.isNotEmpty()) { "SameAsFunction requires at least one inequality" }
    }

    private val n = inequalities.size

    // Binary flag per inequality: u[i] = 1 if inequality i is satisfied / 每个不等式的二值标志：不等式 i 满足时 u[i] = 1
    val satisfactionFlags: List<AbstractVariableItem<*, *>> =
        (0 until n).map { BinVar("${name}_u${it}") }

    // Output binary: y = 1 if all same, 0 otherwise / 输出二值变量：全部相同时 y = 1，否则为 0
    val resultVar: AbstractVariableItem<*, *> = BinVar("${name}_same")

    // Diff variables for measurement mode (XOR between adjacent satisfaction flags) / 度量模式的差异变量（相邻满足标志之间的 XOR）
    private val diffVars: List<AbstractVariableItem<*, *>> =
        if (!constraint && n > 1) (1 until n).map { BinVar("${name}_diff${it}") } else emptyList()

    override val helperVariables: List<AbstractVariableItem<*, *>>
        get() = listOf(resultVar) + satisfactionFlags + diffVars

    override fun evaluate(values: Map<Symbol, V>): V? {
        val flags = mutableListOf<Boolean>()
        for (ineq in inequalities) {
            val lhsVal = ineq.lhs.evaluateWith(values) ?: return null
            val rhsVal = ineq.rhs.evaluateWith(values) ?: return null
            val diff = lhsVal - rhsVal
            val satisfied = when (ineq.comparison) {
                Comparison.LE -> !(diff gr epsilon)
                Comparison.LT -> diff ls epsilon
                Comparison.GE -> !(diff ls -epsilon)
                Comparison.GT -> diff gr -epsilon
                Comparison.EQ -> diff.abs() ls epsilon || diff.abs() eq epsilon
                Comparison.NE -> diff.abs() gr epsilon
            }
            flags += satisfied
        }
        val allSame = flags.all { it == flags[0] }
        return if (allSame) converter.one else converter.zero
    }

    override fun registerAuxiliaryTokens(tokens: AddableTokenCollection<V>): Try {
        return when (val result = tokens.add(helperVariables)) {
            is Ok -> ok
            is Failed -> Failed(result.error)
            is Fatal -> Fatal(result.errors)
        }
    }

    override fun registerConstraints(model: AbstractLinearMechanismModel<V>): Try {
        val zero = converter.zero
        val one = converter.one
        val two = converter.intoValue(Flt64(2.0))
        val allConstraints = mutableListOf<LinearInequality<V>>()

        // Register each inequality with its satisfaction flag using simple indicator constraints
        // 使用简单指示约束为每个不等式注册其满足标志
        for (i in inequalities.indices) {
            allConstraints += simpleIndicatorConstraints(
                inequalities[i], satisfactionFlags[i], m, epsilon, epsilon, "${name}_ineq_${i}")
        }

        // Link constraints: enforce all satisfaction flags are equal / 链接约束：强制所有满足标志相等
        if (constraint) {
            // For constraint mode: force u[0] == u[1] == ... == u[n-1]
            // 约束模式：强制 u[0] == u[1] == ... == u[n-1]
            // This is done by: u[0] - u[i] == 0 for i=1..n-1
            // 通过 u[0] - u[i] == 0（i=1..n-1）实现
            for (i in 1 until n) {
                val eqLhs = LinearPolynomial(
                    listOf(
                        LinearMonomial(one, satisfactionFlags[0]),
                        LinearMonomial(-one, satisfactionFlags[i])
                    ),
                    zero
                )
                val eqRhs = LinearPolynomial(emptyList(), zero)
                allConstraints += LinearInequality(
                    eqLhs, eqRhs, Comparison.EQ, "${name}_equal_${i}")
            }
            // result = u[0] (since all are equal) / 结果 = u[0]（因为所有标志相等）
            val resultLink = LinearPolynomial(
                listOf(
                    LinearMonomial(one, resultVar),
                    LinearMonomial(-one, satisfactionFlags[0])
                ),
                zero
            )
            allConstraints += LinearInequality(
                resultLink, LinearPolynomial(emptyList(), zero),
                Comparison.EQ, "${name}_result_link")
        } else {
            // Measurement mode: y = 1 iff all u[i] are equal / 度量模式：当且仅当所有 u[i] 相等时 y = 1
            if (n == 1) {
                // Single inequality: always "same" with itself / 单个不等式：始终与自身"相同"
                allConstraints += LinearInequality(
                    LinearPolynomial(listOf(LinearMonomial(one, resultVar)), zero),
                    LinearPolynomial(emptyList(), one), Comparison.EQ, "${name}_result_single")
            } else {
                for (i in 1 until n) {
                    val u0 = satisfactionFlags[0]
                    val ui = satisfactionFlags[i]
                    val diffVar = diffVars[i - 1]

                    // diff >= u[i] - u[0]  =>  diff - u[i] + u[0] >= 0 / diff >= u[i] - u[0]，即 diff - u[i] + u[0] >= 0
                    allConstraints += LinearInequality(
                        LinearPolynomial(
                            listOf(
                                LinearMonomial(one, diffVar),
                                LinearMonomial(-one, ui),
                                LinearMonomial(one, u0)
                            ),
                            zero
                        ),
                        LinearPolynomial(emptyList(), zero),
                        Comparison.GE, "${name}_diff_ge_${i}")

                    // diff >= u[0] - u[i]  =>  diff - u[0] + u[i] >= 0 / diff >= u[0] - u[i]，即 diff - u[0] + u[i] >= 0
                    allConstraints += LinearInequality(
                        LinearPolynomial(
                            listOf(
                                LinearMonomial(one, diffVar),
                                LinearMonomial(one, ui),
                                LinearMonomial(-one, u0)
                            ),
                            zero
                        ),
                        LinearPolynomial(emptyList(), zero),
                        Comparison.GE, "${name}_diff_le_${i}")

                    // diff <= u[i] + u[0]  =>  diff - u[i] - u[0] <= 0 / diff <= u[i] + u[0]，即 diff - u[i] - u[0] <= 0
                    allConstraints += LinearInequality(
                        LinearPolynomial(
                            listOf(
                                LinearMonomial(one, diffVar),
                                LinearMonomial(-one, ui),
                                LinearMonomial(-one, u0)
                            ),
                            zero
                        ),
                        LinearPolynomial(emptyList(), zero),
                        Comparison.LE, "${name}_diff_sum_ub_${i}")

                    // diff <= 2 - u[i] - u[0]  =>  diff + u[i] + u[0] <= 2 / diff <= 2 - u[i] - u[0]，即 diff + u[i] + u[0] <= 2
                    allConstraints += LinearInequality(
                        LinearPolynomial(
                            listOf(
                                LinearMonomial(one, diffVar),
                                LinearMonomial(one, ui),
                                LinearMonomial(one, u0)
                            ),
                            zero
                        ),
                        LinearPolynomial(emptyList(), two),
                        Comparison.LE, "${name}_diff_sum_lb_${i}")
                }

                // y = 1 - sum(diff_i)  =>  y + sum(diff_i) = 1 / y = 1 - sum(diff_i)，即 y + sum(diff_i) = 1
                val yPlusSumMonos = listOf(LinearMonomial(one, resultVar)) +
                    diffVars.map { LinearMonomial(one, it) }
                allConstraints += LinearInequality(
                    LinearPolynomial(yPlusSumMonos, zero),
                    LinearPolynomial(emptyList(), one),
                    Comparison.EQ, "${name}_result_sum")
            }
        }

        return addConstraints(model, allConstraints) ?: ok
    }
    companion object {
        /** 创建 [SameAsFunction] 实例。 / Create a [SameAsFunction] instance. */
        operator fun <V> invoke(
            inequalities: List<LinearInequality<V>>,
            constraint: Boolean = true,
            epsilon: V,
            m: V,
            converter: IntoValue<V>,
            name: String,
            displayName: String? = null
        ): SameAsFunction<V> where V : RealNumber<V>, V : NumberField<V> = SameAsFunction(
            inequalities = inequalities,
            constraint = constraint,
            epsilon = epsilon,
            m = m,
            converter = converter,
            name = name,
            displayName = displayName
        )
    }
}
