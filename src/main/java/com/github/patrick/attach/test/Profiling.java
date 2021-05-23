package com.github.patrick.attach.test;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.ClassDefinition;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.Arrays;

public class Profiling {
    public static class Agent implements ClassFileTransformer {
        public static void agentmain(String s, Instrumentation i) throws IOException {
            System.out.println("Agent loaded!");

            Agent transformer = new Agent();
            i.addTransformer(transformer);

            byte[] bytes = getBytes();
            assert bytes != null;
            System.err.println(bytes.length);
            try {
                i.redefineClasses(new ClassDefinition(Test.class, bytes));
            } catch (Exception e) {
                e.printStackTrace();
                System.err.println("Failed to redefine class!");
            }
        }

        @Override
        public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) {
            if (!className.startsWith("com/github/patrick/attach/test/Test")) {
                return classfileBuffer;
            }

            byte[] result = classfileBuffer;

            try {
                ClassReader reader = new ClassReader(result);
                ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
                ProfileClassAdapter profiler = new ProfileClassAdapter(writer, className);
                reader.accept(profiler, 0);
                result = writer.toByteArray();
                System.err.println("Returning reinstrumented class: " + className);
            } catch (Throwable e) {
                e.printStackTrace();
            }
            return result;
        }

        private static byte[] getBytes() throws IOException {
            String name = Test.class.getName().replace('.', '/') + ".class";
            InputStream stream = Test.class.getClassLoader().getResourceAsStream(name);

            if (stream != null) {
                byte[] buffer = new byte[Math.max(1024, stream.available())];
                int offset = 0;

                for (int bytesRead; -1 != (bytesRead = stream.read(buffer, offset, buffer.length - offset)); ) {
                    offset += bytesRead;

                    if (offset == buffer.length) {
                        buffer = Arrays.copyOf(buffer, buffer.length + Math.max(stream.available(), buffer.length >> 1));
                    }
                }

                return (offset == buffer.length) ? buffer : Arrays.copyOf(buffer, offset);
            } else {
                return null;
            }
        }
    }

    public static class ProfileClassAdapter extends ClassVisitor {
        private final String _className;

        public ProfileClassAdapter(ClassVisitor visitor, String className) {
            super(Opcodes.ASM9, visitor);
            _className = className;
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
            if (name.contains("method")) {
                MethodVisitor methodVisitor = super.visitMethod(access, name, descriptor, signature, exceptions);
                return new ProfileMethodAdapter(methodVisitor, _className, name);
            } else {
                return super.visitMethod(access, name, descriptor, signature, exceptions);
            }
        }
    }

    public static class ProfileMethodAdapter extends MethodVisitor {

        public ProfileMethodAdapter(MethodVisitor visitor, String className, String methodName) {
            super(Opcodes.ASM9, visitor);

            System.out.println("Profiled " + className + " in class " + methodName + ".");
        }

        @Override
        public void visitCode() {
            System.err.println("WA PREFIX SHOULD WORK HERE");
            super.visitCode();
        }

        @Override
        public void visitInsn(int inst) {
            switch (inst) {
                case Opcodes.ARETURN:
                case Opcodes.DRETURN:
                case Opcodes.FRETURN:
                case Opcodes.IRETURN:
                case Opcodes.LRETURN:
                case Opcodes.RETURN:
                case Opcodes.ATHROW:
                    System.err.println("WA POSTFIX SHOULD WORK HERE");
                    break;
                default:
                    break;
            }
            super.visitInsn(inst);
        }
    }
}
