package com.github.patrick.attach.agent

import com.sun.tools.attach.VirtualMachine
import com.sun.tools.attach.VirtualMachineDescriptor
import com.sun.tools.attach.spi.AttachProvider

class AttachProviderPlaceHolder : AttachProvider() {
    override fun name(): String? {
        return null
    }

    override fun type(): String? {
        return null
    }

    override fun attachVirtualMachine(id: String): VirtualMachine? {
        return null
    }

    override fun listVirtualMachines(): List<VirtualMachineDescriptor>? {
        return null
    }
}
