/**
 * CP 整数线性表达式 AST。 / Integer-linear CP expression AST.
 */
package fuookami.ospf.kotlin.core.model.constraint_programming

import java.math.BigInteger
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Ret
import fuookami.ospf.kotlin.utils.functional.ok
import fuookami.ospf.kotlin.math.algebra.number.Int64
import fuookami.ospf.kotlin.core.solver.report.VariableId
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem

/**
 * CP 首版整数表达式，只表达常量、整数变量和整数线性组合。 / The first CP expression family, limited to constants, integer variables, and integer-linear combinations.
 */
sealed interface ConstraintProgrammingExpression {
    /** 表达式中使用的稳定变量 ID。 / Stable variable IDs referenced by this expression. */
    val variables: Set<VariableId>

    /**
     * 按稳定变量 ID 求值。 / Evaluate with assignments keyed by stable variable ID.
     *
     * @param values 变量赋值 / Variable assignments
     * @return 精确整数值或结构化错误 / Exact integer value or a structured error
     */
    fun evaluate(values: Map<VariableId, Int64>): Ret<Int64>

    /**
     * 整数常量。 / Integer constant.
     *
     * @property value 常量值 / Constant value
     */
    data class Constant(
        val value: Int64
    ) : ConstraintProgrammingExpression {
        override val variables: Set<VariableId> = emptySet()

        override fun evaluate(values: Map<VariableId, Int64>): Ret<Int64> {
            return ok(value)
        }
    }

    /**
     * 表达式构造溢出或无效时的延迟错误节点。 / Deferred invalid node for expression overflow or invalid construction.
     *
     * @property message 结构化错误消息 / Structured error message
     */
    data class Invalid(
        val message: String
    ) : ConstraintProgrammingExpression {
        override val variables: Set<VariableId> = emptySet()

        override fun evaluate(values: Map<VariableId, Int64>): Ret<Int64> {
            return Failed(ErrorCode.IllegalArgument, message)
        }
    }

    /**
     * 整数变量引用。[domain] 是 CP 建模边界的一部分；构造函数不会偷偷改变源变量的线性模型范围。 / Integer-variable reference. / The [domain] is part of the CP modeling boundary; construction never silently changes the source variable's linear-model range.
     *
     * @property variable 源整数变量 / Source integer variable
     * @property domain CP 值域 / CP domain
     * @property id 模型绑定后的稳定变量 ID；为空时使用 model-local ID / Stable variable ID after model binding; model-local ID when null
     */
    data class Variable(
        val variable: AbstractVariableItem<*, *>,
        val domain: IntegerDomain,
        val id: VariableId? = null
    ) : ConstraintProgrammingExpression {
        /**
         * 使用默认值域构造引用。 / Construct a reference with a default domain.
         *
         * @param variable 源整数变量 / Source integer variable
        */
        constructor(variable: AbstractVariableItem<*, *>) : this(
            variable = variable,
            domain = defaultDomainUnchecked(variable)
        )

        /** 稳定变量 ID。 / Stable variable ID. */
        val variableId: VariableId
            get() = id ?: variableIdOf(variable)

        override val variables: Set<VariableId>
            get() = setOf(variableId)

        override fun evaluate(values: Map<VariableId, Int64>): Ret<Int64> {
            val value = values[variableId] ?: return Failed(
                ErrorCode.DataNotFound,
                "缺少 CP 变量赋值：$variableId / Missing CP variable assignment: $variableId"
            )
            if (value !in domain) {
                return Failed(
                    ErrorCode.IllegalArgument,
                    "CP 变量赋值超出值域：$value not in $domain / " +
                        "CP variable assignment is outside its domain: $value not in $domain"
                )
            }
            return ok(value)
        }
    }

    /**
     * 线性项。 / Linear term.
     *
     * @property variable 源整数变量 / Source integer variable
     * @property coefficient 整数系数 / Integer coefficient
     * @property id 模型绑定后的稳定变量 ID；为空时使用 model-local ID / Stable variable ID after model binding; model-local ID when null
     */
    data class Term(
        val variable: AbstractVariableItem<*, *>,
        val coefficient: Int64,
        val id: VariableId? = null
    ) {
        /** 稳定变量 ID。 / Stable variable ID. */
        val variableId: VariableId
            get() = id ?: variableIdOf(variable)
    }

