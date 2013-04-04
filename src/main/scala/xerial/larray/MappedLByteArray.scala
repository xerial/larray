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




/**
 * Memory-mapped LByteArray
 * @author Taro L. Saito
 */
class MappedLByteArray(f:File, offset:Long = 0, val size:Long = -1, mode:MMapMode=MMapMode.READ_WRITE)(implicit alloc:MemoryAllocator) extends RawByteArray[Byte] {

  import UnsafeUtil.unsafe
  import java.{lang=>jl}

  private val raf = new RandomAccessFile(f, mode.mode)
  private val fc = raf.getChannel
  private val fd = {
    val f = raf.getFD()
    val idf = f.getClass.getDeclaredField("fd")
    idf.setAccessible(true)
    idf.get(f).asInstanceOf[jl.Integer].toInt
  }

  private val pagePosition = {
    val allocationGranularity : Long = unsafe.pageSize
    (offset % allocationGranularity).toInt
  }

  private var m : Memory = _

  val address : Long = {
    try {
      if(!fc.isOpen())
        throw new IOException("closed")

      val fileSize = fc.size()
      if(fileSize < offset + size) {
        // extend file size
        raf.seek(offset + size - 1)
        raf.write(0)
        trace(s"extend file size to ${fc.size}")
      }
      val mapPosition = offset - pagePosition
      val mapSize = size + pagePosition
      // A workaround for the error when calling fc.map(MapMode.READ_WRITE, offset, size) with size more than 2GB
      val rawAddr = LArrayNative.mmap(fd, mode.code, mapPosition, mapSize)
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

  /**
   * Forces any changes made to this buffer to be written to the file
   */
  def flush {
    // We can use a dummy buffer instance since force0 will not access class fields
    LArrayNative.msync(m.address, m.size)
   }

  /**
   * Close the memory mapped file
   */
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