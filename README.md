# aleph-formatter

`AlephFormatter` is a lightweight String Formatter for Java that supports named parameters with a twist: it has a very limited support for object introspection. 

```java
String result = template("#{errNo} -> #{c.simpleName} -> #{c.package.name}")
                .arg("errNo", 101)
                .arg("c", String.class)
                .fmt();


System.out.println(result);
```
Output:
```
Error number: 101 -> String -> java.lang
```

## API

The API is very simple, and has a [fluent](https://en.wikipedia.org/wiki/Fluent_interface) feel to it. 

There a few ways you can achieve the same thing as above. 

For example instead of using `arg()` you can use `args()` and specify the list of parameters on a single line, alternating the argument to be replaced with it's value:

```java
String result = template("#{errNo} -> #{c.simpleName} -> #{c.package.name}")
                .args("errNo", 101, "c", String.class)
                .fmt();
```

It is also allowed to read the content of the String directly from the disk using the `fromFile()` method:

```
String result = fromFile("./file.tmp")
                .args("errNo", 101, "c", String.class)
                .fmt();
```

Escaping the `"#{"` can be done using the ``` ` ``` character:

```java
String result = template("#{errNo} + escaped: `#{errNo}")
                 .arg("errNo", 101)
                 .fmt();

System.out.println(result);
```

If one of the arguments is missing, or the mehod chain on the object cannot be executed, it replaces the value with `"null"` instead of throwing a `NullPointerException`.

Output: 
```
101 + escaped: #{errNo}
```

## Installing

The library is found in the [jcenter()](https://bintray.com/nomemory/maven/aleph-formatter) repo.

**jcenter()** needs to be added as a repository. 

For gradle:

```
compile 'net.andreinc.aleph:aleph-formatter:0.0.3'
```

For maven:


```
<dependency>
  <groupId>net.andreinc.aleph</groupId>
  <artifactId>aleph-formatter</artifactId>
  <version>0.0.3</version>
  <type>pom</type>
</dependency>
```
