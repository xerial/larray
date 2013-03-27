package xerial

import reflect.ClassTag

/**
 * == LArray ==
 * [[xerial.larray.LArray]] is a large off-heap array that can hold more than 2G (2^31) entries.
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
 *  - Rich set of operations for LArray[A]
 *    - map, filter, reduce, zip, etc.
 *    - See [[xerial.larray.LArray]]
 *
 * === Limitations ===
 *
 *  - LArray[A] of generic objects (e.g., LArray[String], LArray[AnyRef]) cannot be released immedeately from the main memory, because objects other than primitive types need to be created on JVM heaps and become subject to GC.
 *   - To release objects from main memory, you need to create *off-heap* objects. For example, create a large `LArray[Byte]`, then align your object data on the array. Object parameters can be retrieved with `LArray[Byte].getInt(offset)`, `getFloat(offset)`, etc.
 *
 */
package object larray {

  implicit class ConvertArrayToLArray[A : ClassTag](arr:Array[A]) {
    def toLArray : LArray[A] = {
      val l = LArray.of[A](arr.length).asInstanceOf[RawByteArray[A]]
      LArray.impl.asInstanceOf[xerial.larray.impl.LArrayNativeAPI].copyFromArray(arr, 0, l.address, l.byteLength.toInt)
      l
    }
  }

  implicit class ConvertIterableToLArray[A : ClassTag](it:Iterable[A]) {
    def toLArray : LArray[A] = {
      val b = LArray.newBuilder[A]
      it.foreach(b += _)
      b.result
    }
  }

  implicit class AsLRange(from:Long) {
    def Until(to:Long) : LIterator[Long] = Until(to, 1)
    def Until(to:Long, step:Long) : LIterator[Long] = new AbstractLIterator[Long] {
      private var cursor = from
      def hasNext = cursor < to
      def next() = {
        val v = cursor
        cursor += step
        v
      }
    }


    def To(to:Long) : LIterator[Long] = To(to, 1)

    def To(to:Long, step:Long) : LIterator[Long] = new AbstractLIterator[Long] {
      private var cursor = from
      def hasNext = cursor <= to
      def next() = {
        val v = cursor
        cursor += step
        v
      }
    }

  }



}