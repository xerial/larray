//--------------------------------------
//
// RawArrayTest.scala
// Since: 2013/12/03 12:02 PM
//
//--------------------------------------

package xerial.larray.core

import org.scalatest._
import xerial.core.io.Resource
import xerial.core.util.Timer
import xerial.core.log.Logger

trait LArraySpec extends WordSpec with ShouldMatchers with MustMatchers with GivenWhenThen with OptionValues with Resource with Timer with Logger
with BeforeAndAfterAll with BeforeAndAfter with BeforeAndAfterEach {
}

/**
 * @author Taro L. Saito
 */
class RawArrayTest extends LArraySpec {
  "RawArray" should {

    "allocate memory" in {
      val m = new RawArray(1000)
      m.putInt(0, 0)
      m.putInt(4, 1)
      m.putInt(8, 130)

      m.release()
    }


    "allocate concurrently" in {

      val N = 1000

      time("concurrent allcation") {
        for(i <- (0 until N).par) yield {
          val m = new RawArray(8192)
        }
      }
    }

  }
}