//--------------------------------------
//
// LArray.scala
// Since: 2013/03/13 13:40
//
//--------------------------------------

package xerial.larray

import scala.reflect.ClassTag
import xerial.core.log.Logger
import java.nio.ByteBuffer
import java.nio.channels.{FileChannel, WritableByteChannel}
import sun.nio.ch.DirectBuffer
import java.lang
import java.io.{FileInputStream, FileOutputStream, File}


/**
 * Read-only interface of [[xerial.larray.LArray]]
 * @tparam A
 */
trait LSeq[A] extends LIterable[A] {
  /**
   * Size of this array
   * @return size of this array
   */
  def size: Long

  /**
   * byte length of this array
   * @return
   */
  def byteLength: Long = elementByteSize * size

  /**
   * Retrieve an element
   * @param i index
   * @return the element value
   */
  def apply(i: Long): A

  /**
   * Byte size of an element. For example, if A is Int, its elementByteSize is 4
   */
  private[larray] def elementByteSize : Int


  /**
   * Create a sequence of DirectByteBuffer that projects LArray contents
   * @return sequence of `java.nio.ByteBuffer`
   */
  def toDirectByteBuffer : Array[ByteBuffer] =
    throw new UnsupportedOperationException("toDirectByteBuffer")


  /**
   * Save to a file.
   * @param f
   * @return
   */
  def saveTo(f:File) : File = {
    val fout = new FileOutputStream(f).getChannel
    try {
      fout.write(this.toDirectByteBuffer)
      f
    }
    finally
      fout.close
  }

  /**
   * Copy the contents of this LSeq[A] into the target LByteArray
   * @param dst
   * @param dstOffset
   */
  def copyTo(dst:LByteArray, dstOffset:Long)

  /**
   * Copy the contents of this sequence into the target LByteArray
   * @param srcOffset
   * @param dst
   * @param dstOffset
   * @param blen the byte length to copy
   */
  def copyTo[B](srcOffset:Long, dst:RawByteArray[B], dstOffset:Long, blen:Long)



}

/**
 * LArray is a mutable large array.
 *
 * The differences from the standard Array[A] are:
 *
 *  - LArray accepts Long type indexes, so it is possible to have more than 2G (2^31-1) entries, which is a limitation of the standard Array[A].
 *  - The memory resource of LArray[A] resides outside of the normal garbage-collected JVM heap and can be released via [[xerial.larray.LArray#free]].
 *    - If [[LArray#free]] is not called, the acquried memory stays until LArray is collected by GC.
 *  - LArray elements are not initialized, so explicit initialization using [[LArray#clear]] is necessary.
 *
 * @tparam A element type
 */
trait LArray[A] extends LSeq[A] with WritableByteChannel  {

  def isOpen: Boolean = true

  def close() { free }

  /**
   * Release the memory of LArray. After calling this method, the results of calling the other methods becomes undefined or might cause JVM crash.
   */
  def free

  /**
   * Wraps with immutable interface
   * @return
   */
  def toLSeq : LSeq[A] = this.asInstanceOf[LSeq[A]]

  /**
   * Clear the contents of the array. It simply fills the array with zero bytes.
   */
  def clear()

  def write(src:ByteBuffer) : Int =
    throw new UnsupportedOperationException("writeToArray(ByteBuffer)")


  /**
   * Update an element
   * @param i index to be updated
   * @param v value to set
   * @return the value
   */
  def update(i: Long, v: A): A

  def view(from:Long, to:Long) : LArrayView[A]

  override def toString = mkString(", ")
}


/**
 * LArray factory
 * @author Taro L. Saito
 */
object LArray {

  private[larray] val impl = xerial.larray.impl.LArrayLoader.load


  import _root_.java.{lang=>jl}

