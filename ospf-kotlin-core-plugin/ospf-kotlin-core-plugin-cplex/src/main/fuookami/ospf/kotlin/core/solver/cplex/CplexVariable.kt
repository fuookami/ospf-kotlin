@file:Suppress("MemberVisibilityCanBePrivate")

package fuookami.ospf.kotlin.core.solver.cplex

import ilog.concert.*
import ilog.cplex.*
import fuookami.ospf.kotlin.core.model.*

/** CPLEX 变量的包装类型 / Wrapper type for CPLEX variable */
data class Variable(
    /** CPLEX 数值变量的引用 / Reference to the CPLEX numeric variable */
    val reference: IloNumVar,
    /** 变量委托 / Variable delegate */
    val delegate: VariableDelegate
)

/**
 * CPLEX 变量的委托类型，提供变量名称获取功能 / Delegate type for CPLEX variable, providing variable name retrieval.
 * @property variable CPLEX 数值变量 / The CPLEX numeric variable
 */
class VariableDelegate(private val variable: IloNumVar) {
    /**
     * 获取变量名称 / Get the variable name.
     * @return 变量的名称字符串 / The name of the variable
     */
    fun name(): String {
        return variable.name
    }
}

/**
 * 将 [IloNumVar] 转换为 [Variable] / Convert [IloNumVar] to [Variable].
 * @param model 当前 CPLEX 模型实例 / The current CPLEX model instance
 * @param sign 约束符号，用于确定变量下界 / The constraint sign, used to determine the variable lower bound
 * @return 转换后的 [Variable] 实例 / The converted [Variable] instance
 */
fun IloNumVar.toVariable(model: IloCplex, sign: ConstraintSign? = null): Variable {
    return Variable(
        reference = this,
        delegate = VariableDelegate(this)
    )
}

/**
 * 将 [IloNumVar] 转换为 [IloNumVarRef] / Convert [IloNumVar] to [IloNumVarRef].
 * @param model 当前 CPLEX 模型实例 / The current CPLEX model instance
 * @return 转换后的 [IloNumVarRef] 实例 / The converted [IloNumVarRef] instance
 */
fun IloNumVar.toNumVarRef(model: IloCplex): IloNumVar {
    return model.numVar(model.columnArray(this))
}

/**
 * 获取所有变量的取值 / Get the values of all variables.
 * @return 所有变量的取值数组 / The array of all variable values
 */
val IloCplex.values: DoubleArray
    get() {
        val values = this.getValues()
        return values
    }

/**
 * 获取指定变量的取值 / Get the value of a specific variable.
 * @param variable 目标变量 / The target variable
 * @return 变量的取值 / The value of the variable
 */
fun IloCplex.values(variable: IloNumVar): Double {
    return this.getValue(variable)
}

/**
 * 获取指定变量的缩减成本 / Get the reduced cost of a specific variable.
 * @param variable 目标变量 / The target variable
 * @return 变量的缩减成本 / The reduced cost of the variable
 */
fun IloCplex.reducedCost(variable: IloNumVar): Double {
    return this.getReducedCost(variable)
}

/**
 * 获取所有变量的缩减成本 / Get the reduced costs of all variables.
 * @return 所有变量的缩减成本数组 / The array of all variable reduced costs
 */
fun IloCplex.reducedCosts(): DoubleArray {
    val values = this.getReducedCosts()
    return values
}

/**
 * 获取指定变量的缩减成本（`reducedCost` 的简写） / Get the reduced cost of a specific variable (shorthand for [reducedCost]).
 * @param variable 目标变量 / The target variable
 * @return 变量的缩减成本 / The reduced cost of the variable
 */
fun IloCplex.rc(variable: IloNumVar): Double {
    return this.getReducedCost(variable)
}
