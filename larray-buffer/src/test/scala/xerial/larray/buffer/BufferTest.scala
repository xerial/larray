//--------------------------------------
//
// RawByteArrayTestTest.scala
// Since: 2013/12/03 12:02 PM
//
//--------------------------------------

package xerial.larray.buffer

import org.scalatest._
import xerial.core.io.Resource
import xerial.core.util.Timer
import xerial.core.log.Logger
import java.nio.ByteBuffer

trait LArraySpec extends WordSpec with ShouldMatchers with MustMatchers with GivenWhenThen with OptionValues with Resource with Timer with Logger
with BeforeAndAfterAll with BeforeAndAfter with BeforeAndAfterEach {

  implicit def toTag(t:String) = Tag(t)

}

/**
 * @author Taro L. Saito
 */
class BufferTest extends LArraySpec {

  implicit class RichArray(m:Buffer) {
    def toCSV = m.toArray.mkString(", ")
  }


  "Buffer" should {

    "allocate memory" in {
      val m = new Buffer(1000)
      m.putInt(0, 0)
      m.putInt(4, 1)
      m.putInt(8, 130)

      m.getInt(0) shouldBe 0
      m.getInt(4) shouldBe 1
      m.getInt(8) shouldBe 130

      m.size() shouldBe 1000

      (0 until m.size()).foreach(i => m.putByte(i, (i % 128).toByte))
      (0 until m.size()).forall(i => m.getByte(i) == (i % 128).toByte) should be (true)

      m.clear()

      (0 until 1000).forall(i => m.getByte(i) == 0) should be (true)

      m.release()
    }

    "convert to array" in {
      val m = new Buffer(12);
      for(i <- 0 until m.size)
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