  /**
   * Load the contents of a file into LArray
   * @param f the file to read
   * @tparam A the element type
   * @return LArray contains the file contents
   */
  def loadFrom[A : ClassTag](f:File) : LArray[A] = {
    val fin = new FileInputStream(f).getChannel
    val tag = implicitly[ClassTag[A]]
    try {
      tag.runtimeClass match {
        case jl.Boolean.TYPE =>  {
          val arr = new Array[Byte](8)
          val bb = ByteBuffer.wrap(arr)
          fin.read(bb)
          val numBits = bb.getLong(0)
          val fileSize = fin.size()
          val b = new LBitArrayBuilder
          var pos = 8L
          b.sizeHint(fileSize - pos)
          while(pos < fileSize) {
            pos += fin.transferTo(pos, fileSize - pos, b)
          }
          b.result(numBits).asInstanceOf[LArray[A]]
        }
        case _ => {
          var pos = 0L
          val fileSize = fin.size()
          val b = LArray.newBuilder[A]
          b.sizeHint(fileSize)
          while(pos < fileSize) {
            pos += fin.transferTo(pos, fileSize - pos, b)
          }
          b.result()
        }
      }
    }
    finally
      fin.close
  }


  object EmptyArray
    extends LArray[Nothing]
    with LIterable[Nothing]
  {
    private[larray] def elementByteSize : Int = 0

    def clear() {}

    override def toDirectByteBuffer = Array.empty

    def newBuilder = LArray.newBuilder[Nothing]

    def size: Long = 0L

    def apply(i: Long): Nothing = {
      sys.error("not allowed")
    }

    def update(i: Long, v: Nothing): Nothing = {
      sys.error("not allowed")
    }

    def free {
      /* do nothing */
    }
    def copyTo(dst: LByteArray, dstOffset: Long) {
      // do nothing
    }

    def copyTo[B](srcOffset:Long, dst:RawByteArray[B], dstOffset:Long, len:Long) {
      // do nothing
    }

    def view(from: Long, to: Long) = LArrayView.EmptyView
  }



  def of[A : ClassTag](size:Long) : LArray[A] = {
    val tag = implicitly[ClassTag[A]]
    tag.runtimeClass match {
      case jl.Integer.TYPE => new LIntArray(size).asInstanceOf[LArray[A]]
      case jl.Byte.TYPE => new LByteArray(size).asInstanceOf[LArray[A]]
      case jl.Character.TYPE => new LCharArray(size).asInstanceOf[LArray[A]]
      case jl.Short.TYPE => new LShortArray(size).asInstanceOf[LArray[A]]
      case jl.Long.TYPE => new LLongArray(size).asInstanceOf[LArray[A]]
      case jl.Float.TYPE => new LFloatArray(size).asInstanceOf[LArray[A]]
      case jl.Double.TYPE => new LDoubleArray(size).asInstanceOf[LArray[A]]
      case jl.Boolean.TYPE => new LBitArray(size).asInstanceOf[LArray[A]]
      case _ => LObjectArray.ofDim[A](size)
    }
  }

  val emptyBooleanArray = LArray.of[Boolean](0)
  val emptyByteArray    = LArray.of[Byte](0)
  val emptyCharArray    = LArray.of[Char](0)
  val emptyDoubleArray  = LArray.of[Double](0)
  val emptyFloatArray   = LArray.of[Float](0)
  val emptyIntArray     = LArray.of[Int](0)
  val emptyLongArray    = LArray.of[Long](0)
  val emptyShortArray   = LArray.of[Short](0)
  val emptyObjectArray  = LArray.of[Object](0)

  def empty[A : ClassTag] : LArray[A] = {
    val tag = implicitly[ClassTag[A]]
    tag.runtimeClass match {
    case jl.Integer.TYPE => emptyIntArray.asInstanceOf[LArray[A]]
    case jl.Byte.TYPE => emptyByteArray.asInstanceOf[LArray[A]]
    case jl.Character.TYPE => emptyCharArray.asInstanceOf[LArray[A]]
    case jl.Short.TYPE => emptyShortArray.asInstanceOf[LArray[A]]
    case jl.Long.TYPE => emptyLongArray.asInstanceOf[LArray[A]]
    case jl.Float.TYPE => emptyFloatArray.asInstanceOf[LArray[A]]
    case jl.Double.TYPE => emptyDoubleArray.asInstanceOf[LArray[A]]
    case jl.Boolean.TYPE => emptyBooleanArray.asInstanceOf[LArray[A]]
    case _ => emptyObjectArray.asInstanceOf[LArray[A]]
    }
  }

