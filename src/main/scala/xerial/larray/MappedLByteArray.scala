//--------------------------------------
//
// MappedLByteArray.scala
// Since: 2013/04/04 10:47 AM
//
//--------------------------------------

package xerial.larray

import impl.LArrayNative
import java.io.{FileDescriptor, RandomAccessFile, File}
import java.nio.channels.FileChannel
import java.nio.channels.FileChannel.MapMode

object MappedLArray {

  private[larray] val PROT_READ = 0x1
  private[larray] val PROT_WRITE = 0x2
  private[larray] val PROT_EXEC = 0x4

  private[larray] val MAP_READONLY = 0x00
  private[larray] val MAP_SHARED = 0x01
  private[larray] val MAP_PRIVATE = 0x02


}


/**
 * Memory-mapped LByteArray
 * @author Taro L. Saito
 */
class MappedLByteArray(f:File, mode:String="rw", pageSize:Long) extends LArray[Byte] {

  def this(f:File, mode:String) = this(f, mode, UnsafeUtil.unsafe.pageSize())

  private val raf = new RandomAccessFile(f, mode)
  private val fd = {
    // Get file descriptoer to use in mmap
    val jfd = raf.getFD
    val idField = classOf[FileDescriptor].getDeclaredField("id")
    idField.setAccessible(true)
    idField.get(jfd).asInstanceOf[java.lang.Integer].toInt
  }
  private val fc = raf.getChannel

  import MappedLArray._

  private val mappedAddr = LArrayNative.mmap(-1L, fc.size(), PROT_READ | PROT_WRITE, MAP_SHARED, fd,

  private def pageIndex(i:Long) = i / pageSize
  private def pageOffset(i:Long) = i % pageSize

  protected[this] def newBuilder = new LByteArrayBuilder

  def free {
    fc.close()
  }

  /**
   * Clear the contents of the array. It simply fills the array with zero bytes.
   */
  def clear() {
    var i = 0L
    val s = size
    while(i < s) {
      mapped.put(0.toByte)
      i+= 1
    }
  }

  /**
   * Update an element
   * @param i index to be updated
   * @param v value to set
   * @return the value
   */
  def update(i: Long, v: Byte) = {}

  def view(from: Long, to: Long) = ???

  /**
   * Size of this array
   * @return size of this array
   */
  def size = fc.size

  /**
   * Retrieve an element
   * @param i index
   * @return the element value
   */
  def apply(i: Long) = {
    //mapped.get(i)
    // TODO impl
    0
  }

  /**
   * Byte size of an element. For example, if A is Int, its elementByteSize is 4
   */
  private[larray] def elementByteSize = 1

  /**
   * Copy the contents of this LSeq[A] into the target LByteArray
   * @param dst
   * @param dstOffset
   */
  def copyTo(dst: LByteArray, dstOffset: Long) {}

  /**
   * Copy the contents of this sequence into the target LByteArray
   * @param srcOffset
   * @param dst
   * @param dstOffset
   * @param blen the byte length to copy
   */
  def copyTo[B](srcOffset: Long, dst: RawByteArray[B], dstOffset: Long, blen: Long) {}
}