package fuookami.ospf.kotlin.utils.concept

import com.poit.aps.execution.task.primitives.TaskPlanId as ApsTaskPlanId
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * PoC2：验证 aps 同名类（不同包）实现 ospf 同名接口，并用全限定符消歧的可行性。
 */
class NameClashPoCTest {

    @Test
    fun `1_aps_class_implements_same_name_ospf_interface_compiles`() {
        val apsId = ApsTaskPlanId(42L)
        // 作为 ospf 接口类型使用（全限定符）
        val ospfId: fuookami.ospf.kotlin.utils.concept.TaskPlanId = apsId
        assertTrue(ospfId is ApsTaskPlanId)
        assertEquals(ApsTaskPlanId(42L), ospfId)
    }

    @Test
    fun `2_aps_class_used_in_ospf_interface_typed_list`() {
        val list: List<fuookami.ospf.kotlin.utils.concept.TaskPlanId> = listOf(
            ApsTaskPlanId(1L), ApsTaskPlanId(2L)
        )
        assertEquals(2, list.size)
        assertEquals(ApsTaskPlanId(1L), list[0])
    }

    @Test
    fun `3_hashmap_key_with_same_name_class_and_interface`() {
        val map = HashMap<fuookami.ospf.kotlin.utils.concept.TaskPlanId, String>()
        map[ApsTaskPlanId(100L)] = "v"
        assertEquals("v", map[ApsTaskPlanId(100L)])
    }
}
