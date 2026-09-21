package fuookami.ospf.kotlin.core.solver.gurobi

import gurobi.*
import fuookami.ospf.kotlin.core.model.intermediate.FunctionNativeCapability
import fuookami.ospf.kotlin.core.model.intermediate.FunctionSolverCapabilities

/**
 * Detect function APIs exposed by the linked Gurobi Java SDK. /
 * 检测当前链接的 Gurobi Java SDK 暴露的函数接口。
 *
 * This describes SDK capability only; the regular matrix dump remains the
 * fallback until a native lowering transaction is installed. /
 * 此处只描述 SDK 能力；在原生 lowering 事务接入前，常规矩阵转储仍是 fallback。
 *
 * @param modelClass 待检查的 SDK 模型类型 / SDK model class to inspect
 * @return 精确方法签名所支持的能力 / Capabilities supported by exact method signatures
 */
fun gurobiFunctionSolverCapabilities(
    modelClass: Class<*> = GRBModel::class.java
): FunctionSolverCapabilities {
    fun has(name: String, returnType: Class<*>, vararg parameters: Class<*>): Boolean {
        return try {
            modelClass.getMethod(name, *parameters).returnType == returnType
        } catch (_: ReflectiveOperationException) {
            false
        } catch (_: LinkageError) {
            false
        }
    }

    val variable = GRBVar::class.java
    val variables = Array<GRBVar>::class.java
    val doubles = DoubleArray::class.java
    val text = String::class.java
    val number = java.lang.Double.TYPE
    val integer = java.lang.Integer.TYPE
    val generalConstraint = GRBGenConstr::class.java
    val supported = buildSet {
        add(FunctionNativeCapability.SemiContinuous)
        if (listOf("addGenConstrAnd", "addGenConstrOr").all { method ->
                has(
                    name = method,
                    returnType = generalConstraint,
                    parameters = *arrayOf(variable, variables, text)
                )
            }
        ) add(FunctionNativeCapability.BinaryLogic)
        if (has(
            name = "setPWLObj",
            returnType = java.lang.Void.TYPE,
            parameters = arrayOf(variable, doubles, doubles)
        )) add(FunctionNativeCapability.PiecewiseLinearObjective)
        if (has(
            name = "addGenConstrPWL",
            returnType = generalConstraint,
            parameters = arrayOf(variable, variable, doubles, doubles, text)
        )) add(FunctionNativeCapability.PiecewiseLinearConstraint)
        if (has(
            name = "addSOS",
            returnType = GRBSOS::class.java,
            parameters = arrayOf(variables, doubles, integer)
        )) {
            add(FunctionNativeCapability.SOS1)
            add(FunctionNativeCapability.SOS2)
        }
        if (has(
            name = "addGenConstrIndicator",
            returnType = generalConstraint,
            parameters = arrayOf(variable, integer, GRBLinExpr::class.java, java.lang.Character.TYPE, number, text)
        )) add(FunctionNativeCapability.Indicator)
        if (has(
            name = "addGenConstrAbs",
            returnType = generalConstraint,
            parameters = arrayOf(variable, variable, text)
        )) add(FunctionNativeCapability.GeneralAbs)
        if (listOf("addGenConstrMax", "addGenConstrMin").all { method ->
            has(
                name = method,
                returnType = generalConstraint,
                parameters = arrayOf(variable, variables, number, text)
            )
        }) {
            add(FunctionNativeCapability.GeneralMinMax)
        }
    }
    return FunctionSolverCapabilities(
        solver = "gurobi",
        version = gurobiNativeVersion(),
        supported = supported
    )
}
