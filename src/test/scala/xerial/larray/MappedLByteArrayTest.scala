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
      m.size shouldBe 1000L
      m.size shouldBe 1000L
      for(i <- 0 Until m.size) {
        m(i) = i.toByte
      }

      m.flush

      trace(m.mkString(", "))

      val m2 = LArray.loadFrom[Byte](f)
      m.sameElements(m2) should be (true)

    }

    "create large memory mapped file more than 2GB" in {
      pending
      val f = File.createTempFile("mmap", ".larray", new File("target"))
      f.deleteOnExit()

      val G = 1024L * 1024 * 1024
      val m = new MappedLByteArray(f, 0, 2L * G + 1024)
      val offset = 100
      m(2L * G + offset) = 34.toByte
      m.close()

      val view = new MappedLByteArray(f, 2L * G, 1024)
      view(offset) shouldBe 34.toByte
      view.close()
    }

  }
}