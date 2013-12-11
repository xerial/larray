LArray
=== 
A library for managing large off-heap arrays that can hold more than 2G (2^31) entries in Java and Scala. Notably LArray is *disposable* by calling `LArray.free` or you can let GC automatically release the memory. LArray also can be used to create an `mmap` (memory-mapped file) whose size is more than 2GB. 

## Features 
 * LArray can create arrays with more than 2G(2^31) entries.
   * 2^31 -1 (2G) is the limitation of the default Java/Scala array size, because these arrays use 32-bit signed integer (int) as indexes. LArray uses long type indexes of 64-bit signed integers to resolve this limitation.
   * For example, the entire human genome data (3GB) can be stored in LArray. 
 * LArray can be released immediately from the memory.
   * Call `LArray.free`.
   * The default arrays in Java/Scala stay in JVM heaps until they are collected by GC, so it is generally difficult to avoid `OutOfMemoryException` when working with large amount of data. For example, call `new Array[Int](1000)` x 10,000 times. You are lucky if you don't see OutOfMemoryException.
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


## Performance

### Memory allocation
Here is a simple benchmark result that compares concurrent memory-allocation performances of LArray (with or without zero-filling), java arrays, `ByteBuffer.allocate` and `ByteBuffer.allocateDirect`, using Mac OS X with 2.9GHz Intelli Core i7. This test allocates 100 x 1MB of memory space concurrently using multiple threads, and repeats this process 20 times. 

```
-concurrent allocation	total:2.426 sec. , count:   10, avg:0.243 sec. , core avg:0.236 sec. , min:0.159 sec. , max:0.379 sec.
  -without zero-filling	total:0.126 sec. , count:   20, avg:6.279 msec., core avg:2.096 msec., min:1.405 msec., max:0.086 sec.
  -with zero-filling	total:0.476 sec. , count:   20, avg:0.024 sec. , core avg:0.023 sec. , min:0.017 sec. , max:0.037 sec.
  -java array     	    total:0.423 sec. , count:   20, avg:0.021 sec. , core avg:0.021 sec. , min:0.014 sec. , max:0.029 sec.
  -byte buffer    	    total:1.028 sec. , count:   20, avg:0.051 sec. , core avg:0.044 sec. , min:0.014 sec. , max:0.216 sec.
  -direct byte buffer   total:0.360 sec. , count:   20, avg:0.018 sec. , core avg:0.018 sec. , min:0.015 sec. , max:0.026 sec.
```

All allocators except LArray are orders of magnitude slower than LArray, and consumes CPUs because they need to fill the allocated memory with zeros due to their specification.

In a single thread execution, you can see more clearly how fast LArray can allocate memories.    
```
-single-thread allocation	total:3.655 sec. , count:   10, avg:0.366 sec. , core avg:0.356 sec. , min:0.247 sec. , max:0.558 sec.
  -without zero-filling	total:0.030 sec. , count:   20, avg:1.496 msec., core avg:1.125 msec., min:0.950 msec., max:8.713 msec.
  -with zero-filling	total:0.961 sec. , count:   20, avg:0.048 sec. , core avg:0.047 sec. , min:0.044 sec. , max:0.070 sec.
  -java array     	    total:0.967 sec. , count:   20, avg:0.048 sec. , core avg:0.037 sec. , min:0.012 sec. , max:0.295 sec.
  -byte buffer    	    total:0.879 sec. , count:   20, avg:0.044 sec. , core avg:0.033 sec. , min:0.014 sec. , max:0.276 sec.
  -direct byte buffer	total:0.812 sec. , count:   20, avg:0.041 sec. , core avg:0.041 sec. , min:0.032 sec. , max:0.049 sec.
```

### Snappy Compression

