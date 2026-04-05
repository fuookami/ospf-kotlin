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
     * 通过手动构造 ContextKey 的 stackTree 来模拟父子关系：
     * 注意：ContextKey.parent 会调用 dump() 将栈顶行号设为 -1
     * - parentKey 的 stackTree: [A(-1), B, C] (栈顶 A 行号为 -1)
     * - childKey1 的 stackTree: [X(-1), A, B, C] -> parent.stackTree = [A(-1), B, C]
     * - childKey2 的 stackTree: [Y(-1), A, B, C] -> parent.stackTree = [A(-1), B, C]
     * - siblingKey 的 stackTree: [D, E, F] (与 parentKey 无关)
     */
    @Test
    fun testRemoveContextKeyDeletesSubtree() {
        val cv = ContextVar<Int>(0)
        val thread = Thread.currentThread()

        // 构造栈帧元素 - 栈顶行号为 -1 以匹配 dump() 的行为
        val frameANeg1 = createStackTraceElement("ClassA", "methodA", -1)
        val frameB = createStackTraceElement("ClassB", "methodB", 2)
        val frameC = createStackTraceElement("ClassC", "methodC", 3)
        val frameXNeg1 = createStackTraceElement("ClassX", "methodX", -1)
        val frameYNeg1 = createStackTraceElement("ClassY", "methodY", -1)
        val frameD = createStackTraceElement("ClassD", "methodD", 4)
        val frameE = createStackTraceElement("ClassE", "methodE", 5)
        val frameF = createStackTraceElement("ClassF", "methodF", 6)

        // 构造父子关系的键
        // parentKey: [A(-1), B, C]
        val parentKey = ContextKey(thread, arrayOf(frameANeg1, frameB, frameC))
        // childKey1: [X(-1), A, B, C] -> parent = ContextKey with stackTree [A(-1), B, C]
        val childKey1 = ContextKey(thread, arrayOf(frameXNeg1, frameANeg1, frameB, frameC))
        // childKey2: [Y(-1), A, B, C] -> parent = ContextKey with stackTree [A(-1), B, C]
        val childKey2 = ContextKey(thread, arrayOf(frameYNeg1, frameANeg1, frameB, frameC))
        // siblingKey: [D, E, F] -> 与 parentKey 无关
        val siblingKey = ContextKey(thread, arrayOf(frameD, frameE, frameF))

        // 验证父子关系
        assertEquals(parentKey, childKey1.parent, "childKey1.parent should equal parentKey")
        assertEquals(parentKey, childKey2.parent, "childKey2.parent should equal parentKey")

        // 设置值
        cv[parentKey] = 100
        cv[childKey1] = 101
        cv[childKey2] = 102
        cv[siblingKey] = 200

        assertEquals(4, cv.stackValues.size, "Should have 4 stack values")

        // 删除 parentKey 及其子树
        cv.remove(parentKey)

        // 验证：parentKey 及其子节点被删除，siblingKey 保留
        assertEquals(0, cv.get(parentKey), "parentKey value should be default after removal")
        assertEquals(0, cv.get(childKey1), "childKey1 value should be default after parent removal")
        assertEquals(0, cv.get(childKey2), "childKey2 value should be default after parent removal")
        assertEquals(200, cv.get(siblingKey), "siblingKey should not be affected")
        assertEquals(1, cv.stackValues.size, "Only siblingKey should remain")
    }

    /**
     * 测试 remove(null) 正确删除当前上下文
     *
     * Test that remove(null) correctly deletes the current context.
     */
    @Test
    fun testRemoveNullDeletesCurrentContext() {
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

    /**
     * 测试多层嵌套子树的删除
     *
     * Test removal of deeply nested subtrees.
     */
    @Test
    fun testRemoveDeepNestedSubtree() {
        val cv = ContextVar<Int>(0)
        val thread = Thread.currentThread()

        // 构造多层嵌套关系 - 栈顶行号为 -1
        // level0: [A(-1)]
        val level0 = ContextKey(thread, arrayOf(createStackTraceElement("A", "a", -1)))
        // level1: [B(-1), A]
        val level1 = ContextKey(thread, arrayOf(
            createStackTraceElement("B", "b", -1),
            createStackTraceElement("A", "a", 1)
        ))
        // level2: [C(-1), B, A]
        val level2 = ContextKey(thread, arrayOf(
            createStackTraceElement("C", "c", -1),
            createStackTraceElement("B", "b", 2),
            createStackTraceElement("A", "a", 1)
        ))
        // level3: [D(-1), C, B, A]
        val level3 = ContextKey(thread, arrayOf(
            createStackTraceElement("D", "d", -1),
            createStackTraceElement("C", "c", 3),
            createStackTraceElement("B", "b", 2),
            createStackTraceElement("A", "a", 1)
        ))

        // 设置值
        cv[level0] = 0
        cv[level1] = 1
        cv[level2] = 2
        cv[level3] = 3

        assertEquals(4, cv.stackValues.size)

        // 删除 level1 应该删除 level1, level2, level3，保留 level0
        cv.remove(level1)

        assertEquals(0, cv.get(level0), "level0 should remain")
        assertEquals(0, cv.get(level1), "level1 should be removed")
        assertEquals(0, cv.get(level2), "level2 should be removed")
        assertEquals(0, cv.get(level3), "level3 should be removed")
        assertEquals(1, cv.stackValues.size, "Only level0 should remain")
    }

    // 辅助方法：创建 StackTraceElement
    private fun createStackTraceElement(className: String, methodName: String, lineNumber: Int = 1): StackTraceElement {
        return StackTraceElement(className, methodName, "TestFile.kt", lineNumber)
    }
}