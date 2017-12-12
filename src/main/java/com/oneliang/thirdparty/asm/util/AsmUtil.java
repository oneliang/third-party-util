package com.oneliang.thirdparty.asm.util;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.optimizer.ClassConstantsCollector;
import org.objectweb.asm.optimizer.ConstantPool;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.LocalVariableAnnotationNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeAnnotationNode;
import org.objectweb.asm.util.TraceClassVisitor;

import com.oneliang.Constant;
import com.oneliang.util.common.StringUtil;
import com.oneliang.util.file.FileUtil;
import com.oneliang.util.logging.Logger;
import com.oneliang.util.logging.LoggerManager;

public final class AsmUtil {

    private static final Logger logger = LoggerManager.getLogger(AsmUtil.class);

    private static final String REGEX = "^((java[x]?)|(android)|(junit)|(dalvik))/[a-zA-Z0-9_/\\$]*$";
    private static final String ANDROID_SUPPORT_REGEX = "^android/support/[a-zA-Z0-9_/\\$]*$";
    private static final String BASIC_CLASS_REGEX = "^\\[*[ZCBSIFDJV]$";
    private static final String CLASS_ARRAY_REGEX = "^(\\[*L)";
    public static final int MODIFIER_ANNOTATION = 0x00002000;

    /**
     * trace class
     * 
     * @param classFullFilename
     * @param printWriter
     */
    public static void traceClass(String classFullFilename, PrintWriter printWriter) {
        TraceClassVisitor traceClassVisitor = new TraceClassVisitor(printWriter);
        ClassReader classReader;
        try {
            classReader = new ClassReader(new FileInputStream(classFullFilename));
            classReader.accept(traceClassVisitor, ClassReader.SKIP_DEBUG);
        } catch (Exception e) {
            throw new AsmUtilException(e);
        }
    }

    /**
     * find class description
     * 
     * @param classFullFilename
     * @return ClassDescription
     */
    public static ClassDescription findClassDescription(String classFullFilename) {
        return findClassDescription(classFullFilename, null);
    }

