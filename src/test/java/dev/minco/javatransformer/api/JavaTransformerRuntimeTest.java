package dev.minco.javatransformer.api;

import java.io.PrintStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import lombok.val;

import org.junit.Assert;
import org.junit.Test;

import dev.minco.javatransformer.api.code.CodeFragment;
import dev.minco.javatransformer.api.code.IntermediateValue;
import dev.minco.javatransformer.internal.ByteCodeInfo;
import dev.minco.javatransformer.internal.asm.DebugPrinter;
import dev.minco.javatransformer.transform.CodeFragmentTesting;

public class JavaTransformerRuntimeTest {
	private static final List<String> EXPECTED_METHOD_CALL_INPUTS = Arrays.asList("1", "2", "3", "4");
	private static final int EXPECTED_METHOD_CALL_COUNT = 4;

	@Test
	public void testTransformRuntime() throws Exception {
		final Path input = JavaTransformer.pathFromClass(JavaTransformerTest.class);
		final String name = "dev.minco.javatransformer.transform.CodeFragmentTesting";
		JavaTransformer transformer = new JavaTransformer();

		val targetClass = this.getClass().getName();
		AtomicBoolean check = new AtomicBoolean(false);

		transformer.addTransformer(name, c -> {
			Assert.assertEquals(name, c.getName());
			check.set(true);
			c.accessFlags(it -> it.makeAccessible(true));
			c.getAnnotations();
			val fields = c.getFields().collect(Collectors.toList());
			{
				Assert.assertEquals(1, fields.size());
				val field = fields.get(0);
				Assert.assertEquals("callback", field.getName());
				Assert.assertEquals("java.util.function.Consumer<java.lang.String>", field.getType().getJavaName());
			}
			val interfaceTypes = c.getInterfaceTypes();
			{
				Assert.assertTrue(interfaceTypes.isEmpty());
			}
			c.getMembers();
			c.getConstructors();
			c.getMethods().forEach(it -> {
				val cf = it.getCodeFragment();
				Assert.assertNotNull(it + " should have a CodeFragment", cf);
				switch (it.getName()) {
					case "testMethodCallExpression":
						val methodCalls = it.findFragments(CodeFragment.MethodCall.class);
						Assert.assertEquals(EXPECTED_METHOD_CALL_COUNT, methodCalls.size());
						for (int i = 1; i <= EXPECTED_METHOD_CALL_COUNT; i++) {
							System.out.println("call " + i);
							val call = methodCalls.get(i - 1);
							Assert.assertEquals("println", call.getName());
							Assert.assertEquals(PrintStream.class.getName(), call.getContainingClassType().getClassName());
							val inputTypes = call.getInputTypes();
							for (val inputType : inputTypes)
								Assert.assertEquals(IntermediateValue.LocationType.STACK, inputType.location.type);
							Assert.assertNotNull("Should find inputTypes for method call expression", inputTypes);
							Assert.assertEquals(2, inputTypes.size());
							Assert.assertEquals(String.valueOf(i), inputTypes.get(1).constantValue);
							Assert.assertEquals("java.io.PrintStream", inputTypes.get(0).type.getClassName());

							val callbackCaller = c.getMethods().filter(method -> method.getName().equals("callbackCaller")).findFirst().get();
							val callbackCallerFragment = callbackCaller.getCodeFragment();

							assert callbackCallerFragment != null;
							call.insert(callbackCallerFragment, CodeFragment.InsertionPosition.OVERWRITE);
							for (val inputType : callbackCallerFragment.getInputTypes())
								Assert.assertEquals(IntermediateValue.LocationType.LOCAL, inputType.location.type);
							DebugPrinter.printByteCode(((ByteCodeInfo.MethodNodeInfo) it).node, "after insert callbackCallerFragment");
						}
						break;
					case "testAbortEarly":
						val aborter = c.getMethods().filter(method -> method.getName().equals("aborter")).findFirst().get().getCodeFragment();
						assert aborter != null;
						cf.insert(aborter, CodeFragment.InsertionPosition.BEFORE);
						break;
				}
			});
		});

		System.out.println("Transforming path '" + input + '\'');
		transformer.load(input);
		val clazz = transformer.defineClass(this.getClass().getClassLoader(), name);
		Assert.assertEquals(name, clazz.getName());
		Assert.assertTrue("Transformer must process " + targetClass, check.get());

		val list = new ArrayList<String>();
		val codeFragmentTesting = new CodeFragmentTesting(list::add);
		codeFragmentTesting.testMethodCallExpression();
		Assert.assertEquals(EXPECTED_METHOD_CALL_INPUTS, list);

		val result = codeFragmentTesting.testAbortEarly();
		Assert.assertTrue("testAbortEarly should return true after patch", result);
		Assert.assertEquals(null, System.getProperty("finishedTestAbortEarly"));
	}

}
