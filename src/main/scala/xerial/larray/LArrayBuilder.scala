//--------------------------------------
//
// LArrayBuilder.scala
// Since: 2013/03/18 22:27
//
//--------------------------------------

package xerial.larray

import collection.{TraversableOnce, TraversableLike, mutable}
import collection.mutable.{ArrayBuilder, Builder}
import reflect.ClassTag
import java.nio.channels.WritableByteChannel
import java.nio.ByteBuffer
import sun.nio.ch.DirectBuffer

/**
 * Extention of [[scala.collection.mutable.Builder]] to Long indexes
 * @tparam Elem element type
 * @tparam To LArray type to generate
 */
trait LBuilder[-Elem, +To] extends WritableByteChannel {

  /** Adds a single element to the builder.
    *  @param elem the element to be added.
    *  @return the builder itself.
    */
  def +=(elem: Elem): this.type

  /** ${Add}s all elements produced by a TraversableOnce to this $coll.
    *
    *  @param xs   the TraversableOnce producing the elements to $add.
    *  @return  the $coll itself.
    */
  def ++=(xs: TraversableOnce[Elem]): this.type = { xs.seq foreach += ; this }

  /** Clears the contents of this builder.
    *  After execution of this method the builder will contain no elements.
    */
  def clear()

  /** Produces a collection from the added elements.
    *  The builder's contents are undefined after this operation.
    *  @return a collection containing the elements added to this builder.
    */
  def result(): To

  /** Gives a hint how many elements are expected to be added
    *  when the next `result` is called. Some builder classes
    *  will optimize their representation based on the hint. However,
    *  builder implementations are still required to work correctly even if the hint is
    *  wrong, i.e. a different number of elements is added.
    *
    *  @param size  the hint how many elements will be added.
    */
  def sizeHint(size: Long) {}

}


abstract class LArrayBuilder[A, Repr <: LArray[A]] extends LBuilder[A, Repr]  {
  protected var elems : LByteArray = _
  protected var capacity: Long = 0L
  protected var byteSize: Long = 0L

  def append(b:Array[Byte], offset:Int, len:Int) = {
    ensureSize(byteSize + len)
    elems.read(b, offset, byteSize, len)
    byteSize += len
    this
  }

  protected def mkArray(size:Long) : LByteArray = {
    val newArray = new LByteArray(size)
    if(this.byteSize > 0L) {
      LArray.copy(elems, 0L, newArray, 0L, this.byteSize)
      elems.free
    }
    newArray
  }


  override def sizeHint(size:Long) {
    if(capacity < size) resize(size)
  }

  protected def ensureSize(size:Long) {
    if(capacity < size || capacity == 0L){
      var newsize = if(capacity == 0L) 16L else (capacity * 1.5).toLong
      while(newsize < size) newsize *= 2
      resize(newsize)
    }
  }

  private def resize(size:Long) {
    elems = mkArray(size)
    capacity = size
  }

  def clear() {
    if(byteSize > 0)
      elems.free
    capacity = 0L
    byteSize = 0L
  }


  def write(src: ByteBuffer): Int = {
    import UnsafeUtil.unsafe
    val len = math.max(src.limit - src.position, 0)
    ensureSize(byteSize + len)
    val writeLen = src match {
      case d:DirectBuffer =>
        unsafe.copyMemory(d.address() + src.position, elems.address + byteSize, len)
        len
      case arr if src.hasArray =>
        elems.read(src.array(), src.position(), byteSize, len)
      case _ =>
        var i = 0L
        while(i < len) {
          elems.putByte(byteSize + i, src.get((src.position() + i).toInt))
          i += 1
        }
        len
    }
    byteSize += writeLen
    src.position(src.position + writeLen)
    writeLen
  }

  def isOpen: Boolean = true

  def close() { clear() }
}


class LByteArrayBuilder extends LArrayBuilder[Byte, LByteArray] {

  def result(): LByteArray = {
    if(capacity != 0L && capacity == byteSize) elems
    else mkArray(byteSize)
  }


  def +=(elem: Byte): this.type = {
    ensureSize(byteSize + 1)
    elems(byteSize) = elem
    byteSize += 1
    this
  }

}

/**
 * @author Taro L. Saito
 */
object LArrayBuilder {

  /** Creates a new arraybuilder of type `T`.
    *
    *  @tparam T     type of the elements for the array builder, with a `ClassTag` context bound.
    *  @return       a new empty array builder.
    */
  def make[T: ClassTag](): LBuilder[T, LArray[T]] = {
    val tag = implicitly[ClassTag[T]]
    tag.runtimeClass match {
      case java.lang.Byte.TYPE      => new LByteArrayBuilder().asInstanceOf[LBuilder[T, LArray[T]]]
      case java.lang.Short.TYPE     => sys.error("Not yet implemented")
      case java.lang.Character.TYPE => sys.error("Not yet implemented")
      case java.lang.Integer.TYPE   => ofInt.asInstanceOf[LBuilder[T, LArray[T]]]
      case java.lang.Long.TYPE      => sys.error("Not yet implemented")
      case java.lang.Float.TYPE     => sys.error("Not yet implemented")
      case java.lang.Double.TYPE    => sys.error("Not yet implemented")
      case java.lang.Boolean.TYPE   => sys.error("Not yet implemented")
      case java.lang.Void.TYPE      => sys.error("Not yet implemented")
      case _                        => ofObject[T].asInstanceOf[LBuilder[T, LArray[T]]]
    }
  }


  /**
   * In the following, we need to define builders for every primitive types because if we extract
   * common functions (e.g., resize, mkArray) using type parameter, we cannot avoid boxing/unboxing.
   *
   */


  def ofInt = new LArrayBuilder[Int, LIntArray] {

    def +=(elem: Int): this.type = {
      ensureSize(byteSize + 4)
      elems.putInt(byteSize, elem)
      byteSize += 4
      this
    }

    def result(): LIntArray = {
      if(capacity != 0L && capacity == byteSize) new LIntArray(byteSize / 4, elems.m)
      else new LIntArray(byteSize / 4, mkArray(byteSize).m)
    }
  }


  // TODO ofChar
  // TODO ofShort
  // TODO ofFloat
  // TODO ofLong
  // TODO ofDouble


  def ofObject[A:ClassTag] = new LBuilder[A, LArray[A]] {

    private var elems : LArray[A] = _
    private var capacity: Long = 0L
    private[larray] var size: Long = 0L

    private def mkArray(size:Long) : LArray[A] = {
      val newArray = LObjectArray.ofDim[A](size)
      if(this.size > 0L) {
        LArray.copy(elems, 0L, newArray, 0L, this.size)
        elems.free
      }
      newArray
    }

    override def sizeHint(size:Long) {
      if(capacity < size) resize(size)
    }

    private def ensureSize(size:Long) {
      if(capacity < size || capacity == 0L){
        var newsize = if(capacity == 0L) 16L else (capacity * 1.5).toLong
        while(newsize < size) newsize *= 2
        resize(newsize)
      }
    }

    private def resize(size:Long) {
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
      if(capacity != 0L && capacity == size) elems
      else mkArray(size)
    }

    def write(src: ByteBuffer): Int = throw new UnsupportedOperationException("LBuilder[A].write(ByteBuffer)")

    def isOpen: Boolean = true

    def close() { clear }
  }

}