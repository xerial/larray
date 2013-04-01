//--------------------------------------
//
// GenomeWeaverSpecverSpec.scala
// Since: 2013/03/01 10:55
//
//--------------------------------------

package xerial.larray

import org.scalatest.matchers.{MustMatchers, ShouldMatchers}
import xerial.core.util.Timer
import org.scalatest._
import java.io.{PrintStream, ByteArrayOutputStream}
import scala.language.implicitConversions
import xerial.core.io.Resource


/**
 * @author leo
 */
trait LArraySpec extends WordSpec with ShouldMatchers with MustMatchers with GivenWhenThen with OptionValues with Resource with Timer with xerial.core.log.Logger
 with BeforeAndAfterAll with BeforeAndAfter with BeforeAndAfterEach {

  implicit def toTag(t:String) = Tag(t)

  /**
   * Captures the output stream and returns the printed messages as a String
   * @param body
   * @tparam U
   * @return
   */
  def captureOut[U](body: => U) : String = {
    val out = new ByteArrayOutputStream
    Console.withOut(out) {
      body
    }
    new String(out.toByteArray)
  }

  /**
   * Captures the output stream and returns the printed messages as a String
   * @param body
   * @tparam U
   * @return
   */
  def captureSystemOut[U](body: => U) : String = {
    val prev = System.out
    val b = new ByteArrayOutputStream
    val out = new PrintStream(b)
    try {
      System.setOut(out)
      body
      out.flush()
    }
    finally
      System.setOut(prev)
    new String(b.toByteArray)
  }

  def captureErr[U](body: => U) : String = {
    val out = new ByteArrayOutputStream
    Console.withErr(out) {
      body
    }
    new String(out.toByteArray)
  }


}
