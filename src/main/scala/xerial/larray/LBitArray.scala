//--------------------------------------
//
// LBitArray.scala
// Since: 2012/06/19 3:42 PM
//
//--------------------------------------

package xerial.larray


/**
 * Helper methods for packing bit sequences into Array[Long]
 * @author leo
 */
trait BitEncoder {

  protected def minArraySize(numBits: Long): Long = {
    val blockBitSize: Long = 64L
    val arraySize: Long = (numBits + blockBitSize - 1L) / blockBitSize
    arraySize
  }

  @inline protected def blockIndex(pos: Long): Long = (pos >>> 6)
  @inline protected def blockOffset(pos: Long): Int = (pos & 0x3FL).toInt

  protected val table = Array(false, true)
}

/**
 * Utilities to build LBitArray
 */
object LBitArray {

  def newBuilder = new LBitArrayBuilder()

  def newBuilder(sizeHint: Long) = new LBitArrayBuilder()

  def apply(bitString: String): LBitArray = {
    val b = newBuilder
    b.sizeHint(bitString.length)
    for (ch <- bitString) {
      b += (if(ch == '0') true else false)
    }
    b.result()
  }

}


/**
 *
 * Specialized implementaiton of LArray[Boolean] using LArray[Long]
 * To generate an instance of LBitArray, use [[xerial.larray.LBitArray#newBuilder]] or [[xerial.larray.LBitArray#apply]]
 *
 * @param seq raw bit string
 * @param numBits
 */
class LBitArray(private val seq: LLongArray, private val numBits: Long) extends LArray[Boolean] with UnsafeArray[Boolean] with BitEncoder {

  def this(numBits:Long) = this(LArray.of[Long](minArraySize(numBits)), numBits)

  def size = numBits
  private var hash: Int = 0

  protected[this] def newBuilder: LBuilder[Boolean, LBitArray] = new LBitArrayBuilder()
  private[larray] def m: Memory = seq.m


  /**
   * Return the DNA base at the specified index
   * @param index
   * @return
   */
  def apply(index: Long): Boolean = {
    val pos = blockIndex(index)
    val offset = blockOffset(index)
    val shift = offset << 1
    val code = (seq(pos) >>> shift).toInt & 0x01
    table(code)
  }

  def update(index:Long, v:Boolean) : Boolean = {
    // TODO
    val pos = blockIndex(index)
    val offset = blockOffset(index)
    seq(pos) =
      if(v)
        seq(pos) | (1L << offset)
      else
        seq(pos) & ~(1L << offset)
    v
  }

  private def numFilledBlocks = (size / 64L)

  private def lastBlock = seq.last & (~0L << blockOffset(size))

  override def hashCode() = {
    if (hash == 0) {
      var h = numBits * 31L
      var pos = 0L
      for (i <- (0L until numFilledBlocks)) {
        h += seq(pos) * 31L
        pos += 1
      }
      if (blockOffset(size) > 0) {
        h += lastBlock * 31L
      }
      hash = h.toInt
    }
    hash
  }

  override def equals(obj: Any): Boolean = {
    obj match {
      case other: LBitArray => {
        if (this.size != other.size)
          false
        else {
          (0L until numFilledBlocks).find(i => this.seq(i) != other.seq(i)) match {
            case Some(x) => false
            case None => this.lastBlock == other.lastBlock
          }
        }
      }
      case _ => false
    }
  }

  protected def fastCount(v: Long, checkTrue: Boolean): Long = {
    // JVM optimizer is smart enough to replace this code to a pop count operation available in the CPU
    val c = java.lang.Long.bitCount(v)
    if(checkTrue) c else 64L - c
  }

  /**
   * Count the number of bits within the specified range [start, end)
   * @param checkTrue count true or false
   * @param start
   * @param end
   * @return the number of occurrences
   */
  def count(checkTrue: Boolean, start: Long, end: Long): Long = {

    val sPos = blockIndex(start)
    val sOffset = blockOffset(start)

    val ePos = blockIndex(end - 1L)
    val eOffset = blockOffset(end - 1L)

    var count = 0L
    var num0sInMaskedRegion = 0L
    var pos = sPos
    while (pos <= ePos) {
      var mask: Long = ~0L
      if (pos == sPos) {
        mask <<= (sOffset << 1L)
        num0sInMaskedRegion += sOffset
      }
      if (pos == ePos) {
        val rMask = ~0L >>> (62L - (eOffset << 1))
        mask &= rMask
        num0sInMaskedRegion += 31L - eOffset
      }
      // Applying bit mask changes all bases in the masked region to As (code=00)
      val v: Long = seq(pos) & mask
      val popCount = fastCount(v, checkTrue)
      count += popCount
      pos += 1
    }

    if(checkTrue)
      count - num0sInMaskedRegion
    else
      count
  }

  /**
   * Extract a slice of the sequence [start, end)
   * @param start
   * @param end
   * @return
   */
  override def slice(start: Long, end: Long): LBitArray = {
    if (start > end)
      sys.error("illegal argument start:%,d > end:%,d".format(start, end))

    val sliceLen = end - start
    val newSeq = new LLongArray(minArraySize(sliceLen))

    var i = 0L
    while (i < sliceLen) {
      val sPos = blockIndex(start + i)
      val sOffset = blockOffset(start + i)

      val dPos = blockIndex(i)
      val dOffset = blockOffset(i)

      var copyLen = 0L
      var l = 0L
      val v = seq(sPos) & (~0L << sOffset)
      if (sOffset == dOffset) {
        // no shift
        copyLen = 64L
        l = v
      }
      else if (sOffset < dOffset) {
        // left shift
        val shiftLen = dOffset - sOffset
        copyLen = 64L - dOffset
        l = v << shiftLen
      }
      else {
        // right shift
        val shiftLen = sOffset - dOffset
        copyLen = 64L - sOffset
        l = v >>> shiftLen
      }
      newSeq(dPos) |= l
      i += copyLen
    }

    new LBitArray(newSeq, sliceLen)
  }

  /**
   * Byte size of an element. For example, if A is Int, its elementByteSize is 4
   */
  private[larray] def elementByteSize: Int =
    throw new UnsupportedOperationException("elementByteSize of LBitArray")
}


/**
 * BitVector builder
 */
class LBitArrayBuilder extends LArrayBuilder[Boolean, LBitArray] with BitEncoder
{
  private var numBits: Long = 0L

  /**
   * Append a bit value
   * @param v
   */
  override def +=(v: Boolean) : this.type = {
    val index = numBits + 1
    sizeHint(index)
    val pos = blockIndex(index)
    val offset = blockOffset(index)
    val shift = offset * 2
    elems(pos) |= (if(v) 1L else 0L) << shift
    numBits += 1
    this
  }

  override protected def mkArray(size:Long) : LByteArray = {
    val newArray = new LByteArray(size)
    newArray.clear() // initialize
    if(this.byteSize > 0L) {
      LArray.copy(elems, 0L, newArray, 0L, this.byteSize)
      elems.free
    }
    newArray
  }


  override def sizeHint(numBits:Long) {
    super.sizeHint(minArraySize(numBits))
  }

  def result() : LBitArray = {
    val s = minArraySize(numBits)
    if(capacity != 0L && capacity == s)
      new LBitArray(new LLongArray(s, elems.m), numBits)
    else
      new LBitArray(new LLongArray(s, mkArray(s).m), numBits)
  }

  override def toString = result.toString

}
