package com.oneliang.thirdparty.test.asm;

import java.io.FileInputStream;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

import com.oneliang.thirdparty.asm.util.AsmUtil;
import com.oneliang.thirdparty.test.asm.PathClassVisitor.InstrumentMethod;
import com.oneliang.util.file.FileUtil;

public class Main {

    public static void main(String[] args) throws Exception {
        AsmUtil.traceClass("/E:/Dandelion/java/githubWorkspace/third-party-util/src/test/java/A.class", new PrintWriter(System.out));
//        AsmUtil.traceClass("/D:/Dandelion/git/wechat/app/build/intermediates/classes/debug/com/tencent/mm/kiss/layout/InflateViewRecycler$2.class", new PrintWriter(System.out));
        System.exit(0);
        // System.out.println(Generator.MD5File("C:/Users/oneliang/Downloads/classes6.dex"));
        // System.exit(0);
        new TestMethod().a();

//        InstrumentMethod instrumentMethod = new InstrumentMethod("com/oneliang/thirdparty/test/asm/PathRecorder", "record", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V");
//        Map<String, Set<String>> containMap = new HashMap<String, Set<String>>();
//        Set<String> invokeMethodSet = new HashSet<String>();
//        invokeMethodSet.add("java/io/PrintStream.println(Ljava/lang/String;)V");
//        invokeMethodSet.add("com/oneliang/thirdparty/test/asm/TestMethod.b()V");
//        containMap.put("com/oneliang/thirdparty/test/asm/TestMethod.a()V", invokeMethodSet);
//
//        String classFullFilename = "E:/Dandelion/java/githubWorkspace/third-party-util/bin/com/oneliang/thirdparty/test/asm/TestMethod.class";
//        // AsmUtil.traceClass(classFullFilename, new PrintWriter(System.out));
//        ClassReader classReader = new ClassReader(new FileInputStream(classFullFilename));
//        ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);
//        PathClassVisitor pathClassVisitor = new PathClassVisitor(classWriter, instrumentMethod, containMap);
//        classReader.accept(pathClassVisitor, 0);
//        FileUtil.writeFile(classFullFilename, classWriter.toByteArray());
//        AsmUtil.traceClass(classFullFilename, new PrintWriter(System.out));
    }
}
