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
      new LArrayExample
    }

    "run Java API" in {
      LArrayJavaExample.main(Array.empty[String])
    }

  }
}