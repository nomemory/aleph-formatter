/**
 * Created by andreinicolinciobanu on 29/03/17.
 */
public class Student {

    private String name;
    private Integer age;
    private String gender;

    public Student(String name, Integer age, String sex) {
        this.name = name;
        this.age = age;
        this.gender = sex;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }
}
