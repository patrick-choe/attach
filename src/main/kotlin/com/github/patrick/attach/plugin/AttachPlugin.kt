package com.github.patrick.attach.plugin

import com.github.patrick.attach.AgentLoader
import com.github.patrick.attach.ClassPathUtils
import com.github.patrick.attach.Tools
import com.github.patrick.attach.test.Profiling
import com.github.patrick.attach.test.Test
import org.bukkit.plugin.java.JavaPlugin
import org.objectweb.asm.ClassVisitor
import java.io.File
import java.util.jar.JarFile

@Suppress("unused")
class AttachPlugin : JavaPlugin() {
    override fun onLoad() {
        plugin = this

        Tools.loadAttachLibrary()
    }

    override fun onEnable() {
//        test()
    }

    private fun test() {
        AgentLoader.loadAgentClass(
            Profiling.Agent::class.java,
            Profiling.ProfileClassAdapter::class.java,
            Profiling.ProfileMethodAdapter::class.java,
            Profiling::class.java,
            ClassPathUtils::class.java,
            Test::class.java,
            *JarFile(File(ClassVisitor::class.java.protectionDomain.codeSource.location.file)).entries().toList()
                .filter {
                    it.name.endsWith(".class") && it.name.startsWith("com/github/patrick/attach/shaded")
                }.map {
                    Class.forName(it.name.substring(0, it.name.length - 6).replace('/', '.'))
                }.toTypedArray()
        )

        plugin.server.scheduler.runTaskLater(plugin, {
            Test()
        }, 100L)
    }

    internal companion object {
        lateinit var plugin: AttachPlugin
            private set
    }
}