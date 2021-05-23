package com.github.patrick.attach

import java.io.InputStream


object ClassPathUtils {
    @JvmStatic
    fun InputStream.toByteArray(): ByteArray {
        var buffer = ByteArray(maxOf(1024, available()))
        var offset = 0
        var bytesRead: Int
        while (-1 != read(buffer, offset, buffer.size - offset).also { bytesRead = it }) {
            offset += bytesRead
            if (offset == buffer.size) buffer =
                buffer.copyOf(buffer.size + maxOf(available(), buffer.size shr 1))
        }
        return if (offset == buffer.size) buffer else buffer.copyOf(offset)
    }

    @JvmStatic
    fun defineClass(loader: ClassLoader, inputStream: InputStream): Class<*> {
        return try {
            val bytes = inputStream.readAllBytes()

            val defineClassMethod = ClassLoader::class.java.getDeclaredMethod(
                "defineClass",
                String::class.java,
                ByteArray::class.java,
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType
            )
            defineClassMethod.isAccessible = true
            defineClassMethod.invoke(loader, null, bytes, 0, bytes.size) as Class<*>
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    @JvmStatic
    val Class<*>.unqualify: String
        get() {
            return name.replace('.', '/') + ".class"
        }
}