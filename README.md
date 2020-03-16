# aleph-formatter

Aleph Formatter is a lightweight library for string formatting that supports both named and positional parameters with a twist: it has a limited support for object introspection.

#### Example - basic usage:

```java
import static net.andreinc.aleph.AlephFormatter.str

//...

String s1 = str("#{1} #{0} #{1} #{0}", 1, 2)
            .fmt();
System.out.println(s1);
```    

Output:

```
2 1 2 1
```

#### Example - simple introspection

Each parameter supports limited method invocation:

```java
String s2 = str("#{1}#{0.simpleName}", String.class, "Class:")
            .fmt();
System.out.println(s2);
```        

Output

```
Class:String
```

On the `String.class` you can invoke the method: `getSimpleName` directly in the template. `String.class` is the `#{0}` param. 

#### Example - Named arguments

```java
String s3 = str("#{date.dayOfMonth}-#{date.month}-#{date.year}")
            .arg("date", LocalDate.now())
            .fmt();
System.out.println(s3);

String s4 = str("#{2.simpleName}, #{1}, #{0}, #{aNumber}, #{anArray}", 1, "A", String.class)
            .args("aNumber", 100, "anArray", new int[]{1,2,3,})
            .fmt();
System.out.println(s4);                        
```                        

Output:

```
8-MAY-2018
String, A, 1, 100, [1, 2, 3]
```

#### Example - Escaping `#{`

```java
String s5 = str("`#{escaped} #{notEscaped}").args("escaped", 1, "notEscaped", 2)
            .fmt();
System.out.println(s5);
```

Output:

```
#{escaped} 2
```

#### Example - Changing Interpolation Styles

```java
//Note the use of ${var} not #{var}
String s6 = str("${dollars}").args("dollars", "notPound")
            .style(Styles.DOLLARS).fmt();
```

Output:

```
notPound
```

# installing

The library is found in the [jcenter()](https://bintray.com/nomemory/maven/aleph-formatter) repo.

**jcenter()** needs to be added as a repository. 

For gradle:

```
compile 'net.andreinc.aleph:aleph-formatter:0.1.1'
```

For maven:


```
<dependency>
  <groupId>net.andreinc.aleph</groupId>
  <artifactId>aleph-formatter</artifactId>
  <version>0.1.1</version>
</dependency>
```
