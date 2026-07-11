package fuookami.ospf.kotlin.framework.model

import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.core.model.basic.ObjectCategory
import fuookami.ospf.kotlin.core.model.mechanism.LinearMetaModel

/** 线性系数类型 / Linear coefficient type */
typealias LinearCoefficient = LinearPolynomial<Flt64>

/** 常数类型 / Constant type */
typealias Constant = Flt64

/** 约束符号类型 / Constraint sign type */
typealias ConstraintSign = Comparison

/** 目标类型 / Objective type */
typealias ObjectiveType = ObjectCategory

/** 类型化模型类型 / Typed model type */
typealias TypedModel = LinearMetaModel<Flt64>

/**
 * 带类型的约束表示 / Typed constraint representation
 * @property coefficient 线性系数 / Linear coefficient
 * @property sign 约束符号 / Constraint sign
 * @property rhs 右侧常数 / Right-hand side constant
*/
data class TypedConstraint(
    val coefficient: LinearCoefficient,
    val sign: ConstraintSign,
    val rhs: Constant
)

/**
 * 带类型的目标表示 / Typed objective representation
 * @property coefficient 线性系数 / Linear coefficient
 * @property type 目标类型 / Objective type
*/
data class TypedObjective(
    val coefficient: LinearCoefficient,
    val type: ObjectiveType
)

/**
 * 带类型的表达式表示 / Typed expression representation
 * @property coefficient 线性系数 / Linear coefficient
*/
data class TypedExpression(
    val coefficient: LinearCoefficient
)

/**
 * 带类型的项表示 / Typed term representation
 * @property coefficient 线性系数 / Linear coefficient
*/
data class TypedTerm(
    val coefficient: LinearCoefficient
)

/**
 * 类型化模型构建器的抽象基类 / Abstract base class for typed model builders
 * @param T 模型类型参数 / Model type parameter
 * @param R 构建结果类型 / Build result type
*/
abstract class TypedModelBuilder<T, R> {

    /**
     * 获取或创建模型 / Get or create the model
     * @param block 用于构建模型的可选闭包 / Optional closure for building the model
     * @return 模型实例 / Model instance
    */
    fun model(block: (TypedModel.() -> Unit)? = null): TypedModel {
        val model = LinearMetaModel()
        if (block != null) {
            model.block()
        }
        return model
    }

    /**
     * 创建带类型的约束 / Create a typed constraint
     * @param left 约束左侧 / Left-hand side of the constraint
     * @param sign 约束符号 / Constraint sign
     * @param right 约束右侧 / Right-hand side of the constraint
     * @return 带类型的约束 / Typed constraint
    */
    fun constraint(
        left: TypedExpression,
        sign: ConstraintSign,
        right: TypedExpression
    ): TypedConstraint {
        return TypedConstraint(left.coefficient - right.coefficient, sign, Flt64.zero)
    }

    /**
     * 创建带类型的约束 / Create a typed constraint
     * @param left 约束左侧 / Left-hand side of the constraint
     * @param sign 约束符号 / Constraint sign
     * @param right 约束右侧常数 / Right-hand side constant of the constraint
     * @return 带类型的约束 / Typed constraint
    */
    fun constraint(
        left: TypedExpression,
        sign: ConstraintSign,
        right: Constant
    ): TypedConstraint {
        return TypedConstraint(left.coefficient, sign, right)
    }

    /**
     * 创建带类型的目标 / Create a typed objective
     * @param coefficient 线性系数 / Linear coefficient
     * @param type 目标类型 / Objective type
     * @return 带类型的目标 / Typed objective
    */
    fun objective(
        coefficient: LinearCoefficient,
        type: ObjectiveType
    ): TypedObjective {
        return TypedObjective(coefficient, type)
    }

    /**
     * 创建带类型的表达式 / Create a typed expression
     * @param coefficient 线性系数 / Linear coefficient
     * @return 带类型的表达式 / Typed expression
    */
    fun expr(coefficient: LinearCoefficient): TypedExpression {
        return TypedExpression(coefficient)
    }

    /**
     * 创建带类型的权重项 / Create a typed weight term
     * @param coefficient 线性系数 / Linear coefficient
     * @return 带类型的项 / Typed term
    */
    fun weight(coefficient: LinearCoefficient): TypedTerm {
        return TypedTerm(coefficient)
    }

    /**
     * 从可变参数创建带类型的权重项列表 / Create a list of typed weight terms from varargs
     * @param coefficient 线性系数可变参数 / Varargs of linear coefficients
     * @return 带类型的项列表 / List of typed terms
    */
    fun weights(vararg coefficient: LinearCoefficient): List<TypedTerm> {
        return coefficient.map { TypedTerm(it) }
    }

    /**
     * 使用给定模型执行操作 / Perform an operation with the given model
     * @param model 模型实例 / Model instance
     * @param block 操作闭包 / Operation closure
     * @return 操作结果 / Operation result
    */
    fun withModel(model: TypedModel, block: T.() -> Unit): R {
        return this.build(model, block)
    }

    /**
     * 构建模型 / Build the model
     * @param model 模型实例 / Model instance
     * @param block 构建闭包 / Build closure
     * @return 构建结果 / Build result
    */
    abstract fun build(model: TypedModel, block: T.() -> Unit): R
}
