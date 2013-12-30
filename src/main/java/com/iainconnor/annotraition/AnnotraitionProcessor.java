package com.iainconnor.annotraition;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.util.Set;

@SupportedAnnotationTypes ("Annotration")
public class AnnotraitionProcessor extends AbstractProcessor {

	@Override
	public boolean process ( Set<? extends TypeElement> typeElements, RoundEnvironment roundEnvironment ) {
		for (Element element : roundEnvironment.getElementsAnnotatedWith(Annotraition.class)) {
			Element classElement = element.getEnclosingElement();
			if (classElement.getKind() == ElementKind.CLASS) {
				Element packageElement = element.getEnclosingElement();
				if (packageElement.getKind() == ElementKind.PACKAGE) {
					try {
						JavaFileObject javaFileObject = processingEnv.getFiler().createSourceFile(((TypeElement) classElement).getQualifiedName() + "Trait");

						// @TODO, this needs to be moved around to support multiple traits in the same file
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}

		return true;
	}
}
