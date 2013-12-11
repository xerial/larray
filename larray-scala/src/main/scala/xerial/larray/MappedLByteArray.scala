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

package xerial.larray

import java.io.{IOException, FileDescriptor, RandomAccessFile, File}
import java.lang.reflect.InvocationTargetException
import xerial.larray._
import xerial.larray.buffer.{Memory, MemoryAllocator}
import xerial.larray.mmap.{MMapMemory, MMapMode, MMapBuffer}
import sun.misc.SharedSecrets
import sun.awt.OSInfo



/**
 * Memory-mapped LByteArray
 * @author Taro L. Saito
 */
class MappedLByteArray(f:File, offset:Long = 0, val size:Long = -1, mode:MMapMode=MMapMode.READ_WRITE)(implicit alloc:MemoryAllocator) extends RawByteArray[Byte] {

  import UnsafeUtil.unsafe
  import java.{lang=>jl}

  private val mmap = new MMapBuffer(f, offset, size, mode);
  private val m : Memory = mmap.m

  protected[this] def newBuilder = new LByteArrayBuilder

  val address = mmap.address()

  def free {
    m.release();
  }

  /**
   * Forces any changes made to this buffer to be written to the file
   */
  def flush {
    mmap.flush()
   }

  /**
   * Close the memory mapped file. To ensure the written data is saved in the file, call flush before closing
   */
  override def close() {
    mmap.close()
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