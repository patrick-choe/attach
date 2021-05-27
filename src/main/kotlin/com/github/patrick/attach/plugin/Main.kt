package com.github.patrick.attach.plugin

import com.github.patrick.attach.Attach.patch

fun main() {

    try {
        val test = Test()
        test.run()

        Test::class.java.patch("run", prefix = {
            println("Foo")
        }, postfix = {
            println("Bar")
        })

        test.run()
    } catch (t: Throwable) {
        t.printStackTrace()
    }
}