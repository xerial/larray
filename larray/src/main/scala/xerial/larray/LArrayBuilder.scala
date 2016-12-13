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
// LArrayBuilder.scala
// Since: 2013/03/18 22:27
//
//--------------------------------------

package xerial.larray

import java.nio.ByteBuffer
import java.nio.channels.WritableByteChannel

import sun.nio.ch.DirectBuffer
import wvlet.log.LogSupport

import scala.collection.TraversableOnce
import scala.reflect.ClassTag

/**
  * Extension of `scala.collection.mutable.Builder` using Long indexes
  *
  * @tparam Elem element type
  * @tparam To   LArray type to generate
  */
trait LBuilder[Elem, +To] extends WritableByteChannel {

  def elementSize: Long

  def append(elem: Elem): this.type = +=(elem)

  def append(seq: LSeq[Elem]): this.type

  /** Adds a single element to the builder.
    *
    * @param elem the element to be added.
    * @return the builder itself.
    */
  def +=(elem: Elem): this.type

  /** Adds all elements produced by a TraversableOnce to this coll.
    *
    * @param xs the TraversableOnce producing the elements to add.
    * @return the coll itself.
    */
  def ++=(xs: TraversableOnce[Elem]): this.type = {xs.seq foreach +=; this}

  def ++=(xs: LIterator[Elem]): this.type = {xs foreach +=; this}

  /** Clears the contents of this builder.
    * After execution of this method the builder will contain no elements.
    */
  def clear()

  /** Produces a collection from the added elements.
    * The builder's contents are undefined after this operation.
    *
    * @return a collection containing the elements added to this builder.
    */
  def result(): To

  /** Gives a hint how many elements are expected to be added
    * when the next `result` is called. Some builder classes
    * will optimize their representation based on the hint. However,
    * builder implementations are still required to work correctly even if the hint is
    * wrong, i.e. a different number of elements is added.
    *
    * @param size the hint how many elements will be added.
    */
  def sizeHint(size: Long): Unit

}

/**
  * In the following, we need to define builders for every primitive types because if we extract
  * common functions (e.g., resize, mkArray) using type parameter, we cannot avoid boxing/unboxing.
  *
  */
abstract class LArrayBuilder[A, Repr <: LArray[A]] extends LBuilder[A, Repr] with LogSupport {
  protected var elems   : LByteArray = _
  protected var capacity: Long       = 0L
  /**
    * Current cursor position in LByteArray
    */
  protected var cursor  : Long       = 0L

  protected def numElems: Long = cursor / elementSize

  def append(b: Array[Byte], offset: Int, len: Int) = {
    val elemsToAdd = (len + elementSize - 1) / elementSize
    ensureSize(numElems + elemsToAdd)
    elems.readFromArray(b, offset, cursor, len)
    cursor += len
    this
  }

  def append(seq: LSeq[A]): this.type = {
    val n = seq.size
    ensureSize(numElems + n)
    seq.copyTo(elems, cursor)
    cursor += n * elementSize
    this
  }

  protected def mkArray(size: Long): LByteArray = {
    val newArray = new LByteArray(size * elementSize)
    if (this.numElems > 0L) {
      LArray.copy(elems, newArray)
      elems.free
    }
    newArray
  }

  def sizeHint(size: Long) {
    if (capacity < size) resize(size)
  }

  protected def ensureSize(size: Long) {
    val factor = 2L
    if (capacity < size || capacity == 0L) {
      var newsize = if (capacity <= 1L) {
        16L
      }
      else {
        capacity * factor
      }
      while (newsize < size) newsize *= factor
      resize(newsize)
    }
  }

  protected def resize(size: Long) {
    elems = mkArray(size)
    capacity = size
  }

  def clear() {
    if (numElems > 0) {
      elems.free
    }
    capacity = 0L
    cursor = 0L
  }

  def write(src: ByteBuffer): Int = {
    import UnsafeUtil.unsafe
    val len = math.max(src.limit - src.position, 0)
    val toAdd = (len + elementSize - 1) / elementSize
    ensureSize(numElems + toAdd)
    val writeLen = src match {
      case d: DirectBuffer =>
        unsafe.copyMemory(d.address() + d.position, elems.address + cursor, len)
        len
      case arr if src.hasArray =>
        elems.readFromArray(src.array(), src.position(), cursor, len)
      case _ =>
        var i = 0L
        val c = cursor
        while (i < len) {
          elems.putByte(c + i, src.get((src.position() + i).toInt))
          i += 1
        }
        len
    }
    cursor += writeLen
    src.position(src.position + writeLen)
    writeLen
  }

  def isOpen: Boolean = true

  def close() {clear()}
}

class LByteArrayBuilder extends LArrayBuilder[Byte, LByteArray] {
  def elementSize = 1

  def +=(elem: Byte): this.type = {
    val i = numElems
    ensureSize(i + 1)
    elems(i) = elem
    cursor += elementSize
    this
  }

  def result(): LByteArray = {
    if (capacity != 0L && capacity == numElems) {
      elems
    }
    else {
      mkArray(numElems)
    }
  }
}

class LCharArrayBuilder extends LArrayBuilder[Char, LCharArray] {
  def elementSize = 2

  def +=(elem: Char): this.type = {
    ensureSize(numElems + 1)
    elems.putChar(cursor, elem)
    cursor += elementSize
    this
  }
  def result(): LCharArray = {
    if (capacity != 0L && capacity == numElems) {
      new LCharArray(numElems, elems.m)
    }
    else {
      new LCharArray(numElems, mkArray(numElems).m)
    }
  }
}

class LShortArrayBuilder extends LArrayBuilder[Short, LShortArray] {
  def elementSize = 2

