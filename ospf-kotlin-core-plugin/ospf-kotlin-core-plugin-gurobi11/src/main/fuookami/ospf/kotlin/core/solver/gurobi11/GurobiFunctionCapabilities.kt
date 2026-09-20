package fuookami.ospf.kotlin.core.solver.gurobi11

import com.gurobi.gurobi.*
import fuookami.ospf.kotlin.core.model.intermediate.FunctionNativeCapability
import fuookami.ospf.kotlin.core.model.intermediate.FunctionSolverCapabilities

/** 解析 Gurobi 11 原生版本，避免暴露环境或许可证信息。 / Resolve the Gurobi 11 native version without exposing environment or license details. */
internal fun gurobi11NativeVersion(): String? {
    return runCatching {
        val major = GRB::class.java.getField("VERSION_MAJOR").getInt(null)
        val minor = GRB::class.java.getField("VERSION_MINOR").getInt(null)
        val technical = GRB::class.java.getField("VERSION_TECHNICAL").getInt(null)
        "$major.$minor.$technical"
    }.getOrNull()
}

/**
 * 检测当前链接的 Gurobi 11 Java SDK 精确函数签名。 / Detect exact function signatures exposed by the linked Gurobi 11 Java SDK.
 *
 * @param modelClass 待检查的 SDK 模型类型 / SDK model class to inspect
 * @return 已确认支持的函数能力 / Confirmed native function capabilities
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
        if (has(
                name = "setPWLObj",
                returnType = java.lang.Void.TYPE,
                parameters = arrayOf(variable, doubles, doubles)
            )) {
            add(FunctionNativeCapability.PiecewiseLinearObjective)
        }
        if (has(
                name = "addGenConstrPWL",
                returnType = generalConstraint,
                parameters = arrayOf(variable, variable, doubles, doubles, text)
            )) {
            add(FunctionNativeCapability.PiecewiseLinearConstraint)
        }
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
            )) {
            add(FunctionNativeCapability.Indicator)
        }
        if (has(
                name = "addGenConstrAbs",
                returnType = generalConstraint,
                parameters = arrayOf(variable, variable, text)
            )) {
            add(FunctionNativeCapability.GeneralAbs)
        }
        if (listOf("addGenConstrMax", "addGenConstrMin").all { method ->
                has(
                    name = method,
                    returnType = generalConstraint,
                    parameters = arrayOf(variable, variables, number, text)
                )
            }) {
            add(FunctionNativeCapability.GeneralMinMax)
        }
        if (runCatching { GRB::class.java.getField("SEMICONT") }.isSuccess) {
        add(FunctionNativeCapability.SemiContinuous)
        if (listOf("addGenConstrAnd", "addGenConstrOr").all { method ->
                has(method, generalConstraint, variable, variables, text)
            }
        ) add(FunctionNativeCapability.BinaryLogic)
        }
    }
    return FunctionSolverCapabilities(
        solver = "gurobi11",
        version = gurobi11NativeVersion(),
        supported = supported
    )
}
