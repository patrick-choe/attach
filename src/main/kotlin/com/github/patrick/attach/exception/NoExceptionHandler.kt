package com.github.patrick.attach.exception

class NoExceptionHandler : Throwable() {
    init {
        throw UnsupportedOperationException("This class only serves as a marker type and should not be instantiated")
    }
}