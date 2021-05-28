package com.github.patrick.attach

import java.io.File
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import javax.tools.ToolProvider

internal object Utils {
    @JvmStatic
    internal val isLegacy = runCatching {
        Runtime::class.java.getDeclaredMethod("version")
    }.isFailure

    @JvmStatic
    internal val isJDK = ToolProvider.getSystemJavaCompiler() != null

    @JvmStatic
    internal val currentJar by lazy {
        File(Attach::class.java.protectionDomain.codeSource.location.toURI()).also {
            println(it.canonicalPath)
        }
    }

    @JvmStatic
    @Volatile
    private var defineMethod: Method? = null

    @JvmStatic
    @Volatile
    private var lookupMethod: Method? = null

    @JvmStatic
    private var lookup: Any? = null

    @JvmStatic
    private val loader = Attach::class.java.classLoader

    @JvmStatic
    internal fun defineClass(data: ByteArray): Class<*> {
        return try {
            if (isLegacy) defineClassLegacy(data) else defineClassModern(data)
        } catch (e: SecurityException) {
            throw RuntimeException("Cannot use reflection to dynamically load a class.", e)
        } catch (e: NoSuchMethodException) {
            throw IllegalStateException("Incompatible JVM.", e)
        } catch (e: ClassNotFoundException) {
            throw IllegalStateException("Incompatible JVM.", e)
        } catch (e: IllegalArgumentException) {
            throw IllegalStateException("Cannot call defineMethod - wrong JVM?", e)
        } catch (e: IllegalAccessException) {
            throw RuntimeException("Security limitation! Cannot dynamically load class.", e)
        } catch (e: InvocationTargetException) {
            throw RuntimeException("Error occurred in code generator.", e)
        }
    }

    @JvmStatic
    private fun defineClassLegacy(data: ByteArray): Class<*> {
        if (defineMethod == null) {
            val defined = ClassLoader::class.java.getDeclaredMethod(
                "defineClass",
                String::class.java, ByteArray::class.java, Int::class.javaPrimitiveType, Int::class.javaPrimitiveType
            )

            defined.isAccessible = true
            defineMethod = defined
        }

        return defineMethod?.invoke(loader, null, data, 0, data.size) as Class<*>
    }

    @JvmStatic
    private fun defineClassModern(data: ByteArray): Class<*> {
        if (defineMethod == null) {
            defineMethod = Class.forName("java.lang.invoke.MethodHandles\$Lookup")
                .getDeclaredMethod("defineClass", ByteArray::class.java)
        }
        if (lookupMethod == null) {
            lookupMethod = Class.forName("java.lang.invoke.MethodHandles").getDeclaredMethod("lookup")
        }
        if (lookup == null) {
            lookup = lookupMethod?.invoke(null)
        }
        return defineMethod?.invoke(lookup, data) as Class<*>
    }
}