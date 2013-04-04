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

      val mOffset = new MappedLByteArray(f, 3, 1000 - 3)
      m.slice(3).sameElements(mOffset) should be (true)

      mOffset.flush
      m.free
      m2.free
    }

    "create large memory mapped file more than 2GB" in {

      val f = File.createTempFile("mmap", ".larray", new File("target"))
      f.deleteOnExit()

      val G = 1024L * 1024 * 1024
      val m = new MappedLByteArray(f, 0, 2L * G + 1024)
      val offset = 100

      val v = 34.toByte
      m(2L * G + offset) = v
      m.close()

      f.length() shouldBe m.size

      val view = new MappedLByteArray(f, 2L * G, 1024)
      view(offset) shouldBe v
      view.close()

      f.delete()
    }

  }
}