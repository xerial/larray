//--------------------------------------
//
// LArrayOutputStream.scala
// Since: 2013/03/21 5:54 PM
//
//--------------------------------------

package xerial.larray

import java.io.OutputStream

object LArrayOutputStream {

  def apply[A](array:LArray[A]) : OutputStream = {
    array match {
      case r:RawByteArray[A] => new RawLArrayOutputStream[A](r)
      case _ => sys.error(s"cannot create OutputStream from this LArray class:${array.getClass}}")
    }
  }
}

/**
 * @author Taro L. Saito
 */
private[larray] class RawLArrayOutputStream[A](array:RawByteArray[A]) extends OutputStream {

  def write(b: Int) {
    // TODO impl
  }
}