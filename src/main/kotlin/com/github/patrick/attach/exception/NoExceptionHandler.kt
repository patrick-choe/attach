package com.github.patrick.attach.exception

import net.bytebuddy.description.type.TypeDescription

class NoExceptionHandler : Throwable() {
    companion object {
        private val DESCRIPTION = TypeDescription.ForLoadedType.of(
            NoExceptionHandler::class.java
        )
    }

    init {
        throw UnsupportedOperationException("This class only serves as a marker type and should not be instantiated")
    }
}