package fuookami.ospf.kotlin.utils

import java.io.*

data object Library {
    fun loadInJar(path: String, toPath: String) {
        val extractedLibFile = File(toPath)
        val lib = extractedLibFile.nameWithoutExtension
        if (!extractedLibFile.exists()) {
            val buffer = ByteArray(1024)
            var readBytes: Int
            val inputStream = this.javaClass.classLoader.getResourceAsStream(path)!!
            val outputStream = FileOutputStream(extractedLibFile)

            try {
                while (true) {
                    readBytes = inputStream.read(buffer)
                    if (readBytes <= 0) {
                        break
                    }
                    outputStream.write(buffer, 0, readBytes)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            inputStream.close()
            outputStream.close()
        }
        System.loadLibrary(lib)
    }
}
