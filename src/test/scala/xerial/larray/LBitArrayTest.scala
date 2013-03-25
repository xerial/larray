//--------------------------------------
//
// LBitArrayTest.scala
// Since: 2013/03/25 12:33
//
//--------------------------------------

package xerial.larray

/**
 * @author Taro L. Saito
 */
class LBitArrayTest extends LArraySpec {
  "LBitArray" should {

    "have consturctor" in {
      val b = new LBitArray(6)
      b.size should be (6)

      b.on(1)
      b.toString should be ("010000")

      b.on(5)
      b.toString should be ("010001")
    }

  }
}