  def apply() = EmptyArray


  private[larray] def wrap[A:ClassTag](byteSize:Long, m:Memory) : LArray[A] = {
    val tag = implicitly[ClassTag[A]]
    tag.runtimeClass match {
      case jl.Integer.TYPE => new LIntArray(byteSize / 4, m).asInstanceOf[LArray[A]]
      case jl.Byte.TYPE => new LByteArray(byteSize, m).asInstanceOf[LArray[A]]
      case jl.Long.TYPE => new LLongArray(byteSize / 8, m).asInstanceOf[LArray[A]]
      case jl.Character.TYPE => new LCharArray(byteSize / 2, m).asInstanceOf[LArray[A]]
      case jl.Short.TYPE => new LShortArray(byteSize/ 2, m).asInstanceOf[LArray[A]]
      case jl.Float.TYPE => new LFloatArray(byteSize / 4, m).asInstanceOf[LArray[A]]
      case jl.Double.TYPE => new LDoubleArray(byteSize / 8, m).asInstanceOf[LArray[A]]
      //case jl.Boolean.TYPE => new LBitArray(byteSize * 8, m).asInstanceOf[LArray[A]]
      case _ => sys.error(s"unsupported type: $tag")
    }
  }


  def toLArray[A:ClassTag](it:LIterator[A]) : LArray[A] = {
    val b = newBuilder[A]
    it.foreach(b += _)
    b.result
  }

  /**
   * Creates an LArray with given elements.
   *
   * @param xs the elements to put in the array
   * @return an array containing all elements from xs.
   */
  def apply[A : ClassTag](xs: A*): LArray[A] = {
    val size = xs.size
    val arr = new LObjectArray32[A](size)
    var i = 0
    for(x <- xs) { arr(i) = x; i += 1 }
    arr
  }

  def apply(first: Byte, elems: Byte*): LArray[Byte] = {
    val size = 1 + elems.size
    val arr = new LByteArray(size)
    arr(0) = first
    for ((e, i) <- elems.zipWithIndex) {
      arr(i + 1) = e
    }
    arr
  }


  def apply(first: Int, elems: Int*): LArray[Int] = {
    // elems: Int* => Seq[Int]
    val size = 1 + elems.size
    val arr = new LIntArray(size)
    // Populate the array elements
    arr(0) = first
    for ((e, i) <- elems.zipWithIndex) {
      arr(i + 1) = e
    }
    arr
  }


  def apply(first: Char, elems: Char*): LArray[Char] = {
    val size = 1 + elems.size
    val arr = new LCharArray(size)
    arr(0) = first
    for ((e, i) <- elems.zipWithIndex) {
      arr(i + 1) = e
    }
    arr
  }

  def apply(first: Short, elems: Short*): LArray[Short] = {
    val size = 1 + elems.size
    val arr = new LShortArray(size)
    arr(0) = first
    for ((e, i) <- elems.zipWithIndex) {
      arr(i + 1) = e
    }
    arr
  }

  def apply(first: Float, elems: Float*): LArray[Float] = {
    val size = 1 + elems.size
    val arr = new LFloatArray(size)
    arr(0) = first
    for ((e, i) <- elems.zipWithIndex) {
      arr(i + 1) = e
    }
    arr
  }


  def apply(first: Double, elems: Double*): LArray[Double] = {
    val size = 1 + elems.size
    val arr = new LDoubleArray(size)
    arr(0) = first
    for ((e, i) <- elems.zipWithIndex) {
      arr(i + 1) = e
    }
    arr
  }

