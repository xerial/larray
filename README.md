LArray
=== 
A library for managing large off-heap arrays that can hold more than 2G (2^31) entries in Java and Scala.

## Features 
 * LAray can create arrays with more than 2G(2^31) entries.
   * 2^31 -1 (2G) is the limitation of the default Java/Scala array size, because these arrays use 32-bit signed integer (int) as indexes. LArray uses long type indexes of 64-bit signed integers to resolve this limitation.
   * For example, the entire human genome data (3GB) can be stored in LArray. 
 * With LArray.free, it can be released immediately from the memory.
   * The default arrays in Java/Scala stay in JVM heaps until they are collected by GC, so it is generally difficult to avoid OutOfMemoryException when working with large amount of data. For example, call `new Array[Int](1000)` x 10,000 times. You are lucky if you don't see OutOfMemoryException.
 * LArray can be collected by Garbage Collector (GC)
   * Even if you forget to call LArray.free, the acquired memory will be released when GC sweeps LArray instances.
   * To prevent accidental memory release, keep a reference to LArray somewhere (e.g., in List) as in the standard Java/Scala program.
 * LArray resides in off-heap memory 
   * LArray uses a memory space outside the JVM heap, so creating LArrays with more than -Xmx(maximum heap size) is possible. This is useful when you need large amount of memory, or it is unknown how much memory is required in your application.
 * Fast memory allocation
   * LArray internally uses a concurrent memory allocator suited to multi-threaded programs, which is faster than the default JVM memory allocator.
   * LArray by default skips the array initialization (zero-filling), which improves the memory allocation speed significantly.
 * LArray can be used as DirectBuffer
   * Enables zero-copy transfer to/from files, network, etc.
   * Zero-copy compression with [snappy-java](https://github.com/xerial/snappy-java) (supported since version 1.1.0-M4. Pass LArray.address to Snappy.rawCompress etc.) 
 * Rich set of operations for LArray[A]
   * map, filter, reduce, zip, etc. Almost all collection operations in Scala are already implemented for LArray[A].
 * Supports Memory-mapped file larger than 2GB 
   * Use `LArray.mmap`
   * It can create memory regions that can be shared between processes.
 
## Limitations

  * LArray[A] of generic objects (e.g., LArray[String], LArray[AnyRef]) cannot be released immedeately from the main memory, because objects other than primitive types need to be created on JVM heaps and they are under the control of GC. 
    * To release objects from main memory, you need to create *off-heap* objects. For example, create a large `LArray[Byte]`, then align your object data on the array. Object parameters can be retrieved with `LArray[Byte].getInt(offset)`, `getFloat(offset)`, etc. 
  

## Supported Platforms

LArray uses OS-specific implementation for mmap and copying memory contents between LArray and Java arrays. Currently, the following CPU architecutres are supported:

 * Windows (32/64-bit)
 * Linux (i368, amd64 (Intel 64-bit), arm, armhf)
 * Mac OSX (Intel 64bit)

In addition, Oracle JVM (standard JVM, HotSpotVM) or OpenJDK must be used since LArray depends on `sun.misc.Unsafe` class.

## History
 * August 28, 2013  version 0.1.2 - improved memory layout
 * August 28, 2013  version 0.1.1 (for Scala 2.10.2)
 * Apr 23, 2013   Released version 0.1

## Usage (Scala)

### sbt settings
Add the following sbt dependency to your project settings:

```scala
libraryDependencies += "org.xerial" % "larray" % "0.1.2"
```

 * Using snapshot versions:

```scala
resolvers += "Sonatype shapshot repo" at "https://oss.sonatype.org/content/repositories/snapshots/"

libraryDependencies += "org.xerial" % "larray" % "0.2-SNAPSHOT"
```
### Example

LArray can be used in the same manner with the standard Scala Arrays: 

```scala
import xerial.larray._

val l = LArray(1, 2, 3)
val e = l(0) // 1
println(l.mkString(", ")) // 1, 2, 3
l(1) = 5
println(l.mkString(", ")) // 1, 5, 3
    
// Create an LArray of Int type
val l2 = LArray.of[Int](10000L)

// Release the memory resource
l2.free 

l2(0) // The result of accessing released LArray is undefined
```

For more examples, see [xerial/larray/example/LArrayExample.scala](https://github.com/xerial/larray/blob/develop/src/main/scala/xerial/larray/example/LArrayExample.scala)

## Usage (Java)

Add the following dependency to your pom.xml (Maven): 
```xml
<dependency>
  <groupId>org.xerial</groupId>
  <artifactId>larray</artifactId>
  <version>0.1.2</version>
</dependency>
```

### Manual download

To use LArray without sbt or Maven, append all of the following jar files to your classpath:

 * [larray-0.1.2.jar](http://repo1.maven.org/maven2/org/xerial/larray/0.1.2/larray-0.1.2.jar)
 * [scala-library-2.10.2.jar](http://repo1.maven.org/maven2/org/scala-lang/scala-library/2.10.2/scala-library-2.10.2.jar)
 * [xerial-core-3.2.1.jar](http://repo1.maven.org/maven2/org/xerial/xerial-core/3.2.1/xerial-core-3.2.1.jar)

### Example 

In Java we cannot provide concise syntaxes as in Scala. Instead, use `apply` and `update` methods to read/write values in an array.

```java
import xerial.larray.japi.LArrayJ;
import xerial.larray.*;

LIntArray l = LArrayJ.newLIntArray(10000L);
l.update(0L, 20); // Set l[0L] = 20
int e0 = l.apply(0L);  //  Get l[0L]

// release 
l.free();
```
For more examples, see [xerial/larray/example/LArrayJavaExample.scala](https://github.com/xerial/larray/blob/develop/src/main/scala/xerial/larray/example/LArrayJavaExample.java)

## Scaladoc

 * [LArray Scala API](https://oss.sonatype.org/service/local/repositories/releases/archive/org/xerial/larray/0.1.2/larray-0.1.2-javadoc.jar/!/index.html#xerial.larray.package)
 
