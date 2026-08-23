/**
 * CP 布尔字面量。 / Boolean literals for constraint programming.
 */
package fuookami.ospf.kotlin.core.model.constraint_programming

import fuookami.ospf.kotlin.core.solver.report.VariableId
import fuookami.ospf.kotlin.core.variable.BinVariable
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Ret
import fuookami.ospf.kotlin.utils.functional.ok

/**
 * 二值变量的正文字面量、否定文字面量或布尔常量。 / A positive/negated binary-variable literal or a Boolean constant.
 */
sealed interface BooleanLiteral {
    /** 关联的二值变量；常量没有变量。 / Associated binary variable, or null for a constant. */
    val variable: BinVariable?

    /** 是否为否定文字。 / Whether this is a negated literal. */
    val negated: Boolean

    /** 常量值；变量文字返回 null。 / Constant value, or null for a variable literal. */
    val constant: Boolean?

    /** 是否为常量。 / Whether this literal is a constant. */
    val isConstant: Boolean
        get() = constant != null

    /** 是否为正文字。 / Whether this is a positive literal. */
    val positive: Boolean
        get() = !negated

    /** 变量稳定标识；常量返回 null。 / Stable variable ID, or null for a constant. */
    val variableId: VariableId?
        get() = variable?.let {
            VariableId("${it.identifier}:${it.index}")
        }

    /**
     * 返回该文字的逻辑否定。 / Return the logical negation of this literal.
     *
     * @return 否定后的文字 / The negated literal
     */
    fun negate(): BooleanLiteral

    /**
     * 变量文字。 / Variable literal.
     *
     * @property variable 关联的二值变量 / Associated binary variable
     * @property negated 是否取反 / Whether the literal is negated
     * @property id 模型绑定后的稳定变量 ID；为空时使用 model-local ID / Stable variable ID after model binding; model-local ID when null
     */
    data class Variable(
        override val variable: BinVariable,
        override val negated: Boolean = false,
        val id: VariableId? = null
    ) : BooleanLiteral {
        override val variableId: VariableId
            get() = id ?: VariableId("${variable.identifier}:${variable.index}")

        override val constant: Boolean? get() = null

        override fun negate(): BooleanLiteral {
            return copy(negated = !negated)
        }
    }

    /**
     * 布尔常量。 / Boolean constant.
     *
     * @property constant 常量值 / Constant value
     */
    data class Constant(
        override val constant: Boolean
    ) : BooleanLiteral {
        override val variable: BinVariable? get() = null
        override val negated: Boolean get() = !constant

        override fun negate(): BooleanLiteral {
            return Constant(!constant)
        }
    }

    companion object {
        /**
         * 构造变量正/负文字。 / Construct a positive or negated variable literal.
         *
         * @param variable 二值变量 / Binary variable
         * @param negated 是否取反 / Whether the literal is negated
         * @param id 稳定变量 ID；为空时使用 model-local ID / Stable variable ID; model-local ID when null
         * @return 变量文字 / The variable literal
         */
        operator fun invoke(
            variable: BinVariable,
            negated: Boolean = false,
            id: VariableId? = null
        ): BooleanLiteral {
            return Variable(variable, negated, id)
        }

        /**
         * 构造布尔常量文字。 / Construct a Boolean constant literal.
         *
         * @param value 常量值 / Constant value
         * @return 布尔常量文字 / The Boolean constant literal
         */
        operator fun invoke(value: Boolean): BooleanLiteral {
            return Constant(value)
        }

        /** 真常量。 / True constant. */
        val True: BooleanLiteral = Constant(true)

        /** 假常量。 / False constant. */
        val False: BooleanLiteral = Constant(false)

        /** 真常量的函数式别名。 / Functional alias for the true constant. */
        val trueLiteral: BooleanLiteral get() = True

        /** 假常量的函数式别名。 / Functional alias for the false constant. */
        val falseLiteral: BooleanLiteral get() = False
    }
}
/**
 * 对布尔文字取逻辑非。 / Apply logical negation to a Boolean literal.
 *
 * @return 否定后的文字 / The negated literal
 */
operator fun BooleanLiteral.not(): BooleanLiteral {
    return negate()
}
/** 将二值变量包装为正文字。 / Wrap a binary variable as a positive literal. */
val BinVariable.literal: BooleanLiteral
    get() = BooleanLiteral.Variable(this)

/** 将二值变量包装为否定文字。 / Wrap a binary variable as a negated literal. */
val BinVariable.negatedLiteral: BooleanLiteral
    get() = BooleanLiteral.Variable(this, negated = true)

/**
 * 使用布尔赋值求值。 / Evaluate this literal against Boolean assignments.
 *
 * @param values 按稳定变量 ID 编索引的赋值 / Assignments indexed by stable variable ID
 * @return 求值结果或缺少赋值错误 / The value or a missing-assignment error
 */
fun BooleanLiteral.evaluate(values: Map<VariableId, Boolean>): Ret<Boolean> {
    val fixed = constant
    if (fixed != null) {
        return ok(fixed)
    }

    val id = variableId ?: return Failed(
        ErrorCode.IllegalArgument,
        "变量文字缺少变量标识 / Variable literal has no variable identifier"
    )
    val value = values[id] ?: return Failed(
        ErrorCode.DataNotFound,
        "缺少布尔变量赋值：$id / Missing Boolean variable assignment: $id"
    )
    return ok(if (negated) !value else value)
}