  def apply(first: Long, elems: Long*): LArray[Long] = {
    val size = 1 + elems.size
    val arr = new LLongArray(size)
    arr(0) = first
    for ((e, i) <- elems.zipWithIndex) {
      arr(i + 1) = e
    }
    arr
  }

  def copy[A](src:LArray[A], srcPos:Long, dest:LArray[A], destPos:Long, length:Long) {
    import UnsafeUtil.unsafe
    val copyLen = math.min(length, math.min(src.size - srcPos, dest.size - destPos))

    (src, dest) match {
      case (a:UnsafeArray[A], b:UnsafeArray[A]) =>
        val elemSize = a.elementByteSize
        // Use fast memcopy
        unsafe.copyMemory(a.m.address + srcPos * elemSize, b.m.address + destPos * elemSize, copyLen * elemSize)
      case _ =>
        // slow copy
        var i = 0L
        while(i < copyLen) {
          dest(destPos+i) = src(srcPos+i)
          i += 1
        }
    }
  }

  /**
   * Create a new LArrayBuilder[A]
   * @tparam A
   * @return
   */
  def newBuilder[A : ClassTag] : LBuilder[A, LArray[A]] = LArrayBuilder.make[A]


  /** Creates LArary with given dimensions */
  def ofDim[A:ClassTag](size:Long) = LArray.of[A](size)


  /**
   * Creates a 2-dimensional array. Returned LArray[LArray[A]] cannot be released immediately. If you need
   * relesable arrays, use [[xerial.larray.LArray2D]].
   *
   * */
  def ofDim[A: ClassTag](n1: Long, n2: Long): LArray[LArray[A]] = {
    val arr: LArray[LArray[A]] = LArray.of[LArray[A]](n1)
    var i = 0L
    while(i < n1) {
      arr(i) = LArray.of[A](n2)
      i += 1
    }
    arr
  }
  /** Creates a 3-dimensional array */
  def ofDim[A: ClassTag](n1: Long, n2: Long, n3: Long): LArray[LArray[LArray[A]]] =
    tabulate(n1)(_ => ofDim[A](n2, n3))
  /** Creates a 4-dimensional array */
  def ofDim[A: ClassTag](n1: Long, n2: Long, n3: Long, n4: Long): LArray[LArray[LArray[LArray[A]]]] =
    tabulate(n1)(_ => ofDim[A](n2, n3, n4))
  /** Creates a 5-dimensional array */
  def ofDim[A: ClassTag](n1: Long, n2: Long, n3: Long, n4: Long, n5: Long): LArray[LArray[LArray[LArray[LArray[A]]]]] =
    tabulate(n1)(_ => ofDim[A](n2, n3, n4, n5))


  def tabulate[A: ClassTag](n: Long)(f: Long => A): LArray[A] = {
    val b = newBuilder[A]
    b.sizeHint(n)
    var i = 0
    while (i < n) {
      b += f(i)
      i += 1
    }
    b.result
  }


}

/**
 * read/write operations that can be supported for LArrays using raw byte arrays as their back-end.
 */
trait RawByteArray[A] extends LArray[A] {

  def address : Long

  /**
   * Get a byte at the index
   * @return
   */
  def readByte(index:Long) : Int


  /**
   * Write the contents of this array to the destination buffer
   * @param srcOffset byte offset
   * @param dest destination array
   * @param destOffset offset in the destination array
   * @param length the byte length to write
   * @return byte length to write
   */
  def writeToArray(srcOffset:Long, dest:Array[Byte], destOffset:Int, length:Int) : Int

  /**
   * Read the contents from a given source buffer
   * @param src source buffer
   * @param srcOffset byte offset in the source buffer
   * @param destOffset byte offset from the destination address
   * @param length byte length to read from the source
   */
  def readFromArray(src:Array[Byte], srcOffset:Int, destOffset:Long, length:Int) : Int


