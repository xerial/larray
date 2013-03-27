//--------------------------------------
//
// SnappyCompressTest.scala
// Since: 2013/03/27 16:17
//
//--------------------------------------

package xerial.larray

import org.xerial.snappy.Snappy
import scala.util.Random

/**
 * @author Taro L. Saito
 */
class SnappyCompressTest extends LArraySpec {

  implicit class AsRaw[A](l:LArray[A]) {
    def address = l.asInstanceOf[RawByteArray[Int]].address
  }

  "Snappy" should {

    "compress LArray" in {
      val l = (for (i <- 0 until 3000) yield math.toDegrees(math.sin(i/360)).toInt).toLArray
      val maxLen = Snappy.maxCompressedLength(l.byteLength.toInt)
      val compressedBuf = LArray.of[Byte](maxLen)
      val compressedLen = Snappy.rawCompress(l.address, l.byteLength, compressedBuf.address)

      val compressed = compressedBuf.slice(0, compressedLen)
      val uncompressedLength = Snappy.uncompressedLength(compressed.address, compressed.byteLength)
      val uncompressed = LArray.of[Int](uncompressedLength / 4)
      Snappy.rawUncompress(compressed.address, compressed.byteLength, uncompressed.address)

      debug(s"byteLength:${l.byteLength}, max compressed length:$maxLen ,compressed length:$compressedLen")
      l.sameElements(uncompressed) should be (true)
    }
  }
}