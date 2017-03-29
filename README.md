# aleph-formatter

This is a simple String Formatter for Java that supports named parameters:

```java
String result = template("#{x} + #{y} = #{z}")
                    .args("x", 5, "y", 10, "z", 15)
                    .format();
System.out.println(result);

// Output: "5 + 10 = 15"
```

It also supports the possibility to chain methods (getters) for the supplied arguments:

```java
Student student = new Student("Andrei", 30, "Male");
String studStr = template("#{id}\tName: #{st.getName}, Age: #{st.getAge}, Gender: #{st.getGender}")
                    .arg("id", 10)
                    .arg("st", student)
                    .format();
System.out.println(studStr);
// Output: "10	Name: Andrei, Age: 30, Gender: Male"
```


# To do:

- Add library to a maven public repo;
- Write more unit tests
