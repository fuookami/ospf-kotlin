package fuookami.ospf.kotlin.core.solver.cplex

import ilog.concert.IloNumVar
import ilog.concert.IloSOS2
import ilog.cplex.IloCplex
import fuookami.ospf.kotlin.core.model.intermediate.FunctionNativeCapability
import fuookami.ospf.kotlin.core.model.intermediate.FunctionSolverCapabilities

/** 检查当前 CPLEX Java SDK 的精确原生函数签名。 / Check exact native-function signatures exposed by the linked CPLEX Java SDK. */
internal fun cplexFunctionSolverCapabilities(
    modelClass: Class<*> = IloCplex::class.java
): FunctionSolverCapabilities {
    fun has(
        name: String,
        returnType: Class<*>,
        vararg parameterTypes: Class<*>
    ): Boolean {
        return try {
            modelClass.getMethod(name, *parameterTypes).returnType == returnType
        } catch (_: ReflectiveOperationException) {
            false
        } catch (_: SecurityException) {
            false
        } catch (_: LinkageError) {
            false
        }
    }

    val supported = buildSet {
        if (has(
                name = "addSOS2",
                returnType = IloSOS2::class.java,
                parameterTypes = arrayOf(Array<IloNumVar>::class.java, DoubleArray::class.java)
            )
        ) {
            add(FunctionNativeCapability.SOS2)
        }
    }
    return FunctionSolverCapabilities(
        solver = "cplex",
        version = IloCplex::class.java.`package`?.implementationVersion,
        supported = supported
    )
}
