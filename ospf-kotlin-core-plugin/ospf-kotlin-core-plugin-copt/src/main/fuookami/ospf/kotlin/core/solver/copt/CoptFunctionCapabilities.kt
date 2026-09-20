package fuookami.ospf.kotlin.core.solver.copt

import copt.Model
import copt.Sos
import copt.Var

/**
 * 检查当前 COPT Java SDK 是否暴露精确的 SOS2 创建签名。 /
 * Check whether the current COPT Java SDK exposes the exact SOS2 creator signature.
 */
internal fun coptSupportsNativePiecewise(
    modelClass: Class<*> = Model::class.java
): Boolean {
    return try {
        val variableArrayClass = java.lang.reflect.Array.newInstance(Var::class.java, 0).javaClass
        val method = modelClass.getMethod(
            "addSos",
            variableArrayClass,
            DoubleArray::class.java,
            Int::class.javaPrimitiveType!!
        )
        method.returnType == Sos::class.java
    } catch (_: ReflectiveOperationException) {
        false
    } catch (_: LinkageError) {
        false
    }
}
