//--------------------------------------
//
// LArrayInputStream.scala
// Since: 2013/03/21 5:28 PM
//
//--------------------------------------

package xerial.larray

import java.io.InputStream


object LArrayInputStream {

  /**
   * Create a new InputStream from a given LArray
   * @param array input
   * @tparam A element type
   * @return input stream
   */
  def apply[A](array:LArray[A]) : InputStream = {
    array match {
      case r:RawByteArray[A] => new RawLArrayInputStream[A](r)
      case _ => sys.error(s"cannot create InputStream from this LArray class:${array.getClass}")
    }
  }

}


/**
 * InputStream implementation for LArrays that uses RawByteArray internally.
 *
 * @author Taro L. Saito
 */
private[larray] class RawLArrayInputStream[A](array:RawByteArray[A]) extends InputStream {

  private var cursor = 0L
  private var mark = 0L

  def read() = {
    val v = array.readByte(cursor)
    cursor += 1
    v
  }

  override def read(b: Array[Byte], offset:Int, len:Int) : Int = {
    val readLen = math.min(len, array.size - cursor).toInt
    array.write(cursor, b, offset, readLen)
    cursor += readLen
    readLen
  }


  override def available = {
    val remaining = array.size - cursor
    math.min(Integer.MAX_VALUE, remaining).toInt
  }

  override def mark(readlimit: Int) {
    // read limit can be ignored since all data is in memory
    mark = cursor
  }

  override def reset() {
    cursor = mark
  }

  override def markSupported() = {
    true
  }
}