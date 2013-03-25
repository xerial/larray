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

    "set bits" in {
      val N = 100
      val b = new LBitArray(N)
      b.size should be (N)
      debug(b)
      b.clear
      debug(b)
      b.forall(_ == false) should be (true)
      for(i <- 0L until b.length) {
        b.on(i)
      }
      b.forall(_ == true) should be (true)
    }

    "on and off specific bits" in {
      val b = new LBitArray(10000)
      b.fill
      b.forall(_ == true) should be (true)

      for(pos <- Seq(91, 34, 5093, 443, 4)) {
        b.off(pos)
        b(pos) should be (false)
      }
    }

  }
}