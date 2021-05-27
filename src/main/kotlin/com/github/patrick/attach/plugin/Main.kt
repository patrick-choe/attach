package com.github.patrick.attach.plugin

import com.github.patrick.attach.Attach.patch
import kotlin.system.measureNanoTime


fun main() {
    val test = Test()
    test.run()

    val nano = measureNanoTime {
        Test::class.java.patch("run", prefix = {
            println("Foo")
        }, postfix = {
            println("Bar")
        })
    }

    println("Measured nanoseconds: $nano")

    test.run()
}