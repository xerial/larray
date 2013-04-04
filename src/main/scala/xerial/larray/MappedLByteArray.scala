//--------------------------------------
//
// MappedLByteArray.scala
// Since: 2013/04/04 10:47 AM
//
//--------------------------------------

package xerial.larray

import impl.LArrayNative
import java.io.{IOException, FileDescriptor, RandomAccessFile, File}
import java.nio.channels.FileChannel
import java.nio.channels.FileChannel.MapMode
import sun.nio.ch.{DirectBuffer, FileChannelImpl}
import java.nio.{ByteBuffer, Buffer}
import java.lang.reflect.InvocationTargetException

object MappedLArray {

  private[larray] val MAP_READONLY = 0
  private[larray] val MAP_RW = 1
  private[larray] val MAP_PV = 2


}


/**
 * Memory-mapped LByteArray
 * @author Taro L. Saito
 */
class MappedLByteArray(f:File, offset:Long = 0, _size:Long = -1, mode:String="rw") extends RawByteArray[Byte] {

  import UnsafeUtil.unsafe
  import java.{lang=>jl}

  private val raf = new RandomAccessFile(f, mode)
  private val fc = raf.getChannel

  def size = _size

  val address : Long = {

    val map0 = classOf[FileChannelImpl].getDeclaredMethod("map0", jl.Integer.TYPE, jl.Long.TYPE, jl.Long.TYPE)
    map0.setAccessible(true)
    import MappedLArray._

    try {
      if(!fc.isOpen())
        throw new IOException("closed")

      val fileSize = fc.size()
      debug(s"file size: $fileSize")

      if(fileSize < offset + _size) {
        // extend file size
        raf.seek(offset + _size - 1)
        raf.write(0)
        debug(s"extend file size to ${fc.size}")
      }

      val allocationGranularity : Long = unsafe.pageSize
      val pagePosition = (offset % allocationGranularity).toInt
      val mapPosition = offset - pagePosition
      val mapSize = size + pagePosition
      // A workaround for the error when calling fc.map(MapMode.READ_WRITE, offset, size) with its size more than 2GB
      val addr = map0.invoke(fc, MAP_RW.asInstanceOf[AnyRef], mapPosition.asInstanceOf[AnyRef], mapSize.asInstanceOf[AnyRef]).asInstanceOf[jl.Long].toLong
      debug(f"mmap addr:$addr%x, start address:${addr+pagePosition}%x")
      addr + pagePosition
    }
    catch {
      case e:InvocationTargetException => throw e.getCause
    }
  }

//  val address : Long = {
//    val addrField = classOf[Buffer].getDeclaredField("address")
//    addrField.setAccessible(true)
//    addrField.getLong(mmap)
//  }

  protected[this] def newBuilder = new LByteArrayBuilder

  def free {
    // TODO munmap
    close()
  }

  def flush {
    //mmap.force()
  }

  override def close() {
    //mmap.force()
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