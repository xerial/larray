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
// AltLArray.scala
// Since: 2013/03/25 16:02
//
//--------------------------------------

package xerial.larray

/**
  * A common trait for alternative implementations of LArray. This implementation is provided only for testing purpose,
  * so many features might be missing in LArrays impemented this trait.
  */
trait AltLIntArrayImpl extends LArray[Int] {

  def address = LArray.EmptyArray.address // throws an exception

  def copyTo(dest: LByteArray, destOffset: Long) {
    throw new UnsupportedOperationException("copyTo")
  }

  def copyTo[B](srcOffset: Long, dest: RawByteArray[B], destOffset: Long, blen: Long) {
    throw new UnsupportedOperationException("copyTo")
  }

  def view(from: Long, to: Long) = new LArrayView.LIntArrayView(this, from, to - from)
}

/**
  * Alternative implementation of LArray that might be inefficient, but written for comparing performances.
  * LIntArraySimple wraps Array[Int] to support Long-type indexes
  * @param size
  *   array size
  */
class LIntArraySimple(val size: Long) extends LArray[Int] with AltLIntArrayImpl {

  protected[this] def newBuilder = LArray.newBuilder[Int]

  private def boundaryCheck(i: Long) {
    if (i > Int.MaxValue)
      sys.error(f"index must be smaller than ${Int.MaxValue}%,d")
  }

  private val arr = {
    new Array[Int](size.toInt)
  }

  def clear() {
    java.util.Arrays.fill(arr, 0, size.toInt, 0)
  }

  def apply(i: Long): Int = {
    // boundaryCheck(i)
    arr.apply(i.toInt)
  }

  // a(i) = a(j) = 1
  def update(i: Long, v: Int): Int = {
    // boundaryCheck(i)
    arr.update(i.toInt, v)
    v
  }

  def free {
    // do nothing
  }

  /**
    * Byte size of an element. For example, if A is Int, its elementByteSize is 4
    */
  private[larray] def elementByteSize: Int = 4

}

/**
  * Emulate large arrays using two-diemensional matrix of Int. Array[Int](page index)(offset in page)
  * @param size
  *   array size
  */
class MatrixBasedLIntArray(val size: Long) extends LArray[Int] with AltLIntArrayImpl {

  private[larray] def elementByteSize: Int = 4

  protected[this] def newBuilder = LArray.newBuilder[Int]

  private val maskLen: Int = 24
  private val B: Int       = 1 << maskLen // block size
  private val mask: Long   = ~(~0L << maskLen)

  @inline private def index(i: Long): Int  = (i >>> maskLen).toInt
  @inline private def offset(i: Long): Int = (i & mask).toInt

  private val numBlocks = ((size + (B - 1L)) / B).toInt
  private val arr       = Array.ofDim[Int](numBlocks, B)

  def clear() {
    for (a <- arr) {
      java.util.Arrays.fill(a, 0, a.length, 0)
    }
  }

  /**
    * Retrieve an element
    * @param i
    *   index
    * @return
    *   the element value
    */
  def apply(i: Long) = arr(index(i))(offset(i))

  /**
    * Update an element
    * @param i
    *   index to be updated
    * @param v
    *   value to set
    * @return
    *   the value
    */
  def update(i: Long, v: Int) = {
    arr(index(i))(offset(i)) = v
    v
  }

  /**
    * Release the memory of LArray. After calling this method, the results of calling the other methods becomes
    * undefined or might cause JVM crash.
    */
  def free {}

}
