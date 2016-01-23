package me.nallar.javatransformer.internal;

import me.nallar.javatransformer.api.Type;
import org.junit.Assert;

public class MethodDescriptorTest {
	@org.junit.Test
	public void testGetReturnType() throws Exception {
		MethodDescriptor d = new MethodDescriptor("()Ljava/lang/String;", null);
		Type t = d.getReturnType();

		Assert.assertEquals("Ljava/lang/String;", t.descriptor);
		Assert.assertEquals(null, t.signature);
		Assert.assertEquals("java.lang.String", t.getClassName());
	}
}
