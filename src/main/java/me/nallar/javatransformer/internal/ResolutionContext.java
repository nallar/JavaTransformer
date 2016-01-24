package me.nallar.javatransformer.internal;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.TypeParameter;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.PrimitiveType;
import lombok.NonNull;
import lombok.val;
import me.nallar.javatransformer.api.Type;
import me.nallar.javatransformer.internal.util.JVMUtil;
import me.nallar.javatransformer.internal.util.Joiner;
import me.nallar.javatransformer.internal.util.NodeUtil;
import me.nallar.javatransformer.internal.util.Splitter;

import java.util.*;

public class ResolutionContext {
	private static final Splitter dotSplitter = Splitter.on('.');
	@NonNull
	private final String packageName;
	@NonNull
	private final Iterable<ImportDeclaration> imports;
	@NonNull
	private final Iterable<TypeParameter> typeParameters;

	private ResolutionContext(String packageName, Iterable<ImportDeclaration> imports, Iterable<TypeParameter> typeParameters) {
		this.packageName = packageName;
		this.imports = imports;
		this.typeParameters = typeParameters;
	}

	public static ResolutionContext of(String packageName, Iterable<ImportDeclaration> imports, Iterable<TypeParameter> typeParameters) {
		return new ResolutionContext(packageName, imports, typeParameters);
	}

	public static ResolutionContext of(Node node) {
		CompilationUnit cu = NodeUtil.getParentNode(node, CompilationUnit.class);
		String packageName = cu.getPackage().getName().getName();
		List<TypeParameter> typeParameters = NodeUtil.getTypeParameters(node);

		return new ResolutionContext(packageName, cu.getImports(), typeParameters);
	}

	static boolean hasPackages(String name) {
		// Guesses whether input name includes packages or is just classes
		return Character.isUpperCase(name.charAt(0)) && name.indexOf('.') != -1;
	}

	static String classNameToDescriptor(String className) {
		// TODO: 23/01/2016 Handle inner classes properly? currently depend on following naming standards
		// depends on: lower case package names, uppercase first letter of class name
		List<String> parts = new ArrayList<>();
		dotSplitter.split(className).forEach(parts::add);

		boolean possibleClass = true;
		for (int i = parts.size() - 1, size = i; i >= 0; i--) {
			String part = parts.get(i);

			boolean last = i == size;

			if (!last && !Character.isUpperCase(part.charAt(0))) {
				possibleClass = false;
			}

			if (!last) {
				parts.set(i, part + (possibleClass ? '$' : '/'));
			}
		}

		return "L" + Joiner.on().join(parts) + ";";
	}

	public Type resolve(com.github.javaparser.ast.type.Type type) {
		if (type instanceof ClassOrInterfaceType) {
			return resolve(((ClassOrInterfaceType) type).getName());
		} else if (type instanceof PrimitiveType) {
			return new Type(JVMUtil.primitiveTypeToDescriptor(((PrimitiveType) type).getType().name().toLowerCase()));
		} else {
			// TODO: 23/01/2016 Is this behaviour correct?
			return resolve(type.toStringWithoutComments());
		}
	}

	/**
	 * Resolves a given part to a JVM type string.
	 * <p>
	 * EG:
	 * ArrayList -> Ljava/util/ArrayList;
	 * T -> TT;
	 * boolean -> Z
	 *
	 * @param name
	 * @return JVM format resolved name
	 */
	public Type resolve(String name) {
		if (name == null)
			return null;

		String real = extractReal(name);
		Type type = resolveReal(real);

		String generic = extractGeneric(name);
		Type genericType = resolve(generic);

		if (generic == null) {
			if (type != null) {
				return type;
			}
		} else {
			if (type != null && genericType != null) {
				return type.withTypeArgument(genericType);
			}
		}

		throw new RuntimeException("Couldn't resolve name: " + name + "\nFound real type: " + type + "\nGeneric type: " + genericType);
	}

	private Type resolveReal(String name) {
		Type result = resolveTypeParameterType(name);
		if (result != null)
			return result;

		result = resolveClassType(name);
		if (result != null)
			return result;

		return null;
	}

	private Type resolveClassType(String name) {
		String dotName = name.contains(".") ? name : '.' + name;

		for (ImportDeclaration anImport : imports) {
			String importName = anImport.getName().getName();
			if (importName.endsWith(dotName)) {
				return new Type("L" + importName + ";", null);
			}
		}

		Type type = resolveIfExists(packageName + '.' + name);
		if (type != null) {
			return type;
		}

		for (ImportDeclaration anImport : imports) {
			String importName = anImport.getName().getName();
			if (importName.endsWith(".*")) {
				type = resolveIfExists(importName.replace(".*", ".") + name);
				if (type != null) {
					return type;
				}
			}
		}

		if (!hasPackages(name) && !Objects.equals(System.getProperty("JarTransformer.allowDefaultPackage"), "true")) {
			return null;
		}

		return new Type(classNameToDescriptor(name));
	}

	private Type resolveIfExists(String s) {
		if (s.startsWith("java.") || s.startsWith("javax.")) {
			try {
				return new Type(Class.forName(s).getName());
			} catch (ClassNotFoundException ignored) {
			}
		}
		// TODO: 23/01/2016 Move to separate class, do actual searching for files
		return null;
	}

	/**
	 * If we have the type parameter "A extends StringBuilder",
	 * then "A" is resolved to a type with:
	 * descriptor: Ljava/lang/StringBuilder;
	 * signature: TA;
	 */
	private Type resolveTypeParameterType(String name) {
		for (TypeParameter typeParameter : typeParameters) {
			String typeName = typeParameter.getName();
			if (typeName.equals(name)) {
				val bounds = typeParameter.getTypeBound();
				String extends_ = "Ljava/lang/Object;";

				if (bounds != null) {
					if (bounds.size() == 1) {
						ClassOrInterfaceType scope = bounds.get(0).getScope();
						if (scope != null) {
							extends_ = resolve(scope.getName()).descriptor;
						}
					} else {
						throw new RuntimeException("Bounds must have one object, found: " + bounds);
					}
				}

				return new Type("L" + extends_, "T" + typeName + ";");
			}
		}

		return null;
	}

	private String extractGeneric(String name) {
		int bracket = name.indexOf('<');
		if (name.lastIndexOf('>') != name.length() - 1)
			throw new RuntimeException("Mismatched angled brackets in: " + name);
		return bracket == -1 ? null : name.substring(bracket + 1, name.length() - 1);
	}

	private String extractReal(String name) {
		int bracket = name.indexOf('<');
		return bracket == -1 ? name : name.substring(0, bracket);
	}

	public String unresolve(Type t) {
		if (t.isPrimitiveType()) {
			return t.getPrimitiveTypeName();
		}
		if (t.isTypeParameter()) {
			return t.getTypeParameterName();
		}
		String className = t.getClassName();

		for (ImportDeclaration anImport : imports) {
			String importName = anImport.getName().getName();
			if (className.startsWith(importName)) {
				return className.replace(importName + ".", "");
			}
		}

		return className;
	}
}