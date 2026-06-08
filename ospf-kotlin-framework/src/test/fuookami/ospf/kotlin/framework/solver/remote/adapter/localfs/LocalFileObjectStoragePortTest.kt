package fuookami.ospf.kotlin.framework.solver.remote.adapter.localfs

import java.nio.file.Path
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import fuookami.ospf.kotlin.framework.solver.remote.domain.ObjectPath
import fuookami.ospf.kotlin.framework.solver.remote.domain.ObjectRef

class LocalFileObjectStoragePortTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun putGetExistsAndDeleteObject() = runBlocking {
        val storage = LocalFileObjectStoragePort(tempDir)
        val bytes = "hello remote solver".toByteArray()

        val ref = storage.put(
            path = ObjectPath.of("tenant-a/models/model.json"),
            bytes = bytes,
            metadata = mapOf("contentType" to "application/json")
        )

        assertEquals(ObjectPath.of("tenant-a/models/model.json"), ref.path)
        assertNotNull(ref.version)
        assertNotNull(ref.etag)
        assertTrue(storage.exists(ref))
        assertEquals("hello remote solver", storage.get(ref)?.toString(Charsets.UTF_8))
        assertTrue(storage.delete(ref))
        assertFalse(storage.exists(ref))
    }

    @Test
    fun rejectsPathEscapingStorageRoot() = runBlocking {
        val storage = LocalFileObjectStoragePort(tempDir)

        assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                storage.put(
                    path = ObjectPath.of("../escape.txt"),
                    bytes = byteArrayOf(1)
                )
            }
        }
    }

    @Test
    fun getReturnsNullForMissingObject() = runBlocking {
        val storage = LocalFileObjectStoragePort(tempDir)

        assertEquals(null, storage.get(ObjectRef.of(path = "missing.bin")))
    }
}
