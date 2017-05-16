package com.oneliang.thirdparty.test.asm;

import java.util.Map;
import java.util.Set;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * @author oneliang
 *         internalClassName[com/oneliang/class]+"."+methodName[method]+
 *         methodDescription[(Ljava/lang/String;)V]
 */
public class PathClassVisitor extends ClassVisitor {

    private Map<String, Set<String>> containMap = null;
    private Map<String, String> methodMap = null;
    private boolean isInterface = false;
    private String className = null;
    private InstrumentMethod instrumentMethod = null;

    public PathClassVisitor(ClassVisitor classVisitor, InstrumentMethod instrumentMethod, Map<String, Set<String>> containMap) {
        this(classVisitor, instrumentMethod, containMap, null);
    }

    public PathClassVisitor(ClassVisitor classVisitor, InstrumentMethod instrumentMethod, Map<String, Set<String>> containMap, Map<String, String> methodMap) {
        super(Opcodes.ASM5, classVisitor);
        this.instrumentMethod = instrumentMethod;
        this.containMap = containMap;
        this.methodMap = methodMap;
    }

    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces);
        this.className = name;
        if ((access & (Opcodes.ACC_INTERFACE)) != 0) {
            isInterface = true;
        }
    }

    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        // vtable.
        if (!isInterface) {
            boolean isDirect = ((access & (Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC)) != 0) || name.equals("<init>");
            if (!isDirect) {

            }
        }
        MethodVisitor methodVisitor = super.visitMethod(access, name, desc, signature, exceptions);
        String methodSignature = this.className + "." + name + desc;
        if (this.methodMap != null) {
            methodSignature = this.methodMap.get(methodSignature);
        }
        if (methodVisitor == null || methodSignature == null || this.containMap == null || !this.containMap.containsKey(methodSignature)) {
            return methodVisitor;
        }
        methodVisitor = new PathMethodVisitor(methodVisitor, this.instrumentMethod, this.containMap.get(methodSignature), this.methodMap);
        return methodVisitor;
    }

    private static class PathMethodVisitor extends MethodVisitor {
        private InstrumentMethod instrumentMethod = null;
        private Set<String> containSet = null;
        private Map<String, String> methodMap = null;

        public PathMethodVisitor(MethodVisitor methodVisitor, InstrumentMethod instrumentMethod, Set<String> containSet, Map<String, String> methodMap) {
            super(Opcodes.ASM5, methodVisitor);
            this.instrumentMethod = instrumentMethod;
            this.containSet = containSet;
            this.methodMap = methodMap;
        }

        public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean sign) {
            super.visitMethodInsn(opcode, owner, name, desc, sign);
            String signature = owner + "." + name + desc;
            if (this.methodMap != null) {
                signature = this.methodMap.get(signature);
            }
            if (signature == null || this.containSet == null || !this.containSet.contains(signature)) {
                return;
            }
            if (this.instrumentMethod != null) {
                super.visitLdcInsn(owner);
                super.visitLdcInsn(name);
                super.visitLdcInsn(desc);
                super.visitMethodInsn(Opcodes.INVOKESTATIC, this.instrumentMethod.internalClassName, this.instrumentMethod.methodName, this.instrumentMethod.methodDescription, sign);
            }
        }
    }

    public static class InstrumentMethod {
        public final String internalClassName;
        public final String methodName;
        public final String methodDescription;

        public InstrumentMethod(String internalClassName, String methodName, String methodDescription) {
            this.internalClassName = internalClassName;
            this.methodName = methodName;
            this.methodDescription = methodDescription;
        }
    }
}
