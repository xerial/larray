//--------------------------------------
//
// LArrayInputStreamTest.scala
// Since: 2013/03/21 23:38
//
//--------------------------------------

package xerial.larray

import xerial.core.io.IOUtil


/**
 * @author Taro L. Saito
 */
class LArrayInputStreamTest extends LArraySpec {

  "LArrayInputStream" should {
    "be created from LArray[A]" in {
      val l = LArray(1, 3, 4, 5)
      debug(s"input ${l.mkString(", ")}")
      val in = LArrayInputStream(l)
      IOUtil.readFully(in) { buf =>
        debug(s"buf length: ${buf.length}")
        val out = new LArrayOutputStream[Int]
        out.write(buf)
        val r = out.result
        debug(s"output ${r.mkString(", ")}")
        l.sameElements(r) should be (true)
      }
    }


  }
}