  def +=(elem: Short): this.type = {
    ensureSize(numElems + 1)
    elems.putShort(cursor, elem)
    cursor += elementSize
    this
  }
  def result(): LShortArray = {
    if (capacity != 0L && capacity == numElems) {
      new LShortArray(numElems, elems.m)
    }
    else {
      new LShortArray(numElems, mkArray(numElems).m)
    }
  }
}

class LIntArrayBuilder extends LArrayBuilder[Int, LIntArray] {
  def elementSize = 4

  def +=(elem: Int): this.type = {
    ensureSize(numElems + 1)
    elems.putInt(cursor, elem)
    cursor += elementSize
    this
  }

  def result(): LIntArray = {
    if (capacity != 0L && capacity == numElems) {
      new LIntArray(numElems, elems.m)
    }
    else {
      new LIntArray(numElems, mkArray(numElems).m)
    }
  }
}

class LFloatArrayBuilder extends LArrayBuilder[Float, LFloatArray] {
  def elementSize = 4

  def +=(elem: Float): this.type = {
    ensureSize(numElems + 1)
    elems.putFloat(cursor, elem)
    cursor += elementSize
    this
  }

  def result(): LFloatArray = {
    if (capacity != 0L && capacity == numElems) {
      new LFloatArray(numElems, elems.m)
    }
    else {
      new LFloatArray(numElems, mkArray(numElems).m)
    }
  }
}

class LLongArrayBuilder extends LArrayBuilder[Long, LLongArray] {
  def elementSize = 8

  def +=(elem: Long): this.type = {
    ensureSize(numElems + 1)
    elems.putLong(cursor, elem)
    cursor += elementSize
    this
  }

  def result(): LLongArray = {
    if (capacity != 0L && capacity == numElems) {
      new LLongArray(numElems, elems.m)
    }
    else {
      new LLongArray(numElems, mkArray(numElems).m)
    }
  }

}

class LDoubleArrayBuilder extends LArrayBuilder[Double, LDoubleArray] {
  def elementSize = 8

  def +=(elem: Double): this.type = {
    ensureSize(numElems + 1)
    elems.putDouble(cursor, elem)
    cursor += elementSize
    this
  }

  def result(): LDoubleArray = {
    if (capacity != 0L && capacity == numElems) {
      new LDoubleArray(numElems, elems.m)
    }
    else {
      new LDoubleArray(numElems, mkArray(numElems).m)
    }
  }
}

class LObjectArrayBuilder[A: ClassTag] extends LBuilder[A, LArray[A]] {

  def elementSize = 4

  private         var elems   : LArray[A] = _
  private         var capacity: Long      = 0L
  private[larray] var size    : Long      = 0L

  private def mkArray(size: Long): LArray[A] = {
    val newArray = LObjectArray.ofDim[A](size)
    if (this.size > 0L) {
      LArray.copy(elems, 0L, newArray, 0L, this.size)
      elems.free
    }
    newArray
  }

  override def sizeHint(size: Long) {
    if (capacity < size) resize(size)
  }

  private def ensureSize(size: Long) {
    val factor = 2L
    if (capacity < size || capacity == 0L) {
      var newsize = if (capacity <= 1L) {
        16L
      }
      else {
        capacity * factor
      }
      while (newsize < size) newsize *= factor
      resize(newsize)
    }
  }

  private def resize(size: Long) {
    elems = mkArray(size)
    capacity = size
  }

  def +=(elem: A): this.type = {
    ensureSize(size + 1)
    elems(size) = elem
    size += 1
    this
  }

  def clear() {
    elems = null
    size = 0L
    capacity = 0L
  }

  def result(): LArray[A] = {
    if (capacity != 0L && capacity == size) {
      elems
    }
    else {
      mkArray(size)
    }
  }

  def write(src: ByteBuffer): Int = throw new UnsupportedOperationException("LBuilder[A].writeToArray(ByteBuffer)")

  def isOpen: Boolean = true

  def close() {clear}

  def append(seq: LSeq[A]) = {
    ensureSize(size + seq.length)
    seq.foreach {e => elems(size) = e; size += 1}
    this
  }
}

/**
  * @author Taro L. Saito
  */
object LArrayBuilder {

  /** Creates a new arraybuilder of type `T`.
    *
    * @tparam T type of the elements for the array builder, with a `ClassTag` context bound.
    * @return a new empty array builder.
    */
  def make[T: ClassTag](): LBuilder[T, LArray[T]] = {
    val tag = implicitly[ClassTag[T]]
    tag.runtimeClass match {
      case java.lang.Byte.TYPE => new LByteArrayBuilder().asInstanceOf[LBuilder[T, LArray[T]]]
      case java.lang.Short.TYPE => new LShortArrayBuilder().asInstanceOf[LBuilder[T, LArray[T]]]
      case java.lang.Character.TYPE => new LCharArrayBuilder().asInstanceOf[LBuilder[T, LArray[T]]]
      case java.lang.Integer.TYPE => new LIntArrayBuilder().asInstanceOf[LBuilder[T, LArray[T]]]
      case java.lang.Long.TYPE => new LLongArrayBuilder().asInstanceOf[LBuilder[T, LArray[T]]]
      case java.lang.Float.TYPE => new LFloatArrayBuilder().asInstanceOf[LBuilder[T, LArray[T]]]
      case java.lang.Double.TYPE => new LDoubleArrayBuilder().asInstanceOf[LBuilder[T, LArray[T]]]
      case java.lang.Boolean.TYPE => new LBitArrayBuilder().asInstanceOf[LBuilder[T, LArray[T]]]
      case _ => new LObjectArrayBuilder[T].asInstanceOf[LBuilder[T, LArray[T]]]
    }
  }

}