package com.github.patrick.attach

import net.bytebuddy.agent.ByteBuddyAgent
import net.bytebuddy.agent.builder.AgentBuilder
import net.bytebuddy.asm.Advice
import net.bytebuddy.dynamic.ClassFileLocator
import net.bytebuddy.jar.asm.ClassWriter
import net.bytebuddy.jar.asm.FieldVisitor
import net.bytebuddy.jar.asm.MethodVisitor
import net.bytebuddy.jar.asm.Opcodes
import net.bytebuddy.matcher.ElementMatchers
import java.net.URLClassLoader


object Attach {
    @JvmStatic
    private var initialized = false

    @JvmStatic
    fun Class<*>.patch(methodName: String, prefix: (() -> Unit)? = null, postfix: (() -> Unit)? = null) {
        if (!initialized) {
            if (Utils.isLegacy && !Utils.isJDK) {
                Tools.loadAttachLibrary()
                ByteBuddyAgent.install(ByteBuddyAgent.AttachmentProvider {
                    ByteBuddyAgent.AttachmentProvider.Accessor.Simple.of(
                        URLClassLoader(arrayOf(Utils.currentJar.toURI().toURL()), ClassLoader.getSystemClassLoader()),
                        Utils.currentJar
                    )
                })
            } else {
                ByteBuddyAgent.install()
            }
            initialized = true
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
                val bytes = getBytes()

                val clazz = Utils.defineClass(bytes).apply {
                    getDeclaredConstructor(Function0::class.java, Function0::class.java).newInstance(prefix, postfix)
                }

                builder.visit(
                    Advice.to(clazz, ClassFileLocator.Simple.of(clazz.name, bytes))
                        .on(ElementMatchers.named(methodName))
                )
            }
            .installOnByteBuddyAgent()
    }


    @JvmStatic
    private fun getBytes(): ByteArray {
        val currentTime = System.nanoTime()
        val name = "com/github/patrick/attach/C$currentTime"

        val classWriter = ClassWriter(ClassWriter.COMPUTE_MAXS)
        var fieldVisitor: FieldVisitor
        var methodVisitor: MethodVisitor

        classWriter.visit(
            Opcodes.V1_8,
            Opcodes.ACC_PUBLIC + Opcodes.ACC_SUPER,
            name,
            null,
            "java/lang/Object",
            null
        )

        // static fields
        run {
            fieldVisitor = classWriter.visitField(
                Opcodes.ACC_PUBLIC + Opcodes.ACC_STATIC,
                "_prefix",
                "Lkotlin/jvm/functions/Function0;",
                "Lkotlin/jvm/functions/Function0<*>;",
                null
            )

            fieldVisitor.visitEnd()

            fieldVisitor = classWriter.visitField(
                Opcodes.ACC_PUBLIC + Opcodes.ACC_STATIC,
                "_postfix",
                "Lkotlin/jvm/functions/Function0;",
                "Lkotlin/jvm/functions/Function0<*>;",
                null
            )

            fieldVisitor.visitEnd()
        }

        // <init>
        run {
            methodVisitor = classWriter.visitMethod(
                Opcodes.ACC_PUBLIC,
                "<init>",
                "(Lkotlin/jvm/functions/Function0;Lkotlin/jvm/functions/Function0;)V",
                "(Lkotlin/jvm/functions/Function0<*>;Lkotlin/jvm/functions/Function0<*>;)V",
                null
            )

            methodVisitor.visitVarInsn(Opcodes.ALOAD, 0)

            methodVisitor.visitMethodInsn(
                Opcodes.INVOKESPECIAL,
                "java/lang/Object",
                "<init>",
                "()V",
                false
            )

            methodVisitor.visitVarInsn(Opcodes.ALOAD, 1)

            methodVisitor.visitFieldInsn(
                Opcodes.PUTSTATIC,
                name,
                "_prefix",
                "Lkotlin/jvm/functions/Function0;"
            )

            methodVisitor.visitVarInsn(Opcodes.ALOAD, 2)

            methodVisitor.visitFieldInsn(
                Opcodes.PUTSTATIC,
                name,
                "_postfix",
                "Lkotlin/jvm/functions/Function0;"
            )

            methodVisitor.visitInsn(Opcodes.RETURN)

            methodVisitor.visitMaxs(0, 0)
            methodVisitor.visitEnd()
        }

        // enter
        run {
            methodVisitor = classWriter.visitMethod(
                Opcodes.ACC_PUBLIC + Opcodes.ACC_STATIC,
                "enter",
                "()V",
                null,
                null
            )

            methodVisitor.visitAnnotation(
                "Lnet/bytebuddy/asm/Advice\$OnMethodEnter;",
                true
            )

            methodVisitor.visitFieldInsn(
                Opcodes.GETSTATIC,
                name,
                "_prefix",
                "Lkotlin/jvm/functions/Function0;"
            )

            methodVisitor.visitMethodInsn(
                Opcodes.INVOKEINTERFACE,
                "kotlin/jvm/functions/Function0",
                "invoke",
                "()Ljava/lang/Object;",
                true
            )

            methodVisitor.visitInsn(Opcodes.POP)

            methodVisitor.visitInsn(Opcodes.RETURN)
            methodVisitor.visitMaxs(0, 0)
        }

        // exit
        run {
            methodVisitor = classWriter.visitMethod(
                Opcodes.ACC_PUBLIC + Opcodes.ACC_STATIC,
                "exit",
                "()V",
                null,
                null
            )

            methodVisitor.visitAnnotation(
                "Lnet/bytebuddy/asm/Advice\$OnMethodExit;",
                true
            )

            methodVisitor.visitFieldInsn(
                Opcodes.GETSTATIC,
                name,
                "_postfix",
                "Lkotlin/jvm/functions/Function0;"
            )

            methodVisitor.visitMethodInsn(
                Opcodes.INVOKEINTERFACE,
                "kotlin/jvm/functions/Function0",
                "invoke",
                "()Ljava/lang/Object;",
                true
            )

            methodVisitor.visitInsn(Opcodes.POP)

            methodVisitor.visitInsn(Opcodes.RETURN)
            methodVisitor.visitMaxs(0, 0)
        }

        classWriter.visitEnd()

        val bytes = classWriter.toByteArray()

        println(bytes.joinToString("") { "%02x".format(it) })
        return bytes
    }
}