//--------------------------------------
//
// LArrayOutputStream.scala
// Since: 2013/03/21 5:54 PM
//
//--------------------------------------

package xerial.larray

import java.io.OutputStream
import reflect.ClassTag

/**
 * Create LArray using `java.io.OutputStream` interface
 *
 * @author Taro L. Saito
 */
class LArrayOutputStream[A : ClassTag] extends OutputStream {

  private val buf = new LByteArrayBuilder

  def write(v: Int) {
    buf += v.toByte
  }

  override def write(b: Array[Byte], off: Int, len: Int) {
    buf.append(b, off, len)
  }

  def result : LArray[A] = {
    val arr = buf.result.asInstanceOf[LByteArray]
    LArray.wrap[A](arr.size, arr.m)
  }

}