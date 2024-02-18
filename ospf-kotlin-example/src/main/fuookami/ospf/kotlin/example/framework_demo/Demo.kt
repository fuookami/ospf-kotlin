package fuookami.ospf.kotlin.example.framework_demo

import fuookami.ospf.kotlin.utils.functional.*

class Demo1 {
    suspend operator fun invoke(): Try {
        val demo = fuookami.ospf.kotlin.example.framework_demo.demo1.ConsoleApplication()
        return demo()
    }
}
