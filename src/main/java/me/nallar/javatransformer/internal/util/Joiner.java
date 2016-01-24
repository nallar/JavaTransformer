package me.nallar.javatransformer.internal.util;

import java.util.*;

public interface Joiner {
	static Joiner on() {
		return parts -> {
			@SuppressWarnings("unchecked")
			StringBuilder sb = new StringBuilder();

			for (Object part : parts) {
				sb.append(part.toString());
			}

			return sb.toString();
		};
	}

	static Joiner on(String join) {
		return parts -> {
			@SuppressWarnings("unchecked")
			Iterator<Object> i = (Iterator<Object>) parts.iterator();

			if (!i.hasNext())
				return "";

			StringBuilder sb = new StringBuilder(i.next().toString());

			while (i.hasNext())
				sb.append(join).append(i.next().toString());

			return sb.toString();
		};
	}

	String join(Iterable<?> s);
}