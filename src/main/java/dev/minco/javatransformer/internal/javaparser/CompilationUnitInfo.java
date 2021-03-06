package dev.minco.javatransformer.internal.javaparser;

import java.util.ArrayList;
import java.util.List;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.TypeDeclaration;

import dev.minco.javatransformer.api.ClassPath;
import dev.minco.javatransformer.internal.SourceInfo;

public final class CompilationUnitInfo {
	public static List<SourceInfo> getSourceInfos(CompilationUnit compilationUnit, ClassPath classPath) {
		List<SourceInfo> sourceInfos = new ArrayList<>();
		String packageName = compilationUnit.getPackageDeclaration().map(it -> it.getNameAsString() + '.').orElse("");
		getSourceInfos(compilationUnit.getTypes(), classPath, sourceInfos, packageName);
		return sourceInfos;
	}

	@SuppressWarnings({"unchecked", "deprecation"})
	public static void getSourceInfos(Iterable<TypeDeclaration<?>> typeDeclarations, ClassPath classPath, List<SourceInfo> sourceInfos, String packageName) {
		for (TypeDeclaration<?> typeDeclaration : typeDeclarations) {
			sourceInfos.add(new SourceInfo(() -> typeDeclaration, packageName + typeDeclaration.getName(), classPath));
			// suppressed deprecation warning for now
			// https://github.com/javaparser/javaparser/issues/1472#issuecomment-424327421
			getSourceInfos(typeDeclaration.getChildNodesByType((Class<TypeDeclaration<?>>) (Object) TypeDeclaration.class), classPath, sourceInfos, packageName + typeDeclaration.getNameAsString() + '$');
		}
	}
}
