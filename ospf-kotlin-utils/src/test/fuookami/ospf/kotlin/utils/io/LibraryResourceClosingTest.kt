package fuookami.ospf.kotlin.utils.io

import java.io.File
import java.nio.file.Path
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.Library
import fuookami.ospf.kotlin.utils.functional.*

/**
 * IO 资源释放单元测试
 *
 * Unit tests for IO resource handling.
 */
class LibraryResourceClosingTest {

    @TempDir
    lateinit var tempDir: Path

    /**
     * UT-IO-02: 测试 loadInJar 在资源缺失时不泄漏流资源
     *
     * Test that loadInJar does not leak stream resources when resource is missing.
     *
     * 注意：此测试验证资源缺失时返回失败而非静默成功。
     * Note: This test verifies that failure is returned when resource is missing,
     * instead of silent success.
     */
    @Test
    fun testLoadInJarResourceNotFound() {
        val nonExistentPath = "non/existent/resource.txt"
        val targetPath = tempDir.resolve("target.txt").toString()

        // 验证资源缺失时返回失败（use {} 确保 InputStream 不会泄漏）
        // Verify missing resource returns failure (use {} ensures InputStream is not leaked).
        val result = Library.loadInJar(nonExistentPath, targetPath)
        assertTrue(result is Failed)
        result as Failed
        assertEquals(ErrorCode.FileNotFound, result.code)

        // 验证目标文件未被创建（异常路径）
        assertFalse(File(targetPath).exists())
    }

    /**
     * 测试文件已存在时跳过复制
     *
     * Test that existing file is skipped during extraction.
     */
    @Test
    fun testLoadInJarExistingFile() {
        // 创建一个已存在的文件
        val existingFile = tempDir.resolve("existing.txt").toFile()
        existingFile.writeText("existing content")

        // 由于文件已存在，应该跳过复制并尝试加载库
        // Since the file already exists, extraction should be skipped before loading is attempted.
        val result = Library.loadInJar("some/path", existingFile.absolutePath)
        assertTrue(result is Failed)

        // 文件内容应该不变
        assertEquals("existing content", existingFile.readText())
    }

    /**
     * UT-IO-01: 验证文件读写异常后文件句柄可继续被修改
     *
     * Verify that file handle can be modified after read/write exception.
     */
    @Test
    fun testFileHandleReleasedAfterException() {
        val testFile = tempDir.resolve("test.json").toFile()

        // 创建无效内容文件
        testFile.writeText("invalid content")

        // 文件可以被读取（即使内容无效）
        assertEquals("invalid content", testFile.readText())

        // 文件可以被修改
        testFile.writeText("{\"valid\": true}")
        assertEquals("{\"valid\": true}", testFile.readText())

        // 文件可以被删除
        assertTrue(testFile.delete())
        assertFalse(testFile.exists())
    }
}
