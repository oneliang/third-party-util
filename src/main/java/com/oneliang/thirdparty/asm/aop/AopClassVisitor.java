package com.oneliang.thirdparty.asm.aop;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;

import com.oneliang.Constants;

public class AopClassVisitor extends ClassVisitor {

	private String className = null;
	private boolean isInterface = false;
	private ClassVisitor classVisitor = null;
	private Jointer jointer = null;

	public AopClassVisitor(ClassVisitor classVisitor) {
		super(Opcodes.ASM5, classVisitor);
		this.classVisitor = classVisitor;
	}

	public AopClassVisitor(ClassVisitor classVisitor, Jointer jointer) {
		this(classVisitor);
		this.jointer = jointer;
		// checkJointer();
	}

	@SuppressWarnings("unused")
	private void checkJointer() {
		if (this.jointer != null) {
			String classMethodName = this.jointer.getStaticMethodBefore();
			checkJointerMethod(classMethodName);
			classMethodName = this.jointer.getStaticMethodAfter();
			checkJointerMethod(classMethodName);
		}
	}

	private void checkJointerMethod(String classMethodName) {
		if (classMethodName != null) {
			int lastIndex = classMethodName.lastIndexOf(Constants.Symbol.DOT);
			String className = classMethodName.substring(0, lastIndex);
			String methodName = classMethodName.substring(lastIndex + 1, classMethodName.length());
			try {
				Class<?> clazz = Class.forName(className);
				Method method = clazz.getMethod(methodName, new Class<?>[] {});
				int modifiers = method.getModifiers();
				if (!((modifiers & Modifier.PUBLIC) == Modifier.PUBLIC && (modifiers & Modifier.STATIC) == Modifier.STATIC)) {
					throw new RuntimeException(classMethodName + " is not a public static method.");
				}
			} catch (ClassNotFoundException e) {
				throw new RuntimeException(className + " is not found.", e);
			} catch (NoSuchMethodException e) {
				throw new RuntimeException("Can not found " + classMethodName + "(),method must be no arguments.", e);
			} catch (SecurityException e) {
				throw new RuntimeException(classMethodName + " is not security method.", e);
			}
		}
	}

	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		this.classVisitor.visit(version, access, name, signature, superName, interfaces);
		this.className = name;
		this.isInterface = (access & Opcodes.ACC_INTERFACE) == Opcodes.ACC_INTERFACE;
	}

	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		MethodVisitor methodVisitor = this.classVisitor.visitMethod(access, name, desc, signature, exceptions);
		if (!name.equals("<init>") && !isInterface && methodVisitor != null) {
			if (this.jointer != null && this.jointer.needToJoint(access, name, desc, signature, exceptions)) {
				methodVisitor = new AopMethodVisitor(methodVisitor, access, name, desc, signature, exceptions, this.jointer);
			}
		}
		return methodVisitor;
	}

	class AopMethodVisitor extends MethodVisitor {

		private MethodVisitor methodVisitor = null;
		private Jointer jointer = null;
		private Type[] argumentTypes = null;
		private List<AnnotationNode> annotationNodeList=null;

		public AopMethodVisitor(MethodVisitor methodVisitor, int access, String name, String desc, String signature, String[] exceptions) {
			super(Opcodes.ASM5, methodVisitor);
			this.methodVisitor = methodVisitor;
			this.argumentTypes = Type.getArgumentTypes(desc);
		}

		public AopMethodVisitor(MethodVisitor methodVisitor, int access, String name, String desc, String signature, String[] exceptions, Jointer jointer) {
			this(methodVisitor, access, name, desc, signature, exceptions);
			this.jointer = jointer;
		}

		public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
			if(this.annotationNodeList==null){
				this.annotationNodeList=new ArrayList<AnnotationNode>();
			}
			AnnotationNode annotationNode=new AnnotationNode(desc);
			this.annotationNodeList.add(annotationNode);
			return annotationNode;
		}

		public void visitEnd() {
			super.visitEnd();
			if(this.annotationNodeList!=null){
				for(AnnotationNode annotationNode:this.annotationNodeList){
					for(Object value:annotationNode.values){
						System.out.println(value);
					}
				}
			}
		}

		public void visitCode() {
			super.visitCode();
			if (this.jointer != null) {
//				if(this.argumentTypes!=null){
//					int argumentSize=this.argumentTypes.length;
//					this.visitIconst(argumentSize);
//					this.methodVisitor.visitTypeInsn(Opcodes.ANEWARRAY,"java/lang/Object");
//					for(int i=0;i<this.argumentTypes.length;i++){
//						this.methodVisitor.visitInsn(Opcodes.DUP);
//						this.visitIconst(i);
//						System.out.println(argumentTypes[i]);
//						this.methodVisitor.visitVarInsn(Opcodes.ALOAD,i+1);
//						this.methodVisitor.visitInsn(Opcodes.AASTORE);
//					}
//				}
				String classMethodName = this.jointer.getStaticMethodBefore();
				int lastIndex = classMethodName.lastIndexOf(Constants.Symbol.DOT);
				String className = classMethodName.substring(0, lastIndex);
				String methodName = classMethodName.substring(lastIndex + 1, classMethodName.length());
				className = className.replace(Constants.Symbol.DOT, Constants.Symbol.SLASH_LEFT);
//				this.methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, className, methodName, "([Ljava/lang/Object;)V", isInterface);
				this.methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, className, methodName, "()V", isInterface);
			}
		}

		public void visitInsn(int opcode) {
			if ((opcode >= Opcodes.IRETURN && opcode <= Opcodes.RETURN) || opcode == Opcodes.ATHROW) {
				if (this.jointer != null) {
					String classMethodName = this.jointer.getStaticMethodAfter();
					int lastIndex = classMethodName.lastIndexOf(Constants.Symbol.DOT);
					String className = classMethodName.substring(0, lastIndex);
					String methodName = classMethodName.substring(lastIndex + 1, classMethodName.length());
					className = className.replace(Constants.Symbol.DOT, Constants.Symbol.SLASH_LEFT);
					this.methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, className, methodName, "()V", isInterface);
				}
			}
			super.visitInsn(opcode);
		}

		private void visitIconst(int i){
			switch(i){
			case 0:
				this.methodVisitor.visitInsn(Opcodes.ICONST_0);
				break;
			case 1:
				this.methodVisitor.visitInsn(Opcodes.ICONST_1);
				break;
			case 2:
				this.methodVisitor.visitInsn(Opcodes.ICONST_2);
				break;
			case 3:
				this.methodVisitor.visitInsn(Opcodes.ICONST_3);
				break;
			case 4:
				this.methodVisitor.visitInsn(Opcodes.ICONST_4);
				break;
			case 5:
				this.methodVisitor.visitInsn(Opcodes.ICONST_5);
				break;
			default:
				this.methodVisitor.visitIntInsn(Opcodes.BIPUSH,i);
				break;
			}
		}
	}

	/**
	 * @return the className
	 */
	public String getClassName() {
		return className;
	}
}