//--------------------------------------
//
// RawByteArrayTestTest.scala
// Since: 2013/12/03 12:02 PM
//
//--------------------------------------

package xerial.larray.buffer

import java.nio.ByteBuffer
import xerial.larray.LArraySpec


/**
 * @author Taro L. Saito
 */
class BufferTest extends LArraySpec {

  implicit class RichArray(m:Buffer) {
    def toCSV = m.toArray.mkString(", ")
  }


  "Buffer" should {

    "allocate memory" in {
      val size = 1000
      val m = new Buffer(size)
      m.putInt(0, 0)
      m.putInt(4, 1)
      m.putInt(8, 130)

      m.getInt(0) shouldBe 0
      m.getInt(4) shouldBe 1
      m.getInt(8) shouldBe 130

      m.size() shouldBe size.toLong

      (0 until size).foreach(i => m.putByte(i, (i % 128).toByte))
      (0 until size).forall(i => m.getByte(i) == (i % 128).toByte) should be (true)

      m.clear()

      (0 until size).forall(i => m.getByte(i) == 0) should be (true)

      m.release()
    }

    "convert to array" in {
      val size = 12
      val m = new Buffer(size);
      for(i <- 0 until size)
        m(i) = i.toByte
      debug(m.toCSV)

      m.clear()
      debug(m.toCSV)
    }


    "allocate concurrently" in {

      val N = 100
      def range = (0 until N).par
      val R = 2
      val S = 1024 * 1024

      info("start buffer allocation test")

      time("concurrent allocation", repeat=10) {
        block("without zero-filling", repeat=R) {
          for(i <- range) yield {
            new Buffer(S)
          }
        }

        block("with zero-filling", repeat=R) {
          for(i <- range) yield {
            val m = new Buffer(S)
            m.clear()
            m
          }
        }

        block("java array", repeat=R) {
          for(i <- range) yield {
            new Array[Byte](S)
          }
        }

        block("byte buffer", repeat=R) {
          for(i <- range) yield {
            ByteBuffer.allocate(S)
          }
        }

        block("direct byte buffer", repeat=R) {
          for(i <- range) yield {
            ByteBuffer.allocateDirect(S)
          }
        }

      }
    }

  }
}