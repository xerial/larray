//--------------------------------------
//
// LArrayBuilderTest.scala
// Since: 2013/03/19 10:37 AM
//
//--------------------------------------

package xerial.larray

import scala.util.Random

/**
 * @author Taro L. Saito
 */
class LArrayBuilderTest extends LArraySpec {

  "LArrayBuilder" should {

    "build LArray" in {

      val b = LArray.newBuilder[Int]

      def elem(i:Long) = math.toDegrees(math.sin(i / 15.0)).toInt

      for(i <- 0L until 100L)
        b += elem(i)

      val l = b.result
      debug(l.mkString(", "))
      l.size should be (100)
      l.zipWithIndex.forall {case (v, i) => v == elem(i) } should be (true)
    }

    "build large LArray" in {

      val b = LArray.newBuilder[Byte]
      val N = 3L * 1024 * 1024
      debug("Creating large array")
      val r = new Random(0)
      var i = 0L
      while(i < N) {
        b += r.nextInt(255).toByte
        i += 1
      }
      val l = b.result
      l.size should be (N)

      debug("Checking the elements")
      val r2 = new Random(0)
      l.forall(v => v == r2.nextInt(255).toByte) should be (true)
    }


  }
}