  /**
   * Create an input stream for reading LArray byte contents
   * @return
   */
  def toInputStream : java.io.InputStream = LArrayInputStream(this)


  def getByte(offset:Long) : Byte
  def getChar(offset:Long) : Char
  def getShort(offset:Long) : Short
  def getInt(offset:Long) : Int
  def getFloat(offset:Long) : Float
  def getLong(offset:Long) : Long
  def getDouble(offset:Long) : Double

  def putByte(offset:Long, v:Byte)
  def putChar(offset:Long, v:Char)
  def putShort(offset:Long, v:Short)
  def putInt(offset:Long, v:Int)
  def putFloat(offset:Long, v:Float)
  def putLong(offset:Long, v:Long)
  def putDouble(offset:Long, v:Double)

}





private[larray] trait UnsafeArray[T] extends RawByteArray[T] with Logger { self: LArray[T] =>

  private[larray] def m: Memory

  import UnsafeUtil.unsafe

  private var cursor = 0L

  def address = m.address

  def clear() {
    unsafe.setMemory(address, byteLength, 0)
  }


  override def write(src:ByteBuffer) : Int = {
    val len = math.max(src.limit - src.position, 0)
    val writeLen = src match {
      case d:DirectBuffer =>
        unsafe.copyMemory(d.address() + src.position(), m.address + cursor, len)
        len
      case arr if src.hasArray =>
        readFromArray(src.array(), src.position(), cursor, len)
      case _ =>
        var i = 0L
        while(i < len) {
          m.putByte(m.address + i, src.get((src.position() + i).toInt))
          i += 1
        }
        len
    }
    cursor += writeLen
    src.position(src.position + writeLen)
    writeLen
  }

  override def toDirectByteBuffer: Array[ByteBuffer] = {
    var pos = 0L
    val b = Array.newBuilder[ByteBuffer]
    val limit = byteLength
    while(pos < limit){
      val len : Long = math.min(limit - pos, Int.MaxValue)
      b += UnsafeUtil.newDirectByteBuffer(m.address + pos, len.toInt)
      pos += len
    }
    b.result()
  }

  /**
   * Write the contents of this array to the destination buffer
   * @param srcOffset byte offset
   * @param dest destination array
   * @param destOffset offset in the destination array
   * @param length the byte length to write
   * @return written byte length
   */
  def writeToArray(srcOffset: Long, dest: Array[Byte], destOffset: Int, length: Int): Int = {
    val writeLen = math.min(dest.length - destOffset, math.min(length, byteLength - srcOffset)).toInt
    trace("copy to array")
    LArray.impl.asInstanceOf[xerial.larray.impl.LArrayNativeAPI].copyToArray(m.address + srcOffset, dest, destOffset, writeLen)
    writeLen
  }

  def readFromArray(src:Array[Byte], srcOffset:Int, destOffset:Long, length:Int) : Int = {
    val readLen = math.min(src.length-srcOffset, math.min(byteLength - destOffset, length)).toInt
    LArray.impl.asInstanceOf[xerial.larray.impl.LArrayNativeAPI].copyFromArray(src, srcOffset, m.address + destOffset, readLen)
    readLen
  }

  def copyTo(dst: LByteArray, dstOffset: Long) {
    unsafe.copyMemory(address, dst.address + dstOffset, byteLength)
  }

  def copyTo[B](srcOffset:Long, dst:RawByteArray[B], dstOffset:Long, blen:Long) {
    unsafe.copyMemory(address + srcOffset, dst.address + dstOffset, blen)
  }

  def readByte(index:Long) = m.getByte(index)

  /**
   * Release the memory of LArray. After calling this method, the results of calling the behavior of the other methods becomes undefined or might cause JVM crash.
   */
  def free { m.free }


  def getByte(offset:Long) : Byte = m.getByte(offset)
  def getChar(offset:Long) : Char = m.getChar(offset)
  def getShort(offset:Long) : Short = m.getShort(offset)
  def getInt(offset:Long) : Int = m.getInt(offset)
  def getFloat(offset:Long) : Float = m.getFloat(offset)
  def getLong(offset:Long) : Long = m.getLong(offset)
  def getDouble(offset:Long) : Double = m.getDouble(offset)

  def putByte(offset:Long, v:Byte) = m.putByte(offset, v)
  def putChar(offset:Long, v:Char) = m.putChar(offset, v)
  def putShort(offset:Long, v:Short) = m.putShort(offset, v)
  def putInt(offset:Long, v:Int) = m.putInt(offset, v)
  def putFloat(offset:Long, v:Float) = m.putFloat(offset, v)
  def putLong(offset:Long, v:Long) = m.putLong(offset, v)
  def putDouble(offset:Long, v:Double) = m.putDouble(offset, v)

}


