package fuookami.ospf.kotlin.example.core_demo

/** Checks at runtime whether the SCIP JNI solver is available on the classpath. */
object ScipAvailability {
    fun isAvailable(): Boolean = runCatching { Class.forName("jscip.Scip") }.isSuccess
}
