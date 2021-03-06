package dev.minco.javatransformer.internal.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.val;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AnnotationNode;

import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.MarkerAnnotationExpr;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;

import dev.minco.javatransformer.api.Annotation;
import dev.minco.javatransformer.api.TransformationException;
import dev.minco.javatransformer.api.Type;
import dev.minco.javatransformer.internal.ResolutionContext;
import dev.minco.javatransformer.internal.javaparser.Expressions;

public final class AnnotationParser {
	public static List<Annotation> parseAnnotations(byte[] bytes) {
		ClassReader cr = new ClassReader(bytes);
		AnnotationVisitor cv = new AnnotationVisitor();
		cr.accept(cv, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);

		return cv.annotations.stream().map(AnnotationParser::annotationFromAnnotationNode).collect(Collectors.toList());
	}

	public static Annotation annotationFromAnnotationNode(AnnotationNode annotationNode) {
		return Annotation.of(new Type(annotationNode.desc), getAnnotationNodeValues(annotationNode));
	}

	private static Map<String, Object> getAnnotationNodeValues(AnnotationNode annotationNode) {
		if (annotationNode.values == null)
			return Collections.emptyMap();

		Map<String, Object> values = new HashMap<>();
		for (int i = 0; i < annotationNode.values.size(); i += 2) {
			values.put((String) annotationNode.values.get(i), annotationNode.values.get(i + 1));
		}
		return values;
	}

	public static Annotation annotationFromAnnotationExpr(AnnotationExpr annotationExpr, ResolutionContext context) {
		Type t = context.resolve(NodeUtil.qualifiedName(annotationExpr.getName()));
		if (annotationExpr instanceof SingleMemberAnnotationExpr) {
			return Annotation.of(t, Expressions.expressionToValue(((SingleMemberAnnotationExpr) annotationExpr).getMemberValue(), context));
		} else if (annotationExpr instanceof NormalAnnotationExpr) {
			val map = new HashMap<String, Object>();
			for (MemberValuePair memberValuePair : ((NormalAnnotationExpr) annotationExpr).getPairs()) {
				map.put(memberValuePair.getName().asString(), Expressions.expressionToValue(memberValuePair.getValue(), context));
			}
			return Annotation.of(t, map);
		} else if (annotationExpr instanceof MarkerAnnotationExpr) {
			return Annotation.of(t);
		}
		throw new TransformationException("Unknown annotation type: " + annotationExpr.getClass().getCanonicalName());
	}

	private static class AnnotationVisitor extends ClassVisitor {
		public final List<AnnotationNode> annotations = new ArrayList<>();

		public AnnotationVisitor() {
			super(Opcodes.ASM5);
		}

		@Override
		public org.objectweb.asm.AnnotationVisitor visitAnnotation(final String desc, final boolean visible) {
			AnnotationNode an = new AnnotationNode(desc);
			annotations.add(an);
			return an;
		}
	}
}
