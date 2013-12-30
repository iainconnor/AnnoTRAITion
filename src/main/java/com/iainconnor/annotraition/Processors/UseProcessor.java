package com.iainconnor.annotraition.Processors;

import com.iainconnor.annotraition.Use;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import java.util.ArrayList;
import java.util.Set;

@SupportedAnnotationTypes ("com.iainconnor.annotraition.Use")
public class UseProcessor extends AbstractProcessor {

	@Override
	public boolean process ( Set<? extends TypeElement> typeElements, RoundEnvironment roundEnvironment ) {
		for (Element element : roundEnvironment.getElementsAnnotatedWith(Use.class)) {
			// Enforce that Traits are only used on Class definitions
			if (element.getKind() == ElementKind.CLASS) {
				Use use = element.getAnnotation(Use.class);

				ArrayList<Use> traits = new ArrayList<Use>();
				traits.add(use);

				Package traitedPackage = null;
				Element packageElement = element.getEnclosingElement();
				if (packageElement.getKind() == ElementKind.PACKAGE) {
					traitedPackage = (Package) packageElement;
				}

				Processor.generateClass(element.getClass(), traitedPackage, traits, processingEnv);
			}
		}

		return true;
	}
}