    /**
     * 整数线性表达式。 / Integer-linear expression.
     *
     * @property terms 线性项 / Linear terms
     * @property constant 常数项 / Constant term
     */
    data class Linear(
        val terms: List<Term>,
        val constant: Int64 = Int64.zero
    ) : ConstraintProgrammingExpression {
        override val variables: Set<VariableId>
            get() = terms.mapTo(linkedSetOf()) { it.variableId }

        override fun evaluate(values: Map<VariableId, Int64>): Ret<Int64> {
            var total = BigInteger.valueOf(constant.toLong())
            for (term in terms) {
                val value = values[term.variableId] ?: return Failed(
                    ErrorCode.DataNotFound,
                    "缺少 CP 变量赋值：${term.variableId} / " +
                        "Missing CP variable assignment: ${term.variableId}"
                )
                total = total
                    .add(
                        BigInteger.valueOf(term.coefficient.toLong())
                            .multiply(BigInteger.valueOf(value.toLong()))
                    )
            }
            return exactInt64(total)
        }
    }

    companion object {
        /**
         * 创建整数常量。 / Create an integer constant.
         *
         * @param value 常量值 / Constant value
         * @return 整数常量表达式 / Integer constant expression
         */
        operator fun invoke(value: Int64): Constant {
            return Constant(value)
        }

        /**
         * 创建 Long 常量。 / Create a Long constant.
         *
         * @param value 常量值 / Constant value
         * @return 整数常量表达式 / Integer constant expression
         */
        @JvmName("constantLong")
        operator fun invoke(value: Long): Constant {
            return Constant(Int64(value))
        }

        /**
         * 创建 Int 常量。 / Create an Int constant.
         *
         * @param value 常量值 / Constant value
         * @return 整数常量表达式 / Integer constant expression
         */
        @JvmName("constantInt")
        operator fun invoke(value: Int): Constant {
            return Constant(Int64(value.toLong()))
        }

        /**
         * 创建并校验整数变量引用。 / Create and validate an integer-variable reference.
         *
         * @param variable 源整数变量 / Source integer variable
         * @param domain 可选 CP 值域 / Optional CP domain
         * @return 变量表达式或结构化错误 / Variable expression or a structured error
         */
        fun variable(
            variable: AbstractVariableItem<*, *>,
            domain: IntegerDomain? = null
        ): Ret<Variable> {
            if (!variable.type.isIntegerType) {
                return Failed(
                    ErrorCode.IllegalArgument,
                    "CP 表达式只支持整数变量：${variable.name} / " +
                        "CP expressions only support integer variables: ${variable.name}"
                )
            }

            val resolvedDomain = domain ?: defaultDomain(variable)
            val domainError = validateDomain(variable, resolvedDomain)
            if (domainError != null) {
                return Failed(ErrorCode.IllegalArgument, domainError)
            }
            return ok(Variable(variable, resolvedDomain))
        }

        /**
         * 创建单个线性项。 / Create a single linear term.
         *
         * @param variable 源整数变量 / Source integer variable
         * @param coefficient 整数系数 / Integer coefficient
         * @return 线性项或结构化错误 / Linear term or a structured error
         */
        fun term(
            variable: AbstractVariableItem<*, *>,
            coefficient: Int64 = Int64.one
        ): Ret<Term> {
            return variable(variable).map { Term(it.variable, coefficient) }
        }

        /**
         * 创建整数线性表达式并合并重复变量。 / Create an integer-linear expression and merge duplicate variables.
         *
         * @param terms 线性项集合 / Linear terms
         * @param constant 常数项 / Constant term
         * @return 线性表达式或结构化错误 / Linear expression or a structured error
         */
        fun linear(
            terms: Iterable<Term>,
            constant: Int64 = Int64.zero
        ): Ret<Linear> {
            val merged = linkedMapOf<VariableId, Pair<AbstractVariableItem<*, *>, BigInteger>>()
            for (term in terms) {
                if (!term.variable.type.isIntegerType) {
                    return Failed(
                        ErrorCode.IllegalArgument,
                        "CP 线性表达式包含非整数变量：${term.variable.name} / " +
                            "CP linear expression contains a non-integer variable: ${term.variable.name}"
                    )
                }
                val id = term.variableId
                val coefficient = BigInteger.valueOf(term.coefficient.toLong())
                val existing = merged[id]
                merged[id] = if (existing == null) {
                    term.variable to coefficient
                } else {
                    existing.first to existing.second.add(coefficient)
                }
            }

            val normalized = ArrayList<Term>(merged.size)
            for ((_, entry) in merged) {
                val coefficient = exactInt64(entry.second)
                if (coefficient.failed) {
                    return propagateFailure(coefficient)
                }
                val coefficientValue = coefficient.value!!
                if (coefficientValue != Int64.zero) {
                    normalized += Term(entry.first, coefficientValue)
                }
            }
            return ok(Linear(normalized, constant))
        }

        /**
         * 使用变量到系数的映射创建线性表达式。 / Create a linear expression from a variable-coefficient map.
         *
         * @param terms 变量到系数的映射 / Variable-to-coefficient map
         * @param constant 常数项 / Constant term
         * @return 线性表达式或结构化错误 / Linear expression or a structured error
         */
        fun linear(
            terms: Map<AbstractVariableItem<*, *>, Int64>,
            constant: Int64 = Int64.zero
        ): Ret<Linear> {
            return linear(terms.map { (variable, coefficient) -> Term(variable, coefficient) }, constant)
        }

        /**
         * 将多个表达式相加。 / Add multiple expressions.
         *
         * @param expressions 待相加的表达式 / Expressions to add
         * @return 线性表达式或结构化错误 / Linear expression or a structured error
         */
        fun sum(expressions: Iterable<ConstraintProgrammingExpression>): Ret<Linear> {
            val terms = ArrayList<Term>()
            var constant = BigInteger.ZERO
            for (expression in expressions) {
                when (expression) {
                    is Constant -> constant = constant.add(BigInteger.valueOf(expression.value.toLong()))
                    is Invalid -> return Failed(ErrorCode.IllegalArgument, expression.message)
                    is Variable -> terms += Term(expression.variable, Int64.one, expression.id)
                    is Linear -> {
                        terms += expression.terms
                        constant = constant.add(BigInteger.valueOf(expression.constant.toLong()))
                    }
                }
            }
            val normalizedConstant = exactInt64(constant)
            if (normalizedConstant.failed) {
                return propagateFailure(normalizedConstant)
            }
            return linear(terms, normalizedConstant.value!!)
        }
    }
}

