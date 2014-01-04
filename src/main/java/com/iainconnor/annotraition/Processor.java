package com.iainconnor.annotraition;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import javax.tools.JavaFileObject;
import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@SupportedAnnotationTypes ("com.iainconnor.annotraition.*")
@SupportedSourceVersion (SourceVersion.RELEASE_6)
public class Processor extends AbstractProcessor {
	protected static final String TRAIT_NAME_PREFIX = "_";
	protected static final String TRAIT_NAME_POSTFIX = "Traited";

	@Override
	public boolean process ( Set<? extends TypeElement> typeElements, RoundEnvironment roundEnvironment ) {
		HashMap<String, TraitInformation> traits = new HashMap<String, TraitInformation>();

		ArrayList<Element> elements = new ArrayList<Element>();

		for (Element element : roundEnvironment.getElementsAnnotatedWith(Uses.class)) {
			elements.add(element);
		}

		for (Element element : roundEnvironment.getElementsAnnotatedWith(Use.class)) {
			elements.add(element);
		}

		for (Element element : elements) {
			if (element.getKind() == ElementKind.CLASS) {
				TypeElement classElement = (TypeElement) element;
				String className = classElement.getSimpleName().toString();

				String packageName = null;
				Element packageElement = element.getEnclosingElement();
				if (packageElement.getKind() == ElementKind.PACKAGE) {
					packageName = packageElement.toString();
				}

				TraitInformation traitInformation = null;
				if (traits.containsKey(className)) {
					traitInformation = traits.get(className);
				} else {
					traitInformation = new TraitInformation(className, packageName);
				}

				Uses uses = element.getAnnotation(Uses.class);
				if (uses != null) {
					traitInformation.addTraits(uses.value());
				}

				Use use = element.getAnnotation(Use.class);
				if (use != null) {
					traitInformation.addTrait(use);
				}

				traits.put(className, traitInformation);
			}
		}

		generateClass(traits, processingEnv);

		return true;
	}

	protected void generateClass ( Map<String, TraitInformation> traits, ProcessingEnvironment processingEnvironment ) {
		for (TraitInformation trait : traits.values()) {
			try {
				String traitedClassName = TRAIT_NAME_PREFIX + trait.getTraitedClass() + TRAIT_NAME_POSTFIX;

				JavaFileObject fileObject = processingEnvironment.getFiler().createSourceFile(traitedClassName);
				BufferedWriter writer = new BufferedWriter(fileObject.openWriter());

				writer.append("package " + trait.getTraitedPackage() + ";");
				writer.newLine();
				writer.newLine();

				writer.append("// Generated by AnnoTRAITion.");
				writer.newLine();
				writer.append("// Do not edit, your changes will be overridden.");
				writer.newLine();

				writer.append("public class " + traitedClassName + " extends " + trait.getTraitedClass() + " {");
				writer.newLine();

				for (Use use : trait.getTraits()) {
					Class traitClass = null;
					try {
						traitClass = use.value();
					} catch (MirroredTypeException exception) {
						TypeMirror typeMirror = exception.getTypeMirror();
						Types types = processingEnvironment.getTypeUtils();
						TypeElement typeElement = (TypeElement) types.asElement(typeMirror);
						try {
							traitClass = Class.forName(typeElement.getQualifiedName().toString());
						} catch (ClassNotFoundException e) {
							e.printStackTrace();
						}
					}
					String traitClassName = traitClass.getSimpleName().toString();

					String variableName = use.localVariable();
					if (variableName.equals("")) {
						variableName = lowerCaseFirst(traitClassName);
					}
					writer.append("\tprotected " + traitClassName + " " + variableName + ";");
					writer.newLine();

					for (Method method : traitClass.getDeclaredMethods()) {
						writer.newLine();
						writer.append("\t/**");
						writer.newLine();
						writer.append("\t * ");
						writer.append("Passes through to `" + traitClassName + "." + method.getName().toString() + "`.");
						writer.newLine();
						writer.append("\t */");
						writer.newLine();

						String methodSignature = "";

						// Process modifiers
						methodSignature += Modifier.toString(method.getModifiers());

						// Process return type
						methodSignature += " " + method.getReturnType().getSimpleName().toString();

						// Process name
						methodSignature += " " + method.getName();

						// Process parameters
						String parameters = "";
						Class<?>[] parameterTypes = method.getParameterTypes();
						if (parameterTypes.length > 0) {
							parameters += " ";
						}
						parameters += "(";
						for (int i = 0; i < parameterTypes.length; i++) {
							if (i != 0) {
								parameters += ", ";
							} else {
								parameters += " ";
							}

							parameters += parameterTypes[i].getSimpleName().toString() + " " + String.valueOf((char) (i + 97)) + parameterTypes[i].getSimpleName().toString();

							if (i == (parameterTypes.length - 1)) {
								parameters += " ";
							}
						}
						parameters += ")";
						methodSignature += parameters;

						// Process exceptions
						String exceptions = "";
						Class<?>[] exceptionTypes = method.getExceptionTypes();
						for (Class exceptionType : exceptionTypes) {
							if (exceptions == "") {
								exceptions += " throws ";
							} else {
								exceptions += ", ";
							}

							exceptions += exceptionType.getSimpleName().toString();
						}
						methodSignature += exceptions;

						writer.append("\t" + methodSignature + " {");
						writer.newLine();

						writer.append("\t\t");
						if (!method.getReturnType().getSimpleName().toString().contains("void")) {
							writer.append("return ");
						}

						writer.append("this." + variableName + "." + method.getName());

						writer.append("(");
						for (int i = 0; i < parameterTypes.length; i++) {
							if (i != 0) {
								writer.append(", ");
							}
							writer.append(String.valueOf((char) (i + 97)) + parameterTypes[i].getSimpleName().toString());
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

	public class TraitInformation {
		String traitedClass;
		String traitedPackage;
		ArrayList<Use> traits;

		public TraitInformation ( String traitedClass, String traitedPackage ) {
			this.traitedClass = traitedClass;
			this.traitedPackage = traitedPackage;
			this.traits = new ArrayList<Use>();
		}

		public void addTrait ( Use trait ) {
			this.traits.add(trait);
		}

		public void addTraits ( Use[] traits ) {
			for (Use trait : traits) {
				this.traits.add(trait);
			}
		}

		public String getTraitedClass () {
			return traitedClass;
		}

		public String getTraitedPackage () {
			return traitedPackage;
		}

		public ArrayList<Use> getTraits () {
			return traits;
		}
	}

	protected String lowerCaseFirst ( String string ) {

		return string.substring(0, 1).toLowerCase() + string.substring(1);
	}
}
