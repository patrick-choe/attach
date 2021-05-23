package com.github.patrick.attach.agent

import com.sun.tools.attach.VirtualMachine
import com.sun.tools.attach.spi.AttachProvider
import java.io.File
import java.lang.management.ManagementFactory
import java.util.Locale

@Suppress("unused")
class AgentLoaderHotSpot {
    fun attachAgentToJVM(agentJar: File, options: String? = null) {
        val vm: VirtualMachine = virtualMachine ?: throw RuntimeException("Not found VirtualMachine")
        try {
            vm.loadAgent(agentJar.canonicalPath, options)
        } finally {
            vm.detach()
        }
    }

    companion object {
        @JvmStatic
        val virtualMachine: VirtualMachine?
            get() {
                if (VirtualMachine.list().size > 0) {
                    // tools jar present
                    return try {
                        VirtualMachine.attach(pid)
                    } catch (e: Exception) {
                        throw RuntimeException(e)
                    }
                }

                val jvm = System.getProperty("java.vm.name").lowercase(Locale.ENGLISH)
                if ("hotspot" in jvm || "openjdk" in jvm || "dynamic code evolution" in jvm) {
                    // tools jar not present, but it's a sun vm
                    val virtualMachineClass = pickVmImplementation()
                    return try {
                        val attachProvider = AttachProviderPlaceHolder()
                        val vmConstructor = virtualMachineClass.getDeclaredConstructor(
                            AttachProvider::class.java,
                            String::class.java
                        )
                        vmConstructor.isAccessible = true
                        vmConstructor.newInstance(attachProvider, pid)
                    } catch (e: UnsatisfiedLinkError) {
                        throw RuntimeException("This jre doesn't support the native library for attaching to the jvm", e)
                    } catch (e: Exception) {
                        throw RuntimeException(e)
                    }
                }

                // not a hotspot based virtual machine
                return null
            }

        @JvmStatic
        private val pid: String
            get() {
                val nameOfRunningVM = ManagementFactory.getRuntimeMXBean().name
                val p = nameOfRunningVM.indexOf('@')
                return nameOfRunningVM.substring(0, p)
            }

        @Suppress("UNCHECKED_CAST")
        @JvmStatic
        private fun pickVmImplementation(): Class<VirtualMachine> {
            val os = System.getProperty("os.name").lowercase(Locale.ENGLISH)
            try {
                if ("win" in os) {
                    return AgentLoaderHotSpot::class.java.classLoader.loadClass("sun.tools.attach.WindowsVirtualMachine") as Class<VirtualMachine>
                }

                if ("nix" in os || "nux" in os || os.indexOf("aix") > 0) {
                    return AgentLoaderHotSpot::class.java.classLoader.loadClass("sun.tools.attach.LinuxVirtualMachine") as Class<VirtualMachine>
                }

                if ("mac" in os) {
                    return AgentLoaderHotSpot::class.java.classLoader.loadClass("sun.tools.attach.BsdVirtualMachine") as Class<VirtualMachine>
                }
            } catch (ex: java.lang.Exception) {
                throw RuntimeException(ex)
            }

            throw RuntimeException("Can't find a vm implementation for the operational system: " + System.getProperty("os.name"))
        }
    }
}