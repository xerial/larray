//--------------------------------------
//
// MMap.scala
// Since: 2013/12/10 3:43 PM
//
//--------------------------------------

package xerial.larray.mmap

import xerial.larray.{MemoryReference, Memory}
import java.lang.ref.ReferenceQueue
import xerial.larray.impl.LArrayNative
import java.io.File

case class MMapMemory(override val address:Long, override val size:Long) extends Memory(address) {
  def headerAddress = address

  def toRef(queue:ReferenceQueue[Memory]) : MMapMemoryReference = new MMapMemoryReference(this, queue)

}

class MMapMemoryReference(m:Memory, queue:ReferenceQueue[Memory], override val address:Long, val size:Long) extends MemoryReference(m, queue, address) {
  def this(m:MMapMemory, queue:ReferenceQueue[Memory]) = this(m, queue, m.address, m.size)

  override def release {
    LArrayNative.munmap(address, size)
  }

  override def name : String = "mmap"

}


object MMap {

  /**
   * Load the native JNI code
   */
  private[larray] val impl = xerial.larray.impl.LArrayLoader.load

  /**
   * Create an LArray[Byte] of a memory mapped file
   * @param f file
   * @param offset offset in file
   * @param size region byte size
   * @param mode open mode.
   */
  def open(f:File, offset:Long, size:Long, mode:MMapMode) : MappedLByteArray = {
    new MappedLByteArray(f, offset, size, mode)
  }

  /**
   * Create an LArray[Byte] of a memory mapped file. The size of this array is determined
   * by the file size.
   * @param f
   * @param mode
   * @return
   */
  def open(f:File, mode:MMapMode) : MappedLByteArray = {
    new MappedLByteArray(f, 0, f.length, mode)
  }


}

