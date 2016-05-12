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
// UInt32Array.scala
// Since: 2013/03/18 3:57 PM
//
//--------------------------------------

package xerial.larray

import xerial.larray.buffer.{MemoryAllocator, Memory}

object UInt32Array {

  def newBuilder = new LArrayBuilder[Long, UInt32Array] {

    def +=(v: Long): this.type = {
      ensureSize(numElems + 1)
      elems.putInt(cursor,  (v & 0xFFFFFFFFL).toInt)
      cursor += elementSize
      this
    }

    /** Produces a collection from the added elements.
      * The builder's contents are undefined after this operation.
      * @return a collection containing the elements added to this builder.
      */
    def result(): UInt32Array = {
      if(capacity != 0L && capacity == numElems) new UInt32Array(numElems, elems.m)
      else new UInt32Array(numElems, mkArray(numElems).m)
    }

    def elementSize = 4
  }



}


private[larray] class UInt32ArrayView(base:UInt32Array, offset:Long, val size:Long) extends LArrayView[Long] {
  protected[this] def newBuilder: LBuilder[Long, UInt32Array] = UInt32Array.newBuilder
  def apply(i: Long) = base.apply(offset + i)
  private[larray] def elementByteSize = 4
  def copyTo(dst: LByteArray, dstOffset: Long) { base.copyTo(offset, dst, dstOffset, byteLength) }
  def copyTo[B](srcOffset: Long, dst: RawByteArray[B], dstOffset: Long, blen: Long) { base.copyTo(offset+srcOffset, dst, dstOffset, blen) }

  def address = base.address + (offset * elementByteSize)
}

/**
 * Array of uint32 values. The internal array representation is the same with LIntArray, but the apply and update methods are based on Long type values.
 *
 * @author Taro L. Saito
 */
class UInt32Array(val size: Long, private[larray] val m:Memory)(implicit val alloc: MemoryAllocator) extends LArray[Long] with UnsafeArray[Long] { self =>
  def this(size:Long)(implicit alloc: MemoryAllocator) = this(size, alloc.allocate(size << 2))(alloc)

  import UnsafeUtil.unsafe

  protected[this] def newBuilder: LBuilder[Long, UInt32Array] = UInt32Array.newBuilder

  def apply(i:Long) : Long = {
    val v : Long = unsafe.getInt(m.address + (i << 2)) & 0xFFFFFFFFL
    v
  }

  def update(i:Long, v:Long) : Long = {
    unsafe.putInt(m.address + (i << 2), (v & 0xFFFFFFFFL).toInt)
    v
  }

  /**
   * Byte size of an element. For example, if A is Int, its elementByteSize is 4
   */
  private[larray] def elementByteSize: Int = 4


  def view(from: Long, to: Long) : LArrayView[Long] = new UInt32ArrayView(self, from, to - from)

}


