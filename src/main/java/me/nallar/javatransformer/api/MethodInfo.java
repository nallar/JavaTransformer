package me.nallar.javatransformer.api;

import me.nallar.javatransformer.internal.SimpleMethodInfo;

import java.util.*;

public interface MethodInfo extends ClassMember {
	static MethodInfo of(AccessFlags accessFlags, String name, Type returnType, Parameter... parameters) {
		return of(accessFlags, name, returnType, Arrays.asList(parameters));
	}

	static MethodInfo of(AccessFlags accessFlags, String name, Type returnType, List<Parameter> parameters) {
		return SimpleMethodInfo.of(accessFlags, name, returnType, parameters);
	}

	static boolean similarParameters(MethodInfo a, MethodInfo b) {
		System.err.println("a: " + a.getParameters() + " b: " + b.getParameters());
		return a.getParameters() == null || b.getParameters() == null || a.getParameters().equals(b.getParameters());
	}

	Type getReturnType();

	void setReturnType(Type returnType);

	List<Parameter> getParameters();

	void setParameters(List<Parameter> parameters);

	default void setAll(MethodInfo info) {
		this.setName(info.getName());
		this.setAccessFlags(info.getAccessFlags());
		this.setReturnType(info.getReturnType());
		this.setParameters(info.getParameters());
	}

	default boolean similar(MethodInfo other) {
		return other.getName().equals(this.getName()) &&
			other.getReturnType().similar(this.getReturnType()) &&
			similarParameters(this, other);
	}
}
