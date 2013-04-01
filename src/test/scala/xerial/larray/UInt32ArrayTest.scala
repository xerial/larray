//--------------------------------------
//
// UInt32ArrayTest.scala
// Since: 2013/03/18 4:08 PM
//
//--------------------------------------

package xerial.larray

/**
 * @author Taro L. Saito
 */
class UInt32ArrayTest extends LArraySpec {

  "UInt32Array" should {

    "record values larger than 2G" in {

      val u = new UInt32Array(10)
      val v : Long = Int.MaxValue.toLong + 10L
      u(0) = v
      u(0) should be (v)

      u(1) = 3141341
      u(1) should be (3141341L)

      val v2 = Integer.MAX_VALUE.toLong * 2L
      u(2) = v2
      u(2) should be (v2)
    }

    "have builder" in {
      val b = UInt32Array.newBuilder
      b += 1
      b += Int.MaxValue.toLong + 3L
      b += 4
      val u = b.result

      u.size should be(3)
      u(0) should be (1L)
      u(1) should be (Int.MaxValue.toLong + 3L)
      u(2) should be (4L)

    }

  }

}