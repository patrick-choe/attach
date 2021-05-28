package com.github.patrick.attach

enum class Platform(val dir: String, val binary: String) {
    LINUX("linux/", "libattach.so"),
    MAC("mac/", "libattach.dylib"),
    WINDOWS("windows/", "attach.dll");

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
            get() {
                val osArch = System.getProperty("os.arch")
                return osArch == "amd64" || osArch == "x86_64"
            }
    }

}