package fuookami.ospf.kotlin.utils.concept

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.HashMap

/**
 * PoC：验证“ospf id 标记接口（语义化命名）+ aps value class 协变 override”方案的技术可行性。
 *
 * 模拟 ospf 侧：ExecutorId 标记接口 + Executor(open val id: ExecutorId) : ManualIndexed()
 * 模拟 aps 侧：WorkUnitId(value class : ExecutorId) + WorkUnitSpec(override val id: WorkUnitId)
 *
 * 核心验证点：value class 能否协变 override 父类 open val id: ExecutorId，
 * 并在静态类型层保住 WorkUnitId（无需 cast）。
 */

// ===== ospf 侧模拟 =====

/** ospf 标记接口（语义化命名，非泛型 Id） */
interface ExecutorId

/** ospf Executor：id 类型为 ExecutorId，open 可 override，继承 ManualIndexed */
open class Executor(
    open val id: ExecutorId,
    open val name: String
) : ManualIndexed()

// ===== aps 侧模拟 =====

/** aps WorkUnitId：value class，实现 ExecutorId */
@JvmInline
value class WorkUnitId(val value: Long) : ExecutorId {
    override fun toString(): String = "WorkUnitId($value)"
}

/** aps WorkUnitSpec：协变 override val id: WorkUnitId（父类是 open val id: ExecutorId） */
data class WorkUnitSpec(
    override val id: WorkUnitId,
    override val name: String
) : Executor(id, name)

class CovariantIdOverridePoCTest {

    @Test
    fun `1_value_class_covariant_override_compiles_and_preserves_static_type`() {
        val spec = WorkUnitSpec(WorkUnitId(42L), "machine-A")
        // 静态类型应为 WorkUnitId，无需 cast
        val id: WorkUnitId = spec.id
        assertEquals(42L, id.value)
    }

    @Test
    fun `2_polymorphic_list_returns_correct_runtime_id`() {
        val executors: List<Executor> = listOf(
            WorkUnitSpec(WorkUnitId(1L), "A"),
            WorkUnitSpec(WorkUnitId(2L), "B")
        )
        val id0 = executors[0].id
        assertTrue(id0 is WorkUnitId)
        assertEquals(WorkUnitId(1L), id0)
        assertEquals(WorkUnitId(2L), executors[1].id)
    }

    @Test
    fun `3_value_class_as_HashMap_key`() {
        val map = HashMap<ExecutorId, String>()
        val key = WorkUnitId(100L)
        map[key] = "v1"
        // 用协变后的 spec.id 作 key 查找
        val spec = WorkUnitSpec(WorkUnitId(100L), "x")
        assertEquals("v1", map[spec.id])
        // 不同 id 不命中
        assertNull(map[WorkUnitId(999L)])
    }

    @Test
    fun `4_equals_and_hashCode_across_interface_boundary`() {
        val a: ExecutorId = WorkUnitId(7L)
        val b: ExecutorId = WorkUnitId(7L)
        val c: ExecutorId = WorkUnitId(8L)
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, c)
    }

    @Test
    fun `5_data_class_copy_preserves_covariant_id_type`() {
        val spec = WorkUnitSpec(WorkUnitId(3L), "n")
        val copied = spec.copy(id = WorkUnitId(4L))
        val id: WorkUnitId = copied.id
        assertEquals(4L, id.value)
    }

    @Test
    fun `6_Set_dedup_based_on_id_equals`() {
        val set = HashSet<WorkUnitId>()
        set.add(WorkUnitId(1L))
        set.add(WorkUnitId(1L))
        set.add(WorkUnitId(2L))
        assertEquals(2, set.size)
    }

    @Test
    fun `7_executor_constructor_param_accepts_subtype`() {
        val wid = WorkUnitId(55L)
        val e: Executor = WorkUnitSpec(wid, "m")
        assertEquals(wid, e.id)
    }

    @Test
    fun `8_id_toString_for_solver_symbol`() {
        val spec = WorkUnitSpec(WorkUnitId(42L), "A")
        val symbol = "executor-${spec.id}"
        assertEquals("executor-WorkUnitId(42)", symbol)
    }
}
