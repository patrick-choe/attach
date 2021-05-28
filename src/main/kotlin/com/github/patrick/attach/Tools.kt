package com.github.patrick.attach

import com.github.patrick.attach.plugin.AttachPlugin
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

object Tools {
    private const val TOOLS_DIR = "tools/"

    private const val ATTACH_DIR = "attach/"

    private const val NATIVE_DIR = "natives/"

    @JvmStatic
    private val parentDir by lazy {
        if (runCatching { Class.forName("org.bukkit.plugin.java.JavaPlugin") }.isSuccess) {
            AttachPlugin.plugin.dataFolder
        } else {
            File(Attach::class.java.protectionDomain.codeSource.location.toURI()).parentFile
        }
    }

    @JvmStatic
    fun loadAttachLibrary(): File {
        val path = installBinary().parentFile.canonicalPath
        val separator = System.getProperty("path.separator")
        val property = "java.library.path"
        val pathProperty = System.getProperty(property)
        val libraryPath = if (pathProperty != null) path + separator + pathProperty else path

        System.setProperty(property, libraryPath)

        val fieldSysPath = ClassLoader::class.java.getDeclaredField("sys_paths")
        fieldSysPath.isAccessible = true
        fieldSysPath[null] = null

        return installToolsJar()
    }

    @JvmStatic
    private fun installBinary(): File {
        val platform = Platform.platform
        val path = TOOLS_DIR + ATTACH_DIR + NATIVE_DIR +
                (if (Platform.is64Bit) "64/" else "32/") + platform.dir + platform.binary

        return installFile(path)
    }

    @JvmStatic
    private fun installToolsJar(): File {
        val path = TOOLS_DIR + ATTACH_DIR + "tools-min.jar"

        return installFile(path)
    }

    @JvmStatic
    private fun installFile(path: String): File {
        val file = File(parentDir, path)

        if (!file.exists()) {
            file.parentFile.mkdirs()

            runCatching {
                this::class.java.classLoader.getResourceAsStream(path)?.use { stream ->
                    Files.copy(stream, file.toPath(), StandardCopyOption.REPLACE_EXISTING)
                }
            }.onFailure { throwable ->
                throwable.printStackTrace()
            }
        }

        return file
    }
}