class LCharArray(val size: Long, private[larray] val m:Memory)(implicit alloc: MemoryAllocator)
  extends LArray[Char]
  with UnsafeArray[Char]
{
  protected[this] def newBuilder = new LCharArrayBuilder

  def this(size: Long)(implicit alloc: MemoryAllocator = MemoryAllocator.default) = this(size, alloc.allocate(size << 1))
  import UnsafeUtil.unsafe

  def apply(i: Long): Char = {
    unsafe.getChar(m.address + (i << 1))
  }

  // a(i) = a(j) = 1
  def update(i: Long, v: Char): Char = {
    unsafe.putChar(m.address + (i << 1), v)
    v
  }

  /**
   * Byte size of an element. For example, if A is Int, its elementByteSize is 4
   */
  private[larray] def elementByteSize: Int = 2
  def view(from: Long, to: Long) = new LArrayView.LCharArrayView(this, from, to - from)

}

/**
 * LArray of Int type
 * @param size  the size of array
 * @param m allocated memory
 * @param alloc memory allocator
 */
class LIntArray(val size: Long, private[larray] val m:Memory)(implicit alloc: MemoryAllocator)
  extends LArray[Int]
  with UnsafeArray[Int]
{
  protected[this] def newBuilder = new LIntArrayBuilder

  def this(size: Long)(implicit alloc: MemoryAllocator = MemoryAllocator.default) = this(size, alloc.allocate(size << 2))
  import UnsafeUtil.unsafe

  def apply(i: Long): Int = {
    unsafe.getInt(m.address + (i << 2))
  }

  // a(i) = a(j) = 1
  def update(i: Long, v: Int): Int = {
    unsafe.putInt(m.address + (i << 2), v)
    v
  }

  /**
   * Byte size of an element. For example, if A is Int, its elementByteSize is 4
   */
  private[larray] def elementByteSize: Int = 4

  def view(from: Long, to: Long) = new LArrayView.LIntArrayView(this, from, to - from)

}

/**
 * LArray of Long type
 * @param size  the size of array
 * @param m allocated memory
 * @param mem memory allocator
 */
class LLongArray(val size: Long, private[larray] val m:Memory)(implicit mem: MemoryAllocator)
  extends LArray[Long]
  with UnsafeArray[Long]
{
  def this(size: Long)(implicit mem: MemoryAllocator) = this(size, mem.allocate(size << 3))

  protected[this] def newBuilder = new LLongArrayBuilder
  private[larray] def elementByteSize: Int = 8

  import UnsafeUtil.unsafe

  def apply(i: Long): Long = {
    unsafe.getLong(m.address + (i << 3))
  }

  // a(i) = a(j) = 1
  def update(i: Long, v: Long): Long = {
    unsafe.putLong(m.address + (i << 3), v)
    v
  }

  def view(from: Long, to: Long) = new LArrayView.LLongArrayView(this, from, to - from)

}


/**
 * LArray of Byte type
 * @param size the size of array
 * @param m allocated memory
 * @param mem memory allocator
 */
