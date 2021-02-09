# aleph-formatter

Aleph Formatter is a lightweight library for string formatting that supports both named and positional parameters with a twist: it has a limited support for object introspection.


# installing

Historically, the library was found in `jcenter()`. But given `jcenter()`'s [service end](https://jfrog.com/blog/into-the-sunset-bintray-jcenter-gocenter-and-chartcenter/) it was moved to maven central:

Maven:
```xml
<dependency>
  <groupId>net.andreinc</groupId>
  <artifactId>aleph</artifactId>
  <version>0.1.1</version>
</dependency>
```

Gradle:
```
implementation 'net.andreinc:aleph:0.1.1'
```

# benchmarks

Aleph Formatter performs better than `String.format` for simple cases. A **jmh** benchmark is showing the following results (smaller is better):

```
CPU: AMD Ryzen 7 5800x, PBO
Windows Version	10.0.19041 Build 19041
Benchmark                     (N)  Mode  Cnt    Score    Error  Units
AlephFormatter.alephFormat      1  avgt    5   67.101 ±  2.103  ns/op
AlephFormatter.stringFormat     1  avgt    5  273.048 ±  5.632  ns/op
AlephFormatter.alephFormat     10  avgt    5   76.470 ±  4.423  ns/op
AlephFormatter.stringFormat    10  avgt    5  264.106 ±  3.018  ns/op
AlephFormatter.alephFormat    100  avgt    5  113.705 ±  5.941  ns/op
AlephFormatter.stringFormat   100  avgt    5  328.445 ± 15.986  ns/op
AlephFormatter.alephFormat   1000  avgt    5  257.674 ± 19.363  ns/op
AlephFormatter.stringFormat  1000  avgt    5  437.688 ± 11.813  ns/op
```

Source [here](https://github.com/PhaseRush/Benched/blob/master/src/main/java/strings/AlephFormatter.java).

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
