import static net.andreinc.aleph.AlephFormatter.template;

public class FormatterTest {
    public static void main(String[] args) {
    }

    public static final void example1() {
        String result = template("#{errNo} -> #{c.simpleName} -> #{c.package.name}")
                            .arg("errNo", 101)
                            .arg("c", String.class)
                            .fmt();

        System.out.println(result);
    }

    public static final void example2() {
        String result = template("#{errNo} -> #{c.simpleName} -> #{c.package.name}")
                .args("errNo", 101, "c", String.class)
                .fmt();

        System.out.println(result);
    }

    public static final void example3() {
        String result = template("#{errNo} + escaped: `#{errNo}")
                        .arg("errNo", 101)
                        .fmt();

        System.out.println(result);
    }
}