package com.oneliang.thirdparty.test.annotation;

import java.util.Iterator;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;

@SupportedAnnotationTypes("com.oneliang.test.java.annotation.TestAnnotation")
@SupportedSourceVersion(SourceVersion.RELEASE_7)
public class AnnotationProcessor extends AbstractProcessor {

	public synchronized void init(ProcessingEnvironment processingEnv) {
		super.init(processingEnv);
		System.out.println("init");
	}

	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
		System.out.println("process");
		for(TypeElement typeElement:annotations){
			System.out.println(typeElement.getSimpleName());
			Iterator<? extends Element> iterator=roundEnv.getElementsAnnotatedWith(typeElement).iterator();
			while(iterator.hasNext()){
				Element element=iterator.next();
				System.out.println(element.getKind().isClass()+element.toString());
			}
		}
		return true;
	}
}
