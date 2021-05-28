package com.github.patrick.attach.plugin

import org.bukkit.plugin.java.JavaPlugin

class AttachPlugin : JavaPlugin() {
    override fun onLoad() {
        plugin = this
    }

    companion object {
        @JvmStatic
        lateinit var plugin: AttachPlugin
            private set
    }
}