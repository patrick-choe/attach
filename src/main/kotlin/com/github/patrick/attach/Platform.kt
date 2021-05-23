package com.github.patrick.attach

import com.google.common.collect.ImmutableList

enum class Platform(val dir: String, val binary: String, classes: List<String>) {
    LINUX(
        "linux/",
        "libattach.so",
        listOf("LinuxVirtualMachine", "LinuxAttachProvider", "LinuxVirtualMachine\$SocketInputStream")
    ),
    MAC(
        "mac/",
        "libattach.dylib",
        listOf("BsdVirtualMachine", "BsdAttachProvider", "BsdVirtualMachine\$SocketInputStream")
    ),
    WINDOWS(
        "windows/",
        "attach.dll",
        listOf("WindowsVirtualMachine", "WindowsAttachProvider", "WindowsVirtualMachine\$PipedInputStream")
    );

    val classes: List<String>

    companion object {
        @JvmStatic
        val platform: Platform
            get() {
                val os = System.getProperty("os.name").lowercase()

                if (os.contains("win")) return WINDOWS

                if (os.contains("nix") || os.contains("nux") || os.contains("aix")) return LINUX

                if (os.contains("mac")) return MAC

                throw UnsupportedOperationException()
            }

        @JvmStatic
        val is64Bit: Boolean
            get () {
                val osArch = System.getProperty("os.arch")
                return osArch == "amd64" || osArch == "x86_64"
            }
    }

    init {
        this.classes = ImmutableList.copyOf(classes)
    }
}