class LByteArray(val size: Long, private[larray] val m:Memory)(implicit mem: MemoryAllocator)
  extends LArray[Byte]
  with UnsafeArray[Byte]
{
  self =>

  def this(size: Long)(implicit mem: MemoryAllocator) = this(size, mem.allocate(size))

  protected[this] def newBuilder = new LByteArrayBuilder

  private[larray] def elementByteSize: Int = 1

  /**
   * Retrieve an element
   * @param i index
   * @return the element value
   */
  def apply(i: Long): Byte = {
    UnsafeUtil.unsafe.getByte(m.address + i)
  }

  /**
   * Update an element
   * @param i index to be updated
   * @param v value to set
   * @return the value
   */
  def update(i: Long, v: Byte): Byte = {
    UnsafeUtil.unsafe.putByte(m.address + i, v)
    v
  }

  def sort {

    def sort(left:Long, right:Long) {
      val NUM_BYTE_VALUES = 256
      // counting sort
      val count: Array[Int] = new Array[Int](NUM_BYTE_VALUES)

      {
        var i : Long = left - 1
        while ({ i += 1; i <= right}) {
          count(self(i) - Byte.MinValue) += 1
        }
      }

      {
        var i = NUM_BYTE_VALUES
        var k : Long = right + 1
        while (k > left) {
          while({ i -= 1; count(i) == 0} ) {}
          val value: Byte = (i + Byte.MinValue).toByte
          var s = count(i)
          do {
            k -= 1
            self(k) = value
          } while (({s -= 1; s}) > 0)
        }
      }
    }

    sort(0L, size-1L)
  }


  def view(from: Long, to: Long) = new LArrayView.LByteArrayView(this, from, to - from)

}

class LDoubleArray(val size: Long, private[larray] val m:Memory)(implicit mem: MemoryAllocator)
  extends LArray[Double]
  with UnsafeArray[Double]
{
  def this(size: Long)(implicit  mem: MemoryAllocator) = this(size, mem.allocate(size << 3))



  private [larray] def elementByteSize = 8

  import UnsafeUtil.unsafe

  def apply(i: Long): Double =
  {
    unsafe.getDouble(m.address + (i << 3))
  }

  // a(i) = a(j) = 1
  def update(i: Long, v: Double): Double =
  {
    unsafe.putDouble(m.address + (i << 2), v)
    v
  }

  protected[this] def newBuilder: LBuilder[Double, LArray[Double]] = new LDoubleArrayBuilder

  def view(from: Long, to: Long) = new LArrayView.LDoubleArrayView(this, from, to - from)
}

class LFloatArray(val size: Long, private[larray] val m:Memory)(implicit mem: MemoryAllocator)
  extends LArray[Float]
  with UnsafeArray[Float]
{
  def this(size: Long)(implicit  mem: MemoryAllocator) = this(size, mem.allocate(size << 2))

  private [larray] def elementByteSize = 4

  import UnsafeUtil.unsafe

  def apply(i: Long): Float =
  {
    unsafe.getFloat(m.address + (i << 2))
  }

  // a(i) = a(j) = 1
  def update(i: Long, v: Float): Float =
  {
    unsafe.putFloat(m.address + (i << 2), v)
    v
  }

  protected[this] def newBuilder: LBuilder[Float, LArray[Float]] = new LFloatArrayBuilder

  def view(from: Long, to: Long) = new LArrayView.LFloatArrayView(this, from, to - from)
}

class LShortArray(val size: Long, private[larray] val m:Memory)(implicit mem: MemoryAllocator)
  extends LArray[Short]
  with UnsafeArray[Short]
{
  def this(size: Long)(implicit  mem: MemoryAllocator) = this(size, mem.allocate(size << 1))

  private [larray] def elementByteSize = 2

  import UnsafeUtil.unsafe

  def apply(i: Long): Short =
  {
    unsafe.getShort(m.address + (i << 1))
  }

  // a(i) = a(j) = 1
  def update(i: Long, v: Short): Short =
  {
    unsafe.putShort(m.address + (i << 1), v)
    v
  }

  protected[this] def newBuilder: LBuilder[Short, LArray[Short]] = new LShortArrayBuilder

  def view(from: Long, to: Long) = new LArrayView.LShortArrayView(this, from, to - from)
}


