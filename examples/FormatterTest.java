import static net.andreinc.aleph.AlephFormatter.template;

public class FormatterTest {
    public static void main(String[] args) {

        String result = template("#{x} + #{y} = #{z}")
                            .args("x", 5, "y", 10, "z", 15)
                            .fmt();
        System.out.println(result);

        Student student = new Student("Andrei", 30, "Male");
        String studStr = template("#{id}\tName: #{st.getName}, Age: #{st.getAge}, Gender: #{st.getGender}")
                            .arg("id", 10)
                            .arg("st", student)
                            .fmt();
        System.out.println(studStr);
    }
}