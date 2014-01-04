package com.iainconnor.annotraition;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
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

				// Ensure that we only add each traited Class once
				TraitInformation traitInformation = null;
				if (traits.containsKey(className)) {
					traitInformation = traits.get(className);
				} else {
					traitInformation = new TraitInformation(className, packageName, element);
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

		generateClass(traits, traitElements);

		return true;
	}

	/**
	 * Converts a supplied TypeMirror into a TypeElement through reflection.
	 *
	 * @param typeMirror
	 *
	 * @return
	 */
	protected TypeElement getElementForMirror ( TypeMirror typeMirror ) {
		Types types = processingEnv.getTypeUtils();
		return (TypeElement) types.asElement(typeMirror);
	}

	/**
	 * Just a helper method to return a supplied string with it's first characer lower-cased.
	 *
	 * @param string
	 *
	 * @return
	 */
	protected String lowerCaseFirst ( String string ) {

		return string.substring(0, 1).toLowerCase() + string.substring(1);
	}

	/**
	 * Gets a the name of a Class once it has had Traits applied to it.
	 *
	 * @param trait
	 *
	 * @return
	 */
	protected String getTraitedClassName ( TraitInformation trait ) {

		return TRAIT_NAME_PREFIX + trait.getTraitedClass() + TRAIT_NAME_POSTFIX;
	}

	/**
	 * Gets the variable name to be used for the local passthrough. First checking if one is defined in the @Use
	 * annotation, and falling back on generating one.
	 *
	 * @param use
	 * @param traitClassName
	 *
	 * @return
	 */
	protected String getPassthroughVariableName ( Use use, String traitClassName ) {
		String variableName = use.localVariable();
		if (variableName.equals("")) {
			variableName = lowerCaseFirst(traitClassName);
		}

		return variableName;
	}

	/**
	 * Returns the matching Element that is annotated with the specificed @Trait Class name.
	 *
	 * @param traitElements
	 * @param traitClassQualifiedName
	 *
	 * @return
	 */
	protected Element getMatchingTraitElement ( ArrayList<Element> traitElements, String traitClassQualifiedName ) {
		// Note: You need to compare String values rather than the Classes themselves,
		// as they exist at different levels of compilation at this point.
		Element traitElement = null;
		for (Element possibleTraitElement : traitElements) {
			if (possibleTraitElement.getKind() == ElementKind.CLASS && possibleTraitElement.toString().equals(traitClassQualifiedName)) {
				traitElement = possibleTraitElement;
				break;
			}
		}

		return traitElement;
	}

	/**
	 * Takes a List of VariableElements and converts it into a formatted String representing those parameters.
	 * Optionally includes or excludes the types of each parameter. ie. [String foo, boolean bar] => " ( String foo,
	 * boolean bar )" or " ( foo, bar )"
	 *
	 * @param method
	 * @param needTypes
	 *
	 * @return
	 */
	protected String getParameters ( ExecutableElement method, boolean needTypes ) {
		String parameters = "";
		List<? extends VariableElement> parameterElements = method.getParameters();

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

			VariableElement parameterElement = parameterElements.get(i);
			if (needTypes) {
				parameters += getElementForMirror(parameterElement.asType()).getSimpleName().toString() + " ";
			}
			parameters += parameterElement.getSimpleName().toString();

			if (i == (parameterElements.size() - 1)) {
				parameters += " ";
			}
		}
		parameters += ")";

		return parameters;
	}

	/**
	 * Gets a String representing the modifiers of the specified method.
	 *
	 * @param method
	 *
	 * @return
	 */
	protected String getModifiers ( ExecutableElement method ) {
		String modifiers = "";
		Modifier[] modifiersArray = method.getModifiers().toArray(new Modifier[method.getModifiers().size()]);
		for (int i = 0; i < modifiersArray.length; i++) {
			if (i != 0) {
				modifiers += " ";
			}
			modifiers += modifiersArray[i].toString();
		}

		return modifiers;
	}

	/**
	 * Gets a String of the specified method's name.
	 *
	 * @param method
	 *
	 * @return
	 */
	protected String getMethodName ( ExecutableElement method, String traitedClassName ) {
		String className = method.getSimpleName().toString();
		if (className.equals("<init>")) {
			className = traitedClassName;
		}

		return className;
	}

	/**
	 * Gets a String representing the return type of the specified method.
	 *
	 * @param method
	 *
	 * @return
	 */
	protected String getReturnType ( ExecutableElement method ) {

		return method.getReturnType().toString();
	}

	/**
	 * Returns a String representing the Exceptions a method throws.
	 *
	 * @param method
	 *
	 * @return
	 */
	protected String getExceptions ( ExecutableElement method ) {
		String exceptions = "";
		List<? extends TypeMirror> exceptionElements = method.getThrownTypes();
		for (int i = 0; i < exceptionElements.size(); i++) {
			if (i == 0) {
				exceptions += " throws ";
			} else {
				exceptions += ", ";
			}

			TypeMirror exceptionElement = exceptionElements.get(i);
			exceptions += getElementForMirror(exceptionElement).getSimpleName().toString();
		}

		return exceptions;
	}

	/**
	 * Generates new .class files for the Traited version of all the Class/Trait combos found in a supplied map.
	 *
	 * @param traits
	 * @param traitElements
	 */
	protected void generateClass ( Map<String, TraitInformation> traits, ArrayList<Element> traitElements ) {
		for (TraitInformation trait : traits.values()) {
			try {
				String traitedClassName = getTraitedClassName(trait);

				JavaFileObject fileObject = processingEnv.getFiler().createSourceFile(traitedClassName, trait.getOriginatingElement());
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
						TypeElement typeElement = getElementForMirror(exception.getTypeMirror());
						traitClassQualifiedName = typeElement.getQualifiedName().toString();
						traitClassName = typeElement.getSimpleName().toString();
					}

					// Add private variable for passthrough to Trait
					String variableName = getPassthroughVariableName(use, traitClassName);
					writer.append("\tprotected " + traitClassName + " " + variableName + ";");
					writer.newLine();

					Element traitElement = getMatchingTraitElement(traitElements, traitClassQualifiedName);
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
								methodSignature += getModifiers((ExecutableElement) traitSubElement);

								// Process return type
								String returnType = getReturnType((ExecutableElement) traitSubElement);
								methodSignature += " " + returnType;

								// Process name
								String methodName = getMethodName((ExecutableElement) traitSubElement, traitClassName);
								methodSignature += " " + methodName;

								// Process parameters
								methodSignature += getParameters((ExecutableElement) traitSubElement, true);

								// Process exceptions
								methodSignature += getExceptions((ExecutableElement) traitSubElement);

								// Process method signature, which is a passthrough call to local instance of Trait
								writer.append("\t" + methodSignature + " {");
								writer.newLine();
								writer.append("\t\t");

								if (traitSubElement.getKind() == ElementKind.CONSTRUCTOR) {
									writer.append("super" + getParameters((ExecutableElement) traitSubElement, false) + ";");
									writer.append("\t\tthis." + variableName + " = new " + traitClassName + " (this);");
									writer.newLine();
								} else {
									if (!returnType.contains("void")) {
										writer.append("return ");
									}
									writer.append("this." + variableName + "." + methodName);
									writer.append(getParameters((ExecutableElement) traitSubElement, false) + ";");
								}

								writer.newLine();
								writer.append("\t}");
								writer.newLine();
							}
						}
					}
				}

				writer.append("}");
				writer.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * A Class used to keep track of all the Traits that are applied to a traited Class, as well as information about
	 * that Class itself.
	 */
	public class TraitInformation {
		String traitedClass;
		String traitedPackage;
		Element originatingElement;
		ArrayList<Use> traits;

		/**
		 * Stores basic information about the Class that has the Trait applied to it.
		 *
		 * @param traitedClass
		 * @param traitedPackage
		 * @param originatingElement
		 */
		public TraitInformation ( String traitedClass, String traitedPackage, Element originatingElement ) {
			this.traitedClass = traitedClass;
			this.traitedPackage = traitedPackage;
			this.originatingElement = originatingElement;
			this.traits = new ArrayList<Use>();
		}

		/**
		 * Adds a Trait to this Class.
		 *
		 * @param trait
		 */
		public void addTrait ( Use trait ) {
			this.traits.add(trait);
		}

		/**
		 * Adds an array of Traits to this Class.
		 *
		 * @param traits
		 */
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

		public Element getOriginatingElement () {
			return originatingElement;
		}

		public ArrayList<Use> getTraits () {
			return traits;
		}
	}
}
