package com.github.patrick.attach

import com.github.patrick.attach.ClassPathUtils.defineClass
import com.github.patrick.attach.ClassPathUtils.toByteArray
import com.github.patrick.attach.ClassPathUtils.unqualify
import com.github.patrick.attach.plugin.AttachPlugin
import java.io.File
import java.util.Vector
import java.util.jar.Attributes
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream
import java.util.jar.Manifest


class AgentLoader {
    @FunctionalInterface
    interface AgentLoaderInterface {
        fun loadAgent(agentJar: File, options: String? = null)
    }

    companion object {
        @JvmStatic
        @Volatile
        private var agentLoader: AgentLoaderInterface? = null

        @JvmStatic
        @Synchronized
        private fun getAgentLoader(): AgentLoaderInterface {
            return agentLoader ?: run {
                val agentLoaderClass = try {
                    Class.forName("com.sun.tools.attach.VirtualMachine")
                    Class.forName("com.github.patrick.attach.agent.AgentLoaderHotSpot")
                } catch (ex: Exception) {
                    val platform = Platform.platform
                    val systemLoader = AttachPlugin::class.java.classLoader

                    val path = Tools.TOOLS_DIR + Tools.ATTACH_DIR + "classes/"
                    val platformPath = path + platform.dir

                    val shaded = listOf(
                        "AttachProvider",
                        "VirtualMachine",
                        "VirtualMachineDescriptor",
                        "HotSpotVirtualMachine",
                        "HotSpotAttachProvider",
                        "HotSpotAttachProvider\$HotSpotVirtualMachineDescriptor",
                        "AgentInitializationException",
                        "AgentLoadException",
                        "AttachNotSupportedException",
                        "AttachOperationFailedException",
                        "AttachPermission"
                    ).map { clazz -> path + clazz } +
                            platform.classes.map { s -> platformPath + s } +
                            "com/github/patrick/attach/agent/AttachProviderPlaceHolder"

                    for (s in shaded) {
                        println("wa sans $s")
                        AgentLoader::class.java.getResourceAsStream("/$s.class")?.use { stream ->
                            try {
                                defineClass(systemLoader, stream)
                                println("good init")
                            } catch (e: java.lang.Exception) {
                                throw RuntimeException("Error defining: $s", e)
                            }
                        }
                    }

                    try {
                        requireNotNull(AgentLoader::class.java.getResourceAsStream("/com/github/patrick/attach/agent/AgentLoaderHotSpot.class")).use { stream ->
                            defineClass(systemLoader, stream)
                        }
                    } catch (e: Exception) {
                        throw RuntimeException("Error loading AgentLoader implementation", e)
                    }
                }
                val loader = try {
                    val agentLoaderObject = agentLoaderClass.getDeclaredConstructor().newInstance()

                    object : AgentLoaderInterface {
                        override fun loadAgent(agentJar: File, options: String?) {
                            try {
                                val loadAgentMethod =
                                    agentLoaderClass.getMethod("attachAgentToJVM", File::class.java, String::class.java)
                                loadAgentMethod.invoke(agentLoaderObject, agentJar, options)
                            } catch (e: Exception) {
                                throw RuntimeException(e)
                            }
                        }
                    }
                } catch (e: Exception) {
                    throw RuntimeException("Error getting agent loader implementation", e)
                }
                agentLoader = loader
                loader
            }
        }

        @JvmStatic
        fun loadAgentClass(agentClass: Class<*>, vararg resources: Class<*>) {
            val jarFile = File.createTempFile("javaagent.${agentClass.name}", ".jar")
            jarFile.deleteOnExit()

            val manifest = Manifest()
            val attributes = manifest.mainAttributes

            attributes[Attributes.Name.MANIFEST_VERSION] = "1.0"
            attributes[Attributes.Name("Agent-Class")] = agentClass.name
            attributes[Attributes.Name("Can-Retransform-Classes")] = "true"
            attributes[Attributes.Name("Can-Redefine-Classes")] = "true"

            jarFile.outputStream().let { fileOutputStream ->
                JarOutputStream(fileOutputStream, manifest).use { jarOutputStream ->
                    val agentClassName = agentClass.unqualify

                    agentClass.classLoader.getResourceAsStream(agentClassName)?.let { inputStream ->
                        jarOutputStream.putNextEntry(JarEntry(agentClassName))
                        jarOutputStream.write(inputStream.toByteArray())
                        jarOutputStream.closeEntry()
                    }

                    resources.forEach { resource ->
                        val resourceName = resource.unqualify

                        resource.classLoader.getResourceAsStream(resourceName)?.let { inputStream ->
                            jarOutputStream.putNextEntry(JarEntry(resourceName))
                            jarOutputStream.write(inputStream.toByteArray())
                            jarOutputStream.closeEntry()
                        }
                    }

                    jarOutputStream.flush()
                }
            }

            getAgentLoader().loadAgent(jarFile)
        }
    }
}