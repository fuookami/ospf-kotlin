package fuookami.ospf.kotlin.example.core_demo

object ScipAvailability {
    fun isAvailable(): Boolean = runCatching { Class.forName("jscip.Scip") }.isSuccess
}