    /**
     * find class description
     * 
     * @param classFullFilename
     * @param fieldProcessor
     * @return ClassDescription
     */
    public static ClassDescription findClassDescription(String classFullFilename, FieldProcessor fieldProcessor) {
        ClassDescription classDescription = null;
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(classFullFilename);
            classDescription = findClassDescription(inputStream, fieldProcessor);
        } catch (Exception e) {
            throw new AsmUtilException(e);
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (Exception e) {
                    throw new AsmUtilException(e);
                }
            }
        }
        return classDescription;
    }

    /**
     * find class description
     * 
     * @param inputStream
     * @return ClassDescription
     */
    public static ClassDescription findClassDescription(InputStream inputStream) {
        return findClassDescription(inputStream, null);
    }

    /**
     * find class description
     * 
     * @param inputStream
     * @param fieldProcessor
     * @return ClassDescription
     */
    public static ClassDescription findClassDescription(InputStream inputStream, FieldProcessor fieldProcessor) {
        ClassReader classReader = null;
        try {
            classReader = new ClassReader(inputStream);
        } catch (Exception e) {
            throw new AsmUtilException(e);
        }
        ClassNode classNode = new ClassNode();
        ConstantPool constantPool = new ConstantPool();
        ClassConstantsCollector classConstantsCollector = new ClassConstantsCollector(classNode, constantPool);
        classReader.accept(classConstantsCollector, 0);
        // class name
        String className = classNode.name;
        // class super class
        String superClassName = classNode.superName;
        ClassDescription classDescription = new ClassDescription();
        classDescription.className = className;
        classDescription.sourceFile = classNode.sourceFile;
        // class access
        classDescription.access = classNode.access;
        if (Modifier.isPublic(classDescription.access)) {
            classDescription.setPublicClass(true);
        }
        if ((classDescription.access & MODIFIER_ANNOTATION) == MODIFIER_ANNOTATION) {
            classDescription.setAnnotationClass(true);
        }
        addDependClassName(classDescription, className);

        classDescription.superClassName = superClassName;
        addDependClassName(classDescription, superClassName);

        // class interfaces
        if (classNode.interfaces != null) {
            for (String string : classNode.interfaces) {
                classDescription.interfaceList.add(string);
                addDependClassName(classDescription, string);
            }
        }
        // class outer class
        if (StringUtil.isNotBlank(classNode.outerClass) && StringUtil.isNotBlank(classNode.outerMethod) && StringUtil.isNotBlank(classNode.outerMethodDesc)) {
            String referenceMethodName = classNode.outerClass + Constant.Symbol.DOT + classNode.outerMethod + Constant.Symbol.DOT + classNode.outerMethodDesc;
            if (!classDescription.referenceMethodNameMap.containsKey(referenceMethodName)) {
                classDescription.referenceMethodNameMap.put(referenceMethodName, referenceMethodName);
            }
        }

        // class constant pool
        Iterator<Entry<org.objectweb.asm.optimizer.Constant, org.objectweb.asm.optimizer.Constant>> iterator = constantPool.entrySet().iterator();
        while (iterator.hasNext()) {
            Entry<org.objectweb.asm.optimizer.Constant, org.objectweb.asm.optimizer.Constant> entry = iterator.next();
            if (entry.getValue().type == 'C') {
                String constantClassName = entry.getValue().strVal1;
                if (!StringUtil.isMatchRegex(constantClassName, BASIC_CLASS_REGEX) && !constantClassName.equals(classNode.superName)) {
                    Type type = Type.getType(constantClassName);
                    String internalName = type.getInternalName();
                    addDependClassName(classDescription, internalName);
                }
            } else if (entry.getValue().type == 'G') {
                String referenceFieldName = entry.getValue().strVal1 + Constant.Symbol.DOT + entry.getValue().strVal2 + Constant.Symbol.DOT + entry.getValue().objVal3;
                if (!classDescription.referenceFieldNameMap.containsKey(referenceFieldName)) {
                    classDescription.referenceFieldNameMap.put(referenceFieldName, referenceFieldName);
                    if (fieldProcessor != null) {
                        String referenceFieldNameWithoutType = entry.getValue().strVal1 + Constant.Symbol.DOT + entry.getValue().strVal2;
                        fieldProcessor.process(referenceFieldNameWithoutType, classDescription);
                    }
                }
            } else if (entry.getValue().type == 'M') {
                String referenceMethodName = entry.getValue().strVal1 + Constant.Symbol.DOT + entry.getValue().strVal2 + Constant.Symbol.DOT + entry.getValue().objVal3;
                if (!classDescription.referenceMethodNameMap.containsKey(referenceMethodName)) {
                    classDescription.referenceMethodNameMap.put(referenceMethodName, referenceMethodName);
                }
            }
        }
        // class annotation
        List<AnnotationNode> annotationNodeList = classNode.visibleAnnotations;
        if (annotationNodeList != null) {
            for (AnnotationNode annotationNode : annotationNodeList) {
                List<String> annotationList = parseAnnotationInAnnotationNode(annotationNode);
                for (String annotationClassName : annotationList) {
                    addDependClassName(classDescription, annotationClassName);
                }
            }
        }
        // class field
        List<FieldNode> fieldNodeList = classNode.fields;
        if (fieldNodeList != null) {
            for (FieldNode fieldNode : fieldNodeList) {
                String fieldName = className + Constant.Symbol.DOT + fieldNode.name + Constant.Symbol.DOT + fieldNode.desc;
                classDescription.fieldNameList.add(fieldName);
                classDescription.fieldFullNameList.add(fieldName + Constant.Symbol.DOT + fieldNode.access);
                // save field desc
                if (!Modifier.isPublic(fieldNode.access)) {
                    if (Modifier.isPrivate(fieldNode.access)) {
                        classDescription.privateFieldNameList.add(fieldName);
                    } else if (Modifier.isProtected(fieldNode.access)) {
                        classDescription.protectedFieldNameList.add(fieldName);
                    } else {
                        classDescription.friendlyFieldNameList.add(fieldName);
                    }
                }
                // find depend class
                String descriptor = fieldNode.desc;
                if (!StringUtil.isMatchRegex(descriptor, BASIC_CLASS_REGEX)) {
                    Type type = Type.getType(descriptor);
                    String internalName = type.getInternalName();
                    addDependClassName(classDescription, internalName);
                }
            }
        }
        // class method
        List<MethodNode> methodNodeList = classNode.methods;
        if (methodNodeList != null) {
            for (MethodNode methodNode : methodNodeList) {
                String methodName = className + Constant.Symbol.DOT + methodNode.name + Constant.Symbol.DOT + methodNode.desc;
                classDescription.methodNameList.add(methodName);
                classDescription.methodFullNameList.add(methodName + Constant.Symbol.DOT + methodNode.access);
                // save method desc
                if (!Modifier.isPublic(methodNode.access) && !methodNode.name.equals("<clinit>")) {
                    if (Modifier.isPrivate(methodNode.access)) {
                        classDescription.privateMethodNameList.add(methodName);
                    } else if (Modifier.isProtected(methodNode.access)) {
                        classDescription.protectedMethodNameList.add(methodName);
                    } else {
                        classDescription.friendlyMethodNameList.add(methodName);
                    }
                }
                // find depend class
                // method argument
                Type[] argumentTypes = Type.getArgumentTypes(methodNode.desc);
                Type returnType = Type.getReturnType(methodNode.desc);
                if (argumentTypes != null) {
                    for (Type type : argumentTypes) {
                        String descriptor = type.getDescriptor();
                        if (!StringUtil.isMatchRegex(descriptor, BASIC_CLASS_REGEX)) {
                            String internalName = type.getInternalName();
                            addDependClassName(classDescription, internalName);
                        }
                    }
                }
                // method return
                if (returnType != null) {
                    String descriptor = returnType.getDescriptor();
                    if (!StringUtil.isMatchRegex(descriptor, BASIC_CLASS_REGEX)) {
                        String internalName = returnType.getInternalName();
                        addDependClassName(classDescription, internalName);
                    }
                }
                // method visible annotation
                List<AnnotationNode> methodVisibleAnnotationNodeList = methodNode.visibleAnnotations;
                if (methodVisibleAnnotationNodeList != null) {
                    for (AnnotationNode annotationNode : methodVisibleAnnotationNodeList) {
                        List<String> annotationList = parseAnnotationInAnnotationNode(annotationNode);
                        for (String annotationClassName : annotationList) {
                            addDependClassName(classDescription, annotationClassName);
                        }
                    }
                }
                // method visible local variable annotation
                List<LocalVariableAnnotationNode> methodVisibleLocalVariableAnnotationNodeList = methodNode.visibleLocalVariableAnnotations;
                if (methodVisibleLocalVariableAnnotationNodeList != null) {
                    for (LocalVariableAnnotationNode localVariableAnnotationNode : methodVisibleLocalVariableAnnotationNodeList) {
                        // logger.verbose(localVariableAnnotationNode.desc);
                    }
                }
                // method visible parameter annotation
                List<AnnotationNode>[] methodVisibleParameterAnnotationNodeArray = methodNode.visibleParameterAnnotations;
                if (methodVisibleParameterAnnotationNodeArray != null) {
                    for (List<AnnotationNode> parameterAnnotationNodeList : methodVisibleParameterAnnotationNodeArray) {
                        if (parameterAnnotationNodeList != null) {
                            for (AnnotationNode parameterAnnotationNode : parameterAnnotationNodeList) {
                                List<String> annotationList = parseAnnotationInAnnotationNode(parameterAnnotationNode);
                                for (String annotationClassName : annotationList) {
                                    addDependClassName(classDescription, annotationClassName);
                                }
                            }
                        }
                    }
                }
                // method visible type annotation
                List<TypeAnnotationNode> methodTypeAnnotationNodeList = methodNode.visibleTypeAnnotations;
                if (methodTypeAnnotationNodeList != null) {
                    for (TypeAnnotationNode typeAnnotationNode : methodTypeAnnotationNodeList) {
                        // logger.verbose(typeAnnotationNode.desc);
                    }
                }
            }
        }

        return classDescription;
    }

    /**
     * find class description map with jar
     * 
     * @param allClassesJarFullFilename
     * @param referencedClassDescriptionListMap
     * @return Map<String,ClassDescription>
     */
    public static Map<String, ClassDescription> findClassDescriptionMapWithJar(String allClassesJarFullFilename, Map<String, List<ClassDescription>> referencedClassDescriptionListMap) {
        return findClassDescriptionMapWithJar(allClassesJarFullFilename, referencedClassDescriptionListMap, null);
    }

    /**
     * find class description map with jar
     * 
     * @param allClassesJarFullFilename
     * @param referencedClassDescriptionListMap
     * @return Map<String,ClassDescription>
     */
    public static Map<String, ClassDescription> findClassDescriptionMapWithJar(String allClassesJarFullFilename, Map<String, List<ClassDescription>> referencedClassDescriptionListMap, FieldProcessor fieldProcessor) {
        Map<String, ClassDescription> classDescriptionMap = new HashMap<String, ClassDescription>();
        try {
            ZipFile zipFile = new ZipFile(allClassesJarFullFilename);
            try {
                Enumeration<? extends ZipEntry> enumeration = zipFile.entries();
                while (enumeration.hasMoreElements()) {
                    ZipEntry zipEntry = enumeration.nextElement();
                    String zipEntryName = zipEntry.getName();
                    if (zipEntryName.endsWith(Constant.Symbol.DOT + Constant.File.CLASS)) {
                        InputStream inputStream = zipFile.getInputStream(zipEntry);
                        ClassDescription classDescription = findClassDescription(inputStream, fieldProcessor);
                        classDescriptionMap.put(zipEntryName, classDescription);
                        if (referencedClassDescriptionListMap != null) {
                            Iterator<Entry<String, String>> iterator = classDescription.dependClassNameMap.entrySet().iterator();
                            while (iterator.hasNext()) {
                                Entry<String, String> entry = iterator.next();
                                String dependClassName = entry.getValue();
                                dependClassName = dependClassName + Constant.Symbol.DOT + Constant.File.CLASS;
                                List<ClassDescription> classDescriptionList = null;
                                if (referencedClassDescriptionListMap.containsKey(dependClassName)) {
                                    classDescriptionList = referencedClassDescriptionListMap.get(dependClassName);
                                } else {
                                    classDescriptionList = new ArrayList<ClassDescription>();
                                    referencedClassDescriptionListMap.put(dependClassName, classDescriptionList);
                                }
                                classDescriptionList.add(classDescription);
                            }
                        }
                    }
                }
            } finally {
                zipFile.close();
            }
        } catch (Exception e) {
            throw new AsmUtilException(e);
        }
        return classDescriptionMap;
    }

    /**
     * find class description map
     * 
     * @param classesRootPath
     * @param referencedClassDescriptionListMap
     * @return Map<String,ClassDescription>
     */
    public static Map<String, ClassDescription> findClassDescriptionMap(String classesRootPath, Map<String, List<ClassDescription>> referencedClassDescriptionListMap) {
        Map<String, ClassDescription> classDescriptionMap = new HashMap<String, ClassDescription>();
        FileUtil.MatchOption matchOption = new FileUtil.MatchOption(classesRootPath);
        matchOption.fileSuffix = Constant.Symbol.DOT + Constant.File.CLASS;
        List<String> allClassFullFilenameList = FileUtil.findMatchFile(matchOption);
        if (allClassFullFilenameList != null) {
            classesRootPath = new File(classesRootPath).getAbsolutePath() + Constant.Symbol.SLASH_LEFT;
            for (String classFullFilename : allClassFullFilenameList) {
                classFullFilename = new File(classFullFilename).getAbsolutePath();
                ClassDescription classDescription = findClassDescription(classFullFilename);
                classDescriptionMap.put(classFullFilename, classDescription);
                if (referencedClassDescriptionListMap != null) {
                    Iterator<Entry<String, String>> iterator = classDescription.dependClassNameMap.entrySet().iterator();
                    while (iterator.hasNext()) {
                        Entry<String, String> entry = iterator.next();
                        String dependClassName = entry.getValue();
                        // String dependClassFullFilename=new
                        // File(classesRootPath+dependClassName+Constant.Symbol.DOT+Constant.File.CLASS).getAbsolutePath();
                        dependClassName = dependClassName + Constant.Symbol.DOT + Constant.File.CLASS;
                        List<ClassDescription> classDescriptionList = null;
                        if (referencedClassDescriptionListMap.containsKey(dependClassName)) {
                            classDescriptionList = referencedClassDescriptionListMap.get(dependClassName);
                        } else {
                            classDescriptionList = new ArrayList<ClassDescription>();
                            referencedClassDescriptionListMap.put(dependClassName, classDescriptionList);
                        }
                        classDescriptionList.add(classDescription);
                    }
                }
            }
        }
        return classDescriptionMap;
    }

    /**
     * find class description map
     * 
     * @param classNameByteArrayMap
     * @param referencedClassDescriptionListMap
     * @return Map<String,ClassDescription>
     */
    public static Map<String, ClassDescription> findClassDescriptionMap(Map<String, byte[]> classNameByteArrayMap, Map<String, List<ClassDescription>> referencedClassDescriptionListMap) {
        Map<String, ClassDescription> classDescriptionMap = new HashMap<String, ClassDescription>();
        Iterator<Entry<String, byte[]>> classNameByteArrayEntryIterator = classNameByteArrayMap.entrySet().iterator();
        while (classNameByteArrayEntryIterator.hasNext()) {
            Entry<String, byte[]> classNameByteArrayEntry = classNameByteArrayEntryIterator.next();
            String className = classNameByteArrayEntry.getKey();
            byte[] byteArray = classNameByteArrayEntry.getValue();
            ClassDescription classDescription = findClassDescription(new ByteArrayInputStream(byteArray));
            classDescriptionMap.put(className, classDescription);
            if (referencedClassDescriptionListMap != null) {
                Iterator<Entry<String, String>> iterator = classDescription.dependClassNameMap.entrySet().iterator();
                while (iterator.hasNext()) {
                    Entry<String, String> entry = iterator.next();
                    String dependClassName = entry.getValue();
                    dependClassName = dependClassName + Constant.Symbol.DOT + Constant.File.CLASS;
                    List<ClassDescription> classDescriptionList = null;
                    if (referencedClassDescriptionListMap.containsKey(dependClassName)) {
                        classDescriptionList = referencedClassDescriptionListMap.get(dependClassName);
                    } else {
                        classDescriptionList = new ArrayList<ClassDescription>();
                        referencedClassDescriptionListMap.put(dependClassName, classDescriptionList);
                    }
                    classDescriptionList.add(classDescription);
                }
            }
        }
        return classDescriptionMap;
    }

    /**
     * add depend class name
     * 
     * @param classDescription
     * @param internalName
     */
    private static void addDependClassName(ClassDescription classDescription, String internalName) {
        String className = internalName;
        if (StringUtil.isMatchRegex(internalName, CLASS_ARRAY_REGEX)) {
            List<String> groupList = StringUtil.parseRegexGroup(internalName, CLASS_ARRAY_REGEX);
            if (groupList != null && !groupList.isEmpty()) {
                className = internalName.substring(groupList.get(0).length(), internalName.length() - 1);
            }
        }
        if (StringUtil.isMatchRegex(className, ANDROID_SUPPORT_REGEX) || !StringUtil.isMatchRegex(className, REGEX)) {
            if (!classDescription.dependClassNameMap.containsKey(className)) {

                if (Modifier.isInterface(classDescription.access) && Modifier.isPublic(classDescription.access)) {
                    if (isInnerInterface(classDescription.className, className)) {
                        return;
                    }
                }

                classDescription.dependClassNameMap.put(className, className);
                classDescription.dependClassNameList.add(className);
            }
        }
    }

    /**
     * is inner interface
     * 
     * @param interfaceName
     * @param className
     * @return boolean
     */
    private static boolean isInnerInterface(String interfaceName, String className) {
        if (StringUtil.isBlank(interfaceName) || StringUtil.isBlank(className)) {
            return false;
        }
        if (interfaceName.length() <= className.length()) {
            return false;
        }
        String interfacePrefixName = interfaceName.substring(0, className.length());
        if (!interfacePrefixName.equals(className)) {
            return false;
        }
        String interfaceSuffixName = interfaceName.substring(className.length(), interfaceName.length());
        if (interfaceSuffixName.startsWith(Constant.Symbol.DOLLAR)) {
            return true;
        }
        return false;
    }

    /**
     * parse annotation in annotation node
     * 
     * @param rootAnnotationNode
     * @return List<String>
     */
    private static List<String> parseAnnotationInAnnotationNode(AnnotationNode rootAnnotationNode) {
        Map<String, String> annotationMap = new HashMap<String, String>();
        List<String> annotationList = new ArrayList<String>();
        Queue<AnnotationNode> queue = new ConcurrentLinkedQueue<AnnotationNode>();
        queue.add(rootAnnotationNode);
        while (!queue.isEmpty()) {
            AnnotationNode annotationNode = queue.poll();
            String describe = annotationNode.desc;
            String internalName = Type.getType(describe).getInternalName();
            if (!annotationMap.containsKey(internalName)) {
                annotationMap.put(internalName, internalName);
                annotationList.add(internalName);
                if (annotationNode.values != null) {
                    for (Object object : annotationNode.values) {
                        if (object instanceof AnnotationNode) {
                            queue.add((AnnotationNode) object);
                        } else if (object instanceof List) {
                            for (Object subObject : (List<?>) object) {
                                if (subObject instanceof AnnotationNode) {
                                    queue.add((AnnotationNode) subObject);
                                }
                            }
                        }
                    }
                }
            }
        }
        return annotationList;
    }

    /**
     * is need to put into the same class loader
     * 
     * @param classDescription
     * @param referencedClassDescription,which
     *            class reference classDescription
     * @param classDescriptionMap
     * @return boolean
     */
    public static boolean isNeedToPutIntoTheSameClassLoader(ClassDescription classDescription, ClassDescription referencedClassDescription, Map<String, ClassDescription> classDescriptionMap) {
        boolean result = false;
        if (classDescription != null && referencedClassDescription != null) {
            String className = classDescription.className + Constant.Symbol.DOT + Constant.File.CLASS;
            String referencedClassName = referencedClassDescription.className + Constant.Symbol.DOT + Constant.File.CLASS;
            if (!referencedClassDescription.dependClassNameMap.containsKey(classDescription.className)) {
                return result;
            }
            // if depend class in the same package then
            // it may use it friendly method or
            // protected method or inner class(use
            // private method),others in not same
            // package must use the use public or
            // protected method,keyword 'extends'
            String classPackage = StringUtil.BLANK;
            int classNameSlashLeftLastIndex = className.lastIndexOf(Constant.Symbol.SLASH_LEFT);
            if (classNameSlashLeftLastIndex > 0) {
                classPackage = className.substring(0, classNameSlashLeftLastIndex);
            }
            String referencedClassPackage = StringUtil.BLANK;
            int referencedClassNameSlashLeftLastIndex = referencedClassName.lastIndexOf(Constant.Symbol.SLASH_LEFT);
            if (referencedClassNameSlashLeftLastIndex > 0) {
                referencedClassPackage = referencedClassName.substring(0, referencedClassNameSlashLeftLastIndex);
            }
            if (!classPackage.equals(referencedClassPackage)) {
                return result;
            }

            // not public class chain//&&
            // !referencedClassDescription.isPublicClass()
            // class has referenced class,no matter public or not need to put in
            // same class loader include annotation,annotation class has no
            // field or all method are public,if use
            // !classDescription.isPublicClass() &&
            // !referencedClassDescription.isPublicClass() will has a bug,a
            // public class may use a same package annotation(default modifier)
            if (!classDescription.isPublicClass()) {
                logger.verbose("\tIt is not public class:" + className);
                result = true;
                return result;
            }
            // has private field
            if (!classDescription.isNoPrivateField()) {
                for (String privateFieldName : classDescription.privateFieldNameList) {
                    if (referencedClassDescription.referenceFieldNameMap.containsKey(privateFieldName)) {
                        logger.verbose("\tReferenced class:" + referencedClassName + ", has use private field:" + privateFieldName);
                        result = true;
                        return result;
                    }
                }
            }
            // has private method
            if (!classDescription.isNoPrivateMethod()) {
                for (String privateMethodName : classDescription.privateMethodNameList) {
                    if (referencedClassDescription.referenceMethodNameMap.containsKey(privateMethodName)) {
                        logger.verbose("\tReferenced class:" + referencedClassName + ", has use private method:" + privateMethodName);
                        result = true;
                        return result;
                    }
                }
            }
            // has friendly field
            if (!classDescription.isNoFriendlyField()) {
                for (String friendlyFieldName : classDescription.friendlyFieldNameList) {
                    if (referencedClassDescription.referenceFieldNameMap.containsKey(friendlyFieldName)) {
                        logger.verbose("\tReferenced class:" + referencedClassName + ", has use friendly field:" + friendlyFieldName);
                        result = true;
                        return result;
                    }
                }
            }
            // has friendly method
            if (!classDescription.isNoFriendlyMethod()) {
                for (String friendlyMethodName : classDescription.friendlyMethodNameList) {
                    if (referencedClassDescription.referenceMethodNameMap.containsKey(friendlyMethodName)) {
                        logger.verbose("\tReferenced class:" + referencedClassName + ", has use friendly method:" + friendlyMethodName);
                        result = true;
                        return result;
                    }
                }
            }
            // same package,but no friendly reference, will do this
            // sentence,sub class can invoke protected fields and methods of
            // super class,no need to put into the same class loader
            if (isSuperClass(classDescription, referencedClassDescription, classDescriptionMap)) {
                logger.verbose("\tReferenced class:" + referencedClassName + ", is the sub class of " + className);
                result = false;
                return result;
            }
            // same package,not inherit,must check protected fields and methods
            // reference
            // has protected field
            if (!classDescription.isNoProtectedField()) {
                for (String protectedFieldName : classDescription.protectedFieldNameList) {
                    if (referencedClassDescription.referenceFieldNameMap.containsKey(protectedFieldName)) {
                        logger.verbose("\tReferenced class:" + referencedClassName + ", has use protected field:" + protectedFieldName);
                        result = true;
                        return result;
                    }
                }
            }
            // has protected method
            if (!classDescription.isNoProtectedMethod()) {
                for (String protectedMethodName : classDescription.protectedMethodNameList) {
                    if (referencedClassDescription.referenceMethodNameMap.containsKey(protectedMethodName)) {
                        logger.verbose("\tReferenced class:" + referencedClassName + ", has use protected method:" + protectedMethodName);
                        result = true;
                        return result;
                    }
                }
            }
        }
        return result;
    }

    /**
     * is super class, just class is it the super class of referenced class
     * 
     * @param classDescription
     * @param referencedClassDescription
     * @param classDescriptionMap
     * @return boolean
     */
    public static boolean isSuperClass(ClassDescription classDescription, ClassDescription referencedClassDescription, Map<String, ClassDescription> classDescriptionMap) {
        String className = classDescription.className + Constant.Symbol.DOT + Constant.File.CLASS;
        if (StringUtil.isBlank(referencedClassDescription.superClassName)) {
            return false;
        }
        Set<String> superClassNameSet = new HashSet<String>();
        String referencedSuperClassName = referencedClassDescription.superClassName + Constant.Symbol.DOT + Constant.File.CLASS;
        while (referencedSuperClassName != null) {
            if (classDescriptionMap.containsKey(referencedSuperClassName)) {
                ClassDescription referencedSuperClassDescription = classDescriptionMap.get(referencedSuperClassName);
                superClassNameSet.add(referencedSuperClassName);
                if (StringUtil.isBlank(referencedSuperClassDescription.superClassName)) {
                    break;
                }
                referencedSuperClassName = referencedSuperClassDescription.superClassName + Constant.Symbol.DOT + Constant.File.CLASS;
            } else {// may be library class,class description must be program
                    // class,and library class must be not depend program class
                break;
            }
        }
        return superClassNameSet.contains(className);
    }

    /**
     * find all depend class name map
     * 
     * @param classesJar
     * @param rootClassNameSet
     * @param deep
     * @return Map<String,String>
     */
    public static Map<String, String> findAllDependClassNameMap(String classesJar, Set<String> rootClassNameSet, boolean deep) {
        Map<String, List<ClassDescription>> referencedClassDescriptionListMap = new HashMap<String, List<ClassDescription>>();
        Map<String, ClassDescription> classDescriptionMap = findClassDescriptionMapWithJar(classesJar, referencedClassDescriptionListMap);
        Map<String, String> allClassNameMap = new HashMap<String, String>();
        Set<String> classNameKeySet = classDescriptionMap.keySet();
        for (String className : classNameKeySet) {
            allClassNameMap.put(className, className);
        }
        return findAllDependClassNameMap(rootClassNameSet, classDescriptionMap, referencedClassDescriptionListMap, allClassNameMap, deep);
    }

    /**
     * find all depend class name map
     * 
     * @param rootClassNameSet
     * @param classDescriptionMap
     * @param referencedClassDescriptionListMap
     * @param allClassNameMap
     * @param deep
     * @return Map<String,String>
     */
    public static Map<String, String> findAllDependClassNameMap(Set<String> rootClassNameSet, Map<String, ClassDescription> classDescriptionMap, Map<String, List<ClassDescription>> referencedClassDescriptionListMap, Map<String, String> allClassNameMap, boolean deep) {
        Map<String, String> dependClassNameMap = new HashMap<String, String>();
        Queue<String> queue = new ConcurrentLinkedQueue<String>();
        queue.addAll(rootClassNameSet);
        int count = 0;
        final int maxCount = 500;
        while (!queue.isEmpty()) {
            String className = queue.poll();
            ClassDescription classDescription = classDescriptionMap.get(className);
            if (classDescription == null) {
                logger.verbose("\tclass is not exist:" + className);
                continue;
            }
            logger.verbose(className + "," + classDescription.access + "," + Modifier.isPublic(classDescription.access) + "," + (!classDescription.isNoPrivateField()) + "," + (!classDescription.isNoFriendlyField()) + "," + (!classDescription.isNoProtectedField()) + "," + (!classDescription.isNoPrivateMethod()) + "," + (!classDescription.isNoFriendlyMethod()) + ","
                    + (!classDescription.isNoProtectedMethod()));
            if (!dependClassNameMap.containsKey(className)) {
                dependClassNameMap.put(className, className);
            }
            for (String dependClassName : classDescription.dependClassNameMap.keySet()) {
                dependClassName = dependClassName + Constant.Symbol.DOT + Constant.File.CLASS;
                logger.verbose("\tdepend:" + dependClassName);
                if (dependClassNameMap.containsKey(dependClassName)) {
                    continue;
                }
                if (!allClassNameMap.containsKey(dependClassName)) {
                    continue;
                }
                // has found
                dependClassNameMap.put(dependClassName, dependClassName);
                if (deep) {
                    queue.add(dependClassName);
                } else if (count < maxCount) {
                    queue.add(dependClassName);
                }
                // set public chain
                // if(classDescriptionMap.containsKey(dependClassName)){
                // ClassDescription
                // dependClassDescription=classDescriptionMap.get(dependClassName);
                // if(!dependClassDescription.isPublicClass()&&classDescription.isPublicClassChain()){
                // classDescription.setPublicClassChain(false);
                // }
                // }
            }

            // find sub class or referenced class
            List<ClassDescription> referencedClassDescriptionList = referencedClassDescriptionListMap.get(className);
            if (referencedClassDescriptionList == null || referencedClassDescriptionList.isEmpty()) {
                logger.verbose("\tclass is unreferenced:" + className);
                continue;
            }

            if (!classDescription.isPublicClass() || !classDescription.isNoPrivateField() || !classDescription.isNoFriendlyField() || !classDescription.isNoProtectedField() || !classDescription.isNoPrivateMethod() || !classDescription.isNoFriendlyMethod() || !classDescription.isNoProtectedMethod()) {
                // if(classDescription!=null&&(!classDescription.isPublicClassChain()||!classDescription.isNoPrivateField()||!classDescription.isNoFriendlyField()||!classDescription.isNoProtectedField()||!classDescription.isNoPrivateMethod()||!classDescription.isNoFriendlyMethod()||!classDescription.isNoProtectedMethod())){
                for (ClassDescription referencedClassDescription : referencedClassDescriptionList) {
                    String referencedClassName = referencedClassDescription.className + Constant.Symbol.DOT + Constant.File.CLASS;
                    if (dependClassNameMap.containsKey(referencedClassName)) {
                        continue;
                    }
                    // 1.sub class(no friendly field and method or
                    // have),2.same package(independence or depend)
                    // only focus the same package,no matter the
                    // same package or not the same package

                    // if depend class in the same package then
                    // it may use it friendly method or
                    // protected method or inner class(use
                    // private method),others in not same
                    // package must use the use public or
                    // protected method,keyword 'extends'
                    // if(!classDescription.isPublicClassChain()&&referencedClassDescription.isPublicClassChain()){
                    // referencedClassDescription.setPublicClassChain(false);
                    // }
                    boolean result = isNeedToPutIntoTheSameClassLoader(classDescription, referencedClassDescription, classDescriptionMap);
                    logger.verbose("\tNeed to put into the same class loader:" + result + ",class:" + className + ",referenced class:" + referencedClassName);
                    if (!result) {
                        continue;
                    }
                    if (dependClassNameMap.containsKey(referencedClassName)) {
                        continue;
                    }
                    if (!allClassNameMap.containsKey(referencedClassName)) {
                        continue;
                    }
                    dependClassNameMap.put(referencedClassName, referencedClassName);
                    if (deep) {
                        queue.add(referencedClassName);
                    } else if (count < maxCount) {
                        queue.add(referencedClassName);
                    }
                }
            }

            count++;
        }
        return dependClassNameMap;
    }

    /**
     * AsmUtilException
     */
    public static final class AsmUtilException extends RuntimeException {
        private static final long serialVersionUID = 73059698225184574L;

        public AsmUtilException(Throwable cause) {
            super(cause);
        }
    }

    /**
     * FieldProcessor
     */
    public static abstract interface FieldProcessor {
        /**
         * process
         * 
         * @param referenceFieldNameWithoutType
         * @param classDescription
         */
        public abstract void process(String referenceFieldNameWithoutType, ClassDescription classDescription);
    }
}
