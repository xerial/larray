//--------------------------------------
//
// WrappedLBufferTest.scala
// Since: 2013/12/11 23:07
//
//--------------------------------------

package xerial.larray.buffer

import xerial.larray.LArraySpec

/**
 * @author Taro L. Saito
 */
class WrappedLBufferTest extends LArraySpec {

  "WrappedLBuffer" should {

    "be a subrange of LBuffer" in {
      val l = new LBuffer(10)
      for(i <- 0 until l.size().toInt) {
        l(i) = (10 - i).toByte
      }

      debug(l.toArray.mkString(", "))
      val v = l.view(3, 8)

      debug(v.toArray.mkString(", "))
      v.size() shouldBe 8 - 3
      v.toArray.zipWithIndex.forall{case (a, i) => a == l(i+3)}
    }

  }
}