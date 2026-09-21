package fuookami.ospf.kotlin.core.solver

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.ok

class NativeFunctionWriterRegistryTest {
    @Test
    fun writesRegisteredBatchesInOrderAndSkipsEmptyBatches() {
        val calls = mutableListOf<String>()
        val registry = NativeFunctionWriterRegistry<String, String>(
            listOf(
                NativeFunctionWriter { _, _, batch ->
                    calls += "first:${batch.single()}"
                    ok
                },
                NativeFunctionWriter { _, _, batch ->
                    calls += "second:${batch.single()}"
                    ok
                }
            )
        )

        val result = registry.write(
            model = "model",
            variables = emptyMap(),
            batches = listOf(listOf("a"), emptyList())
        )

        assertTrue(result is Ok)
        assertEquals(listOf("first:a"), calls)
    }
}
