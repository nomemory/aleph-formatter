package net.andreinc.aleph;

import org.junit.Test;

import static net.andreinc.aleph.AlephFormatter.template;
import static org.junit.Assert.assertTrue;

public class AlephFormatterTest {

    @Test
    public void testFormatSimpleArguments() throws Exception {
        String result = template("#{int3}-#{int2}").arg("int3", 5).arg("int2", 7).format();
        assertTrue("5-7".equals(result));
    }

    @Test
    public void testFormatSimpleFormatNullArgument() throws Exception {
        String result = template("#{NULL}-#{a}").args("NULL", null, "a", 5).format();
        assertTrue("null-5".equals(result));
    }

    @Test
    public void testFormatSimpleMultiplePointsArgument() throws Exception {
        String result = template("#{} #{a}").args("", "A", "a", 2).format();
        assertTrue("A 2".equals(result));
    }

    @Test
    public void testWithInvalidParam() throws Exception {
        String result = template("#{#{}} #{a}").args("", "A", "a", 3).format();
        System.out.println(result);
    }
}