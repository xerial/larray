/*--------------------------------------------------------------------------
 *  Copyright 2013 Taro L. Saito
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *--------------------------------------------------------------------------*/
//--------------------------------------
//
// MappedLByteArray.scala
// Since: 2013/04/04 10:47 AM
//
//--------------------------------------

package xerial.larray.mmap

import java.io.{IOException, FileDescriptor, RandomAccessFile, File}
import java.lang.reflect.InvocationTargetException
import xerial.larray._
import xerial.larray.impl.{LArrayNative, OSInfo}
import xerial.larray.buffer.MemoryAllocator
import sun.misc.SharedSecrets


/**
 * Memory-mapped LByteArray
 * @author Taro L. Saito
 */
class MappedLByteArray(f:File, offset:Long = 0, val size:Long = -1, mode:MMapMode=MMapMode.READ_WRITE)(implicit alloc:MemoryAllocator) extends RawByteArray[Byte] {

  import UnsafeUtil.unsafe
  import java.{lang=>jl}

  private val raf = new RandomAccessFile(f, mode.mode)
  private val fc = raf.getChannel
  private val fd : Long = {
    val f = raf.getFD()
    if(!OSInfo.isWindows) {
      val idf = f.getClass.getDeclaredField("fd")
      idf.setAccessible(true)
      idf.getInt(f)
    }
    else {
      val idf = f.getClass.getDeclaredField("handle")
      idf.setAccessible(true)
      idf.getLong(f)
    }
  }
  private var winHandle : Long = -1L

  private val pagePosition = {
    val allocationGranularity : Long = unsafe.pageSize
    (offset % allocationGranularity).toInt
  }

  private var m : MMapMemory = _

  val address : Long = {
    try {
      if(!fc.isOpen())
        throw new IOException("closed")

      val fileSize = fc.size()
      if(fileSize < offset + size) {
        // extend the file size
        raf.seek(offset + size - 1)
        raf.write(0)
        trace(s"extend file size to ${fc.size}")
      }
      val mapPosition = offset - pagePosition
      val mapSize = size + pagePosition
      // A workaround for the error when calling fc.map(MapMode.READ_WRITE, offset, size) with size more than 2GB

      val rawAddr = LArrayNative.mmap(fd, mode.code, mapPosition, mapSize)
      trace(f"mmap addr:$rawAddr%x, start address:${rawAddr+pagePosition}%x")

      if(OSInfo.isWindows) {
        val a = SharedSecrets.getJavaIOFileDescriptorAccess
        winHandle = LArrayNative.duplicateHandle(a.getHandle(raf.getFD))
        debug(f"win handle: $winHandle%x")
      }

      m = new MMapMemory(rawAddr, mapSize)
      alloc.register(m)

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
    LArrayNative.msync(winHandle, m.address, m.size)
   }

  /**
   * Close the memory mapped file. To ensure the written data is saved in the file, call flush before closing
   */
  override def close() {
    //flush
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