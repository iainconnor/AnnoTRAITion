package com.iainconnor.annotraition;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import javax.tools.JavaFileObject;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.*;

@SupportedAnnotationTypes ("com.iainconnor.annotraition.*")
@SupportedSourceVersion (SourceVersion.RELEASE_6)
public class Processor extends AbstractProcessor {
	protected static final String TRAIT_NAME_PREFIX = "_";
	protected static final String TRAIT_NAME_POSTFIX = "Traited";

	@Override
	public boolean process ( Set<? extends TypeElement> typeElements, RoundEnvironment roundEnvironment ) {
		HashMap<String, TraitInformation> traits = new HashMap<String, TraitInformation>();

		ArrayList<Element> traitElements = new ArrayList<Element>();
		for (Element element : roundEnvironment.getElementsAnnotatedWith(Trait.class)) {
			traitElements.add(element);
		}

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

		generateClass(traits, traitElements, processingEnv);

		return true;
	}

	protected void generateClass ( Map<String, TraitInformation> traits, ArrayList<Element> traitElements, ProcessingEnvironment processingEnvironment ) {
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
					// This is a bit of insanity to work around the fact that we can't get a Class directly from an
					// annotation, but we can if we catch it in an exception.
					// See http://blog.retep.org/2009/02/13/getting-class-values-from-annotations-in-an-annotationprocessor/
					String traitClassName = null;
					String traitClassQualifiedName = null;
					try {
						Class traitClass = use.value();
						traitClassName = traitClass.getSimpleName().toString();
						traitClassQualifiedName = traitClass.getName().toString();
					} catch (MirroredTypeException exception) {
						// This try/catch will always fall into the catch
						TypeMirror typeMirror = exception.getTypeMirror();
						Types types = processingEnvironment.getTypeUtils();
						TypeElement typeElement = (TypeElement) types.asElement(typeMirror);
						traitClassQualifiedName = typeElement.getQualifiedName().toString();
						traitClassName = typeElement.getSimpleName().toString();
					}

					String variableName = use.localVariable();
					if (variableName.equals("")) {
						variableName = lowerCaseFirst(traitClassName);
					}
					writer.append("\tprotected " + traitClassName + " " + variableName + ";");
					writer.newLine();

					// Look through the list of elements annotated with @Trait until we find a matching class
					// Note: You need to compare String values rather than the Classes themselves,
					// as they exist at different levels of compilation at this point.
					Element traitElement = null;
					for (Element possibleTraitElement : traitElements) {
						if (possibleTraitElement.getKind() == ElementKind.CLASS && possibleTraitElement.toString().equals(traitClassQualifiedName)) {
							traitElement = possibleTraitElement;
							break;
						}
					}

					if (traitElement != null) {
						for (Element traitSubElement : traitElement.getEnclosedElements()) {
							if (traitSubElement.getKind() == ElementKind.CONSTRUCTOR || traitSubElement.getKind() == ElementKind.METHOD) {
								writer.newLine();
								writer.append("\t/**");
								writer.newLine();
								writer.append("\t * ");
								writer.append("Passes through to `" + traitClassName + "." + ((ExecutableElement) traitSubElement).getSimpleName() + "`.");
								writer.newLine();
								writer.append("\t */");
								writer.newLine();

								String methodSignature = "";

								// Process modifiers
								Modifier[] modifiers = ((ExecutableElement) traitSubElement).getModifiers().toArray(new Modifier[((ExecutableElement) traitSubElement).getModifiers().size()]);
								for (int i = 0; i < modifiers.length; i++) {
									if (i != 0) {
										methodSignature += " ";
									}
									methodSignature += modifiers[i].toString();
								}

								// Process return type
								methodSignature += " " + ((ExecutableElement) traitSubElement).getReturnType().toString();

								// Process name
								methodSignature += " " + ((ExecutableElement) traitSubElement).getSimpleName();

								// Process parameters
								String parameters = "";
								List<? extends VariableElement> parameterElements = ((ExecutableElement) traitSubElement).getParameters();
								if (parameterElements.size() > 0) {
									parameters += " ";
								}
								parameters += "(";
								for (int i = 0; i < parameterElements.size(); i++) {
									if (i != 0) {
										parameters += ", ";
									} else {
										parameters += " ";
									}

									parameters += parameterElements.toArray(new VariableElement[parameterElements.size()])[i].getSimpleName().toString();

									//parameters += parameterElements[i].getSimpleName().toString() + " " + String.valueOf((char) (i + 97)) + parameterTypes[i].getSimpleName().toString();

									if (i == (parameterElements.size() - 1)) {
										parameters += " ";
									}
								}
								parameters += ")";
								methodSignature += parameters;

								writer.append(methodSignature);
							}
						}
					}

					/*
					for (Method method : traitClass.getDeclaredMethods()) {
						writer.newLine();
						writer.append("\t/**");
						writer.newLine();
						writer.append("\t * ");
						writer.append("Passes through to `" + traitClassName + "." + method.getName().toString() + "`.");
						writer.newLine();
						writer.append("\t /");
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
					*/
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
