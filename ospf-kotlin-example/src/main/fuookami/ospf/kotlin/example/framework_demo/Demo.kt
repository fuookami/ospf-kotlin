package fuookami.ospf.kotlin.example.framework_demo

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.example.framework_demo.demo1.*
import fuookami.ospf.kotlin.example.framework_demo.demo3.*

/** Demo1：使用 OSPF 框架的最短路径问题（SPP）示例。Demo1: Shortest Path Problem (SPP) example using the OSPF framework. */
class Demo1 {
    /** @return 执行结果。 */
    suspend operator fun invoke(): Try {
        val demo = SPP()
        return demo()
    }
}

/** Demo2：飞机货物装载优化（占位符，尚未实现）。Demo2: Aircraft cargo loading optimization (placeholder, not yet implemented). */
class Demo2 {
    /** @return 执行结果。 */
    suspend operator fun invoke(): Try {
        TODO("not implemented yet")
    }
}

/** Demo3：使用列生成的一维下料问题（CSP1D）。Demo3: One-dimensional Cutting Stock Problem (CSP1D) using column generation. */
class Demo3 {
    /** @return 执行结果。 */
    suspend operator fun invoke(): Try {
        val demo = CSP()
        return demo()
    }
}

/** Demo4：为未来框架演示保留的占位符。Demo4: Reserved placeholder for a future framework demo. */
class Demo4 {
    /** @return 执行结果。 */
    suspend operator fun invoke(): Try {
        TODO("not implemented yet")
    }
}
