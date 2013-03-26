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


  implicit class LIterableMatcher[A:ClassTag](left:LIterator[A]) {
    def ===[A:ClassTag](answer:Seq[A]) {
      val l = left.mkString(", ")
      val a = answer.mkString(", ")
      l should be (a)
    }
  }

  implicit class LArrayMatcher[A:ClassTag](left:LSeq[A]) {
    def ===[A:ClassTag](answer:Seq[A]) {
      val l = left.mkString(", ")
      val a = answer.mkString(", ")
      l should be (a)
    }
  }

  "LArray" should {

    /**
     * Sample input data
     */
    trait Input1 {
      val a = Seq(0, 1, 2, 3, 4)
      val l = a.toLArray
    }

    "have iterator" in new Input1 {
      l.iterator.mkString(", ") should be (a.mkString(", "))
    }

    "map elements" in new Input1 {
      l.map(_*2) === a.map(_*2)
    }

    "filter elements" in new Input1 {
      l.filter(_ % 2 == 1) === a.filter(_ % 2 == 1)
    }


    "have slice" in new Input1 {
      l.slice(1, 3) === a.slice(1, 3)
      l.slice(2) === a.slice(2, a.length)
    }


  }
}