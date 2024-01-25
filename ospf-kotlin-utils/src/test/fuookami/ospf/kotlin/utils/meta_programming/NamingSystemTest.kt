package fuookami.ospf.kotlin.utils.meta_programming

import org.junit.jupiter.api.*

class NamingSystemTest {
    @Test
    fun camelCaseFrontEndTest() {
        assert(
            NamingSystem.CamelCase.frontend("bpp3dFaker", setOf("bpp")).toTypedArray() contentEquals arrayOf(
                "bpp",
                "3d",
                "faker"
            )
        )
        assert(
            NamingSystem.CamelCase.frontend("bpp3dFaker", setOf("bpp", "bpp3d"))
                .toTypedArray() contentEquals arrayOf("bpp3d", "faker")
        )
        assert(
            NamingSystem.CamelCase.frontend("askBPP3DFaker", setOf("bpp", "bpp3d"))
                .toTypedArray() contentEquals arrayOf("ask", "bpp3d", "faker")
        )
        assert(
            NamingSystem.CamelCase.frontend("askBpp3dFaker", setOf("bpp", "bpp3d"))
                .toTypedArray() contentEquals arrayOf("ask", "bpp3d", "faker")
        )
    }
}
