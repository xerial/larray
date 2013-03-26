//--------------------------------------
//
// UInt32Array.scala
// Since: 2013/03/18 3:57 PM
//
//--------------------------------------

package xerial.larray

object UInt32Array {

  def newBuilder = new LArrayBuilder[Long, UInt32Array] {
    def +=(v: Long): this.type = {
      ensureSize(byteSize + 4)
      elems.putInt(byteSize,  (v & 0xFFFFFFFFL).toInt)
      byteSize += 4
      this
    }

    /** Produces a collection from the added elements.
      * The builder's contents are undefined after this operation.
      * @return a collection containing the elements added to this builder.
      */
    def result(): UInt32Array = {
      if(capacity != 0L && capacity == byteSize) new UInt32Array(byteSize / 4, elems.m)
      else new UInt32Array(byteSize / 4, mkArray(byteSize).m)
    }
  }



}


private[larray] class UInt32ArrayView(base:UInt32Array, offset:Long, val size:Long) extends LArrayView[Long] {
  protected[this] def newBuilder: LBuilder[Long, UInt32Array] = UInt32Array.newBuilder
  def apply(i: Long) = base.apply(offset + i)
  private[larray] def elementByteSize = base.elementByteSize
  def copyTo(dst: LByteArray, dstOffset: Long) { base.copyTo(offset, dst, dstOffset, byteLength) }
  def copyTo(srcOffset: Long, dst: LByteArray, dstOffset: Long, blen: Long) { base.copyTo(offset+srcOffset, dst, dstOffset, blen) }
}

/**
 * Array of uint32 values. The internal array representation is the same with LIntArray, but the apply and update methods are based on Long type values.
 *
 * @author Taro L. Saito
 */
class UInt32Array(val size: Long, private[larray] val m:Memory)(implicit mem: MemoryAllocator) extends LArray[Long] with UnsafeArray[Long] { self =>
  def this(size:Long)(implicit mem: MemoryAllocator) = this(size, mem.allocate(size << 2))

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


