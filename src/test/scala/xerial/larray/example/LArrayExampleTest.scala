//--------------------------------------
//
// LArrayExampleTest.scala
// Since: 2013/03/26 15:04
//
//--------------------------------------

package xerial.larray.example

import xerial.larray.LArraySpec


/**
 * @author Taro L. Saito
 */
class LArrayExampleTest extends LArraySpec {

  "LArrayExample" should {
    "run Scala API" in {
      val out = captureOut(new LArrayExample)
      out should (include ("done."))
    }

    "run Java API" in {
      val out = captureSystemOut(LArrayJavaExample.main(Array.empty[String]))
      out should (include ("done."))
    }

  }
}