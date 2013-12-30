package com.iainconnor.annotraition.Processors;

import com.iainconnor.annotraition.Use;

import javax.annotation.processing.ProcessingEnvironment;
import javax.tools.JavaFileObject;
import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;

public class Processor {
	protected static final String TRAIT_NAME_EXTENSION = "Traited";

	public static void generateClass ( Class traitedClass, Package traitedPackage, ArrayList<Use> traits, ProcessingEnvironment processingEnvironment ) {
		try {
			String className = traitedClass.getName().toString();
			String traitedClassName = className + TRAIT_NAME_EXTENSION;

			JavaFileObject fileObject = processingEnvironment.getFiler().createSourceFile(traitedClassName);
			BufferedWriter writer = new BufferedWriter(fileObject.openWriter());

			writer.append("package " + traitedPackage.getName() + ";");
			writer.newLine();
			writer.newLine();

			writer.append("public class " + traitedClassName + " extends " + traitedClass.getName().toString() + "{");
			writer.newLine();

			for (Use use : traits) {
				Class trait = use.value();
				String traitClassName = trait.getName().toString();

				String variableName = use.localVariable();
				if (variableName.equals("")) {
					variableName = traitClassName.substring(0, 1).toLowerCase() + traitClassName.substring(1);
				}
				writer.append("protected " + use.value().getName().toString() + " " + variableName + ";");
				writer.newLine();
				writer.newLine();

				for (Method method : trait.getMethods()) {
					writer.append(method.toString() + " {");
					writer.newLine();
					writer.append(variableName + "." + method.getName());
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
				}
			}

			writer.append("}");
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
