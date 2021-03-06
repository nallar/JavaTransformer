package dev.minco.javatransformer.internal.util;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import lombok.val;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import dev.minco.javatransformer.api.AccessFlags;
import dev.minco.javatransformer.api.AccessFlags.AccessFlagsConstant;
import dev.minco.javatransformer.api.TransformationException;

public final class JVMUtil {
	private static final Splitter dotSplitter = Splitter.on('.');

	public static String getDescriptor(Class<?> clazz) {
		if (clazz.isPrimitive()) {
			return descriptorToPrimitiveType(clazz.getSimpleName());
		}
		return 'L' + clazz.getCanonicalName() + ';';
	}

	public static String descriptorToPrimitiveType(String descriptor) {
		switch (descriptor) {
			case "B":
				return "byte";
			case "C":
				return "char";
			case "D":
				return "double";
			case "F":
				return "float";
			case "I":
				return "int";
			case "J":
				return "long";
			case "S":
				return "short";
			case "V":
				return "void";
			case "Z":
				return "boolean";
		}

		throw new TransformationException("Invalid descriptor: " + descriptor);
	}

	public static String primitiveTypeToDescriptor(String primitive) {
		return primitiveTypeToDescriptor(primitive, false);
	}

	@Nullable
	@Contract("_, false -> !null")
	public static String primitiveTypeToDescriptor(String primitive, boolean allowMissing) {
		switch (primitive) {
			case "byte":
				return "B";
			case "char":
				return "C";
			case "double":
				return "D";
			case "float":
				return "F";
			case "int":
				return "I";
			case "long":
				return "J";
			case "short":
				return "S";
			case "void":
				return "V";
			case "boolean":
				return "Z";
		}

		if (allowMissing)
			return null;

		throw new TransformationException("Invalid primitive type: " + primitive);
	}

	public static <T extends Enum<?>> T searchEnum(Class<T> enumeration, String search) {
		for (T each : enumeration.getEnumConstants()) {
			if (each.name().equalsIgnoreCase(search)) {
				return each;
			}
		}
		throw new IllegalArgumentException("Can't find enum value with name " + search + " in " + enumeration);
	}

	public static String getParameterList(Method m) {
		List<Class<?>> parameterClasses = new ArrayList<>(Arrays.asList(m.getParameterTypes()));
		StringBuilder parameters = new StringBuilder();
		for (Class<?> clazz : parameterClasses) {
			parameters.append(getDescriptor(clazz));
		}
		return parameters.toString();
	}

	public static String accessIntToString(@AccessFlagsConstant int access) {
		StringBuilder result = new StringBuilder();

		if (hasFlag(access, AccessFlags.ACC_PUBLIC))
			result.append(" public");

		if (hasFlag(access, AccessFlags.ACC_PRIVATE))
			result.append(" private");

		if (hasFlag(access, AccessFlags.ACC_PROTECTED))
			result.append(" protected");

		if (hasFlag(access, AccessFlags.ACC_STATIC))
			result.append(" static");

		if (hasFlag(access, AccessFlags.ACC_FINAL))
			result.append(" final");

		return result.toString().trim();
	}

	@AccessFlagsConstant
	public static int accessStringToInt(String access) {
		int a = 0;
		for (String accessPart : Splitter.on(' ').splitIterable(access)) {
			switch (accessPart) {
				case "public":
					a |= AccessFlags.ACC_PUBLIC;
					break;
				case "protected":
					a |= AccessFlags.ACC_PROTECTED;
					break;
				case "private":
					a |= AccessFlags.ACC_PRIVATE;
					break;
				case "static":
					a |= AccessFlags.ACC_STATIC;
					break;
				case "synthetic":
					a |= AccessFlags.ACC_SYNTHETIC;
					break;
				case "final":
					a |= AccessFlags.ACC_FINAL;
					break;
				default:
					throw new TransformationException("Unknown access string " + access);
			}
		}
		return a;
	}

	public static String fileNameToClassName(String f) {
		f = removeFromEnd(f, ".class");
		f = removeFromEnd(f, ".java");
		val result = f.replace('\\', '.').replace('/', '.');
		return result.isEmpty() ? "" : (result.charAt(0) == '.' ? result.substring(1) : result);
	}

	public static String classNameToFileName(String f) {
		return classNameToSlashName(f) + ".class";
	}

	public static String classNameToSlashName(String f) {
		return f.replace('.', '/');
	}

	public static String classNameToSlashName(Class<?> returnClass) {
		return classNameToSlashName(returnClass.getName());
	}

	private static String removeFromEnd(String s, String f) {
		return s.endsWith(f) ? s.substring(0, s.length() - f.length()) : s;
	}

	public static boolean hasFlag(int access, int flag) {
		return (access & flag) != 0;
	}

	@AccessFlagsConstant
	public static int replaceFlag(@AccessFlagsConstant int in, @AccessFlagsConstant int from, @AccessFlagsConstant int to) {
		if ((in & from) != 0) {
			in &= ~from;
			in |= to;
		}
		return in;
	}

	@AccessFlagsConstant
	public static int makeAccess(@AccessFlagsConstant int access, boolean makePublic) {
		access = makeAtLeastProtected(access);
		if (makePublic) {
			access = replaceFlag(access, AccessFlags.ACC_PROTECTED, AccessFlags.ACC_PUBLIC);
		}
		return access;
	}

	@AccessFlagsConstant
	public static int makeAtLeastProtected(@AccessFlagsConstant int access) {
		if (hasFlag(access, AccessFlags.ACC_PUBLIC) || hasFlag(access, AccessFlags.ACC_PROTECTED)) {
			// already protected or public
			return access;
		}
		if (hasFlag(access, AccessFlags.ACC_PRIVATE)) {
			// private -> protected
			return replaceFlag(access, AccessFlags.ACC_PRIVATE, AccessFlags.ACC_PROTECTED);
		}
		// not public, protected or private so must be package-local
		// change to public - protected doesn't include package-local.
		return access | AccessFlags.ACC_PUBLIC;
	}

	public static String classNameToJLSName(String className) {
		List<String> parts = new ArrayList<>();
		dotSplitter.splitIterable(className).forEach(parts::add);

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

		return Joiner.on().join(parts);
	}
}
