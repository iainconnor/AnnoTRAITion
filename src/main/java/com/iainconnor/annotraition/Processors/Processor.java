package com.iainconnor.annotraition.Processors;

import com.iainconnor.annotraition.Use;
import com.iainconnor.annotraition.demo.DemoTrait;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeMirror;
import javax.tools.JavaFileObject;
import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;

public class Processor {
	protected static final String TRAIT_NAME_EXTENSION = "Traited";

	private static TypeMirror getClassValue ( Use annotation ) {
		try {
			annotation.value();
		} catch (MirroredTypeException mte) {
			return mte.getTypeMirror();
		}

		return null;
	}

	public static void generateClass ( String traitedClass, String traitedPackage, ArrayList<Use> traits, ProcessingEnvironment processingEnvironment ) {
		try {
			String traitedClassName = traitedClass + TRAIT_NAME_EXTENSION;

			JavaFileObject fileObject = processingEnvironment.getFiler().createSourceFile(traitedClassName);
			BufferedWriter writer = new BufferedWriter(fileObject.openWriter());

			writer.append("package " + traitedPackage + ";");
			writer.newLine();
			writer.newLine();

			writer.append("public class " + traitedClassName + " extends " + traitedClass + " {");
			writer.newLine();

			for (Use use : traits) {

				// @TODO This needs to pull from use.value();
				// See http://blog.retep.org/2009/02/13/getting-class-values-from-annotations-in-an-annotationprocessor/ for possible fixes
				Class trait = DemoTrait.class;
				String traitClassName = trait.getSimpleName().toString();

				String variableName = use.localVariable();
				if (variableName.equals("")) {
					variableName = traitClassName.substring(0, 1).toLowerCase() + traitClassName.substring(1);
				}
				writer.append("\tprotected " + traitClassName + " " + variableName + ";");
				writer.newLine();
				writer.newLine();

				for (Method method : trait.getDeclaredMethods()) {
					writer.append("\t// Generated by AnnoTRAITion");
					writer.newLine();
					String methodSignature = method.toString().replaceFirst(trait.getName().toString() + ".", "");
					writer.append("\t" + methodSignature + " {");
					writer.newLine();
					writer.append("\t\tthis." + variableName + "." + method.getName());
					writer.append("(");

					Class[] parameters = method.getParameterTypes();
					for (int i = 0; i < parameters.length; i++) {
						if (i > 0) {
							writer.append(", ");
						}
						writer.append(parameters[i].getName());
					}

					writer.append(");");
					writer.newLine();
					writer.append("\t}");
					writer.newLine();
				}
			}

			writer.append("}");
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
