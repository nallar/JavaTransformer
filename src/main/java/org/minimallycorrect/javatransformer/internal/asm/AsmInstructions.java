package org.minimallycorrect.javatransformer.internal.asm;

import lombok.experimental.UtilityClass;
import lombok.val;
import org.minimallycorrect.javatransformer.api.code.IntermediateValue;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;

/**
 * asm is half an abstraction level above java bytecode, and very inconsistent.
 * <p>
 * const instructions - no common interface to handle short versions for 0/1/-1/etc
 * var instructions - auto converts the 0/1/-1 to varinsnnode.
 * <p>
 * THANKS!!!!!
 */
@SuppressWarnings("Duplicates")
@UtilityClass
public class AsmInstructions implements Opcodes {
	public static Object getConstant(AbstractInsnNode insn) {
		switch (insn.getOpcode()) {
			case ACONST_NULL:
				return null;
			case DCONST_0:
				return 0d;
			case DCONST_1:
				return 1d;
			case FCONST_0:
				return 0f;
			case FCONST_1:
				return 1f;
			case FCONST_2:
				return 2f;
			case LCONST_0:
				return 0L;
			case LCONST_1:
				return 1L;
			case ICONST_M1:
				return -1;
			case ICONST_0:
				return 0;
			case ICONST_1:
				return 1;
			case ICONST_2:
				return 2;
			case ICONST_3:
				return 3;
			case ICONST_4:
				return 4;
			case ICONST_5:
				return 5;
			case SIPUSH:
			case BIPUSH:
			case NEWARRAY:
				return ((IntInsnNode) insn).operand;
			case LDC:
				return ((LdcInsnNode) insn).cst;
		}
		return IntermediateValue.UNKNOWN;
	}

	public static int getStoreInstructionForType(IntermediateValue iv) {
		val type = iv.type;
		if (type == null)
			throw new NullPointerException();
		switch (type.getDescriptorType()) {
			case BYTE:
			case CHAR:
			case INT:
			case SHORT:
			case BOOLEAN:
				return ISTORE;
			case DOUBLE:
				return DSTORE;
			case FLOAT:
				return FSTORE;
			case LONG:
				return LSTORE;
			case VOID:
				throw new IllegalArgumentException(iv.toString());
			case ARRAY:
			case CLASS:
				return ASTORE;
			case VALUE:
			case UNION:
			default:
				throw new UnsupportedOperationException();
		}
	}

	public static int getLoadInstructionForType(IntermediateValue iv) {
		val type = iv.type;
		if (type == null)
			throw new NullPointerException();
		switch (type.getDescriptorType()) {
			case BYTE:
			case CHAR:
			case INT:
			case SHORT:
			case BOOLEAN:
				return ILOAD;
			case DOUBLE:
				return DLOAD;
			case FLOAT:
				return FLOAD;
			case LONG:
				return LLOAD;
			case VOID:
				throw new IllegalArgumentException(iv.toString());
			case ARRAY:
			case CLASS:
				return ALOAD;
			case VALUE:
			case UNION:
			default:
				throw new UnsupportedOperationException();
		}
	}
}
