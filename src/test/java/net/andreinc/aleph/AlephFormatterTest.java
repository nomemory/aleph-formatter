package net.andreinc.aleph;

import org.junit.Test;

import net.andreinc.aleph.AlephFormatter.Style;
import net.andreinc.aleph.AlephFormatter.Styles;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertTrue;

public class AlephFormatterTest {

    @Test
    public void testFormatSimpleArguments() throws Exception {
        String result = AlephFormatter.str("#{int3}-#{int2}").arg("int3", 5).arg("int2", 7).fmt();
        assertTrue("5-7".equals(result));
    }

    @Test
    public void testFormatSimpleFormatNullArgument() throws Exception {
        String result = AlephFormatter.str("#{NULL}-#{a}").args("NULL", null, "a", 5).fmt();
        assertTrue("null-5".equals(result));
    }

    @Test
    public void testFormatSimpleMultiplePointsArgument() throws Exception {
        String result = AlephFormatter.str("#{} #{a}").args("", "A", "a", 2).fmt();
        assertTrue("A 2".equals(result));
    }

    @Test(expected = UncheckedFormatterException.class)
    public void testWithInvalidParam() throws Exception {
        AlephFormatter.str("#{#{}} #{a}").args().fmt();
    }

    @Test
    public void testWithList() throws Exception {
        List<String> stress = Arrays.asList("1", "2", "3");
        String result = AlephFormatter.str("#{l}").args("l", stress).fmt();
        assertTrue("[1, 2, 3]".equals(result));
    }

    @Test
    public void testWithPrimitiveArray() throws Exception {
        int[] arr = {1, 3, 4};
        String result = AlephFormatter.str("#{a}").args("a", arr).fmt();
        assertTrue("[1, 3, 4]".equals(result));
    }

    @Test
    public void testWithPrimitiveArrayEmpty() throws Exception {
        int[] a = {};
        String result = AlephFormatter.str("#{a}").args("a", a).fmt();
        assertTrue("[]".equals(result));
    }

    @Test
    public void testEscape() throws Exception {
        String result = AlephFormatter.str("`#{q},#{q}").args("q", "Q").fmt();
        assertTrue("#{q},Q".equals(result));
    }

    @Test
    public void testDoubleEscape() throws Exception {
        String result = AlephFormatter.str("``#{q},#{q}").args("q", "Q").fmt();
        assertTrue("`Q,Q".equals(result));
    }

    @Test(expected = UncheckedFormatterException.class)
    public void testEscapeInParamName() throws Exception {
        AlephFormatter.str("#{`q}#{q`}").args("q", "Q").fmt();
    }

    @Test
    public void testEscapeLastCharInTemplate() throws Exception {
        String result = AlephFormatter.str("#{q}`").args("q", "Q").fmt();
        assertTrue("Q".equals(result));
    }

    @Test
    public void testWithGetters() throws Exception {
        Person person = new Person("A", "B", 20);
        String result = AlephFormatter.str("#{p.getName}/#{p.getText}/#{p.getAge}").args("p", person).fmt();
        assertTrue("A/B/20".equals(result));
    }

    @Test
    public void testWithFieldNames() throws Exception {
        Person person = new Person("A", "B", 20);
        String result = AlephFormatter.str("#{p.name}/#{p.text}/#{p.age}").arg("p", person).fmt();
        assertTrue("A/B/20".equals(result));
    }

    @Test
    public void testFromFile() throws Exception {
        File tmp = File.createTempFile("aleph" + UUID.randomUUID().toString(), ".tmp");
        tmp.deleteOnExit();

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(tmp))) {
            bw.write("#{p.name}/#{p.text}/#{p.age}");
            bw.flush();

        }

        Person person = new Person("A", "B", 20);
        String result = AlephFormatter.file(tmp.getPath()).args("p", person).fmt();

        assertTrue("A/B/20".equals(result));
    }

    @Test
    public void posArgsTest() throws Exception {
        String result = AlephFormatter
                            .str("#{0} #{0} #{oneParam.simpleName} #{1}", "A", "B")
                            .arg("oneParam", String.class)
                            .fmt();
        assertTrue(result.equals("A A String B"));
    }
    
    @Test
    public void testWithDefaultStyle() throws Exception {
        String result = AlephFormatter
                            .str("#{a} #{b} `#{c}")
                            .style(Styles.DEFAULT)
                            .args("a","A","b","B","c","C")
                            .fmt();
        assertTrue(result.equals("A B #{c}")); 
    }
    
    @Test
    public void testWithDollarsStyle() throws Exception {
        String result = AlephFormatter
                            .str("${a} ${b} `${c}")
                            .style(Styles.DOLLARS)
                            .args("a","A","b","B","c","C")
                            .fmt();
        assertTrue(result.equals("A B ${c}")); 
    }
    
    @Test
    public void testWithCustomStyle() throws Exception {
        String result = AlephFormatter
                .str("@^a$ @^b$ !@^c$")
                .style(new Style() {
                    @Override public char getStartCharacter() { return '@'; }
                    @Override public char getOpenBracket() { return '^'; }
                    @Override public char getCloseBracket() { return '$'; }
                    @Override public char getEscapeCharacter() { return '!'; }
                })
                .args("a","A","b","B","c","C")
                .fmt();
        assertTrue(result.equals("A B @^c$")); 
    }
    
    @Test
    public void testWithRepeatedCharInStyle() throws Exception {
        String result = AlephFormatter
                .str("@$a$ @$b$ `@$c$")
                .style(new Style() {
                    @Override public char getStartCharacter() { return '@'; }
                    @Override public char getOpenBracket() { return '$'; }
                    @Override public char getCloseBracket() { return '$'; }
                    @Override public char getEscapeCharacter() { return '`'; }
                })
                .args("a","A","b","B","c","C")
                .fmt();
        assertTrue(result.equals("A B @$c$")); 
    }
}