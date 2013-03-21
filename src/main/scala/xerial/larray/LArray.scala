//--------------------------------------
//
// LArray.scala
// Since: 2013/03/13 13:40
//
//--------------------------------------

package xerial.larray

import scala.reflect.ClassTag
import xerial.core.log.Logger


/**
 * Large Array (LArray) interface. The differences from Array[A] includes:
 *
 * - LArray accepts Long type indexes, so it is possible to create arrays more than 2GB entries, a limitation of Array[A].
 * - The memory of LArray[A] resides outside of the normal garbage-collected JVM heap. So the user must release the memory via [[xerial.larray.LArray#free]].
 * - LArray elements are not initialized, so explicit initialization is needed
 * -
 * @tparam A element type
 */
trait LArray[A] extends LIterable[A] {

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
   * Update an element
   * @param i index to be updated
   * @param v value to set
   * @return the value
   */
  def update(i: Long, v: A): A

  /**
   * Release the memory of LArray. After calling this method, the results of calling the other methods becomes undefined or might cause JVM crash.
   */
  def free

  /**
   * Byte size of an element. For example, if A is Int, its elementByteSize is 4
   */
  private[larray] def elementByteSize : Int
}


/**
 * @author Taro L. Saito
 */
object LArray {

  private[larray] val impl = xerial.larray.impl.LArrayLoader.load



  object EmptyArray
    extends LArray[Nothing]
    with LIterable[Nothing]
  {
    private[larray] def elementByteSize : Int = 0

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
  }

  def empty = EmptyArray

  def apply() = EmptyArray

  import _root_.java.{lang=>jl}

  private[larray] def wrap[A:ClassTag](size:Long, m:Memory) : LArray[A] = {
    val tag = implicitly[ClassTag[A]]
    tag.runtimeClass match {
      case jl.Integer.TYPE => new LIntArray(size / 4, m).asInstanceOf[LArray[A]]
      case jl.Byte.TYPE => new LByteArray(size, m).asInstanceOf[LArray[A]]
      case jl.Long.TYPE => new LLongArray(size / 8, m).asInstanceOf[LArray[A]]
      // TODO Short, Char, Float, Double
      case _ => sys.error(s"unsupported type: $tag")
    }
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

  def apply(first: Byte, elems: Byte*): LArray[Byte] = {
    val size = 1 + elems.size
    val arr = new LByteArray(size)
    arr(0) = first
    for ((e, i) <- elems.zipWithIndex) {
      arr(i + 1) = e
    }
    arr
  }

  // TODO apply(Char..)
  // TODO apply(Short..)
  // TODO apply(Float ..)
  // TODO apply(Long ..)
  // TODO apply(Double ..)
  // TODO apply(AnyRef ..)


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
  def newBuilder[A : ClassTag] : LArrayBuilder[A] = LArrayBuilder.make[A]

}

/**
 * read/write operations that can be supported for LArrays using raw byte arrays as their back-end.
 */
trait RawByteArray[A] extends LArray[A] {

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
  def write(srcOffset:Long, dest:Array[Byte], destOffset:Int, length:Int) : Int

  /**
   * Read the contents from a given source buffer
   * @param src source buffer
   * @param srcOffset byte offset in the source buffer
   * @param destOffset byte offset from the destination address
   * @param length byte length to read from the source
   */
  def read(src:Array[Byte], srcOffset:Int, destOffset:Long, length:Int) : Int


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



/**
 * Wrapping Array[Int] to support Long-type indexes
 * @param size array size
 */
class LIntArraySimple(val size: Long) extends LArray[Int] {

  private def boundaryCheck(i: Long) {
    if (i > Int.MaxValue)
      sys.error(f"index must be smaller than ${Int.MaxValue}%,d")
  }

  private val arr = {
    new Array[Int](size.toInt)
  }

  def apply(i: Long): Int = {
    //boundaryCheck(i)
    arr.apply(i.toInt)
  }

  // a(i) = a(j) = 1
  def update(i: Long, v: Int): Int = {
    //boundaryCheck(i)
    arr.update(i.toInt, v)
    v
  }

  def free {
    // do nothing
  }

  /**
   * Byte size of an element. For example, if A is Int, its elementByteSize is 4
   */
  private[larray] def elementByteSize: Int = 4
}


/**
 * Emulate large arrays using two-diemensional matrix of Int. Array[Int](page index)(offset in page)
 * @param size array size
 */
class MatrixBasedLIntArray(val size:Long) extends LArray[Int] {

  private[larray] def elementByteSize: Int = 4


  private val maskLen : Int = 24
  private val B : Int = 1 << maskLen // block size
  private val mask : Long = ~(~0L << maskLen)

  @inline private def index(i:Long) : Int = (i >>> maskLen).toInt
  @inline private def offset(i:Long) : Int = (i & mask).toInt

  private val numBlocks = ((size + (B - 1L))/ B).toInt
  private val arr = Array.ofDim[Int](numBlocks, B)

  /**
   * Retrieve an element
   * @param i index
   * @return the element value
   */
  def apply(i: Long) = arr(index(i))(offset(i))

  /**
   * Update an element
   * @param i index to be updated
   * @param v value to set
   * @return the value
   */
  def update(i: Long, v: Int) = {
    arr(index(i))(offset(i)) = v
    v
  }

  /**
   * Release the memory of LArray. After calling this method, the results of calling the other methods becomes undefined or might cause JVM crash.
   */
  def free {}

}


private[larray] trait UnsafeArray[T] extends RawByteArray[T] with Logger { self: LArray[T] =>

  private[larray] def m: Memory

  /**
   * Write the contents of this array to the destination buffer
   * @param srcOffset byte offset
   * @param dest destination array
   * @param destOffset offset in the destination array
   * @param length the byte length to write
   * @return written byte length
   */
  def write(srcOffset: Long, dest: Array[Byte], destOffset: Int, length: Int): Int = {
    val writeLen = math.min(dest.length - destOffset, math.min(length, byteLength - srcOffset)).toInt
    trace("copy to array")
    LArray.impl.asInstanceOf[xerial.larray.impl.LArrayNativeAPI].copyToArray(m.address + srcOffset, dest, destOffset, writeLen)
    writeLen
  }

  def read(src:Array[Byte], srcOffset:Int, destOffset:Long, length:Int) : Int = {
    val readLen = math.min(src.length-srcOffset, math.min(byteLength - destOffset, length)).toInt
    LArray.impl.asInstanceOf[xerial.larray.impl.LArrayNativeAPI].copyFromArray(src, srcOffset, m.address + destOffset, readLen)
    readLen
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
  def this(size: Long)(implicit mem: MemoryAllocator) = this(size, mem.allocate(size << 4))

  private[larray] def elementByteSize: Int = 8

  import UnsafeUtil.unsafe

  def apply(i: Long): Long = {
    unsafe.getLong(m.address + (i << 4))
  }

  // a(i) = a(j) = 1
  def update(i: Long, v: Long): Long = {
    unsafe.putLong(m.address + (i << 4), v)
    v
  }
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
}

/**
 * LArray[A] of Object of more than 2G entries.
 * @param size array size
 * @tparam A object type
 */
class LObjectArrayLarge[A : ClassTag](val size:Long) extends LArray[A] {

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

  def apply(i: Long) = array(index(i))(offset(i))
  def update(i: Long, v: A) = { array(index(i))(offset(i)) = v; v }
  def free { array = null }
  private[larray] def elementByteSize = 4
}