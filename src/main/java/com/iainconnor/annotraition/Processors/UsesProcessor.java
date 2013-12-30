package com.iainconnor.annotraition.Processors;

import com.iainconnor.annotraition.Use;
import com.iainconnor.annotraition.Uses;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import java.util.ArrayList;
import java.util.Set;

@SupportedAnnotationTypes ("com.iainconnor.annotraition.Uses")
public class UsesProcessor extends AbstractProcessor {

	@Override
	public boolean process ( Set<? extends TypeElement> typeElements, RoundEnvironment roundEnvironment ) {
		for (Element element : roundEnvironment.getElementsAnnotatedWith(Uses.class)) {
			// Enforce that Traits are only used on Class definitions
			if (element.getKind() == ElementKind.CLASS) {
				TypeElement classElement = (TypeElement) element;

				Uses uses = element.getAnnotation(Uses.class);

				ArrayList<Use> traits = new ArrayList<Use>();
				for (Use use : uses.value()) {
					traits.add(use);
				}

				String traitedPackage = null;
				Element packageElement = element.getEnclosingElement();
				if (packageElement.getKind() == ElementKind.PACKAGE) {
					traitedPackage = packageElement.toString();
				}

				Processor.generateClass(classElement.getSimpleName().toString(), traitedPackage, traits, processingEnv);
			}
		}

		return true;
	}
}
