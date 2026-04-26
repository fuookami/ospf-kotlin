package fuookami.ospf.kotlin.example.framework_demo

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.example.framework_demo.demo1.*
import fuookami.ospf.kotlin.example.framework_demo.demo3.*

class Demo1 {
    suspend operator fun invoke(): Try {
        val demo = SPP()
        return demo()
    }
}

class Demo2 {
    suspend operator fun invoke(): Try {
        TODO("not implemented yet")
    }
}

class Demo3 {
    suspend operator fun invoke(): Try {
        val demo = CSP()
        return demo()
    }
}

class Demo4 {
    suspend operator fun invoke(): Try {
        TODO("not implemented yet")
    }
}
