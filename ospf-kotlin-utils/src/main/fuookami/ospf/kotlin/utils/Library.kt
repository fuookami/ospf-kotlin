/**
 * 本文件提供从 JAR 中提取并加载本地库的工具。
 * This file provides a utility for extracting and loading native libraries from JAR files.
 */
package fuookami.ospf.kotlin.utils

import java.io.*

/**
 * 库加载工具
 *
 * Library loading utility for extracting and loading native libraries from JAR.
 *
 * RVW-003 修复：使用 use {} 确保资源正确关闭。
 */
data object Library {
    /**
     * 从 JAR 中加载本地库
     *
     * Load native library from JAR by extracting it to a temporary location.
     *
     * @param path      JAR 内资源路径 / Resource path inside JAR
     * @param toPath    目标提取路径 / Target extraction path
     */
    fun loadInJar(path: String, toPath: String) {
        val extractedLibFile = File(toPath)
        val lib = extractedLibFile.nameWithoutExtension
        if (!extractedLibFile.exists()) {
            val buffer = ByteArray(1024)
            var readBytes: Int

            // 使用 use {} 确保资源正确关闭 / Use {} ensures proper resource closing
            this.javaClass.classLoader.getResourceAsStream(path)?.use { inputStream ->
                FileOutputStream(extractedLibFile).use { outputStream ->
                    while (true) {
                        readBytes = inputStream.read(buffer)
                        if (readBytes <= 0) {
                            break
                        }
                        outputStream.write(buffer, 0, readBytes)
                    }
                }
            } ?: throw IllegalArgumentException("Resource not found: $path")
        }
        System.loadLibrary(lib)
    }
}
