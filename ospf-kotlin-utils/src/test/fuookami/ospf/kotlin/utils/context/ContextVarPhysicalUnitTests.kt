package fuookami.ospf.kotlin.utils.context

import org.junit.jupiter.api.*

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
}
