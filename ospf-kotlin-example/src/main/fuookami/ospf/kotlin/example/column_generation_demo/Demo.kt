package fuookami.ospf.kotlin.example.column_generation_demo

import fuookami.ospf.kotlin.utils.functional.*

class Demo1 {
    suspend operator fun invoke(): Try {
        val demo = fuookami.ospf.kotlin.example.column_generation_demo.demo1.CSP()
        return demo()
    }
}
