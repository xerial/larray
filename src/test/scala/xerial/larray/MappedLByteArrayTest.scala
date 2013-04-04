//--------------------------------------
//
// MappedLByteArrayTest.scala
// Since: 2013/04/04 1:58 PM
//
//--------------------------------------

package xerial.larray

import java.io.File

/**
 * @author Taro L. Saito
 */
class MappedLByteArrayTest extends LArraySpec {

  "MappedLByteArray" should {

    "create memory mapped file" in {

      val f = File.createTempFile("mmap", ".larray", new File("target"))
      f.deleteOnExit()

      val m = new MappedLByteArray(f, 0, 1000)
      for(i <- 0 Until m.size)
        m(i) = i.toByte

      m.flush

      trace(m.mkString(", "))

      val m2 = LArray.loadFrom[Byte](f)
      m.sameElements(m2) should be (true)

    }

  }
}