LArray (and LBuffer) has memory address that can be used for seamlessly interacting with fast native methods through JNI. Here is an example of using `rawCompress(...)` in [snappy-java](http://github.com/xerial/snappy-java), which can take raw-memory address to compress/uncompress the data using C++ code, and is generally faster than [Dain's pure-java version of Snappy](http://github.com/dain/snappy).

```
[SnappyCompressTest]
-compress       	total:0.017 sec. , count:   10, avg:1.669 msec., core avg:0.769 msec., min:0.479 msec., max:0.010 sec.
  -LBuffer -> LBuffer (raw)	total:1.760 msec., count:   50, avg:0.035 msec., core avg:0.030 msec., min:0.024 msec., max:0.278 msec.
  -Array -> Array (raw) 	total:1.450 msec., count:   50, avg:0.029 msec., core avg:0.027 msec., min:0.023 msec., max:0.110 msec.
  -Array -> Array (dain)	total:0.011 sec. , count:   50, avg:0.225 msec., core avg:0.141 msec., min:0.030 msec., max:4.441 msec.
[SnappyCompressTest]
-decompress     	total:7.722 msec., count:   10, avg:0.772 msec., core avg:0.473 msec., min:0.418 msec., max:3.521 msec.
  -LBuffer -> LBuffer (raw)	total:1.745 msec., count:   50, avg:0.035 msec., core avg:0.029 msec., min:0.020 msec., max:0.331 msec.
  -Array -> Array (raw) 	total:1.189 msec., count:   50, avg:0.024 msec., core avg:0.021 msec., min:0.018 msec., max:0.149 msec.
  -Array -> Array (dain)	total:2.571 msec., count:   50, avg:0.051 msec., core avg:0.027 msec., min:0.025 msec., max:1.240 msec.
```

 * [Test code](larray/src/test/scala/xerial/larray/SnappyCompressTest.scala)

  

## Modules

LArray consists of three-modules.

 * **larray-buffer** (Java) Off-heap memory buffer `LBuffer` and its allocator with GC support.
 * **larray-mmap**   (Java + JNI (C code)) Memory-mapped file implementaiton `MMapBuffer`
 * **larray** (Scala and Java API) Provdes rich set of array operations through `LArray` class.

You can use each module independently. For example, if you only need an off-heap memory allocator that collects memory upon GC, use `LBuffer` in **larray-buffer**. 

Simply you can include **larray** to the dependency in Maven or SBT so that all modules will be added to your classpaths.

## Supported Platforms

A standard JVM, (e.g. Oracle JVM (standard JVM, HotSpotVM) or OpenJDK) must be used since 
**larray-buffer** depends on `sun.misc.Unsafe` class to allocate off-heap memory.

**larray-mmap** (MMapBuffer and LArray.mmap) uses JNI and is available for the following major CPU architectures:

 * Windows (32/64-bit)
 * Linux (i368, amd64 (Intel 64-bit), arm, armhf)
 * Mac OSX (Intel 64bit)


## History
 * November 11, 2013  version 0.2.1 - Use orgnization name `org.xerial.larray`. Add LBuffer.view.  
 * November 11, 2013  version 0.2 - Extracted pure-java modules (larray-buffer.jar and larray-mmap.jar) from larray.jar (for Scala). 
 * August 28, 2013  version 0.1.2 - improved memory layout
 * August 28, 2013  version 0.1.1 (for Scala 2.10.2)
 * Apr 23, 2013   Released version 0.1

## Usage (Scala)

### sbt settings
Add the following sbt dependency to your project settings:

```scala
libraryDependencies += "org.xerial.larray" % "larray" % "0.2.1"
```

 * Using snapshot versions:

```scala
resolvers += "Sonatype shapshot repo" at "https://oss.sonatype.org/content/repositories/snapshots/"

libraryDependencies += "org.xerial.larray" % "larray" % "0.2.2-SNAPSHOT"
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

For more examples, see [xerial/larray/example/LArrayExample.scala](larray/src/main/scala/xerial/larray/example/LArrayExample.scala)

## Usage (Java)

Add the following dependency to your pom.xml (Maven):
```xml
<dependency>
  <groupId>org.xerial.larray</groupId>
  <artifactId>larray</artifactId>
  <version>0.2.1</version>
</dependency>
```

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
For more examples, see [xerial/larray/example/LArrayJavaExample.scala](larray/src/main/scala/xerial/larray/example/LArrayJavaExample.java)

## Scaladoc

 * [LArray Scala API](https://oss.sonatype.org/service/local/repositories/releases/archive/org/xerial/larray/larray/0.2.1/larray-0.2.1-javadoc.jar/!/index.html#xerial.larray.package)
 * [larray-buffer Java API](https://oss.sonatype.org/service/local/repositories/releases/archive/org/xerial/larray/larray-buffer/0.2.1/larray-buffer-0.2.1-javadoc.jar/!/index.html#)
 * [larray-mmap Java API](https://oss.sonatype.org/service/local/repositories/releases/archive/org/xerial/larray/larray-mmap/0.2.1/larray-mmap-0.2.1-javadoc.jar/!/index.html#)
 
## For developers

* Building LArray: `./sbt compile`
* Run tests: `./sbt ~test`
* Creating IntelliJ IDEA project: `./sbt gen-idea`


