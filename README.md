LArray
=== 
A library for managing large arrays that can hold more than 2G (2^31) entries in Java and Scala.

## Features 
 * 2^31 (2GB) is the limitation of the default Java/Scala array size, because 32-bit signed integer (int) is used for the array indexes. To resolve this, LArray uses long type indexes that uses 64-bit signed integer.
   * For example the entire human genome data (3GB) can be stored in LArray. 
 * LArray can be released from the main memory at any time. 
   * The default arrays in Java/Scala consumes JVM heaps heavily and often causes OutOfMemory error when working with such large amount of data. 
   * Call LArray.free to release acquired memory resources immediately.
   * Even if you forget to call LArray.free, the acquired memory will be released when GC sweeps LArray instances.
 * LArray is free from the limitation of JVM memory manager.
   * LArray uses memory space outside of the default JVM heap, so creating LArrays with more than -Xmx(maximum memory size) is possible. This is useful when you need large amount of memory or its size is unknown.


## Supported Platforms

LArray uses OS-specific implementation for copying memory contents between LArray and Java arrays. Currently, the following CPU architecutres are supported:

 * Windows (32/64-bit)
 * Linux (amd64 (Intel 64-bit), arm, armhf)
 * Mac OSX (Intel 64bit)

In addition, Oracle JVM (standard JVM) must be used since LArray depends on `sun.misc.Unsafe` class.

## Usage (Scala)
Add the following sbt dependencies to your project settings:

    libraryDependencies += "org.xerial" % "larray" % "0.1"

You can use LArray in the same manner with the standard Scala Arrays: 

    val l = LArray(1, 2, 3)
    val l = LArray[Int].ofDim(10000L)

## Usage (Java)

    (to be written)

