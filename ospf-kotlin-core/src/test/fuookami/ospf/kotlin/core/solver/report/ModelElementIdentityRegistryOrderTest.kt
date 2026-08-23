package fuookami.ospf.kotlin.core.solver.report

import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.utils.functional.Ok

/**
 * 稳定身份注册顺序独立性测试。 / Stable identity registration-order independence tests.
 */
class ModelElementIdentityRegistryOrderTest {
    /**
     * 验证相同元素以不同顺序注册时，解析出的稳定 ID 完全一致。
     * Verifies that resolving stable IDs does not depend on registration order.
     */
    @Test
    fun resolvedStableIdsDoNotDependOnRegistrationOrder() {
        val first = Any()
        val second = Any()

        val ascending = ModelElementIdentityRegistry(namespace = "order-model", schemaVersion = "1.0")
        assertTrue(ascending.registerVariable(first, VariableId("variable:first")) is Ok)
        assertTrue(ascending.registerVariable(second, VariableId("variable:second")) is Ok)

        val descending = ModelElementIdentityRegistry(namespace = "order-model", schemaVersion = "1.0")
        assertTrue(descending.registerVariable(second, VariableId("variable:second")) is Ok)
        assertTrue(descending.registerVariable(first, VariableId("variable:first")) is Ok)

        assertEquals(ascending.variableId(first, 0), descending.variableId(first, 0))
        assertEquals(ascending.variableId(second, 0), descending.variableId(second, 0))
        assertEquals(ascending.identity(first)!!.origin, descending.identity(first)!!.origin)
        assertTrue(ascending.validate() is Ok)
        assertTrue(descending.validate() is Ok)
    }
}
