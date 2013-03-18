//--------------------------------------
//
// LArrayBuilder.scala
// Since: 2013/03/18 22:27
//
//--------------------------------------

package xerial.larray

import collection.{TraversableOnce, TraversableLike, mutable}
import collection.mutable.Builder

/**
 * Extention of [[scala.collection.mutable.Builder]] to Long indexes
 * @tparam Elem
 * @tparam To
 */
trait LBuilder[-Elem, +To] {

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


abstract class LArrayBuilder[A] extends LBuilder[A, LArray[A]]

/**
 * @author Taro L. Saito
 */
object LArrayBuilder {

  def ofInt = new LArrayBuilder[Int] {

    private var elems : LArray[Int] = _
    private var capacity: Long = 0L
    private var size: Long = 0L

    private def mkArray(size:Long) : LArray[Int] = {
      val newArray = new LIntArray(size)
      if(this.size > 0L)
        LArray.copy(elems, 0L, newArray, 0L, this.size)
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
      elems.free
      elems = mkArray(size)
      capacity = size
    }

    def +=(elem: Int): this.type = {
      ensureSize(size + 1)
      elems(size) = elem
      size += 1
      this
    }

    def clear() {
      size = 0
    }

    def result(): LArray[Int] = {
      if(capacity != 0L && capacity == size) elems
      else mkArray(size)
    }
  }

  // TODO ofByte
  // TODO ofChar
  // TODO ofShort
  // TODO ofFloat
  // TODO ofLong
  // TODO ofDouble

}