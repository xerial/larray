package xerial

/**
 * == LArray ==
 * A library for managing large off-heap arrays that can hold more than 2G (2^31) entries in Java and Scala.
 *
 * === Features ===
 *
 *  - Supporting huge array size upto 2^63 -1 entries.
 *    - 2^31 -1 (2G) is the limitation of the default Java/Scala array size, because 32-bit signed integer (int) is used for the array indexes. To resolve this, LArray uses long type indexes of 64-bit signed integers.
 *    - For example the entire human genome data (3GB) can be stored in LArray.
 *  - LArray can be released from the main memory immediately.
 *    - Call LArray.free to release acquired memory resources.
 *    - The default arrays in Java/Scala are resident in JVM heaps, in which users cannot free the allocate arrays space even if they become unnecessary. That means it is hard to avoid OutOfMemoryException when working with large amount of data.
 *  - LArray is in sync with Garbage Collection (GC)
 *    - Even if you forget to call LArray.free, the acquired memory will be released when GC sweeps LArray instances.
 *    - To prevent accidental memory release, keep a reference to LArray somewhere (e.g., in List)
 *  - LArray uses off-heap memory, so it is free from the limitation of JVM memory manager.
 *    - LArray uses memory space outside of the default JVM heap, so creating LArrays with more than -Xmx(maximum memory size) is possible. This is useful when you need large amount of memory or the amount of memory required in your application is unknown.
 *  - Fast copy and memory allocation
 *    - LArray internally uses concurrent memory allocator suited to multi-threaded programs.
 *  - Rich set of operations for LArray[A]
 *    - map, filter, reduce, zip, etc.
 *
 * === Limitations ===
 *
 *  - LArray[A] of generic objects (e.g., LArray[String], LArray[AnyRef]) cannot be released immedeately from the main memory, because objects other than primitive types need to be created on JVM heaps and become subject to GC.
 *   - To release objects from main memory, you need to create *off-heap* objects. For example, create a large `LArray[Byte]`, then align your object data on the array. Object parameters can be retrieved with `LArray[Byte].getInt(offset)`, `getFloat(offset)`, etc.
 *
 */
package object larray {


}