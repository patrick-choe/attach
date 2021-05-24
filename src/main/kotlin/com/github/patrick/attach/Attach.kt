package com.github.patrick.attach

import com.github.patrick.attach.exception.NoExceptionHandler
import net.bytebuddy.ByteBuddy
import net.bytebuddy.agent.ByteBuddyAgent
import net.bytebuddy.agent.builder.AgentBuilder
import net.bytebuddy.asm.Advice
import net.bytebuddy.description.annotation.AnnotationDescription
import net.bytebuddy.description.modifier.Ownership
import net.bytebuddy.description.modifier.Visibility
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy
import net.bytebuddy.implementation.MethodCall
import net.bytebuddy.matcher.ElementMatchers

object Attach {
    @JvmStatic
    private var initialized = false

    @JvmStatic
    fun Class<*>.patch(methodName: String, prefix: (() -> Unit)? = null, postfix: (() -> Unit)? = null) {
        if (!initialized) {
            initialized = true
            ByteBuddyAgent.install()
        }

        if (prefix == null && postfix == null) {
            return
        }

        AgentBuilder.Default()
            .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
            .with(AgentBuilder.InitializationStrategy.NoOp.INSTANCE)
            .with(AgentBuilder.TypeStrategy.Default.REDEFINE)
            .type(ElementMatchers.named(name))
            .transform { builder, _, _, _ ->
                val clazz = getClass(prefix, postfix)
                val instance = clazz.getDeclaredConstructor().newInstance()
                println(clazz.declaredMethods.joinToString("\n") { it.declaredAnnotations.joinToString { annotation -> annotation.toString() } })
                clazz.getDeclaredMethod("enter").invoke(instance)
                clazz.getDeclaredMethod("exit").invoke(instance)
                builder.visit(Advice.to(clazz).on(ElementMatchers.named(methodName)))
            }
            .installOnByteBuddyAgent()
    }

    @JvmStatic
    private fun getClass(prefix: (() -> Unit)?, postfix: (() -> Unit)?): Class<out Any> {
        val buddy = ByteBuddy()
            .subclass(Object::class.java)
            .name("Patched")
            .defineMethod("enter", Void.TYPE, Visibility.PUBLIC, Ownership.STATIC)
            .intercept(MethodCall.call(prefix))
            .annotateMethod(
                AnnotationDescription
                    .Builder
                    .ofType(Advice.OnMethodEnter::class.java)
                    .define("suppress", NoExceptionHandler::class.java)
                    .build()
            )
            .defineMethod("exit", Void.TYPE, Visibility.PUBLIC, Ownership.STATIC)
            .intercept(MethodCall.call(postfix))
            .annotateMethod(
                AnnotationDescription
                    .Builder
                    .ofType(Advice.OnMethodExit::class.java)
                    .define("suppress", NoExceptionHandler::class.java)
                    .define("onThrowable", NoExceptionHandler::class.java)
                    .build()
            )
            .make()
            .load(Attach::class.java.classLoader, ClassLoadingStrategy.Default.WRAPPER)

        println(buddy.bytes.joinToString("") { "%02x".format(it) })

        return buddy.loaded
    }
}