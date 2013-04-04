//--------------------------------------
//
// LBitArray.scala
// Since: 2012/06/19 3:42 PM
//
//--------------------------------------

package xerial.larray

import xerial.core.log.Logger
import java.nio.ByteBuffer
import java.io.{FileOutputStream, File}
import sun.nio.ch.DirectBuffer


/**
 * Helper methods for packing bit sequences into Array[Long]
 * @author leo
 */
object BitEncoder {

  def minArraySize(numBits: Long): Long = {
    val blockBitSize: Long = 64L
    val arraySize: Long = (numBits + blockBitSize - 1L) / blockBitSize
    arraySize
  }

  @inline def blockAddr(pos: Long): Long = blockIndex(pos) << 3
  @inline def blockIndex(pos: Long): Long = (pos >>> 6)
  @inline def blockOffset(pos: Long): Int = (pos & 0x3FL).toInt

  val table = Array(false, true)
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


trait LBitArrayOps {

  /**
   * Count the number of bits within the specified range [start, end)
   * @param checkTrue count true or false
   * @param start
   * @param end
   * @return the number of occurrences
   */
  def count(checkTrue: Boolean, start: Long, end: Long): Long

  /**
   * Extract a slice of the sequence [start, end)
   * @param start
   * @param end
   * @return
   */
  def slice(start: Long, end: Long): LBitArray

}

/**
 *
 * Specialized implementaiton of LArray[Boolean] using LArray[Long]
 * To generate an instance of LBitArray, use ``LBitArray.newBuilder(Long)`` or [[xerial.larray.LBitArray#apply]]
 *
 * @param seq raw bit string
 * @param numBits
 */
class LBitArray(private[larray] val seq: LLongArray, private val numBits: Long) extends LArray[Boolean] with UnsafeArray[Boolean] with LBitArrayOps {

  self =>

  import UnsafeUtil.unsafe

  import BitEncoder._

  private[larray] def alloc = seq.alloc

  def this(numBits:Long) = this(new LLongArray(BitEncoder.minArraySize(numBits)), numBits)
  def this(numBits:Long, m:Memory) = this(new LLongArray(BitEncoder.minArraySize(numBits), m), numBits)

  def size = numBits
  private var hash: Int = 0

  override def byteLength = seq.byteLength

  protected[this] def newBuilder: LBuilder[Boolean, LBitArray] = new LBitArrayBuilder()
  private[larray] def m: Memory = seq.m


  override def toString = {
    val displaySize = math.min(500L, numBits)
    val b = new StringBuilder
    for(i <- 0L until displaySize)
      b.append(if(self(i)) "1" else "0")
    b.result()
  }

  def on(index:Long) = update(index, true)
  def off(index:Long) = update(index, false)

  /**
   * Set all bits to 1
   * @return
   */
  def fill {
    unsafe.setMemory(seq.m.address, byteLength, 0xFF.toByte)
  }


  /**
   * Return the DNA base at the specified index
   * @param index
   * @return
   */
  def apply(index: Long): Boolean = {
    val addr = blockAddr(index)
    val offset = blockOffset(index)
    val code = ((seq.getLong(addr) >>> offset) & 1L).toInt
    table(code)
  }

  def update(index:Long, v:Boolean) : Boolean = {
    val addr = blockAddr(index)
    val offset = blockOffset(index)
    if(v)
      seq.putLong(addr, seq.getLong(addr) | (1L << offset))
    else
      seq.putLong(addr, seq.getLong(addr) & (~(1L << offset)))
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
      // Applying bit mask changes all bits to 0s, so we need to subtract num0s
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
    newSeq.clear

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

  def view(from: Long, to: Long) = new LArrayView.LBitArrayView(this, from, to-from)

  /**
   * Save to a file.
   * @param f
   * @return
   */
  override def saveTo(f:File) : File = {
    val fout = new FileOutputStream(f).getChannel
    try {
      // LBitArray need to record numBits
      val b = new Array[Byte](8)
      val bb = ByteBuffer.wrap(b).putLong(numBits)
      bb.flip()
      fout.write(bb)
      fout.write(this.toDirectByteBuffer)
      f
    }
    finally
      fout.close
  }


}


/**
 * BitVector builder
 */
class LBitArrayBuilder extends LBuilder[Boolean, LBitArray] with Logger
{
  import BitEncoder._

  def elementSize = throw new UnsupportedOperationException("elementSize of LBitArrayBuilder")

  private var elems : LLongArray = _
  private var capacity : Long = 0L
  private var numBits: Long = 0L

  protected def mkArray(size:Long) : LLongArray = {
    val newArray = new LLongArray(minArraySize(size))
    newArray.clear()
    if(capacity > 0L) {
      LArray.copy(elems, newArray)
      elems.free
    }
    newArray
  }

  def sizeHint(size:Long) {
    if(capacity < size) resize(size)
  }

  protected def ensureSize(size:Long) {
    val factor = 2L
    if(capacity < size || capacity == 0L){
      var newsize = if(capacity <= 1L) 64L else capacity * factor
      while(newsize < size) newsize *= factor
      resize(newsize)
    }
  }

  protected def resize(size:Long) {
    elems = mkArray(size)
    capacity = size
  }

  def clear() {
    if(numBits > 0)
      elems.free
    capacity = 0L
    numBits = 0L
  }

  /**
   * Append a bit value
   * @param v
   */
  override def +=(v: Boolean) : this.type = {
    ensureSize(numBits + 1)
    val pos = blockIndex(numBits)
    val offset = blockOffset(numBits)
    if(v)
      elems(pos) |= (1L << offset)
    else
      elems(pos) &= ~(1L << offset)
    numBits += 1
    this
  }

  def result() : LBitArray = {
    val s = minArraySize(numBits)
    if(capacity != 0L && capacity == s)
      new LBitArray(elems, numBits)
    else
      new LBitArray(mkArray(numBits), numBits)
  }

  def result(numBits:Long) : LBitArray = {
    this.numBits = numBits
    result()
  }

  override def toString = result.toString

  def append(seq: LSeq[Boolean]) = {
    val N = seq.length
    ensureSize(numBits + N)
    var i = 0L
    while(i < N) {
      +=(seq(i))
      i += 1
    }
    numBits += N
    this
  }


  def write(src: ByteBuffer) = throw new UnsupportedOperationException("write(ByteBuffer)")

  def isOpen = true

  def close() {}
}
