package org.minimallycorrect.javatransformer.api.code;

import lombok.*;
import lombok.experimental.Wither;
import org.jetbrains.annotations.Nullable;
import org.minimallycorrect.javatransformer.api.TransformationException;
import org.minimallycorrect.javatransformer.api.Type;

import java.util.*;

@SuppressWarnings("serial")
public interface CodeFragment {
	/*
	* TODO: implement this
	*
	* fall-through/abort considerations:
	*
	* insertion before is always allowed
	*
	* overwrite is only allowed if can already fall through the fragment being overwritten, or can't fall through the fragment being inserted
	*
	* after is only allowed if execution can fall through
	*
	* input/output considerations:
	*
	* before - inputs can be none or the same. if same duplicate inputs? or allow changing inputs?. outputs must be none
	*
	* configurable?? by default duplicate, option to allow changing
	*
	* overwrite - inputs must be the same outputs must be the same
	*
	* after - inputs must be none, outputs must be none or same?
	* */

	void insert(@NonNull CodeFragment codeFragment, @NonNull InsertionPosition position, @NonNull InsertionOptions insertionOptions);

	default void insert(@NonNull CodeFragment codeFragment, @NonNull InsertionPosition position) {
		insert(codeFragment, position, new InsertionOptions());
	}

	default <T extends CodeFragment> List<T> findFragments(Class<T> fragmentType) {
		return Collections.emptyList();
	}

	ExecutionOutcome getExecutionOutcome();

	@NonNull
	List<IntermediateValue> getInputTypes();

	@NonNull
	List<IntermediateValue> getOutputTypes();

	enum InsertionPosition {
		BEFORE,
		OVERWRITE,
		AFTER
	}

	interface MethodCallExpression extends CodeFragment, HasInputTypes, HasContainingClassType, HasName {
	}

	interface FieldAccessFragment extends CodeFragment, HasInputTypes, HasContainingClassType, HasName {
	}

	interface FieldLoadFragment extends FieldAccessFragment {
	}

	interface FieldStoreFragment extends FieldAccessFragment {
	}

	interface ReturnFragment extends CodeFragment, HasInputType {
	}

	interface NewExpression extends CodeFragment, HasInputTypes {
	}

	@FunctionalInterface
	interface HasInputType extends HasInputTypes {
		@Nullable
		default IntermediateValue getInputType() {
			val inputTypes = getInputTypes();
			if (inputTypes.size() != 1)
				throw new TransformationException("Found multiple input types, expected 1");
			return inputTypes.get(0);
		}
	}

	@FunctionalInterface
	interface HasInputTypes {
		@NonNull
		List<IntermediateValue> getInputTypes();
	}

	@FunctionalInterface
	interface HasOutputType extends HasOutputTypes {
		@Nullable
		default IntermediateValue getOutputType() {
			val outputTypes = getOutputTypes();
			if (outputTypes.size() != 1)
				throw new TransformationException("Found multiple input types, expected 1");
			return outputTypes.get(0);
		}
	}

	@FunctionalInterface
	interface HasOutputTypes {
		@NonNull
		List<IntermediateValue> getOutputTypes();
	}

	@FunctionalInterface
	interface HasContainingClassType {
		@NonNull
		Type getContainingClassType();
	}

	@FunctionalInterface
	interface HasName {
		@NonNull
		String getName();
	}

	@AllArgsConstructor
	@Getter
	@NoArgsConstructor
	@Wither
	class InsertionOptions {
		public static InsertionOptions DEFAULT = new InsertionOptions();
		public boolean convertReturnToOutputTypes = true;
		public boolean convertReturnCallToReturnInstruction = true;
	}

	@RequiredArgsConstructor
	@ToString
	final class ExecutionOutcome {
		public final boolean canFallThrough;
		public final boolean canThrow;
		public final boolean canReturn;
	}

	class TypeMismatchException extends TransformationException {
		final Class<? extends CodeFragment> expected;
		final Class<? extends CodeFragment> actual;

		public TypeMismatchException(Class<? extends CodeFragment> expected, @NonNull CodeFragment actual) {
			super("Expected CodeFragment of type " + expected + ". Actual type " + actual.getClass());
			this.expected = expected;
			this.actual = actual.getClass();
		}
	}

	class UnreachableInsertionException extends TransformationException {
		public UnreachableInsertionException(CodeFragment fragment, InsertionPosition position) {
			super("Can't insert into '" + fragment + "' at position " + position + " as the code would be unreachable");
		}
	}
}
