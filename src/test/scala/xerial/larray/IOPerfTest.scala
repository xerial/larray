//--------------------------------------
//
// IOPerfTest.scala
// Since: 2013/03/26 23:27
//
//--------------------------------------

package xerial.larray

import java.io._
import scala.util.Random
import xerial.core.io.IOUtil

/**
 * @author Taro L. Saito
 */
class IOPerfTest extends LArraySpec {

  def createSampleFile : File = {
    val file = File.createTempFile("sample", ".larray", new File("target"))
    file.deleteOnExit()
    val b = new Array[Byte](1024 * 1024)
    //val P = 1024 * 1024
    val P = 256
    val f = new FileOutputStream(file)
    for(i <- 0 until P) {
      Random.nextBytes(b)
      f.write(b)
    }
    f.close
    file
  }

  "LArray" should {
    "compare I/O performance" in {

      val f1 = createSampleFile

      time("read", repeat=10) {
        block("LArray.loadFrom") {
          trace("Loading to LArray")
          val l = LArray.loadFrom[Byte](f1)
          l.free
        }
        block("FileOutputStream") {
          trace("Loading to Array")
          IOUtil.readFully(new BufferedInputStream(new FileInputStream(f1))) { buf =>
            // do nothing
          }
        }
      }

    }

  }
}