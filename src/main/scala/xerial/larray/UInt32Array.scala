//--------------------------------------
//
// UInt32Array.scala
// Since: 2013/03/18 3:57 PM
//
//--------------------------------------

package xerial.larray

/**
 * Array of uint32 values. The internal array representation is the same with LIntArray, but the apply and update methods are based on Long type values.
 *
 * @author Taro L. Saito
 */
class UInt32Array(val size: Long, private[larray] val m:Memory)(implicit mem: MemoryAllocator) extends LArray[Long] with UnsafeArray[Long] {
  def this(size:Long)(implicit mem: MemoryAllocator) = this(size, mem.allocate(size << 2))

  import UnsafeUtil.unsafe

  // TODO Extend LArrayBuilder type
  protected[this] def newBuilder: LBuilder[Long, UInt32Array] = throw new UnsupportedOperationException("Uint32Array.newBuilder")

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


}
