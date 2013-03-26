//--------------------------------------
//
// LArrayFunctionTest.scala
// Since: 2013/03/26 15:41
//
//--------------------------------------

package xerial.larray

import reflect.ClassTag

/**
 * @author Taro L. Saito
 */
class LArrayFunctionTest extends LArraySpec {

  def test[A:ClassTag, B:ClassTag](input:Array[A], answer:Array[A] => Seq[B], target:LArray[A] => LSeq[B]) : Boolean = {
    val l = input.toLArray
    val o1 = answer(input)
    val o2 = target(l)
    (o1, o2) match {
      case (oi:Iterable[B], o2:LIterable[B]) => oi.toLArray.sameElements(o2)
      case _ => warn(s"cannot compare the result: $o1 and $o2"); false
    }
  }


  implicit class LArrayMatcher[A:ClassTag](left:LSeq[A]) {
    def shouldBe[A:ClassTag](answer:Seq[A]) {
      left.mkString(", ") should be (answer.mkString(", "))
    }
  }


  "LArray" should {

    trait Input1 {
      val in = Array(0, 1, 2, 3, 4)
      val l = in.toLArray
    }

    "have slice" in new Input1 {
      l.slice(1, 3) shouldBe in.slice(1, 3)
      l.slice(2) shouldBe in.slice(2, in.length)

      l.map(_*2) shouldBe in.map(_*2)

    }


  }
}