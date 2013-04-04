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
import sun.nio.ch.{DirectBuffer, FileChannelImpl}
import java.nio.{ByteBuffer, Buffer}

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
class MappedLByteArray(f:File, offset:Long = 0, _size:Long = -1, mode:String="rw") extends RawByteArray[Byte] {

  import UnsafeUtil.unsafe
  import java.{lang=>jl}

  private val fc = new RandomAccessFile(f, mode).getChannel
  val size = if(_size < 0L) fc.size() - offset else _size

  private val mmap = {
    fc.map(MapMode.READ_WRITE, offset, size)
  }

  val address = {
    val addrField = classOf[Buffer].getDeclaredField("address")
    addrField.setAccessible(true)
    addrField.get(mmap).asInstanceOf[jl.Long].toLong
  }

  protected[this] def newBuilder = new LByteArrayBuilder

  def free {
    close()
  }

  def flush {
    mmap.force()
  }

  override def close() {
    mmap.force()
    fc.close()
  }

  /**
   * Update an element
   * @param i index to be updated
   * @param v value to set
   * @return the value
   */
  def update(i: Long, v: Byte) = { unsafe.putByte(address+i, v); v }

  def view(from: Long, to: Long) = new LArrayView.LByteArrayView(this, from , to - from)

  /**
   * Retrieve an element
   * @param i index
   * @return the element value
   */
  def apply(i: Long) = unsafe.getByte(address + i)

  /**
   * Byte size of an element. For example, if A is Int, its elementByteSize is 4
   */
  private[larray] def elementByteSize = 1

}