object LObjectArray {
  def ofDim[A:ClassTag](size:Long) =
    if(size < Int.MaxValue)
      new LObjectArray32[A](size)
    else
      new LObjectArrayLarge[A](size)
}

/**
 * LArray[A] of Objects. This implementation is a simple wrapper of Array[A] and used when the array size is less than 2G
 * @param size array size
 * @tparam A object type
 */
class LObjectArray32[A : ClassTag](val size:Long) extends LArray[A] {
  require(size < Int.MaxValue)
  private var array = new Array[A](size.toInt)

  protected[this] def newBuilder = new LObjectArrayBuilder[A]


  def clear() {
    java.util.Arrays.fill(array.asInstanceOf[Array[AnyRef]], 0, length.toInt, null)
  }

  def apply(i: Long) = array(i.toInt)
  def update(i: Long, v: A) = {
    array(i.toInt) = v
    v
  }
  def free {
    // Dereference the array to let the array garbage-collected
    array = null
  }

  private[larray] def elementByteSize = 4

  def copyTo(dst: LByteArray, dstOffset: Long) {
    throw new UnsupportedOperationException("copyTo(LByteArray, Long)")
  }

  /**
   * Copy the contents of this sequence into the target LByteArray
   * @param srcOffset
   * @param dst
   * @param dstOffset
   * @param blen the byte length to copy
   */
  def copyTo[B](srcOffset: Long, dst: RawByteArray[B], dstOffset: Long, blen: Long) {
    throw new UnsupportedOperationException("copyTo(Long, LByteArray, Long, Long)")
  }

  def view(from: Long, to: Long) = new LArrayView.LObjectArrayView[A](this, from, to - from)
}

/**
 * LArray[A] of Object of more than 2G entries.
 * @param size array size
 * @tparam A object type
 */
class LObjectArrayLarge[A : ClassTag](val size:Long) extends LArray[A] {

  protected[this] def newBuilder = new LObjectArrayBuilder[A]

  /**
   * block size in pow(2, B)
   */
  private val B = 31
  private val mask = (1L << B) - 1L

  @inline private def index(i:Long) : Int = (i >>> B).toInt
  @inline private def offset(i:Long) : Int = (i & mask).toInt

  private var array : Array[Array[A]] = {
    val BLOCK_SIZE = (1L << B).toInt
    val NUM_BLOCKS = index(size-1) + 1
    // initialize the array
    val a = new Array[Array[A]](NUM_BLOCKS)
    var remaining = size
    for(i <- 0 until NUM_BLOCKS) {
      val s = math.min(remaining, BLOCK_SIZE).toInt
      a(i) = new Array[A](s)
      remaining -= s
    }
    a
  }

  def clear() {
    for(arr <- array) {
      val a = arr.asInstanceOf[Array[AnyRef]]
      java.util.Arrays.fill(a, 0, a.length, null)
    }
  }

  def copyTo(dst: LByteArray, dstOffset: Long) {
    throw new UnsupportedOperationException("copyTo(LByteArray, Long)")
  }

  def copyTo[B](srcOffset: Long, dst: RawByteArray[B], dstOffset: Long, blen: Long) {
    throw new UnsupportedOperationException("copyTo(Long, LByteArray, Long, Long)")
  }


  def apply(i: Long) = array(index(i))(offset(i))
  def update(i: Long, v: A) = { array(index(i))(offset(i)) = v; v }
  def free { array = null }
  private[larray] def elementByteSize = 4

  def view(from: Long, to: Long) = new LArrayView.LObjectArrayView[A](this, from, to - from)

}