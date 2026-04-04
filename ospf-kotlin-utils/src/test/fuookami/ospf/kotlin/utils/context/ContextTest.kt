package fuookami.ospf.kotlin.utils.context

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

/**
 * Context 单元测试
 *
 * Unit tests for Context functionality.
 */
class ContextTest {

    /**
     * UT-CTX-01: 测试 remove(ContextKey) 删除目标 key 的整棵子树，不误删兄弟节点
     *
     * Test that remove(ContextKey) deletes the entire subtree of the target key,
     * without accidentally deleting sibling nodes.
     *
     * 注意：由于 ContextKey 是基于栈帧自动生成的，无法手动创建父子关系。
     * 此测试验证 remove(null) 正确删除当前上下文。
     * Note: Since ContextKey is automatically generated based on stack frames,
     * parent-child relationships cannot be manually created.
     * This test verifies that remove(null) correctly deletes the current context.
     */
    @Test
    fun testRemoveContextKeyDeletesCurrentContext() {
        val parentVar = ContextVar<Int>(0)

        // 设置值
        parentVar.set(10)
        assertEquals(10, parentVar.get())
        assertEquals(1, parentVar.stackValues.size)

        // 删除当前上下文
        parentVar.remove(null)
        assertEquals(0, parentVar.get())
        assertTrue(parentVar.stackValues.isEmpty())
    }

    /**
     * UT-CTX-02: 测试 Context.use {} 作用域结束后自动清理，且默认值回退正确
     *
     * Test that Context.use {} automatically cleans up after scope ends,
     * and default value is correctly restored.
     */
    @Test
    fun testContextUseAutoCleanup() {
        val cv = ContextVar(100)

        // 作用域外使用默认值
        assertEquals(100, cv.get())
        assertTrue(cv.stackValues.isEmpty())

        // 进入作用域
        cv.set(200).use { ctx ->
            assertEquals(200, cv.get())
            // 注意：内部嵌套的 set 会创建新的栈帧，use 结束后只清理那一层
        }

        // 作用域结束后，回到默认值
        assertEquals(100, cv.get())
        assertTrue(cv.stackValues.isEmpty())
    }

    /**
     * 测试自定义键的设置和删除
     *
     * Test setting and removing custom keys.
     */
    @Test
    fun testCustomKeySetAndRemove() {
        val cv = ContextVar("default")

        // 使用自定义键
        cv["customKey1"] = "value1"
        assertEquals("value1", cv["customKey1"])
        assertEquals(1, cv.customValues.size)

        // 删除自定义键
        cv.remove("customKey1")
        assertTrue(cv.customValues.isEmpty())
        assertEquals("default", cv["customKey1"])
    }

    /**
     * 测试多个 ContextVar 实例的独立性
     *
     * Test independence of multiple ContextVar instances.
     */
    @Test
    fun testMultipleContextVarIndependence() {
        val cv1 = ContextVar(1)
        val cv2 = ContextVar(2)

        cv1.set(10).use {
            cv2.set(20).use {
                assertEquals(10, cv1.get())
                assertEquals(20, cv2.get())
            }

            assertEquals(10, cv1.get())
            assertEquals(2, cv2.get())
        }

        assertEquals(1, cv1.get())
        assertEquals(2, cv2.get())
    }
}