/**
 * 将两个 CP 表达式相加；溢出以 Ret 失败表示。 / Add two CP expressions, returning overflow as a Ret failure.
 *
 * @param rhs 右侧表达式 / Right-hand expression
 * @return 相加后的表达式或结构化错误 / Sum expression or a structured error
 */
operator fun ConstraintProgrammingExpression.plus(
    rhs: ConstraintProgrammingExpression
): Ret<ConstraintProgrammingExpression.Linear> {
    return ConstraintProgrammingExpression.sum(listOf(this, rhs))
}

/**
 * 将两个 CP 表达式相减。 / Subtract two CP expressions.
 *
 * @param rhs 右侧表达式 / Right-hand expression
 * @return 相减后的表达式或结构化错误 / Difference expression or a structured error
 */
operator fun ConstraintProgrammingExpression.minus(
    rhs: ConstraintProgrammingExpression
): Ret<ConstraintProgrammingExpression.Linear> {
    return ConstraintProgrammingExpression.sum(listOf(this, rhs.negate()))
}

/**
 * 取 CP 表达式相反数。 / Negate a CP expression.
 *
 * @return 相反数表达式 / Negated expression
 */
fun ConstraintProgrammingExpression.negate(): ConstraintProgrammingExpression {
    return when (this) {
        is ConstraintProgrammingExpression.Constant -> negateInt64OrNull(value)?.let {
            ConstraintProgrammingExpression.Constant(it)
        } ?: ConstraintProgrammingExpression.Invalid(
            "CP 整数表达式取负溢出 Int64 / Negating CP integer expression overflows Int64"
        )

        is ConstraintProgrammingExpression.Invalid -> this

        is ConstraintProgrammingExpression.Variable -> ConstraintProgrammingExpression.Linear(
            terms = listOf(ConstraintProgrammingExpression.Term(variable, Int64(-1L), id)),
            constant = Int64.zero
        )

        is ConstraintProgrammingExpression.Linear -> {
            val negatedConstant = negateInt64OrNull(constant)
            val negatedTerms = terms.map { term ->
                negateInt64OrNull(term.coefficient)?.let {
                    ConstraintProgrammingExpression.Term(term.variable, it, term.id)
                }
            }
            if (negatedConstant == null || negatedTerms.any { it == null }) {
                ConstraintProgrammingExpression.Invalid(
                    "CP 整数表达式取负溢出 Int64 / Negating CP integer expression overflows Int64"
                )
            } else {
                ConstraintProgrammingExpression.Linear(
                    terms = negatedTerms.filterNotNull(),
                    constant = negatedConstant
                )
            }
        }
    }
}

