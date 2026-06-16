package fuookami.ospf.kotlin.example.framework_demo

import fuookami.ospf.kotlin.example.framework_demo.demo1.*
import fuookami.ospf.kotlin.example.framework_demo.demo3.*

import fuookami.ospf.kotlin.utils.functional.*

/** Demo1: Shortest Path Problem (SPP) example using the OSPF framework. */
class Demo1 {
    suspend operator fun invoke(): Try {
        val demo = SPP()
        return demo()
    }
}

/** Demo2: Aircraft cargo loading optimization (placeholder, not yet implemented). */
class Demo2 {
    suspend operator fun invoke(): Try {
        TODO("not implemented yet")
    }
}

/** Demo3: One-dimensional Cutting Stock Problem (CSP1D) using column generation. */
class Demo3 {
    suspend operator fun invoke(): Try {
        val demo = CSP()
        return demo()
    }
}

/** Demo4: Reserved placeholder for a future framework demo. */
class Demo4 {
    suspend operator fun invoke(): Try {
        TODO("not implemented yet")
    }
}
