package fuookami.ospf.kotlin.utils.context

import org.junit.jupiter.api.Test

class ContextVarPhysicalUnitTests {
    val contextVar = ContextVar(1)

    private fun stack2() {
        assert(contextVar.get() == 2)
        contextVar.set(3)
        assert(contextVar.get() == 3)
    }

    private fun stack1() {
        assert(contextVar.get() == 1)
        contextVar.set(2)
        assert(contextVar.get() == 2)
        stack2()
        assert(contextVar.get() == 2)
    }

    @Test
    fun test() {
        assert(contextVar.get() == 1)
        stack1()
        assert(contextVar.get() == 1)
    }

    /**
     * 测试 UTL-001: 验证 remove(null) 删除当前上下文
     *
     * Test UTL-001: Verify remove(null) removes current context
     */
    @Test
    fun testRemoveNullDeletesCurrentContext() {
        val cv = ContextVar(0)

        cv.set(10)
        assert(cv.get() == 10)
        assert(cv.stackValues.size == 1)

        // remove(null) 应删除当前上下文
        cv.remove(null)
        assert(cv.stackValues.isEmpty())
        assert(cv.get() == 0)
    }

    /**
     * 测试自定义键的设置和删除
     *
     * Test setting and removing custom keys
     */
    @Test
    fun testCustomKeySetAndRemove() {
        val cv = ContextVar("default")

        // 使用自定义键
        cv["customKey1"] = "value1"
        assert(cv["customKey1"] == "value1")
        assert(cv.customValues.size == 1)

        // 删除自定义键
        cv.remove("customKey1")
        assert(cv.customValues.isEmpty())
        assert(cv["customKey1"] == "default")
    }

    /**
     * 测试 Context.use 自动清理
     *
     * Test Context.use automatic cleanup
     */
    @Test
    fun testContextUseAutoCleanup() {
        val cv = ContextVar(0)

        cv.set(10).use { ctx ->
            assert(cv.get() == 10)
            assert(cv.stackValues.size == 1)
        }

        // use 结束后应该自动清理
        assert(cv.stackValues.isEmpty())
        assert(cv.get() == 0)
    }
}
