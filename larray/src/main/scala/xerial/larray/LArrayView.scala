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
// LArrayView.scala
// Since: 2013/03/25 18:04
//
//--------------------------------------

package xerial.larray

import reflect.ClassTag
import xerial.larray.LArray.EmptyArray

/**
 * Provides view of LArray
 * @author Taro L. Saito
 */
object LArrayView {

  class LBitArrayView(base:LBitArray, offset:Long, val size:Long) extends LArrayView[Boolean] with LBitArrayOps {
    protected[this] def newBuilder = new LBitArrayBuilder
    def apply(i:Long) = base.apply(i + offset)

    def address = LArray.EmptyArray.address

    /**
     * Byte size of an element. For example, if A is Int, its elementByteSize is 4
     */
    private[larray] def elementByteSize = throw new UnsupportedOperationException("elementByteSize of LBitArray")

    /**
     * Copy the contents of this LSeq[A] into the target LByteArray
     * @param dst
     * @param dstOffset
     */
    def copyTo(dst: LByteArray, dstOffset: Long) {
      throw new UnsupportedOperationException("copyTo of LBitArray")
    }

    /**
     * Copy the contents of this sequence into the target LByteArray
     * @param srcOffset
     * @param dst
     * @param dstOffset
     * @param blen the byte length to copy
     */
    def copyTo[B](srcOffset: Long, dst: RawByteArray[B], dstOffset: Long, blen: Long) {
      throw new UnsupportedOperationException("copyTo of LBitArray")
    }

    /**
     * Count the number of bits within the specified range [start, end)
     * @param checkTrue count true or false
     * @param start
     * @param end
     * @return the number of occurrences
     */
    def count(checkTrue: Boolean, start: Long, end: Long) = base.count(checkTrue, start+offset, end+offset)

    override def slice(from: Long, until: Long) = base.slice(from+offset, until+offset)
  }

  class LByteArrayView(base:LArray[Byte], offset:Long, size:Long) extends AbstractLArrayView[Byte](base, offset, size) {
    protected[this] def newBuilder: LBuilder[Byte, LArray[Byte]] = new LByteArrayBuilder
    private[larray] def elementByteSize: Int = 1
  }

  class LCharArrayView(base:LArray[Char], offset:Long, size:Long) extends AbstractLArrayView[Char](base, offset, size) {
    protected[this] def newBuilder: LBuilder[Char, LArray[Char]] = new LCharArrayBuilder
    private[larray] def elementByteSize: Int = 2
  }

  class LShortArrayView(base:LArray[Short], offset:Long, size:Long) extends AbstractLArrayView[Short](base, offset, size) {
    protected[this] def newBuilder: LBuilder[Short, LArray[Short]] = new LShortArrayBuilder
    private[larray] def elementByteSize: Int = 2
  }

  class LIntArrayView(base:LArray[Int], offset:Long, size:Long) extends AbstractLArrayView[Int](base, offset, size) {
    protected[this] def newBuilder: LBuilder[Int, LArray[Int]] = new LIntArrayBuilder
    private[larray] def elementByteSize: Int = 4
  }

  class LFloatArrayView(base:LArray[Float], offset:Long, size:Long) extends AbstractLArrayView[Float](base, offset, size) {
    protected[this] def newBuilder: LBuilder[Float, LArray[Float]] = new LFloatArrayBuilder
    private[larray] def elementByteSize: Int = 4
  }

  class LLongArrayView(base:LArray[Long], offset:Long, size:Long) extends AbstractLArrayView[Long](base, offset, size) {
    protected[this] def newBuilder: LBuilder[Long, LArray[Long]] = new LLongArrayBuilder
    private[larray] def elementByteSize: Int = 8
  }

  class LDoubleArrayView(base:LArray[Double], offset:Long, size:Long) extends AbstractLArrayView[Double](base, offset, size) {
    protected[this] def newBuilder: LBuilder[Double, LArray[Double]] = new LDoubleArrayBuilder
    private[larray] def elementByteSize: Int = 8
  }

  class LObjectArrayView[A : ClassTag](base:LArray[A], offset:Long, size:Long) extends AbstractLArrayView[A](base, offset, size) {
    protected[this] def newBuilder: LBuilder[A, LArray[A]] = new LObjectArrayBuilder
    private[larray] def elementByteSize: Int = 4
  }

  object EmptyView extends LArrayView[Nothing] {
    protected[this] def newBuilder = EmptyArray.newBuilder
    def size = 0L
    def apply(i: Long) = EmptyArray.apply(i)

    def address = LArray.EmptyArray.address

    /**
     * Byte size of an element. For example, if A is Int, its elementByteSize is 4
     */
    private[larray] def elementByteSize = EmptyArray.elementByteSize

    /**
     * Copy the contents of this LSeq[A] into the target LByteArray
     * @param dst
     * @param dstOffset
     */
    def copyTo(dst: LByteArray, dstOffset: Long) { EmptyArray.copyTo(dst, dstOffset)}

    /**
     * Copy the contents of this sequence into the target LByteArray
     * @param srcOffset
     * @param dst
     * @param dstOffset
     * @param blen the byte length to copy
     */
    def copyTo[B](srcOffset: Long, dst: RawByteArray[B], dstOffset: Long, blen: Long) { EmptyArray.copyTo(srcOffset, dst, dstOffset, blen)}
  }

}

/**
 * Shallow-copy reference of the part of LArray
 * @tparam A
 */
trait LArrayView[A] extends LSeq[A] {

}

abstract class AbstractLArrayView[A : ClassTag](base:LSeq[A], offset:Long, val size:Long) extends LArrayView[A] {

  def address = base.address + (offset * elementByteSize)

  /**
   * Retrieve an element
   * @param i index
   * @return the element value
   */
  def apply(i: Long): A = base.apply(i + offset)

  /**
   * Copy the contents of this LSeq[A] into the target LByteArray
   * @param dst
   * @param dstOffset
   */
  def copyTo(dst: LByteArray, dstOffset: Long) {
    base.copyTo(offset, dst, dstOffset, byteLength)
  }

  /**
   * Copy the contents of this sequence into the target LByteArray
   * @param srcOffset
   * @param dst
   * @param dstOffset
   * @param blen the byte length to copy
   */
  def copyTo[B](srcOffset: Long, dst: RawByteArray[B], dstOffset: Long, blen: Long) {
    base.copyTo[B](offset + srcOffset, dst, dstOffset, blen)
  }



}

