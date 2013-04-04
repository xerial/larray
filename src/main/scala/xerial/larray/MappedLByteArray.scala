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
import java.nio.{MappedByteBuffer, ByteBuffer, Buffer}
import java.lang.reflect.InvocationTargetException

object MappedLArray {

  private[larray] val MAP_READONLY = 0
  private[larray] val MAP_READWRITE = 1
  private[larray] val MAP_PRIVATE = 2

  import java.{lang=>jl}
  private[larray] val force0 = classOf[MappedByteBuffer].getDeclaredMethod("force0", classOf[FileDescriptor], jl.Long.TYPE, jl.Long.TYPE)
  force0.setAccessible(true)

  private[larray] val unmap0 = classOf[FileChannelImpl].getDeclaredMethod("unmap0", jl.Long.TYPE, jl.Long.TYPE)
  unmap0.setAccessible(true)

}






/**
 * Memory-mapped LByteArray
 * @author Taro L. Saito
 */
class MappedLByteArray(f:File, offset:Long = 0, val size:Long = -1, mode:String="rw")(implicit alloc:MemoryAllocator) extends RawByteArray[Byte] {

  import UnsafeUtil.unsafe
  import java.{lang=>jl}
  import MappedLArray._

  private val raf = new RandomAccessFile(f, mode)
  private val fc = raf.getChannel

  val pagePosition = {
    val allocationGranularity : Long = unsafe.pageSize
    (offset % allocationGranularity).toInt
  }

  private var m : Memory = _

  val address : Long = {
    try {
      if(!fc.isOpen())
        throw new IOException("closed")

      val fileSize = fc.size()
      trace(s"file size: $fileSize")

      if(fileSize < offset + size) {
        // extend file size
        raf.seek(offset + size - 1)
        raf.write(0)
        trace(s"extend file size to ${fc.size}")
      }
      val mapPosition = offset - pagePosition
      val mapSize = size + pagePosition
      // A workaround for the error when calling fc.map(MapMode.READ_WRITE, offset, size) with size more than 2GB
      val map0 = classOf[FileChannelImpl].getDeclaredMethod("map0", jl.Integer.TYPE, jl.Long.TYPE, jl.Long.TYPE)
      map0.setAccessible(true)
      val rawAddr = map0.invoke(fc, MAP_READWRITE.asInstanceOf[AnyRef], mapPosition.asInstanceOf[AnyRef], mapSize.asInstanceOf[AnyRef]).asInstanceOf[jl.Long].toLong
      trace(f"mmap addr:$rawAddr%x, start address:${rawAddr+pagePosition}%x")

      m = Memory(rawAddr, mapSize)
      alloc.registerMMapMemory(m)

      rawAddr + pagePosition
    }
    catch {
      case e:InvocationTargetException => throw e.getCause
    }
  }


  protected[this] def newBuilder = new LByteArrayBuilder

  def free {
    alloc.release(m)
    m = null
  }

  private val dummyBuffer = fc.map(MapMode.READ_ONLY, 0, 0)


  /**
   * Forces any changes made to this buffer to be written to the file
   */
  def flush {
    // We can use a dummy buffer instance since force0 will not access class fields
    force0.invoke(dummyBuffer, raf.getFD, m.address.asInstanceOf[AnyRef], m.size.asInstanceOf[AnyRef])
  }

  override def close() {
    flush
    free
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