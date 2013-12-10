/*--------------------------------------------------------------------------
 *  Copyright 2013 Taro L. Saito
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *--------------------------------------------------------------------------*/
//--------------------------------------
//
// JSnappyCompressTest.scala
// Since: 2013/03/27 16:17
//
//--------------------------------------

package xerial.larray.mmap


import java.io.File
import xerial.larray.{LIntArray, RawByteArray, LArray}
import xerial.larray.buffer.LArraySpec
import org.xerial.snappy.Snappy

/**
 * @author Taro L. Saito
 */
class SnappyCompressTest extends LArraySpec {

  implicit class AsRaw[A](l:LArray[A]) {
    def address = l.asInstanceOf[RawByteArray[Int]].address
  }

  "Snappy" should {

    import xerial.larray._

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

    "compress LIntArray" taggedAs("it") in {
      val N = 100000000L
      val l = new LIntArray(N)
      debug(f"preparing data set. N=$N%,d")
      for(i <- 0 Until N) l(i) =  math.toDegrees(math.sin(i/360)).toInt

      debug("compressing the data")
      val maxLen = Snappy.maxCompressedLength(l.byteLength.toInt)
      val compressedBuf = LArray.of[Byte](maxLen)
      val compressedLen = Snappy.rawCompress(l.address, l.byteLength, compressedBuf.address)
      val compressed = compressedBuf.slice(0, compressedLen)
      val f = File.createTempFile("snappy", ".dat", new File("target"))
      f.deleteOnExit()
      compressed.saveTo(f)

      debug("decompressing the data")
      val b = MMap.open(f, 0, f.length, MMapMode.READ_ONLY)
      val len = Snappy.uncompressedLength(b.address, b.length)
      val decompressed = new LIntArray(len / 4)
      Snappy.rawUncompress(b.address, b.length, decompressed.address)
      b.close

      debug(f"l.length:${l.length}%,d, decompressed.length:${decompressed.length}%,d")

      l.sameElements(decompressed) should be (true)
      info("start bench")
      time("iterate", repeat=10) {
        block("new array") {
          var sum = 0L
          var i = 0
          while(i < l.length) {
            sum += l(i)
            i += 1
          }
        }

        block("decompressed") {
          var sum = 0L
          var i = 0
          while(i < decompressed.length) {
            sum += decompressed(i)
            i += 1
          }
        }
      }
    }
  }
}