/** 线性项别名。 / Linear-term alias. */
typealias ConstraintProgrammingLinearTerm = ConstraintProgrammingExpression.Term

private fun variableIdOf(variable: AbstractVariableItem<*, *>): VariableId {
    return VariableId("${variable.identifier}:${variable.index}")
}

private fun defaultDomain(variable: AbstractVariableItem<*, *>): IntegerDomain {
    return when {
        variable.type.isBinaryType -> IntegerDomain.boolean
        variable.type.isUnsignedType -> IntegerDomain.Interval(Int64.zero, Int64.maximum)
        else -> IntegerDomain.Interval(Int64.minimum, Int64.maximum)
    }
}

private fun defaultDomainUnchecked(variable: AbstractVariableItem<*, *>): IntegerDomain {
    return when {
        variable.type.isBinaryType -> IntegerDomain.boolean
        variable.type.isUnsignedType -> IntegerDomain.Interval(Int64.zero, Int64.maximum)
        else -> IntegerDomain.Interval(Int64.minimum, Int64.maximum)
    }
}

private fun validateDomain(
    variable: AbstractVariableItem<*, *>,
    domain: IntegerDomain
): String? {
    if (domain.cardinality == 0UL) {
        return "CP 变量值域不能为空 / CP variable domain must not be empty"
    }
    if (variable.type.isBinaryType &&
        (domain.lowerBound < Int64.zero || domain.upperBound > Int64.one)
    ) {
        return "二值变量值域必须位于 [0, 1] / Binary variable domain must be within [0, 1]"
    }
    if (variable.type.isUnsignedType && domain.lowerBound < Int64.zero) {
        return "无符号变量值域下界不得为负 / Unsigned variable domain cannot have a negative lower bound"
    }
    return null
}

private fun exactInt64(value: BigInteger): Ret<Int64> {
    val minimum = BigInteger.valueOf(Long.MIN_VALUE)
    val maximum = BigInteger.valueOf(Long.MAX_VALUE)
    if (value < minimum || value > maximum) {
        return Failed(
            ErrorCode.IllegalArgument,
            "CP 整数表达式溢出 Int64 / CP integer expression overflows Int64"
        )
    }
    return ok(Int64(value.toLong()))
}

private fun negateInt64OrNull(value: Int64): Int64? {
    return if (value == Int64.minimum) {
        null
    } else {
        Int64(-value.toLong())
    }
}

private fun <T> propagateFailure(result: Ret<*>): Ret<T> {
    return when (result) {
        is Failed -> Failed(result.error)
        is Fatal -> Fatal(result.errors)
        else -> Failed(
            ErrorCode.ApplicationError,
            "CP 表达式结果状态无效 / Invalid CP expression result state"
        )
    }
}
